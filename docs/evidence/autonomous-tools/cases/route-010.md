# route-010: Capas bipartitas con emparejamientos de coste desigual

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-010-r1",
  "caseId": "route-010",
  "round": 1,
  "family": "route",
  "description": "Capas bipartitas con emparejamientos de coste desigual",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        3
      ],
      [
        1,
        4,
        2
      ],
      [
        1,
        5,
        3
      ],
      [
        1,
        6,
        4
      ],
      [
        2,
        4,
        3
      ],
      [
        2,
        5,
        4
      ],
      [
        2,
        6,
        1
      ],
      [
        3,
        4,
        4
      ],
      [
        3,
        5,
        1
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        7,
        3
      ],
      [
        5,
        7,
        2
      ],
      [
        6,
        7,
        1
      ]
    ],
    "source": 0,
    "target": 7
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
      "cost": 4,
      "path": [
        0,
        2,
        6,
        7
      ]
    },
    "operations": 3540,
    "elapsedNanos": 2609240
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 4,
    "path": [
      0,
      2,
      6,
      7
    ]
  },
  "independent": {
    "id": "route-010-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 4,
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
  "operations": 3540,
  "elapsedNanos": 3889029,
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
  "id": "route-010-r2",
  "caseId": "route-010",
  "round": 2,
  "family": "route",
  "description": "Capas bipartitas con emparejamientos de coste desigual",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        3
      ],
      [
        1,
        4,
        2
      ],
      [
        1,
        5,
        3
      ],
      [
        1,
        6,
        4
      ],
      [
        2,
        4,
        3
      ],
      [
        2,
        6,
        1
      ],
      [
        3,
        4,
        4
      ],
      [
        3,
        5,
        1
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        7,
        3
      ],
      [
        5,
        7,
        2
      ],
      [
        6,
        7,
        1
      ]
    ],
    "source": 0,
    "target": 7
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
      "cost": 4,
      "path": [
        0,
        2,
        6,
        7
      ]
    },
    "operations": 3320,
    "elapsedNanos": 3024653
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 4,
    "path": [
      0,
      2,
      6,
      7
    ]
  },
  "independent": {
    "id": "route-010-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 4,
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
  "operations": 3320,
  "elapsedNanos": 4955061,
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
  "id": "route-010-r3",
  "caseId": "route-010",
  "round": 3,
  "family": "route",
  "description": "Capas bipartitas con emparejamientos de coste desigual",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        -10
      ],
      [
        0,
        2,
        -3
      ],
      [
        0,
        3,
        -13
      ],
      [
        1,
        4,
        3
      ],
      [
        1,
        5,
        10
      ],
      [
        1,
        6,
        0
      ],
      [
        2,
        4,
        -2
      ],
      [
        2,
        5,
        5
      ],
      [
        2,
        6,
        -9
      ],
      [
        3,
        4,
        10
      ],
      [
        3,
        5,
        13
      ],
      [
        3,
        6,
        3
      ],
      [
        4,
        7,
        4
      ],
      [
        5,
        7,
        -3
      ],
      [
        6,
        7,
        7
      ],
      [
        0,
        4,
        -6
      ]
    ],
    "source": 0,
    "target": 7
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
      "cost": -5,
      "path": [
        0,
        2,
        6,
        7
      ]
    },
    "operations": 3723,
    "elapsedNanos": 3210768
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": -5,
    "path": [
      0,
      2,
      6,
      7
    ]
  },
  "independent": {
    "id": "route-010-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -5,
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
  "operations": 3723,
  "elapsedNanos": 5200834,
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
  "id": "route-010-r4",
  "caseId": "route-010",
  "round": 4,
  "family": "route",
  "description": "Capas bipartitas con emparejamientos de coste desigual",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        3
      ],
      [
        1,
        4,
        2
      ],
      [
        1,
        5,
        3
      ],
      [
        1,
        6,
        4
      ],
      [
        2,
        4,
        3
      ],
      [
        2,
        5,
        4
      ],
      [
        2,
        6,
        1
      ],
      [
        3,
        4,
        4
      ],
      [
        3,
        5,
        1
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        7,
        3
      ],
      [
        5,
        7,
        2
      ],
      [
        6,
        7,
        1
      ],
      [
        7,
        8,
        1
      ]
    ],
    "source": 0,
    "target": 8
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
        6,
        7,
        8
      ]
    },
    "operations": 4428,
    "elapsedNanos": 2331984
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
      6,
      7,
      8
    ]
  },
  "independent": {
    "id": "route-010-r4",
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
  "operations": 4428,
  "elapsedNanos": 3894041,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
