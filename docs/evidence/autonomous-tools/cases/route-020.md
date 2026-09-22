# route-020: Árbol dirigido con hojas irrelevantes

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-020-r1",
  "caseId": "route-020",
  "round": 1,
  "family": "route",
  "description": "Árbol dirigido con hojas irrelevantes",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 13,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        1
      ],
      [
        1,
        4,
        2
      ],
      [
        2,
        5,
        3
      ],
      [
        2,
        6,
        1
      ],
      [
        3,
        7,
        2
      ],
      [
        3,
        8,
        3
      ],
      [
        4,
        9,
        1
      ],
      [
        4,
        10,
        2
      ],
      [
        5,
        11,
        3
      ],
      [
        5,
        12,
        1
      ]
    ],
    "source": 0,
    "target": 12
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
        5,
        12
      ]
    },
    "operations": 5663,
    "elapsedNanos": 1523508
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
      5,
      12
    ]
  },
  "independent": {
    "id": "route-020-r1",
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
  "operations": 5663,
  "elapsedNanos": 2263640,
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
  "id": "route-020-r2",
  "caseId": "route-020",
  "round": 2,
  "family": "route",
  "description": "Árbol dirigido con hojas irrelevantes",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 13,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        1
      ],
      [
        1,
        4,
        2
      ],
      [
        2,
        5,
        3
      ],
      [
        2,
        6,
        1
      ],
      [
        3,
        8,
        3
      ],
      [
        4,
        9,
        1
      ],
      [
        4,
        10,
        2
      ],
      [
        5,
        11,
        3
      ],
      [
        5,
        12,
        1
      ]
    ],
    "source": 0,
    "target": 12
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
        5,
        12
      ]
    },
    "operations": 5621,
    "elapsedNanos": 2192345
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
      5,
      12
    ]
  },
  "independent": {
    "id": "route-020-r2",
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
  "operations": 5621,
  "elapsedNanos": 4189352,
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
  "id": "route-020-r3",
  "caseId": "route-020",
  "round": 3,
  "family": "route",
  "description": "Árbol dirigido con hojas irrelevantes",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 13,
    "edges": [
      [
        0,
        1,
        -9
      ],
      [
        0,
        2,
        -2
      ],
      [
        1,
        3,
        -4
      ],
      [
        1,
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
        -9
      ],
      [
        3,
        7,
        9
      ],
      [
        3,
        8,
        16
      ],
      [
        4,
        9,
        -3
      ],
      [
        4,
        10,
        4
      ],
      [
        5,
        11,
        5
      ],
      [
        5,
        12,
        -8
      ],
      [
        0,
        3,
        -12
      ]
    ],
    "source": 0,
    "target": 12
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
      "cost": -6,
      "path": [
        0,
        2,
        5,
        12
      ]
    },
    "operations": 6069,
    "elapsedNanos": 2371029
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": -6,
    "path": [
      0,
      2,
      5,
      12
    ]
  },
  "independent": {
    "id": "route-020-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -6,
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
  "operations": 6069,
  "elapsedNanos": 4096145,
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
  "id": "route-020-r4",
  "caseId": "route-020",
  "round": 4,
  "family": "route",
  "description": "Árbol dirigido con hojas irrelevantes",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 14,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        1
      ],
      [
        1,
        4,
        2
      ],
      [
        2,
        5,
        3
      ],
      [
        2,
        6,
        1
      ],
      [
        3,
        7,
        2
      ],
      [
        3,
        8,
        3
      ],
      [
        4,
        9,
        1
      ],
      [
        4,
        10,
        2
      ],
      [
        5,
        11,
        3
      ],
      [
        5,
        12,
        1
      ],
      [
        12,
        13,
        1
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
      "cost": 8,
      "path": [
        0,
        2,
        5,
        12,
        13
      ]
    },
    "operations": 7211,
    "elapsedNanos": 1714523
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 8,
    "path": [
      0,
      2,
      5,
      12,
      13
    ]
  },
  "independent": {
    "id": "route-020-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 8,
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
  "operations": 7211,
  "elapsedNanos": 2921045,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
