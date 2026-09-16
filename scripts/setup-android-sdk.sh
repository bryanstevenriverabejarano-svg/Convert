#!/usr/bin/env bash
set -euo pipefail

# Instala únicamente los componentes que necesita este proyecto. Se puede cambiar
# la ubicación sin editar el script: ANDROID_SDK_ROOT=$HOME/Android/Sdk ./scripts/setup-android-sdk.sh
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android-sdk}}"
readonly CMDLINE_TOOLS_VERSION="${CMDLINE_TOOLS_VERSION:-13114758}"
readonly COMMAND_LINE_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
readonly DOWNLOAD_URL="https://dl.google.com/android/repository/${COMMAND_LINE_ZIP}"
readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf 'Falta el comando requerido: %s\n' "$1" >&2
        exit 1
    fi
}

require_command curl
require_command unzip

if [[ ! -w "$(dirname "$ANDROID_SDK_ROOT")" ]]; then
    printf 'No se puede escribir en %s. Usa ANDROID_SDK_ROOT con una ruta accesible.\n' \
        "$(dirname "$ANDROID_SDK_ROOT")" >&2
    exit 1
fi

mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"

if [[ ! -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]]; then
    archive="$(mktemp --suffix=.zip)"
    extracted="$(mktemp -d)"
    trap 'rm -f "$archive"; rm -rf "$extracted"' EXIT

    printf 'Descargando Android SDK Command-line Tools...\n'
    curl --fail --location --retry 3 --output "$archive" "$DOWNLOAD_URL"
    unzip -q "$archive" -d "$extracted"
    rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    mv "$extracted/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
fi

sdkmanager="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
yes | "$sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null || true
"$sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0"

# Gradle admite barras normales incluso en local.properties y así no hay que escapar la ruta.
escaped_sdk_dir="${ANDROID_SDK_ROOT//\\/\\\\}"
printf 'sdk.dir=%s\n' "$escaped_sdk_dir" > "$PROJECT_ROOT/local.properties"

profile_file="$HOME/.config/convert/android-sdk-env.sh"
mkdir -p "$(dirname "$profile_file")"
cat > "$profile_file" <<EOF
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH"
EOF

printf '\nAndroid SDK listo en %s.\n' "$ANDROID_SDK_ROOT"
printf 'Gradle quedó configurado en %s/local.properties.\n' "$PROJECT_ROOT"
printf 'Para cargar las variables en esta terminal ejecuta:\n  source %s\n' "$profile_file"
