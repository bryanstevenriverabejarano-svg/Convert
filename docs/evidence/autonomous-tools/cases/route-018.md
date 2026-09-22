# route-018: Vecino barato conduce a un callejón costoso

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-018-r1",
  "caseId": "route-018",
  "round": 1,
  "family": "route",
  "description": "Vecino barato conduce a un callejón costoso",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        0
      ],
      [
        1,
        2,
        0
      ],
      [
        2,
        6,
        30
      ],
      [
        0,
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
      "cost": 12,
      "path": [
        0,
        3,
        4,
        5,
        6
      ]
    },
    "operations": 2444,
    "elapsedNanos": 1222474
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 12,
    "path": [
      0,
      3,
      4,
      5,
      6
    ]
  },
  "independent": {
    "id": "route-018-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 12,
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
  "operations": 2444,
  "elapsedNanos": 2124245,
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
  "id": "route-018-r2",
  "caseId": "route-018",
  "round": 2,
  "family": "route",
  "description": "Vecino barato conduce a un callejón costoso",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        0
      ],
      [
        1,
        2,
        0
      ],
      [
        2,
        6,
        30
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
      "cost": 30,
      "path": [
        0,
        1,
        2,
        6
      ]
    },
    "operations": 2258,
    "elapsedNanos": 1163547
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 30,
    "path": [
      0,
      1,
      2,
      6
    ]
  },
  "independent": {
    "id": "route-018-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 30,
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
  "operations": 2258,
  "elapsedNanos": 2566508,
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
  "id": "route-018-r3",
  "caseId": "route-018",
  "round": 3,
  "family": "route",
  "description": "Vecino barato conduce a un callejón costoso",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        -11
      ],
      [
        1,
        2,
        6
      ],
      [
        2,
        6,
        20
      ],
      [
        0,
        3,
        -13
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
        0,
        2,
        -1
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
      "cost": -3,
      "path": [
        0,
        3,
        4,
        5,
        6
      ]
    },
    "operations": 2723,
    "elapsedNanos": 1793587
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": -3,
    "path": [
      0,
      3,
      4,
      5,
      6
    ]
  },
  "independent": {
    "id": "route-018-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -3,
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
  "operations": 2723,
  "elapsedNanos": 3538271,
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
  "id": "route-018-r4",
  "caseId": "route-018",
  "round": 4,
  "family": "route",
  "description": "Vecino barato conduce a un callejón costoso",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        0
      ],
      [
        1,
        2,
        0
      ],
      [
        2,
        6,
        30
      ],
      [
        0,
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
      "cost": 13,
      "path": [
        0,
        3,
        4,
        5,
        6,
        7
      ]
    },
    "operations": 3364,
    "elapsedNanos": 1433358
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 13,
    "path": [
      0,
      3,
      4,
      5,
      6,
      7
    ]
  },
  "independent": {
    "id": "route-018-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 13,
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
  "operations": 3364,
  "elapsedNanos": 2654801,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
