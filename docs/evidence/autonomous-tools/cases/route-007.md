# route-007: Escalera con peldaños asimétricos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-007-r1",
  "caseId": "route-007",
  "round": 1,
  "family": "route",
  "description": "Escalera con peldaños asimétricos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 8,
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
        4,
        5,
        1
      ],
      [
        5,
        6,
        1
      ],
      [
        6,
        7,
        1
      ],
      [
        0,
        4,
        2
      ],
      [
        1,
        5,
        2
      ],
      [
        2,
        6,
        2
      ],
      [
        3,
        7,
        2
      ],
      [
        5,
        2,
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
      "cost": 5,
      "path": [
        0,
        4,
        5,
        6,
        7
      ]
    },
    "operations": 2080,
    "elapsedNanos": 1496348
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
      4,
      5,
      6,
      7
    ]
  },
  "independent": {
    "id": "route-007-r1",
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
  "operations": 2080,
  "elapsedNanos": 2470896,
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
  "id": "route-007-r2",
  "caseId": "route-007",
  "round": 2,
  "family": "route",
  "description": "Escalera con peldaños asimétricos",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 8,
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
        4,
        5,
        1
      ],
      [
        5,
        6,
        1
      ],
      [
        0,
        4,
        2
      ],
      [
        1,
        5,
        2
      ],
      [
        2,
        6,
        2
      ],
      [
        3,
        7,
        2
      ],
      [
        5,
        2,
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
      "cost": 9,
      "path": [
        0,
        4,
        5,
        2,
        3,
        7
      ]
    },
    "operations": 1931,
    "elapsedNanos": 1351204
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": 9,
    "path": [
      0,
      4,
      5,
      2,
      3,
      7
    ]
  },
  "independent": {
    "id": "route-007-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 9,
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
  "operations": 1931,
  "elapsedNanos": 3051743,
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
  "id": "route-007-r3",
  "caseId": "route-007",
  "round": 3,
  "family": "route",
  "description": "Escalera con peldaños asimétricos",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 8,
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
        4,
        5,
        7
      ],
      [
        5,
        6,
        -10
      ],
      [
        6,
        7,
        7
      ],
      [
        0,
        4,
        -8
      ],
      [
        1,
        5,
        9
      ],
      [
        2,
        6,
        -8
      ],
      [
        3,
        7,
        9
      ],
      [
        5,
        2,
        0
      ],
      [
        0,
        2,
        -1
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
      "cost": -4,
      "path": [
        0,
        4,
        5,
        6,
        7
      ]
    },
    "operations": 2133,
    "elapsedNanos": 1717134
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "cost": -4,
    "path": [
      0,
      4,
      5,
      6,
      7
    ]
  },
  "independent": {
    "id": "route-007-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -4,
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
  "operations": 2133,
  "elapsedNanos": 3407488,
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
  "id": "route-007-r4",
  "caseId": "route-007",
  "round": 4,
  "family": "route",
  "description": "Escalera con peldaños asimétricos",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 9,
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
        4,
        5,
        1
      ],
      [
        5,
        6,
        1
      ],
      [
        6,
        7,
        1
      ],
      [
        0,
        4,
        2
      ],
      [
        1,
        5,
        2
      ],
      [
        2,
        6,
        2
      ],
      [
        3,
        7,
        2
      ],
      [
        5,
        2,
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
      "cost": 6,
      "path": [
        0,
        4,
        5,
        6,
        7,
        8
      ]
    },
    "operations": 2789,
    "elapsedNanos": 1333760
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
      4,
      5,
      6,
      7,
      8
    ]
  },
  "independent": {
    "id": "route-007-r4",
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
  "operations": 2789,
  "elapsedNanos": 2517438,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
