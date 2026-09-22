# route-013: DAG con descuento negativo que invalida Dijkstra ingenuo

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-013-r1",
  "caseId": "route-013",
  "round": 1,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        0,
        2,
        5
      ],
      [
        2,
        1,
        -7
      ],
      [
        1,
        3,
        3
      ],
      [
        3,
        5,
        1
      ],
      [
        2,
        4,
        0
      ],
      [
        4,
        5,
        2
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
    "passed": false,
    "regressionChecks": 0,
    "feedback": "Datos o precondiciones incompatibles con el candidato",
    "operations": 140,
    "elapsedNanos": 247284
  },
  {
    "strategy": "FEWEST_EDGES",
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
          "strategy": "FEWEST_EDGES"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        1,
        3,
        5
      ]
    },
    "operations": 452,
    "elapsedNanos": 285191
  },
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
      "cost": 2,
      "path": [
        0,
        2,
        1,
        3,
        5
      ]
    },
    "operations": 2155,
    "elapsedNanos": 1781719
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 2,
    "path": [
      0,
      2,
      1,
      3,
      5
    ]
  },
  "independent": {
    "id": "route-013-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 2,
      "reachable": true
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Descarté candidatos que fallaron el contrato y verifiqué una alternativa con regresiones.",
  "operations": 2747,
  "elapsedNanos": 6692476,
  "version": 3,
  "adapted": true,
  "reused": false,
  "promoted": true,
  "previousRejected": true,
  "status": "verified"
}
```

## Ronda 2

Se retira una conexión: deben revisarse ruta y alcanzabilidad.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-013-r2",
  "caseId": "route-013",
  "round": 2,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        0,
        2,
        5
      ],
      [
        2,
        1,
        -7
      ],
      [
        3,
        5,
        1
      ],
      [
        2,
        4,
        0
      ],
      [
        4,
        5,
        2
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
      "cost": 7,
      "path": [
        0,
        2,
        4,
        5
      ]
    },
    "operations": 2058,
    "elapsedNanos": 1870320
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 7,
    "path": [
      0,
      2,
      4,
      5
    ]
  },
  "independent": {
    "id": "route-013-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 7,
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
  "operations": 2058,
  "elapsedNanos": 4351232,
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
  "id": "route-013-r3",
  "caseId": "route-013",
  "round": 3,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        -9
      ],
      [
        0,
        2,
        0
      ],
      [
        2,
        1,
        1
      ],
      [
        1,
        3,
        -2
      ],
      [
        3,
        5,
        13
      ],
      [
        2,
        4,
        -5
      ],
      [
        4,
        5,
        8
      ],
      [
        0,
        3,
        -12
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
        3,
        5
      ]
    },
    "operations": 2234,
    "elapsedNanos": 1791734
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
      3,
      5
    ]
  },
  "independent": {
    "id": "route-013-r3",
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
  "operations": 2234,
  "elapsedNanos": 3536319,
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
  "id": "route-013-r4",
  "caseId": "route-013",
  "round": 4,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        0,
        2,
        5
      ],
      [
        2,
        1,
        -7
      ],
      [
        1,
        3,
        3
      ],
      [
        3,
        5,
        1
      ],
      [
        2,
        4,
        0
      ],
      [
        4,
        5,
        2
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
      "cost": 3,
      "path": [
        0,
        2,
        1,
        3,
        5,
        6
      ]
    },
    "operations": 3001,
    "elapsedNanos": 1470412
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
      2,
      1,
      3,
      5,
      6
    ]
  },
  "independent": {
    "id": "route-013-r4",
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
  "operations": 3001,
  "elapsedNanos": 2643786,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
