# route-024: Destino aislado y muchas ramas alcanzables

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "route-024-r1",
  "caseId": "route-024",
  "round": 1,
  "family": "route",
  "description": "Destino aislado y muchas ramas alcanzables",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 14,
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
        0,
        4,
        0
      ],
      [
        0,
        5,
        1
      ],
      [
        0,
        6,
        2
      ],
      [
        0,
        7,
        3
      ],
      [
        0,
        8,
        0
      ],
      [
        0,
        9,
        1
      ],
      [
        10,
        11,
        1
      ],
      [
        11,
        12,
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
      "status": "unreachable",
      "path": []
    },
    "operations": 6544,
    "elapsedNanos": 1253760
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
    "id": "route-024-r1",
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
  "operations": 6544,
  "elapsedNanos": 1756061,
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
  "id": "route-024-r2",
  "caseId": "route-024",
  "round": 2,
  "family": "route",
  "description": "Destino aislado y muchas ramas alcanzables",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 14,
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
        0,
        4,
        0
      ],
      [
        0,
        5,
        1
      ],
      [
        0,
        7,
        3
      ],
      [
        0,
        8,
        0
      ],
      [
        0,
        9,
        1
      ],
      [
        10,
        11,
        1
      ],
      [
        11,
        12,
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
      "status": "unreachable",
      "path": []
    },
    "operations": 6524,
    "elapsedNanos": 308956
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
    "id": "route-024-r2",
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
  "operations": 6524,
  "elapsedNanos": 5967986,
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
  "id": "route-024-r3",
  "caseId": "route-024",
  "round": 3,
  "family": "route",
  "description": "Destino aislado y muchas ramas alcanzables",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 14,
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
        0,
        4,
        -10
      ],
      [
        0,
        5,
        -3
      ],
      [
        0,
        6,
        -13
      ],
      [
        0,
        7,
        -6
      ],
      [
        0,
        8,
        -3
      ],
      [
        0,
        9,
        -13
      ],
      [
        10,
        11,
        7
      ],
      [
        11,
        12,
        -10
      ],
      [
        0,
        10,
        -4
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
      "status": "unreachable",
      "path": []
    },
    "operations": 6994,
    "elapsedNanos": 2252845
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
    "id": "route-024-r3",
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
  "operations": 6994,
  "elapsedNanos": 4329509,
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
  "id": "route-024-r4",
  "caseId": "route-024",
  "round": 4,
  "family": "route",
  "description": "Destino aislado y muchas ramas alcanzables",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 14,
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
        0,
        4,
        0
      ],
      [
        0,
        5,
        1
      ],
      [
        0,
        6,
        2
      ],
      [
        0,
        7,
        3
      ],
      [
        0,
        8,
        0
      ],
      [
        0,
        9,
        1
      ],
      [
        10,
        11,
        1
      ],
      [
        11,
        12,
        1
      ],
      [
        0,
        10,
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
    "operations": 7321,
    "elapsedNanos": 1994336
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
    "id": "route-024-r4",
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
  "operations": 7321,
  "elapsedNanos": 3232095,
  "version": 3,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
