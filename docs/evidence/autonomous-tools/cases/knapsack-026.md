# knapsack-026: Catorce candidatos en el límite de enumeración

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-026-r1",
  "caseId": "knapsack-026",
  "round": 1,
  "family": "knapsack",
  "description": "Catorce candidatos en el límite de enumeración",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 26,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 2,
        "value": 15
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 7,
        "value": 4
      },
      {
        "weight": 1,
        "value": 17
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 5
      },
      {
        "weight": 4,
        "value": 18
      },
      {
        "weight": 5,
        "value": 12
      },
      {
        "weight": 6,
        "value": 6
      },
      {
        "weight": 7,
        "value": 19
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "knapsack",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 108,
      "weight": 26,
      "selected": [
        1,
        4,
        7,
        8,
        10,
        11,
        13
      ]
    },
    "operations": 21723,
    "elapsedNanos": 3339598
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 108,
    "weight": 26,
    "selected": [
      1,
      4,
      7,
      8,
      10,
      11,
      13
    ]
  },
  "independent": {
    "id": "knapsack-026-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 108
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 21723,
  "elapsedNanos": 3826857,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 2

Aparece una nueva alternativa de alta recompensa; recalcular la selección.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-026-r2",
  "caseId": "knapsack-026",
  "round": 2,
  "family": "knapsack",
  "description": "Catorce candidatos en el límite de enumeración",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 26,
    "items": [
      {
        "weight": 2,
        "value": 15
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 7,
        "value": 4
      },
      {
        "weight": 1,
        "value": 17
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 5
      },
      {
        "weight": 4,
        "value": 18
      },
      {
        "weight": 5,
        "value": 12
      },
      {
        "weight": 6,
        "value": 6
      },
      {
        "weight": 7,
        "value": 19
      },
      {
        "weight": 8,
        "value": 20
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "knapsack",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 108,
      "weight": 26,
      "selected": [
        0,
        3,
        6,
        7,
        9,
        10,
        12
      ]
    },
    "operations": 25977,
    "elapsedNanos": 570271
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 108,
    "weight": 26,
    "selected": [
      0,
      3,
      6,
      7,
      9,
      10,
      12
    ]
  },
  "independent": {
    "id": "knapsack-026-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 108
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 25977,
  "elapsedNanos": 1015559,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 3

Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-026-r3",
  "caseId": "knapsack-026",
  "round": 3,
  "family": "knapsack",
  "description": "Catorce candidatos en el límite de enumeración",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 27,
    "items": [
      {
        "weight": 2,
        "value": 15
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 7,
        "value": 4
      },
      {
        "weight": 1,
        "value": 17
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 5
      },
      {
        "weight": 4,
        "value": 18
      },
      {
        "weight": 5,
        "value": 12
      },
      {
        "weight": 6,
        "value": 6
      },
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 1,
        "value": 2
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "knapsack",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 106,
      "weight": 27,
      "selected": [
        0,
        1,
        3,
        6,
        7,
        8,
        9,
        10,
        12,
        13
      ]
    },
    "operations": 26053,
    "elapsedNanos": 2656300
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 106,
    "weight": 27,
    "selected": [
      0,
      1,
      3,
      6,
      7,
      8,
      9,
      10,
      12,
      13
    ]
  },
  "independent": {
    "id": "knapsack-026-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 106
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 26053,
  "elapsedNanos": 4339404,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 4

Un recurso deja de estar disponible y disminuye el presupuesto.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-026-r4",
  "caseId": "knapsack-026",
  "round": 4,
  "family": "knapsack",
  "description": "Catorce candidatos en el límite de enumeración",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 24,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 2,
        "value": 15
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 7,
        "value": 4
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 5
      },
      {
        "weight": 4,
        "value": 18
      },
      {
        "weight": 5,
        "value": 12
      },
      {
        "weight": 6,
        "value": 6
      },
      {
        "weight": 7,
        "value": 19
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "knapsack",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 90,
      "weight": 24,
      "selected": [
        0,
        1,
        2,
        4,
        7,
        9,
        12
      ]
    },
    "operations": 11259,
    "elapsedNanos": 113748
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 90,
    "weight": 24,
    "selected": [
      0,
      1,
      2,
      4,
      7,
      9,
      12
    ]
  },
  "independent": {
    "id": "knapsack-026-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 90
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 11259,
  "elapsedNanos": 291610,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
