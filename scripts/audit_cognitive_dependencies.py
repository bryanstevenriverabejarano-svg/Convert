#!/usr/bin/env python3
"""Inventario estático reproducible de dependencias Java/Kotlin de Salve.

No presupone que una clase sea funcional por existir, ni pretende capturar
enlaces por reflexión, XML, Android Manifest, DI dinámica, JNI o llamadas HTTP.
Se examinan imports y referencias FQCN del código versionado; los ciclos son
entre subsistemas (pueden ser legítimos), no prueba automática de duplicación.

Uso: python3 scripts/audit_cognitive_dependencies.py --json build/dependencies.json
"""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re

PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*;?", re.M)
IMPORT = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)(?:\.\*)?(?:\s+as\s+\w+)?\s*;?", re.M)
QUALIFIED = re.compile(r"(?<![\w.])(?:salve|ai\.mlc)(?:\.[A-Za-z_]\w*){2,}")
CODE_DIRS = ("app/src/main/java", "mlc4j/src/main/java")


def subsystem(package):
    """Stable component boundaries; core root is the legacy coordination layer."""
    parts = package.split(".")
    if len(parts) >= 3 and parts[:2] == ["salve", "core"]:
        return "core/" + parts[2]
    if len(parts) >= 2 and parts[0] == "salve":
        return parts[1]
    if len(parts) >= 2 and parts[:2] == ["ai", "mlc"]:
        return "mlc4j"
    return package


def java_kotlin_files(root):
    for directory in CODE_DIRS:
        base = root / directory
        if base.is_dir():
            yield from sorted(p for p in base.rglob("*")
                              if p.is_file() and p.suffix in (".java", ".kt")
                              and not p.name.startswith("package-info"))


def sources(root):
    units = {}
    for path in java_kotlin_files(root):
        text = path.read_text(encoding="utf-8", errors="replace")
        match = PACKAGE.search(text)
        if match is None:
            continue
        name = match.group(1) + "." + path.stem
        units[name] = {"path": path.relative_to(root).as_posix(),
                       "module": subsystem(match.group(1)),
                       "text": text,
                       "imports": IMPORT.findall(text)}
    return units


def target_for(reference, known):
    """Resolve static imports of methods to their declaring, versioned class."""
    current = reference
    while "." in current:
        if current in known:
            return current
        current = current.rsplit(".", 1)[0]
    return None


def inventory(root):
    known = sources(root)
    files_per_module = Counter(v["module"] for v in known.values())
    imported_edges = Counter()
    example = defaultdict(list)
    for source, unit in known.items():
        refs = set(unit["imports"])
        refs.update(QUALIFIED.findall(unit["text"]))
        destinations = set()
        for ref in refs:
            target = target_for(ref, known)
            if target is None or target == source:
                continue
            destination = known[target]["module"]
            if destination != unit["module"]:
                destinations.add(destination)
                if len(example[(unit["module"], destination)]) < 3:
                    example[(unit["module"], destination)].append([unit["path"], known[target]["path"]])
        for destination in destinations:
            imported_edges[(unit["module"], destination)] += 1

    graph = defaultdict(set)
    for a, b in imported_edges:
        graph[a].add(b)
    for module in files_per_module:
        graph[module]

    # Tarjan SCC on module graph, not the class graph.
    counter = [0]
    index = {}
    low = {}
    stack = []
    active = set()
    cycles = []

    def visit(node):
        index[node] = low[node] = counter[0]
        counter[0] += 1
        stack.append(node)
        active.add(node)
        for dest in sorted(graph[node]):
            if dest not in index:
                visit(dest)
                low[node] = min(low[node], low[dest])
            elif dest in active:
                low[node] = min(low[node], index[dest])
        if low[node] == index[node]:
            component = []
            while True:
                nxt = stack.pop()
                active.remove(nxt)
                component.append(nxt)
                if nxt == node:
                    break
            if len(component) > 1:
                cycles.append(sorted(component))

    for module in sorted(graph):
        if module not in index:
            visit(module)

    edges = [{"from": a, "to": b, "files": count, "examples": example[(a, b)]}
             for (a, b), count in imported_edges.items()]
    edges.sort(key=lambda x: (-x["files"], x["from"], x["to"]))
    return {
        "scope": "Static Java/Kotlin imports and fully qualified references, main sources only",
        "limitations": [
            "No runtime or reflection call graph; class absence from this report is not proof of disconnection",
            "No resource XML, AndroidManifest, native JNI, Gradle or external dependency resolution",
            "Package/module cycles are architecture signals, not confirmed duplicate behavior",
        ],
        "classes_indexed": len(known),
        "modules": dict(sorted(files_per_module.items())),
        "cross_module_edges": edges,
        "module_cycles": sorted(cycles, key=lambda c: (-len(c), c)),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--json", type=Path, default=None)
    args = parser.parse_args()
    report = inventory(args.root)
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Salve: {} archivos fuente, {} subsistemas, {} enlaces cruzados, {} grupos cíclicos".format(
        report["classes_indexed"], len(report["modules"]),
        len(report["cross_module_edges"]), len(report["module_cycles"])))
    for e in report["cross_module_edges"][:12]:
        print("  {} -> {} ({} archivos)".format(e["from"], e["to"], e["files"]))
    if report["module_cycles"]:
        print("  SCC mayores (dependencias estáticas):")
        for cycle in report["module_cycles"][:4]:
            print("   - " + " ↔ ".join(cycle))
    if args.json:
        print("Informe JSON:", args.json)


if __name__ == "__main__":
    main()
