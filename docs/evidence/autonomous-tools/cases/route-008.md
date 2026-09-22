# route-008: Cuadrícula dirigida con casillas de diferente coste

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-008-r1",
  "caseId": "route-008",
  "round": 1,
  "family": "route",
  "description": "Cuadrícula dirigida con casillas de diferente coste",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        2
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
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
        2
      ],
      [
        0,
        3,
        2
      ],
      [
        1,
        4,
        2
      ],
      [
        2,
        5,
        2
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        7,
        2
      ],
      [
        5,
        8,
        2
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
      "cost": 7,
      "path": [
        0,
        1,
        2,
        5,
        8
      ]
    },
    "operations": 2826,
    "elapsedNanos": 1562856
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
      1,
      2,
      5,
      8
    ]
  },
  "independent": {
    "id": "route-008-r1",
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
  "operations": 2826,
  "elapsedNanos": 2600046,
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
  "id": "route-008-r2",
  "caseId": "route-008",
  "round": 2,
  "family": "route",
  "description": "Cuadrícula dirigida con casillas de diferente coste",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        2
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
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
        2
      ],
      [
        1,
        4,
        2
      ],
      [
        2,
        5,
        2
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        7,
        2
      ],
      [
        5,
        8,
        2
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
      "cost": 7,
      "path": [
        0,
        1,
        2,
        5,
        8
      ]
    },
    "operations": 2639,
    "elapsedNanos": 1609495
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
      1,
      2,
      5,
      8
    ]
  },
  "independent": {
    "id": "route-008-r2",
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
  "operations": 2639,
  "elapsedNanos": 3441098,
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
  "id": "route-008-r3",
  "caseId": "route-008",
  "round": 3,
  "family": "route",
  "description": "Cuadrícula dirigida con casillas de diferente coste",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        1,
        -10
      ],
      [
        1,
        2,
        8
      ],
      [
        3,
        4,
        7
      ],
      [
        4,
        5,
        8
      ],
      [
        6,
        7,
        7
      ],
      [
        7,
        8,
        8
      ],
      [
        0,
        3,
        -14
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
        3,
        6,
        3
      ],
      [
        4,
        7,
        3
      ],
      [
        5,
        8,
        3
      ],
      [
        0,
        2,
        -1
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
      "cost": 4,
      "path": [
        0,
        1,
        2,
        5,
        8
      ]
    },
    "operations": 2879,
    "elapsedNanos": 1720979
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
      1,
      2,
      5,
      8
    ]
  },
  "independent": {
    "id": "route-008-r3",
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
  "operations": 2879,
  "elapsedNanos": 3361670,
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
  "id": "route-008-r4",
  "caseId": "route-008",
  "round": 4,
  "family": "route",
  "description": "Cuadrícula dirigida con casillas de diferente coste",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        2
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
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
        2
      ],
      [
        0,
        3,
        2
      ],
      [
        1,
        4,
        2
      ],
      [
        2,
        5,
        2
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        7,
        2
      ],
      [
        5,
        8,
        2
      ],
      [
        8,
        9,
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
      "cost": 8,
      "path": [
        0,
        1,
        2,
        5,
        8,
        9
      ]
    },
    "operations": 3649,
    "elapsedNanos": 1769464
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
      1,
      2,
      5,
      8,
      9
    ]
  },
  "independent": {
    "id": "route-008-r4",
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
  "operations": 3649,
  "elapsedNanos": 2973673,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
