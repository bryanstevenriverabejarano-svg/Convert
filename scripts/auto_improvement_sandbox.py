"""Ejecuta Gradle en Docker local, sin montar el checkout ni credenciales."""

from __future__ import annotations

import json
from contextlib import contextmanager
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import tempfile
import uuid


TIMEOUT_SECONDS = 600
IMAGE_PATTERN = re.compile(r"(?:[a-z0-9][a-z0-9._:/-]*@)?sha256:[0-9a-f]{64}")

# Código fijo del ejecutor; ninguna parte proviene de la propuesta.
BUILD_SCRIPT = """\
mkdir -p /work/source /work/gradle-home /tmp/salve-home
cp -R /opt/salve/gradle-home/. /work/gradle-home/
cd /work/source
tar -xf /input/source.tar
exec /bin/sh ./gradlew testDebugUnitTest --offline --no-daemon \\
    --no-build-cache --rerun-tasks -Dorg.gradle.java.installations.auto-download=false
"""


class SandboxError(RuntimeError):
    pass


@contextmanager
def handle_stop_signal():
    # El supervisor usa el hilo principal. SIGTERM debe atravesar el finally
    # de limpieza, igual que Ctrl+C; SIGKILL no es interceptable.
    def stop(signum, frame):
        raise KeyboardInterrupt("Sandbox detenido por SIGTERM")
    previous = signal.signal(signal.SIGTERM, stop)
    try:
        yield
    finally:
        signal.signal(signal.SIGTERM, previous)


def configured_image() -> str:
    image = os.environ.get("SALVE_SANDBOX_IMAGE", "")
    if IMAGE_PATTERN.fullmatch(image) is None:
        raise SandboxError("Configura SALVE_SANDBOX_IMAGE con un ID sha256 o un digest fijo")
    if shutil.which("docker") is None:
        raise SandboxError("Docker no está disponible; Gradle no se ejecutará en el anfitrión")
    return image


def docker_prefix(config_dir: Path) -> list[str]:
    executable = shutil.which("docker")
    if executable is None:
        raise SandboxError("Docker no está disponible")
    endpoint = os.environ.get("SALVE_DOCKER_HOST", "unix:///var/run/docker.sock")
    if re.fullmatch(r"unix:///[A-Za-z0-9_./-]+", endpoint) is None:
        raise SandboxError("SALVE_DOCKER_HOST debe ser un socket Unix local")
    return [executable, "--config", str(config_dir), "--host", endpoint]


def container_command(prefix: list[str], archive: Path, image: str, name: str) -> list[str]:
    # Docker interpreta --mount como CSV; no aceptar un origen ambiguo.
    source = str(archive.resolve())
    if any(character in source for character in (",", "\n", "\r", '"')):
        raise SandboxError("Ruta de snapshot no compatible con el montaje de Docker")
    return prefix + [
        "run", "--rm", "--name", name, "--pull", "never",
        "--network", "none", "--read-only", "--user", "65534:65534",
        "--cap-drop", "ALL", "--security-opt", "no-new-privileges=true",
        "--pids-limit", "256", "--cpus", "2", "--memory", "6g", "--memory-swap", "6g",
        "--log-driver", "none", "--no-healthcheck", "--init",
        "--mount", f"type=bind,src={source},dst=/input/source.tar,readonly",
        "--tmpfs", "/work:rw,nosuid,nodev,exec,size=4g,mode=1777",
        "--tmpfs", "/tmp:rw,nosuid,nodev,exec,size=1g,mode=1777",
        "--env", "HOME=/tmp/salve-home", "--env", "GRADLE_USER_HOME=/work/gradle-home",
        "--workdir", "/work", "--entrypoint", "/bin/sh", image, "-eu", "-c", BUILD_SCRIPT,
    ]


def run_sandbox(archive: Path, image: str) -> None:
    if IMAGE_PATTERN.fullmatch(image) is None:
        raise SandboxError("Se requiere una imagen inmutable")
    if not archive.is_file() or archive.is_symlink():
        raise SandboxError("Falta el snapshot regular del candidato")
    # El usuario no privilegiado del contenedor solo necesita leer este archivo.
    archive.chmod(0o444)
    with tempfile.TemporaryDirectory(prefix="salve-docker-config-") as directory:
        prefix = docker_prefix(Path(directory))
        # No heredar GH_TOKEN, GITHUB_TOKEN, SSH_AUTH_SOCK, proxies, configuración
        # Docker del usuario ni el resto del entorno de la estación.
        environment = {"PATH": os.defpath, "LANG": "C.UTF-8"}
        try:
            inspection = subprocess.run(
                prefix + ["image", "inspect", "--format", "{{json .Config.Volumes}}", image],
                env=environment, check=True, capture_output=True, text=True, timeout=30,
            )
            if json.loads(inspection.stdout) not in (None, {}):
                raise SandboxError("La imagen no debe declarar volúmenes persistentes")
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            raise SandboxError("No se pudo validar la imagen local del sandbox") from error

        name = "salve-test-" + uuid.uuid4().hex
        command = container_command(prefix, archive, image, name)
        with handle_stop_signal():
            try:
                # No permitir que la salida llene disco/memoria del anfitrión.
                subprocess.run(command, env=environment, check=True, timeout=TIMEOUT_SECONDS,
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            except subprocess.TimeoutExpired as error:
                raise SandboxError(f"Sandbox excedió {TIMEOUT_SECONDS} segundos") from error
            except subprocess.CalledProcessError as error:
                raise SandboxError(f"Sandbox falló con código {error.returncode}; no se publica") from error
            except OSError as error:
                raise SandboxError("No se pudo iniciar el sandbox") from error
            finally:
                # Matar el cliente Docker no mata necesariamente el contenedor.
                try:
                    cleanup = subprocess.run(prefix + ["rm", "--force", name], env=environment,
                                             capture_output=True, text=True, timeout=30)
                    if cleanup.returncode and "No such container" not in cleanup.stderr:
                        raise SandboxError(f"No se pudo retirar el contenedor {name}; revisar Docker")
                except (OSError, subprocess.SubprocessError) as error:
                    raise SandboxError(f"No se pudo retirar el contenedor {name}; revisar Docker") from error
