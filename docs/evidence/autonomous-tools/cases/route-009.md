# route-009: Grafo denso dirigido

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-009-r1",
  "caseId": "route-009",
  "round": 1,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        8
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        9
      ],
      [
        0,
        4,
        3
      ],
      [
        0,
        5,
        10
      ],
      [
        1,
        0,
        4
      ],
      [
        1,
        2,
        5
      ],
      [
        1,
        3,
        12
      ],
      [
        1,
        4,
        6
      ],
      [
        1,
        5,
        13
      ],
      [
        2,
        0,
        7
      ],
      [
        2,
        1,
        1
      ],
      [
        2,
        3,
        2
      ],
      [
        2,
        4,
        9
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        0,
        10
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        11
      ],
      [
        3,
        4,
        12
      ],
      [
        3,
        5,
        6
      ],
      [
        4,
        0,
        13
      ],
      [
        4,
        1,
        7
      ],
      [
        4,
        2,
        1
      ],
      [
        4,
        3,
        8
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        0,
        3
      ],
      [
        5,
        1,
        10
      ],
      [
        5,
        2,
        4
      ],
      [
        5,
        3,
        11
      ],
      [
        5,
        4,
        5
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "route",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "DIJKSTRA"
    },
    {
      "op": "verify_exact"
    }
  ]
}
```

### Intentos y feedback observable

```json
[
  {
    "strategy": "DIJKSTRA",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DIJKSTRA"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 5,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 3084,
    "elapsedNanos": 3078022
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 5,
    "path": [
      0,
      2,
      5
    ]
  },
  "independent": {
    "id": "route-009-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 5,
      "reachable": true
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 3084,
  "elapsedNanos": 4987560,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 2

Se retira una conexión: deben revisarse ruta y alcanzabilidad.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-009-r2",
  "caseId": "route-009",
  "round": 2,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        8
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        9
      ],
      [
        0,
        4,
        3
      ],
      [
        0,
        5,
        10
      ],
      [
        1,
        0,
        4
      ],
      [
        1,
        2,
        5
      ],
      [
        1,
        3,
        12
      ],
      [
        1,
        4,
        6
      ],
      [
        1,
        5,
        13
      ],
      [
        2,
        0,
        7
      ],
      [
        2,
        1,
        1
      ],
      [
        2,
        3,
        2
      ],
      [
        2,
        4,
        9
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        11
      ],
      [
        3,
        4,
        12
      ],
      [
        3,
        5,
        6
      ],
      [
        4,
        0,
        13
      ],
      [
        4,
        1,
        7
      ],
      [
        4,
        2,
        1
      ],
      [
        4,
        3,
        8
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        0,
        3
      ],
      [
        5,
        1,
        10
      ],
      [
        5,
        2,
        4
      ],
      [
        5,
        3,
        11
      ],
      [
        5,
        4,
        5
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "route",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "BELLMAN_FORD"
    },
    {
      "op": "verify_exact"
    }
  ]
}
```

### Intentos y feedback observable

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 5,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 2894,
    "elapsedNanos": 2962602
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 5,
    "path": [
      0,
      2,
      5
    ]
  },
  "independent": {
    "id": "route-009-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 5,
      "reachable": true
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 2894,
  "elapsedNanos": 4905909,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 3

Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-009-r3",
  "caseId": "route-009",
  "round": 3,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        -3
      ],
      [
        0,
        2,
        -3
      ],
      [
        0,
        3,
        -7
      ],
      [
        0,
        4,
        -7
      ],
      [
        0,
        5,
        6
      ],
      [
        1,
        0,
        15
      ],
      [
        1,
        2,
        11
      ],
      [
        1,
        3,
        7
      ],
      [
        1,
        4,
        7
      ],
      [
        1,
        5,
        20
      ],
      [
        2,
        0,
        12
      ],
      [
        2,
        1,
        -5
      ],
      [
        2,
        3,
        -9
      ],
      [
        2,
        4,
        4
      ],
      [
        2,
        5,
        4
      ],
      [
        3,
        0,
        26
      ],
      [
        3,
        1,
        9
      ],
      [
        3,
        2,
        22
      ],
      [
        3,
        4,
        18
      ],
      [
        3,
        5,
        18
      ],
      [
        4,
        0,
        23
      ],
      [
        4,
        1,
        6
      ],
      [
        4,
        2,
        6
      ],
      [
        4,
        3,
        2
      ],
      [
        4,
        5,
        15
      ],
      [
        5,
        0,
        7
      ],
      [
        5,
        1,
        3
      ],
      [
        5,
        2,
        3
      ],
      [
        5,
        3,
        -1
      ],
      [
        5,
        4,
        -1
      ],
      [
        5,
        6,
        -9
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "route",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "BELLMAN_FORD"
    },
    {
      "op": "verify_exact"
    }
  ]
}
```

### Intentos y feedback observable

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 1,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 3266,
    "elapsedNanos": 3058423
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 1,
    "path": [
      0,
      2,
      5
    ]
  },
  "independent": {
    "id": "route-009-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 1,
      "reachable": true
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 3266,
  "elapsedNanos": 5006528,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 4

Cambia el destino o el sentido de la consulta y se modifica la topología.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-009-r4",
  "caseId": "route-009",
  "round": 4,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        8
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        9
      ],
      [
        0,
        4,
        3
      ],
      [
        0,
        5,
        10
      ],
      [
        1,
        0,
        4
      ],
      [
        1,
        2,
        5
      ],
      [
        1,
        3,
        12
      ],
      [
        1,
        4,
        6
      ],
      [
        1,
        5,
        13
      ],
      [
        2,
        0,
        7
      ],
      [
        2,
        1,
        1
      ],
      [
        2,
        3,
        2
      ],
      [
        2,
        4,
        9
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        0,
        10
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        11
      ],
      [
        3,
        4,
        12
      ],
      [
        3,
        5,
        6
      ],
      [
        4,
        0,
        13
      ],
      [
        4,
        1,
        7
      ],
      [
        4,
        2,
        1
      ],
      [
        4,
        3,
        8
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        0,
        3
      ],
      [
        5,
        1,
        10
      ],
      [
        5,
        2,
        4
      ],
      [
        5,
        3,
        11
      ],
      [
        5,
        4,
        5
      ],
      [
        5,
        6,
        1
      ]
    ],
    "source": 0,
    "target": 6
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "route",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "BELLMAN_FORD"
    },
    {
      "op": "verify_exact"
    }
  ]
}
```

### Intentos y feedback observable

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        2,
        5,
        6
      ]
    },
    "operations": 3900,
    "elapsedNanos": 2392725
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 6,
    "path": [
      0,
      2,
      5,
      6
    ]
  },
  "independent": {
    "id": "route-009-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 6,
      "reachable": true
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 3900,
  "elapsedNanos": 3687847,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
