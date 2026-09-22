# route-019: Cadena larga con saltos de distintas longitudes

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-019-r1",
  "caseId": "route-019",
  "round": 1,
  "family": "route",
  "description": "Cadena larga con saltos de distintas longitudes",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        1,
        2,
        2
      ],
      [
        2,
        3,
        2
      ],
      [
        3,
        4,
        2
      ],
      [
        4,
        5,
        2
      ],
      [
        5,
        6,
        2
      ],
      [
        6,
        7,
        2
      ],
      [
        7,
        8,
        2
      ],
      [
        8,
        9,
        2
      ],
      [
        0,
        3,
        3
      ],
      [
        3,
        7,
        2
      ],
      [
        1,
        6,
        2
      ],
      [
        6,
        9,
        1
      ],
      [
        7,
        9,
        8
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
        1,
        6,
        9
      ]
    },
    "operations": 3139,
    "elapsedNanos": 1511661
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
      1,
      6,
      9
    ]
  },
  "independent": {
    "id": "route-019-r1",
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
  "operations": 3139,
  "elapsedNanos": 2305172,
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
  "id": "route-019-r2",
  "caseId": "route-019",
  "round": 2,
  "family": "route",
  "description": "Cadena larga con saltos de distintas longitudes",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        1,
        2,
        2
      ],
      [
        2,
        3,
        2
      ],
      [
        3,
        4,
        2
      ],
      [
        4,
        5,
        2
      ],
      [
        5,
        6,
        2
      ],
      [
        6,
        7,
        2
      ],
      [
        8,
        9,
        2
      ],
      [
        0,
        3,
        3
      ],
      [
        3,
        7,
        2
      ],
      [
        1,
        6,
        2
      ],
      [
        6,
        9,
        1
      ],
      [
        7,
        9,
        8
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
        1,
        6,
        9
      ]
    },
    "operations": 3068,
    "elapsedNanos": 1994262
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
      1,
      6,
      9
    ]
  },
  "independent": {
    "id": "route-019-r2",
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
  "operations": 3068,
  "elapsedNanos": 3914736,
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
  "id": "route-019-r3",
  "caseId": "route-019",
  "round": 3,
  "family": "route",
  "description": "Cadena larga con saltos de distintas longitudes",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        1,
        -9
      ],
      [
        1,
        2,
        8
      ],
      [
        2,
        3,
        -9
      ],
      [
        3,
        4,
        8
      ],
      [
        4,
        5,
        8
      ],
      [
        5,
        6,
        -9
      ],
      [
        6,
        7,
        8
      ],
      [
        7,
        8,
        8
      ],
      [
        8,
        9,
        -9
      ],
      [
        0,
        3,
        -13
      ],
      [
        3,
        7,
        9
      ],
      [
        1,
        6,
        -2
      ],
      [
        6,
        9,
        2
      ],
      [
        7,
        9,
        3
      ],
      [
        0,
        2,
        -1
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
        1,
        6,
        9
      ]
    },
    "operations": 3369,
    "elapsedNanos": 1936428
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
      1,
      6,
      9
    ]
  },
  "independent": {
    "id": "route-019-r3",
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
  "operations": 3369,
  "elapsedNanos": 3689205,
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
  "id": "route-019-r4",
  "caseId": "route-019",
  "round": 4,
  "family": "route",
  "description": "Cadena larga con saltos de distintas longitudes",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 11,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        1,
        2,
        2
      ],
      [
        2,
        3,
        2
      ],
      [
        3,
        4,
        2
      ],
      [
        4,
        5,
        2
      ],
      [
        5,
        6,
        2
      ],
      [
        6,
        7,
        2
      ],
      [
        7,
        8,
        2
      ],
      [
        8,
        9,
        2
      ],
      [
        0,
        3,
        3
      ],
      [
        3,
        7,
        2
      ],
      [
        1,
        6,
        2
      ],
      [
        6,
        9,
        1
      ],
      [
        7,
        9,
        8
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
        1,
        6,
        9,
        10
      ]
    },
    "operations": 4194,
    "elapsedNanos": 1627604
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
      1,
      6,
      9,
      10
    ]
  },
  "independent": {
    "id": "route-019-r4",
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
  "operations": 4194,
  "elapsedNanos": 2942496,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
