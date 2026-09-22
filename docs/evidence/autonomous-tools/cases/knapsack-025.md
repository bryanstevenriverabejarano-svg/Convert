# knapsack-025: Doce candidatos y recompensas dispersas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-025-r1",
  "caseId": "knapsack-025",
  "round": 1,
  "family": "knapsack",
  "description": "Doce candidatos y recompensas dispersas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 23,
    "items": [
      {
        "weight": 1,
        "value": 0
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 22
      },
      {
        "weight": 4,
        "value": 10
      },
      {
        "weight": 5,
        "value": 21
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 7,
        "value": 20
      },
      {
        "weight": 8,
        "value": 8
      },
      {
        "weight": 9,
        "value": 19
      },
      {
        "weight": 10,
        "value": 7
      },
      {
        "weight": 11,
        "value": 18
      },
      {
        "weight": 12,
        "value": 6
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
      "value": 84,
      "weight": 21,
      "selected": [
        1,
        2,
        3,
        4,
        6
      ]
    },
    "operations": 5171,
    "elapsedNanos": 890615
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 84,
    "weight": 21,
    "selected": [
      1,
      2,
      3,
      4,
      6
    ]
  },
  "independent": {
    "id": "knapsack-025-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4096,
      "optimum": 84
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 5171,
  "elapsedNanos": 1454917,
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
  "id": "knapsack-025-r2",
  "caseId": "knapsack-025",
  "round": 2,
  "family": "knapsack",
  "description": "Doce candidatos y recompensas dispersas",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 23,
    "items": [
      {
        "weight": 1,
        "value": 0
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 22
      },
      {
        "weight": 4,
        "value": 10
      },
      {
        "weight": 5,
        "value": 21
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 7,
        "value": 20
      },
      {
        "weight": 8,
        "value": 8
      },
      {
        "weight": 9,
        "value": 19
      },
      {
        "weight": 10,
        "value": 7
      },
      {
        "weight": 11,
        "value": 18
      },
      {
        "weight": 12,
        "value": 6
      },
      {
        "weight": 7,
        "value": 23
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
      "value": 87,
      "weight": 21,
      "selected": [
        1,
        2,
        3,
        4,
        12
      ]
    },
    "operations": 9584,
    "elapsedNanos": 300594
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 87,
    "weight": 21,
    "selected": [
      1,
      2,
      3,
      4,
      12
    ]
  },
  "independent": {
    "id": "knapsack-025-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 87
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 9584,
  "elapsedNanos": 751228,
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
  "id": "knapsack-025-r3",
  "caseId": "knapsack-025",
  "round": 3,
  "family": "knapsack",
  "description": "Doce candidatos y recompensas dispersas",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 24,
    "items": [
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 22
      },
      {
        "weight": 4,
        "value": 10
      },
      {
        "weight": 5,
        "value": 21
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 7,
        "value": 20
      },
      {
        "weight": 8,
        "value": 8
      },
      {
        "weight": 9,
        "value": 19
      },
      {
        "weight": 10,
        "value": 7
      },
      {
        "weight": 11,
        "value": 18
      },
      {
        "weight": 12,
        "value": 6
      },
      {
        "weight": 1,
        "value": 0
      },
      {
        "weight": 1,
        "value": 1
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
      "value": 85,
      "weight": 22,
      "selected": [
        0,
        1,
        2,
        3,
        5,
        12
      ]
    },
    "operations": 9646,
    "elapsedNanos": 1720599
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 85,
    "weight": 22,
    "selected": [
      0,
      1,
      2,
      3,
      5,
      12
    ]
  },
  "independent": {
    "id": "knapsack-025-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 85
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 9646,
  "elapsedNanos": 3302404,
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
  "id": "knapsack-025-r4",
  "caseId": "knapsack-025",
  "round": 4,
  "family": "knapsack",
  "description": "Doce candidatos y recompensas dispersas",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 21,
    "items": [
      {
        "weight": 1,
        "value": 0
      },
      {
        "weight": 2,
        "value": 11
      },
      {
        "weight": 3,
        "value": 22
      },
      {
        "weight": 4,
        "value": 10
      },
      {
        "weight": 5,
        "value": 21
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 8,
        "value": 8
      },
      {
        "weight": 9,
        "value": 19
      },
      {
        "weight": 10,
        "value": 7
      },
      {
        "weight": 11,
        "value": 18
      },
      {
        "weight": 12,
        "value": 6
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
      "value": 73,
      "weight": 19,
      "selected": [
        1,
        2,
        4,
        7
      ]
    },
    "operations": 2861,
    "elapsedNanos": 62122
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 73,
    "weight": 19,
    "selected": [
      1,
      2,
      4,
      7
    ]
  },
  "independent": {
    "id": "knapsack-025-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2048,
      "optimum": 73
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 2861,
  "elapsedNanos": 209750,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
