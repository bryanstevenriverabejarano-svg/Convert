# route-026: Límite de vértices y atajo interior decisivo

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-026-r1",
  "caseId": "route-026",
  "round": 1,
  "family": "route",
  "description": "Límite de vértices y atajo interior decisivo",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 14,
    "edges": [
      [
        0,
        1,
        3
      ],
      [
        1,
        2,
        3
      ],
      [
        2,
        3,
        3
      ],
      [
        3,
        4,
        3
      ],
      [
        4,
        5,
        3
      ],
      [
        5,
        6,
        3
      ],
      [
        6,
        7,
        3
      ],
      [
        7,
        8,
        3
      ],
      [
        8,
        9,
        3
      ],
      [
        9,
        10,
        3
      ],
      [
        10,
        11,
        3
      ],
      [
        11,
        12,
        3
      ],
      [
        12,
        13,
        3
      ],
      [
        1,
        11,
        1
      ],
      [
        0,
        13,
        40
      ],
      [
        8,
        3,
        2
      ]
    ],
    "source": 0,
    "target": 13
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
      "cost": 10,
      "path": [
        0,
        1,
        11,
        12,
        13
      ]
    },
    "operations": 8289,
    "elapsedNanos": 1088696
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 10,
    "path": [
      0,
      1,
      11,
      12,
      13
    ]
  },
  "independent": {
    "id": "route-026-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 10,
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
  "operations": 8289,
  "elapsedNanos": 1558339,
  "version": 3,
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
  "id": "route-026-r2",
  "caseId": "route-026",
  "round": 2,
  "family": "route",
  "description": "Límite de vértices y atajo interior decisivo",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 14,
    "edges": [
      [
        0,
        1,
        3
      ],
      [
        1,
        2,
        3
      ],
      [
        2,
        3,
        3
      ],
      [
        3,
        4,
        3
      ],
      [
        4,
        5,
        3
      ],
      [
        5,
        6,
        3
      ],
      [
        6,
        7,
        3
      ],
      [
        7,
        8,
        3
      ],
      [
        9,
        10,
        3
      ],
      [
        10,
        11,
        3
      ],
      [
        11,
        12,
        3
      ],
      [
        12,
        13,
        3
      ],
      [
        1,
        11,
        1
      ],
      [
        0,
        13,
        40
      ],
      [
        8,
        3,
        2
      ]
    ],
    "source": 0,
    "target": 13
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
      "cost": 10,
      "path": [
        0,
        1,
        11,
        12,
        13
      ]
    },
    "operations": 8269,
    "elapsedNanos": 281735
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 10,
    "path": [
      0,
      1,
      11,
      12,
      13
    ]
  },
  "independent": {
    "id": "route-026-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 10,
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
  "operations": 8269,
  "elapsedNanos": 784968,
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
  "id": "route-026-r3",
  "caseId": "route-026",
  "round": 3,
  "family": "route",
  "description": "Límite de vértices y atajo interior decisivo",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 14,
    "edges": [
      [
        0,
        1,
        -8
      ],
      [
        1,
        2,
        9
      ],
      [
        2,
        3,
        -8
      ],
      [
        3,
        4,
        9
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        6,
        -8
      ],
      [
        6,
        7,
        9
      ],
      [
        7,
        8,
        9
      ],
      [
        8,
        9,
        -8
      ],
      [
        9,
        10,
        9
      ],
      [
        10,
        11,
        9
      ],
      [
        11,
        12,
        -8
      ],
      [
        12,
        13,
        9
      ],
      [
        1,
        11,
        10
      ],
      [
        0,
        13,
        33
      ],
      [
        8,
        3,
        -11
      ],
      [
        0,
        2,
        -1
      ]
    ],
    "source": 0,
    "target": 13
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
      "cost": 3,
      "path": [
        0,
        1,
        11,
        12,
        13
      ]
    },
    "operations": 9008,
    "elapsedNanos": 2306714
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 3,
    "path": [
      0,
      1,
      11,
      12,
      13
    ]
  },
  "independent": {
    "id": "route-026-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 3,
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
  "operations": 9008,
  "elapsedNanos": 4052700,
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
  "id": "route-026-r4",
  "caseId": "route-026",
  "round": 4,
  "family": "route",
  "description": "Límite de vértices y atajo interior decisivo",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 14,
    "edges": [
      [
        0,
        1,
        3
      ],
      [
        1,
        2,
        3
      ],
      [
        2,
        3,
        3
      ],
      [
        3,
        4,
        3
      ],
      [
        4,
        5,
        3
      ],
      [
        5,
        6,
        3
      ],
      [
        6,
        7,
        3
      ],
      [
        7,
        8,
        3
      ],
      [
        8,
        9,
        3
      ],
      [
        9,
        10,
        3
      ],
      [
        10,
        11,
        3
      ],
      [
        11,
        12,
        3
      ],
      [
        12,
        13,
        3
      ],
      [
        1,
        11,
        1
      ],
      [
        0,
        13,
        40
      ],
      [
        8,
        3,
        2
      ],
      [
        0,
        2,
        4
      ]
    ],
    "source": 13,
    "target": 0
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
      "status": "unreachable",
      "path": []
    },
    "operations": 8363,
    "elapsedNanos": 1530411
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "unreachable",
    "path": []
  },
  "independent": {
    "id": "route-026-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "reachable": false
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 8363,
  "elapsedNanos": 2737173,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
