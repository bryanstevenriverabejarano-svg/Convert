#!/usr/bin/env python3
"""Generate 104 bounded challenges and independently certify four runs per case.

This evaluator does not call the Java solver and puts no expected answers in the
challenge corpus.  Optimality is checked using Floyd-Warshall and exhaustive
subset enumeration, independently of the generated tool programs.  Passing this
finite benchmark is evidence of these capabilities, not of superintelligence.
"""
from __future__ import annotations

import argparse
import copy
import itertools
import json
import math
from pathlib import Path
from typing import Any


FAMILIES = ("route", "knapsack", "schedule", "dependencies")


def _graph(nodes: int, edges: list, source: int = 0, target: int | None = None) -> dict:
    return {"nodes": nodes, "edges": edges, "source": source,
            "target": nodes - 1 if target is None else target}


def _route_cases() -> list[tuple[str, dict]]:
    """Different graph motifs, not 26 copies with different random weights."""
    return [
        ("Única conexión y ausencia de ruta tras retirarla", _graph(2, [[0, 1, 7]])),
        ("Diamante: el primer vecino no es el mejor", _graph(4, [[0,1,1],[1,3,9],[0,2,4],[2,3,2]])),
        ("Cadena con acceso directo caro", _graph(6, [[i,i+1,1] for i in range(5)] + [[0,5,12]])),
        ("Estrella con destino alcanzable por una sola rama", _graph(6, [[0,i,i] for i in range(1,5)]+[[3,5,1]])),
        ("Desvío dirigido que comienza alejándose del destino", _graph(5, [[0,3,1],[3,1,1],[1,2,1],[2,4,1],[0,4,9]])),
        ("Anillo con ciclo positivo y dos salidas", _graph(6, [[i,(i+1)%5,2] for i in range(5)]+[[2,5,4],[4,5,1]])),
        ("Escalera con peldaños asimétricos", _graph(8, [[i,i+1,3] for i in range(3)]+[[i,i+1,1] for i in range(4,7)]+[[i,i+4,2] for i in range(4)]+[[5,2,1]])),
        ("Cuadrícula dirigida con casillas de diferente coste", _graph(9, [[i,i+1,1+i%3] for i in range(9) if i%3<2]+[[i,i+3,2] for i in range(6)])),
        ("Grafo denso dirigido", _graph(6, [[u,v,1+(u*3+v*7)%13] for u in range(6) for v in range(6) if u!=v])),
        ("Capas bipartitas con emparejamientos de coste desigual", _graph(8, [[0,i,i] for i in range(1,4)]+[[i,j,1+(i+j)%4] for i in range(1,4) for j in range(4,7)]+[[i,7,7-i] for i in range(4,7)])),
        ("Dos componentes sin puente", _graph(6, [[0,1,1],[1,2,1],[3,4,1],[4,5,1]])),
        ("Componente de coste cero sin ciclo negativo", _graph(6, [[0,1,2],[1,2,0],[2,3,0],[3,1,0],[3,5,1],[0,4,1],[4,5,8]])),
        ("DAG con descuento negativo que invalida Dijkstra ingenuo", _graph(6, [[0,1,2],[0,2,5],[2,1,-7],[1,3,3],[3,5,1],[2,4,0],[4,5,2]])),
        ("Dos concentradores con atajos cruzados", _graph(8, [[0,1,1],[0,2,3],[1,3,9],[1,4,4],[2,4,1],[2,5,2],[3,6,1],[4,6,2],[5,7,8],[6,7,1]])),
        ("Etiquetas de vértices que no siguen el recorrido", _graph(7, [[0,5,1],[5,3,1],[3,1,1],[1,4,1],[4,2,1],[2,6,1],[0,6,15]])),
        ("Arcos de ida y vuelta con pesos diferentes", _graph(5, [[0,1,8],[1,0,1],[0,2,2],[2,1,1],[1,3,1],[3,1,7],[3,4,1]])),
        ("Puente entre dos subgrafos", _graph(8, [[0,1,2],[0,2,1],[1,3,1],[2,3,1],[3,4,7],[4,5,1],[4,6,2],[5,7,4],[6,7,1]])),
        ("Vecino barato conduce a un callejón costoso", _graph(7, [[0,1,0],[1,2,0],[2,6,30],[0,3,3],[3,4,3],[4,5,3],[5,6,3]])),
        ("Cadena larga con saltos de distintas longitudes", _graph(10, [[i,i+1,2] for i in range(9)]+[[0,3,3],[3,7,2],[1,6,2],[6,9,1],[7,9,8]])),
        ("Árbol dirigido con hojas irrelevantes", _graph(13, [[(i-1)//2,i,1+i%3] for i in range(1,13)])),
        ("Árbol con enlaces entre ramas", _graph(10, [[(i-1)//2,i,3] for i in range(1,10)]+[[2,7,1],[7,9,1],[4,8,1]])),
        ("Tres caminos óptimos empatados", _graph(7, [[0,1,1],[1,4,1],[4,6,1],[0,2,1],[2,5,1],[5,6,1],[0,3,2],[3,6,1]])),
        ("Dos diamantes unidos por una articulación", _graph(9, [[0,1,1],[0,2,2],[1,3,3],[2,3,1],[3,4,1],[4,5,5],[4,6,1],[5,7,1],[6,7,2],[7,8,1]])),
        ("Destino aislado y muchas ramas alcanzables", _graph(14, [[0,i,i%4] for i in range(1,10)]+[[10,11,1],[11,12,1]])),
        ("Origen y destino iguales con distracciones", _graph(5, [[0,1,1],[1,2,1],[2,0,1],[3,4,1]], target=0)),
        ("Límite de vértices y atajo interior decisivo", _graph(14, [[i,i+1,3] for i in range(13)]+[[1,11,1],[0,13,40],[8,3,2]])),
    ]


def _knapsack_cases() -> list[tuple[str, dict]]:
    specs = [
        ("Una elección indivisible", 5, [(5,8)]),
        ("Densidad voraz pierde frente a dos piezas", 10, [(6,13),(5,10),(5,10)]),
        ("Valor individual voraz pierde frente a un conjunto", 8, [(8,15),(4,9),(4,9)]),
        ("Pesos iguales y valores diferentes", 9, [(3,4),(3,9),(3,7),(3,1),(3,8)]),
        ("Valores iguales y pesos diferentes", 9, [(8,6),(2,6),(4,6),(3,6),(6,6)]),
        ("Capacidad cero", 0, [(1,4),(3,9),(6,11)]),
        ("Todas las piezas exceden la capacidad", 2, [(3,20),(5,1),(8,30),(4,9)]),
        ("Todo cabe exactamente", 15, [(1,2),(2,4),(3,3),(4,8),(5,6)]),
        ("Valores cero y una recompensa útil", 11, [(1,0),(5,0),(6,8),(4,0),(3,0),(8,0)]),
        ("Dominancia parcial entre objetos", 12, [(4,8),(5,7),(3,9),(8,15),(7,17)]),
        ("Pesos coprimos y residuo imposible de rellenar", 19, [(4,7),(7,12),(9,16),(11,20)]),
        ("Duplicados seleccionables por índice", 12, [(4,7),(4,7),(4,7),(6,11),(6,11)]),
        ("Un objeto voluminoso compite con muchas piezas", 13, [(13,30)]+[(2,5)]*6),
        ("Potencias de dos en pesos", 31, [(1,1),(2,5),(4,9),(8,15),(16,34),(32,100)]),
        ("Pesos próximos a la mitad de capacidad", 20, [(9,15),(10,18),(11,24),(12,23),(8,12)]),
        ("Frontera de Pareto con múltiples empates", 16, [(2,3),(4,6),(6,9),(8,12),(10,15),(12,18)]),
        ("Pieza gratis en valor pero costosa en espacio", 14, [(9,0),(7,12),(6,11),(3,4),(2,3)]),
        ("Óptimo deja espacio sobrante", 13, [(6,14),(6,14),(5,8),(8,17)]),
        ("Recompensas altas cerca de una restricción estrecha", 7, [(6,1000),(4,700),(3,600),(2,1)]),
        ("Muchas piezas pequeñas y dos alternativas medianas", 9, [(1,2),(1,3),(1,1),(1,4),(1,2),(1,5),(5,16),(4,15)]),
        ("Capacidad máxima acotada", 100, [(51,89),(49,91),(34,67),(33,64),(33,65)]),
        ("Pesos en progresión y valores no monótonos", 21, [(2,8),(4,3),(6,15),(8,6),(10,23),(12,9),(14,30)]),
        ("Combinación de tres supera parejas atractivas", 15, [(8,18),(7,15),(5,13),(5,13),(5,13)]),
        ("Capacidad impar con pesos pares", 17, [(2,2),(4,7),(6,10),(8,14),(10,19),(12,20)]),
        ("Doce candidatos y recompensas dispersas", 23, [(i+1, (i*11)%23) for i in range(12)]),
        ("Catorce candidatos en el límite de enumeración", 26, [(1+i%7, 2+(i*13)%19) for i in range(14)]),
    ]
    return [(name, {"capacity": cap, "items": [{"weight": w, "value": v} for w,v in items]})
            for name,cap,items in specs]


def _schedule_cases() -> list[tuple[str, dict]]:
    specs = [
        ("Un único intervalo", [(0,3,7)]),
        ("Cadena de trabajos que se tocan", [(i,i+1,2+i%3) for i in range(6)]),
        ("Un trabajo largo pierde frente a dos cortos", [(0,10,15),(0,5,9),(5,10,9)]),
        ("Terminar antes no maximiza la recompensa", [(0,1,1),(1,2,1),(0,2,9),(2,3,2)]),
        ("Intervalos completamente anidados", [(0,12,10),(1,11,12),(2,10,8),(3,9,14),(4,8,6)]),
        ("Ventanas idénticas con diferentes recompensas", [(2,6,3),(2,6,12),(2,6,8),(6,8,2)]),
        ("Empates en comienzo", [(0,3,4),(0,5,8),(0,7,9),(3,5,5),(5,7,4)]),
        ("Empates en final", [(0,6,8),(2,6,12),(4,6,5),(0,2,3),(6,8,4)]),
        ("Solapamiento escalonado", [(i,i+3,3+(i*5)%11) for i in range(8)]),
        ("Dos islas temporales independientes", [(0,2,4),(1,3,6),(3,4,2),(10,12,5),(11,14,8),(12,14,4)]),
        ("Todos los trabajos se solapan", [(0,8,3),(1,9,8),(2,10,10),(3,11,7),(4,12,14)]),
        ("Trabajos sin recompensa", [(0,2,0),(2,5,0),(1,4,0),(5,6,0)]),
        ("Trabajo puente bloquea dos grupos", [(0,2,8),(2,4,5),(3,9,11),(8,10,7),(10,12,8)]),
        ("Huecos grandes no alteran la compatibilidad", [(0,1,2),(100,101,8),(102,104,4),(101,103,7)]),
        ("Elecciones simétricas con óptimos empatados", [(0,2,5),(2,4,5),(0,1,5),(1,4,5),(4,5,1)]),
        ("Tiempos negativos válidos", [(-8,-5,4),(-6,-2,9),(-5,-3,5),(-3,0,6),(0,2,2)]),
        ("Trabajo caro al final cambia las elecciones previas", [(0,4,7),(0,2,4),(2,5,8),(4,6,5),(5,7,20)]),
        ("Duración corta no implica buen valor", [(0,1,1),(1,2,1),(2,3,1),(3,4,1),(0,4,12)]),
        ("Alternancia entre ventanas cortas y largas", [(0,2,5),(1,5,8),(2,4,6),(4,6,5),(5,9,10),(6,8,6),(8,10,5)]),
        ("Ventanas duplicadas son trabajos distintos", [(0,2,5),(0,2,5),(2,4,5),(2,4,5),(4,6,5),(4,6,5)]),
        ("Óptimo evita el mayor valor individual", [(0,9,20),(0,3,8),(3,6,8),(6,9,8)]),
        ("Desorden de entrada completo", [(9,12,7),(0,3,4),(6,9,8),(3,6,5),(2,10,11)]),
        ("Límite exacto permite encadenar tres tareas", [(0,4,6),(4,7,5),(7,10,8),(3,8,14),(0,10,18)]),
        ("Doble peine de intervalos", [(i,i+2,4) for i in range(0,12,2)]+[(i,i+3,7) for i in range(1,10,3)]),
        ("Doce trabajos con diferentes grados de conflicto", [(i, i+1+(i%4), 2+(i*7)%17) for i in range(12)]),
        ("Catorce trabajos y subproblemas repetidos", [(i//2*2, i//2*2+1+i%2, 3+(i*5)%13) for i in range(14)]),
    ]
    return [(name, {"jobs": [{"start": s, "end": e, "value": v} for s,e,v in jobs]})
            for name,jobs in specs]


def _dependency_cases() -> list[tuple[str, dict]]:
    specs = [
        ("Un nodo sin dependencias", 1, []),
        ("Cadena lineal", 6, [[i,i+1] for i in range(5)]),
        ("Estrella de distribución", 7, [[0,i] for i in range(1,7)]),
        ("Estrella de agregación", 7, [[i,6] for i in range(6)]),
        ("Diamante con reunión", 4, [[0,1],[0,2],[1,3],[2,3]]),
        ("Dos cadenas independientes", 8, [[i,i+1] for i in range(3)]+[[i,i+1] for i in range(4,7)]),
        ("Grafo vacío con nodos aislados", 6, []),
        ("Orden de etiquetas inverso", 6, [[i,i-1] for i in range(1,6)]),
        ("Árbol de construcción", 13, [[(i-1)//2,i] for i in range(1,13)]),
        ("Árbol de agregación", 10, [[i,(i-1)//2] for i in range(1,10)]),
        ("DAG completo", 6, [[i,j] for i in range(6) for j in range(i+1,6)]),
        ("Dos capas bipartitas completas", 8, [[i,j] for i in range(4) for j in range(4,8)]),
        ("Tres capas y un trabajo aislado", 10, [[i,j] for i in range(3) for j in range(3,6)]+[[i,j] for i in range(3,6) for j in range(6,9)]),
        ("Ciclo de dos nodos", 2, [[0,1],[1,0]]),
        ("Ciclo de tres nodos", 3, [[0,1],[1,2],[2,0]]),
        ("Autodependencia", 4, [[0,1],[1,2],[2,2],[2,3]]),
        ("Ciclo oculto en componente separada", 7, [[0,1],[1,2],[3,4],[4,5],[5,3]]),
        ("Ciclo con cola dependiente", 7, [[0,1],[1,2],[2,0],[2,3],[3,4],[4,5],[5,6]]),
        ("Prefijo acíclico desemboca en ciclo", 7, [[i,i+1] for i in range(6)]+[[6,4]]),
        ("Dos componentes cíclicas", 6, [[0,1],[1,0],[2,3],[3,4],[4,2]]),
        ("Dependencias transitivas redundantes", 7, [[i,i+1] for i in range(6)]+[[0,3],[1,5],[0,6],[3,6]]),
        ("Escalera de compilación", 8, [[i,i+1] for i in range(3)]+[[i,i+1] for i in range(4,7)]+[[i,i+4] for i in range(4)]),
        ("Dos diamantes con articulación", 7, [[0,1],[0,2],[1,3],[2,3],[3,4],[3,5],[4,6],[5,6]]),
        ("Catorce nodos y ancho máximo", 14, [[0,i] for i in range(1,13)]+[[i,13] for i in range(1,13)]),
        ("Orden válido mezclado con índices", 9, [[0,5],[5,2],[2,7],[1,6],[6,3],[3,8],[7,8]]),
        ("Ciclo largo con cuerdas", 12, [[i,(i+1)%12] for i in range(12)]+[[0,4],[4,8],[2,7]]),
    ]
    return [(name, {"nodes": n, "edges": edges}) for name,n,edges in specs]


def _add_missing_edge(data: dict, weighted: bool, prefer_reverse: bool = False) -> None:
    edges, n = data["edges"], data["nodes"]
    pairs = {(edge[0], edge[1]) for edge in edges}
    candidates = [(v,u) for u,v,*_ in edges] if prefer_reverse else []
    candidates += [(u,v) for u in range(n) for v in range(n) if u != v]
    for u,v in candidates:
        if (u,v) not in pairs:
            edges.append([u,v,4] if weighted else [u,v])
            return
    if n < 14:
        data["nodes"] += 1
        edges.append([n-1,n,2] if weighted else [n-1,n])
    elif edges:
        del edges[-1]


def _mutate(family: str, original: dict, round_number: int) -> tuple[dict, str]:
    data = copy.deepcopy(original)
    if round_number == 1:
        return data, "Situación inicial; el programa no recibe respuestas esperadas."
    if family == "route":
        if round_number == 2:
            del data["edges"][len(data["edges"])//2]
            return data, "Se retira una conexión: deben revisarse ruta y alcanzabilidad."
        if round_number == 3:
            # Reweight with vertex potentials: every cycle retains nonnegative
            # weight, but negative edges prevent assuming Dijkstra is applicable.
            _add_missing_edge(data, weighted=True)
            data["edges"] = [[u,v,abs(w)+((u*11)%17)-((v*11)%17)] for u,v,w in data["edges"]]
            return data, "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos."
        if data["nodes"] < 14:
            old = data["nodes"]
            data["nodes"] += 1
            data["edges"] += [[data["target"],old,1]]
            data["target"] = old
        else:
            data["source"],data["target"] = data["target"],data["source"]
            _add_missing_edge(data, weighted=True)
        return data, "Cambia el destino o el sentido de la consulta y se modifica la topología."
    if family == "knapsack":
        if round_number == 2:
            if len(data["items"]) == 14:
                data["items"].pop(0)
            data["items"].append({"weight": max(1,data["capacity"]//3), "value": 1+max(x["value"] for x in original["items"])})
            return data, "Aparece una nueva alternativa de alta recompensa; recalcular la selección."
        if round_number == 3:
            item = data["items"].pop(0)
            if len(data["items"]) == 13:
                data["items"].pop()
            data["items"] += [{"weight": max(1,item["weight"]//2), "value": item["value"]//2},
                              {"weight": max(1,item["weight"]-item["weight"]//2), "value": item["value"]-item["value"]//2+1}]
            data["capacity"] = min(100,data["capacity"]+1)
            return data, "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto."
        data["items"].pop(len(data["items"])//2)
        data["capacity"] = max(0,data["capacity"]-2)
        return data, "Un recurso deja de estar disponible y disminuye el presupuesto."
    if family == "schedule":
        jobs = data["jobs"]
        if round_number == 2:
            if len(jobs) == 14:
                jobs.pop(0)
            jobs.append({"start": min(j["start"] for j in original["jobs"])-1,
                         "end": max(j["end"] for j in original["jobs"])+1,
                         "value": 1+sum(j["value"] for j in original["jobs"])//2})
            return data, "Aparece un encargo largo que compite con todo el calendario."
        if round_number == 3:
            job = jobs.pop(0)
            if len(jobs) == 13:
                jobs.pop()
            # Scale time so even a unit-length job can be split into two valid
            # adjacent jobs. This is an actual interval-graph change.
            for other in jobs:
                other["start"] *= 2
                other["end"] *= 2
            start,end = job["start"]*2,job["end"]*2
            middle = (start+end)//2
            jobs += [{"start": start,"end": middle,"value": job["value"]//2},
                     {"start": middle,"end": end,"value": job["value"]-job["value"]//2+1}]
            return data, "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos."
        jobs.pop(len(jobs)//2)
        jobs.reverse()
        return data, "Se cancela un encargo y llega el calendario en orden inverso."
    if round_number == 2:
        _add_missing_edge(data, weighted=False, prefer_reverse=True)
        return data, "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo."
    if round_number == 3:
        if data["edges"]:
            data["edges"].pop(len(data["edges"])//2)
        else:
            data["nodes"] += 1
        return data, "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden."
    if data["nodes"] < 14:
        old = data["nodes"]
        data["nodes"] += 1
        data["edges"].append([old,0])
    else:
        data["edges"] = [[v,u] for u,v in data["edges"]]
    return data, "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias."


def generate_corpus() -> list[dict]:
    cases = {"route": _route_cases(), "knapsack": _knapsack_cases(),
             "schedule": _schedule_cases(), "dependencies": _dependency_cases()}
    output = []
    for family in FAMILIES:
        if len(cases[family]) != 26:
            raise AssertionError("Each family needs 26 explicit motifs")
        for index,(description,original) in enumerate(cases[family],1):
            case_id = f"{family}-{index:03d}"
            for round_number in range(1,5):
                data,mutation = _mutate(family,original,round_number)
                output.append({"schema": 1, "id": f"{case_id}-r{round_number}",
                               "caseId": case_id,"round": round_number,"family": family,
                               "description": description,"mutation": mutation,"input": data})
    # A round is a full pass over all 104 cases. The Java CLI reconstructs the
    # laboratory from its persisted state exactly at these round boundaries.
    return sorted(output,key=lambda challenge: (challenge["round"],FAMILIES.index(challenge["family"]),challenge["caseId"]))


def _integer(value: Any, description: str) -> int:
    if type(value) is not int:
        raise ValueError(f"{description}: se requiere un entero")
    return value


def _indices(value: Any, size: int, description: str) -> list[int]:
    if not isinstance(value,list):
        raise ValueError(f"{description}: se requiere una lista")
    for index in value:
        _integer(index,description)
        if not 0 <= index < size:
            raise ValueError(f"{description}: índice fuera de rango")
    if len(set(value)) != len(value):
        raise ValueError(f"{description}: índice duplicado")
    return value


def _floyd(data: dict) -> list[list[float | int]]:
    n = data["nodes"]
    distances = [[math.inf]*n for _ in range(n)]
    for i in range(n):
        distances[i][i] = 0
    for u,v,w in data["edges"]:
        distances[u][v] = min(distances[u][v],w)
    for k in range(n):
        for i in range(n):
            for j in range(n):
                distances[i][j] = min(distances[i][j],distances[i][k]+distances[k][j])
    if any(distances[i][i] < 0 for i in range(n)):
        raise ValueError("El corpus contiene un ciclo negativo no admitido")
    return distances


def _subset_optimum(items: list, feasible, value) -> int:
    best = 0
    # Exhaustively check all subsets; no dynamic program or generated strategy is
    # reused, so optimality does not depend on trusting a solver's own verifier.
    for mask in range(1 << len(items)):
        chosen = [i for i in range(len(items)) if mask & (1 << i)]
        if feasible(chosen):
            best = max(best,value(chosen))
    return best


def _acyclic(data: dict) -> bool:
    remaining = set(range(data["nodes"]))
    while remaining:
        ready = {v for v in remaining if not any(u in remaining and w == v for u,w in data["edges"])}
        if not ready:
            return False
        remaining -= ready
    return True


def verify_result(challenge: dict, result: dict) -> dict:
    """Return an independent certificate summary or raise ValueError."""
    if not isinstance(result,dict):
        raise ValueError("Falta el resultado estructurado")
    family,data = challenge["family"],challenge["input"]
    if family == "route":
        optimum = _floyd(data)[data["source"]][data["target"]]
        if optimum == math.inf:
            if result.get("status") != "unreachable":
                raise ValueError("Declara una ruta hacia un destino inalcanzable")
            return {"oracle": "Floyd–Warshall independiente", "reachable": False}
        if result.get("status") != "ok":
            raise ValueError("No resolvió una ruta alcanzable")
        path = _indices(result.get("path"),data["nodes"],"path")
        if not path or path[0] != data["source"] or path[-1] != data["target"]:
            raise ValueError("Extremos de la ruta incorrectos")
        edge_cost = {}
        for u,v,w in data["edges"]:
            edge_cost[u,v] = min(edge_cost.get((u,v),math.inf),w)
        cost = 0
        for u,v in zip(path,path[1:]):
            if (u,v) not in edge_cost:
                raise ValueError("La ruta usa una conexión inexistente")
            cost += edge_cost[u,v]
        if _integer(result.get("cost"),"cost") != cost or cost != optimum:
            raise ValueError(f"Ruta no óptima o coste declarado incorrecto; óptimo={optimum}, calculado={cost}")
        return {"oracle": "Floyd–Warshall independiente", "optimum": optimum,"reachable": True}
    if result.get("status") != "ok" and family != "dependencies":
        raise ValueError("No produjo una solución factible")
    if family == "knapsack":
        items = data["items"]
        chosen = _indices(result.get("selected"),len(items),"selected")
        weight = sum(items[i]["weight"] for i in chosen)
        value = sum(items[i]["value"] for i in chosen)
        if weight > data["capacity"]:
            raise ValueError("La selección excede la capacidad")
        if _integer(result.get("weight"),"weight") != weight or _integer(result.get("value"),"value") != value:
            raise ValueError("Valor o peso declarado no coincide con la selección")
        optimum = _subset_optimum(items,
            lambda ids: sum(items[i]["weight"] for i in ids) <= data["capacity"],
            lambda ids: sum(items[i]["value"] for i in ids))
        if value != optimum:
            raise ValueError(f"Selección subóptima: {value}, óptimo={optimum}")
        return {"oracle": "Enumeración exhaustiva independiente","subsets": 1 << len(items),"optimum": optimum}
    if family == "schedule":
        jobs = data["jobs"]
        chosen = _indices(result.get("selected"),len(jobs),"selected")
        def feasible(ids):
            return all(jobs[a]["end"] <= jobs[b]["start"] or jobs[b]["end"] <= jobs[a]["start"]
                       for a,b in itertools.combinations(ids,2))
        if not feasible(chosen):
            raise ValueError("La selección contiene trabajos solapados")
        value = sum(jobs[i]["value"] for i in chosen)
        if _integer(result.get("value"),"value") != value:
            raise ValueError("Valor declarado no coincide con los trabajos")
        optimum = _subset_optimum(jobs,feasible,lambda ids: sum(jobs[i]["value"] for i in ids))
        if value != optimum:
            raise ValueError(f"Calendario subóptimo: {value}, óptimo={optimum}")
        return {"oracle": "Enumeración exhaustiva independiente","subsets": 1 << len(jobs),"optimum": optimum}
    if family != "dependencies":
        raise ValueError("Familia desconocida")
    n,edges = data["nodes"],{tuple(edge) for edge in data["edges"]}
    if not _acyclic(data):
        cycle = result.get("cycle")
        if result.get("status") != "cycle" or not isinstance(cycle,list) or len(cycle) < 2:
            raise ValueError("Falta un testigo explícito del ciclo")
        for node in cycle:
            _integer(node,"cycle")
            if not 0 <= node < n:
                raise ValueError("Vértice del ciclo fuera de rango")
        if cycle[0] != cycle[-1] or any((u,v) not in edges for u,v in zip(cycle,cycle[1:])):
            raise ValueError("El testigo no forma un ciclo cerrado existente")
        return {"oracle": "Eliminación independiente de fuentes y testigo de ciclo","acyclic": False}
    if result.get("status") != "ok":
        raise ValueError("Declara un ciclo inexistente")
    order = _indices(result.get("order"),n,"order")
    if len(order) != n:
        raise ValueError("El orden omite nodos")
    positions = {node:i for i,node in enumerate(order)}
    if any(positions[u] >= positions[v] for u,v in edges):
        raise ValueError("El orden incumple una dependencia")
    layers = result.get("layers")
    if not isinstance(layers,list) or any(not isinstance(layer,list) or not layer for layer in layers):
        raise ValueError("Capas vacías o formato incorrecto")
    flattened = _indices([node for layer in layers for node in layer],n,"layers")
    if len(flattened) != n:
        raise ValueError("Las capas omiten nodos")
    layer_of = {node:i for i,layer in enumerate(layers) for node in layer}
    if any(layer_of[u] >= layer_of[v] for u,v in edges):
        raise ValueError("Una dependencia no precede estrictamente a su consumidor")
    return {"oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
            "acyclic": True,"layer_count": len(layers)}


def _json_block(value: Any) -> str:
    return "```json\n"+json.dumps(value,ensure_ascii=False,indent=2,allow_nan=False)+"\n```\n"


def _observed_metrics(reports: list[dict]) -> dict:
    latencies = sorted(report["elapsedNanos"]/1_000_000 for report in reports
                       if type(report.get("elapsedNanos")) is int and report["elapsedNanos"] >= 0)
    def percentile(fraction):
        return latencies[max(0,math.ceil(len(latencies)*fraction)-1)] if latencies else None
    attempts = [attempt for report in reports if isinstance(report.get("attempts"),list)
                for attempt in report["attempts"] if isinstance(attempt,dict)]
    return {"executions": len(reports),"adapted": sum(report.get("adapted") is True for report in reports),
            "reused": sum(report.get("reused") is True for report in reports),
            "promoted": sum(report.get("promoted") is True for report in reports),
            "previous_program_rejected": sum(report.get("previousRejected") is True for report in reports),
            "candidate_attempts": len(attempts),"rejected_candidates": sum(attempt.get("passed") is False for attempt in attempts),
            "regression_checks": sum(attempt["regressionChecks"] for attempt in attempts
                                     if type(attempt.get("regressionChecks")) is int and attempt["regressionChecks"] >= 0),
            "operations": sum(report["operations"] for report in reports
                              if type(report.get("operations")) is int and report["operations"] >= 0),
            "latency_ms": {"environment": "JVM host; no mide latencia del móvil ni inferencia de un LLM",
                           "percentile_method": "nearest_rank","samples": len(latencies),
                           "p50": percentile(.5),"p95": percentile(.95),"max": latencies[-1] if latencies else None}}


def verify_reports(reports: list[dict], output_dir: Path | None = None) -> dict:
    expected = {challenge["id"]: challenge for challenge in generate_corpus()}
    seen: set[str] = set()
    assessments: list[dict] = []
    grouped: dict[str,list[tuple[dict,dict]]] = {}
    for report in reports:
        challenge = report.get("challenge") if isinstance(report,dict) else None
        challenge_id = challenge.get("id") if isinstance(challenge,dict) else None
        assessment = {"id": challenge_id,"passed": False}
        try:
            if challenge_id not in expected:
                raise ValueError("Identificador fuera del corpus canónico")
            if challenge_id in seen:
                raise ValueError("Ejecución duplicada")
            seen.add(challenge_id)
            if json.dumps(challenge,sort_keys=True,separators=(",",":")) != json.dumps(expected[challenge_id],sort_keys=True,separators=(",",":")):
                raise ValueError("El desafío ejecutado no coincide con el corpus canónico")
            if report.get("verified") is not True:
                raise ValueError("El ejecutor no certificó la ejecución")
            if not isinstance(report.get("program"),(dict,str)) or not report["program"]:
                raise ValueError("Falta el programa generado")
            certificate = verify_result(challenge,report.get("result"))
            assessment.update(passed=True,certificate=certificate)
        except (ValueError,KeyError,TypeError,IndexError) as error:
            assessment["error"] = str(error)
        assessments.append(assessment)
        if isinstance(challenge,dict) and challenge_id in expected:
            grouped.setdefault(expected[challenge_id]["caseId"],[]).append((report,assessment))
    missing = sorted(set(expected)-seen)
    total_passed = sum(item["passed"] for item in assessments)
    certified_reports = [report for report,assessment in zip(reports,assessments) if assessment["passed"]]
    summary = {"schema": 1,"expected_cases": 104,"expected_rounds_per_case": 4,
               "expected_executions": 416,"received_executions": len(reports),
               "passed_executions": total_passed,"failed_executions": len(reports)-total_passed,
               "missing_executions": missing,
               "passed": total_passed == 416 and len(reports) == 416 and not missing,
               "families": {family: {"cases": 26,"expected_executions": 104,
                    "passed_executions": sum(item["passed"] and str(item["id"]).startswith(family+"-") for item in assessments)}
                    for family in FAMILIES},
               "scope": "Certificación finita de herramientas acotadas; no demuestra superinteligencia ni creatividad general.",
               "reasoning_record": "Decisiones y feedback observables, no cadenas de pensamiento privadas.",
               "observed_metrics": _observed_metrics(certified_reports),
               "rounds": {str(round_number): _observed_metrics([report for report in certified_reports
                              if report["challenge"]["round"] == round_number]) for round_number in range(1,5)},
               "assessments": assessments}
    if output_dir is not None:
        output_dir.mkdir(parents=True,exist_ok=True)
        (output_dir/"summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        case_dir = output_dir/"cases"
        case_dir.mkdir(exist_ok=True)
        for case_id in sorted({c["caseId"] for c in expected.values()}):
            canonical = expected[case_id+"-r1"]
            lines = [f"# {case_id}: {canonical['description']}\n",
                     "Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. "
                     "Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.\n"]
            for report,assessment in sorted(grouped.get(case_id,[]),key=lambda pair: pair[0]["challenge"]["round"]):
                challenge = report["challenge"]
                lines += [f"## Ronda {challenge['round']}\n",challenge["mutation"]+"\n",
                          "### Desafío ejecutado\n",_json_block(challenge),
                          "### Programa generado\n",_json_block(report.get("program")),
                          "### Intentos y feedback observable\n"]
                attempts = report.get("attempts",[])
                if isinstance(attempts,list):
                    allowed = {"attempt","index","strategy","program","verified","feedback","result",
                               "decision","decisionSummary","durationMs","durationNanos","validation",
                               "accepted","error","reason","source","repair","passed","regressionChecks",
                               "programSha256","operations","elapsedNanos"}
                    attempts = [{key:value for key,value in attempt.items() if key in allowed}
                                if isinstance(attempt,dict) else attempt for attempt in attempts]
                lines += [_json_block(attempts),"### Resultado y comprobación independiente\n",
                          _json_block({"result": report.get("result"),"independent": assessment})]
                measures = {key: report[key] for key in ("durationMs","durationNanos","metrics","decisionSummary","decision","source",
                            "operations","elapsedNanos","version","adapted","reused","promoted","previousRejected","status") if key in report}
                if measures:
                    lines += ["### Medidas y resumen de decisión\n",
                              "Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.\n",_json_block(measures)]
            if not grouped.get(case_id):
                lines.append("No se recibió ninguna ejecución de este caso.\n")
            (case_dir/(case_id+".md")).write_text("\n".join(lines),encoding="utf-8")
    return summary


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("--generate",type=Path,metavar="CORPUS_JSONL")
    action.add_argument("--verify",type=Path,metavar="REPORTS_JSONL")
    parser.add_argument("--output",type=Path,help="Directorio para summary.json y 104 informes de casos")
    args = parser.parse_args(argv)
    if args.generate:
        args.generate.parent.mkdir(parents=True,exist_ok=True)
        corpus = generate_corpus()
        args.generate.write_text("".join(json.dumps(item,ensure_ascii=False,separators=(",",":"))+"\n" for item in corpus),encoding="utf-8")
        print(json.dumps({"cases": 104,"rounds": 4,"executions": len(corpus),"corpus": str(args.generate)}))
        return 0
    if not args.output:
        parser.error("--verify requiere --output")
    try:
        reports = [json.loads(line) for line in args.verify.read_text(encoding="utf-8").splitlines() if line.strip()]
        summary = verify_reports(reports,args.output)
    except (OSError,ValueError) as error:
        parser.exit(2,f"No se pudo verificar el informe: {error}\n")
    print(json.dumps({key:summary[key] for key in ("passed","received_executions","passed_executions","failed_executions","missing_executions")},ensure_ascii=False))
    return 0 if summary["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
