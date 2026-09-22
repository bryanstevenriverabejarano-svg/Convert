# route-021: Árbol con enlaces entre ramas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-021-r1",
  "caseId": "route-021",
  "round": 1,
  "family": "route",
  "description": "Árbol con enlaces entre ramas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        3
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        3
      ],
      [
        1,
        4,
        3
      ],
      [
        2,
        5,
        3
      ],
      [
        2,
        6,
        3
      ],
      [
        3,
        7,
        3
      ],
      [
        3,
        8,
        3
      ],
      [
        4,
        9,
        3
      ],
      [
        2,
        7,
        1
      ],
      [
        7,
        9,
        1
      ],
      [
        4,
        8,
        1
      ]
    ],
    "source": 0,
    "target": 9
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
        7,
        9
      ]
    },
    "operations": 6274,
    "elapsedNanos": 1500204
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
      7,
      9
    ]
  },
  "independent": {
    "id": "route-021-r1",
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
  "operations": 6274,
  "elapsedNanos": 2315296,
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
  "id": "route-021-r2",
  "caseId": "route-021",
  "round": 2,
  "family": "route",
  "description": "Árbol con enlaces entre ramas",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        3
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        3
      ],
      [
        1,
        4,
        3
      ],
      [
        2,
        5,
        3
      ],
      [
        2,
        6,
        3
      ],
      [
        3,
        8,
        3
      ],
      [
        4,
        9,
        3
      ],
      [
        2,
        7,
        1
      ],
      [
        7,
        9,
        1
      ],
      [
        4,
        8,
        1
      ]
    ],
    "source": 0,
    "target": 9
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
        7,
        9
      ]
    },
    "operations": 6251,
    "elapsedNanos": 2308897
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
      7,
      9
    ]
  },
  "independent": {
    "id": "route-021-r2",
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
  "operations": 6251,
  "elapsedNanos": 4690732,
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
  "id": "route-021-r3",
  "caseId": "route-021",
  "round": 3,
  "family": "route",
  "description": "Árbol con enlaces entre ramas",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        -8
      ],
      [
        0,
        2,
        -2
      ],
      [
        1,
        3,
        -2
      ],
      [
        1,
        4,
        4
      ],
      [
        2,
        5,
        4
      ],
      [
        2,
        6,
        -7
      ],
      [
        3,
        7,
        10
      ],
      [
        3,
        8,
        16
      ],
      [
        4,
        9,
        -1
      ],
      [
        2,
        7,
        -3
      ],
      [
        7,
        9,
        -4
      ],
      [
        4,
        8,
        8
      ],
      [
        0,
        3,
        -12
      ]
    ],
    "source": 0,
    "target": 9
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
      "cost": -9,
      "path": [
        0,
        2,
        7,
        9
      ]
    },
    "operations": 6716,
    "elapsedNanos": 2488613
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": -9,
    "path": [
      0,
      2,
      7,
      9
    ]
  },
  "independent": {
    "id": "route-021-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -9,
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
  "operations": 6716,
  "elapsedNanos": 4343149,
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
  "id": "route-021-r4",
  "caseId": "route-021",
  "round": 4,
  "family": "route",
  "description": "Árbol con enlaces entre ramas",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 11,
    "edges": [
      [
        0,
        1,
        3
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        3
      ],
      [
        1,
        4,
        3
      ],
      [
        2,
        5,
        3
      ],
      [
        2,
        6,
        3
      ],
      [
        3,
        7,
        3
      ],
      [
        3,
        8,
        3
      ],
      [
        4,
        9,
        3
      ],
      [
        2,
        7,
        1
      ],
      [
        7,
        9,
        1
      ],
      [
        4,
        8,
        1
      ],
      [
        9,
        10,
        1
      ]
    ],
    "source": 0,
    "target": 10
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
        7,
        9,
        10
      ]
    },
    "operations": 7948,
    "elapsedNanos": 1833027
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
      7,
      9,
      10
    ]
  },
  "independent": {
    "id": "route-021-r4",
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
  "operations": 7948,
  "elapsedNanos": 3181910,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
