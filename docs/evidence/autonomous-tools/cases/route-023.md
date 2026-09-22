# route-023: Dos diamantes unidos por una articulación

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-023-r1",
  "caseId": "route-023",
  "round": 1,
  "family": "route",
  "description": "Dos diamantes unidos por una articulación",
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
        0,
        2,
        2
      ],
      [
        1,
        3,
        3
      ],
      [
        2,
        3,
        1
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
        5
      ],
      [
        4,
        6,
        1
      ],
      [
        5,
        7,
        1
      ],
      [
        6,
        7,
        2
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
      "cost": 8,
      "path": [
        0,
        2,
        3,
        4,
        6,
        7,
        8
      ]
    },
    "operations": 5931,
    "elapsedNanos": 1241782
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
      3,
      4,
      6,
      7,
      8
    ]
  },
  "independent": {
    "id": "route-023-r1",
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
  "operations": 5931,
  "elapsedNanos": 1899854,
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
  "id": "route-023-r2",
  "caseId": "route-023",
  "round": 2,
  "family": "route",
  "description": "Dos diamantes unidos por una articulación",
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
        0,
        2,
        2
      ],
      [
        1,
        3,
        3
      ],
      [
        2,
        3,
        1
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        6,
        1
      ],
      [
        5,
        7,
        1
      ],
      [
        6,
        7,
        2
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
      "cost": 8,
      "path": [
        0,
        2,
        3,
        4,
        6,
        7,
        8
      ]
    },
    "operations": 5911,
    "elapsedNanos": 1894976
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
      3,
      4,
      6,
      7,
      8
    ]
  },
  "independent": {
    "id": "route-023-r2",
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
  "operations": 5911,
  "elapsedNanos": 3675524,
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
  "id": "route-023-r3",
  "caseId": "route-023",
  "round": 3,
  "family": "route",
  "description": "Dos diamantes unidos por una articulación",
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
        0,
        2,
        -3
      ],
      [
        1,
        3,
        -2
      ],
      [
        2,
        3,
        -10
      ],
      [
        3,
        4,
        7
      ],
      [
        4,
        5,
        11
      ],
      [
        4,
        6,
        -4
      ],
      [
        5,
        7,
        -4
      ],
      [
        6,
        7,
        8
      ],
      [
        7,
        8,
        7
      ],
      [
        0,
        3,
        -12
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
        3,
        4,
        6,
        7,
        8
      ]
    },
    "operations": 6354,
    "elapsedNanos": 2370598
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
      3,
      4,
      6,
      7,
      8
    ]
  },
  "independent": {
    "id": "route-023-r3",
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
  "operations": 6354,
  "elapsedNanos": 4113571,
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
  "id": "route-023-r4",
  "caseId": "route-023",
  "round": 4,
  "family": "route",
  "description": "Dos diamantes unidos por una articulación",
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
        0,
        2,
        2
      ],
      [
        1,
        3,
        3
      ],
      [
        2,
        3,
        1
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
        5
      ],
      [
        4,
        6,
        1
      ],
      [
        5,
        7,
        1
      ],
      [
        6,
        7,
        2
      ],
      [
        7,
        8,
        1
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
      "cost": 9,
      "path": [
        0,
        2,
        3,
        4,
        6,
        7,
        8,
        9
      ]
    },
    "operations": 7539,
    "elapsedNanos": 1813719
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
      2,
      3,
      4,
      6,
      7,
      8,
      9
    ]
  },
  "independent": {
    "id": "route-023-r4",
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
  "operations": 7539,
  "elapsedNanos": 2956497,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
