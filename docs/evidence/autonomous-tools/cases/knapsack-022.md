# knapsack-022: Pesos en progresión y valores no monótonos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-022-r1",
  "caseId": "knapsack-022",
  "round": 1,
  "family": "knapsack",
  "description": "Pesos en progresión y valores no monótonos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 21,
    "items": [
      {
        "weight": 2,
        "value": 8
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 6,
        "value": 15
      },
      {
        "weight": 8,
        "value": 6
      },
      {
        "weight": 10,
        "value": 23
      },
      {
        "weight": 12,
        "value": 9
      },
      {
        "weight": 14,
        "value": 30
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
      "value": 46,
      "weight": 18,
      "selected": [
        0,
        2,
        4
      ]
    },
    "operations": 1352,
    "elapsedNanos": 394763
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 46,
    "weight": 18,
    "selected": [
      0,
      2,
      4
    ]
  },
  "independent": {
    "id": "knapsack-022-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 46
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1352,
  "elapsedNanos": 919657,
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
  "id": "knapsack-022-r2",
  "caseId": "knapsack-022",
  "round": 2,
  "family": "knapsack",
  "description": "Pesos en progresión y valores no monótonos",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 21,
    "items": [
      {
        "weight": 2,
        "value": 8
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 6,
        "value": 15
      },
      {
        "weight": 8,
        "value": 6
      },
      {
        "weight": 10,
        "value": 23
      },
      {
        "weight": 12,
        "value": 9
      },
      {
        "weight": 14,
        "value": 30
      },
      {
        "weight": 7,
        "value": 31
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
      "value": 62,
      "weight": 19,
      "selected": [
        0,
        4,
        7
      ]
    },
    "operations": 1914,
    "elapsedNanos": 162430
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 62,
    "weight": 19,
    "selected": [
      0,
      4,
      7
    ]
  },
  "independent": {
    "id": "knapsack-022-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 62
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1914,
  "elapsedNanos": 539797,
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
  "id": "knapsack-022-r3",
  "caseId": "knapsack-022",
  "round": 3,
  "family": "knapsack",
  "description": "Pesos en progresión y valores no monótonos",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 22,
    "items": [
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 6,
        "value": 15
      },
      {
        "weight": 8,
        "value": 6
      },
      {
        "weight": 10,
        "value": 23
      },
      {
        "weight": 12,
        "value": 9
      },
      {
        "weight": 14,
        "value": 30
      },
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 1,
        "value": 5
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
      "value": 54,
      "weight": 22,
      "selected": [
        1,
        5,
        6,
        7
      ]
    },
    "operations": 1983,
    "elapsedNanos": 1215393
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 54,
    "weight": 22,
    "selected": [
      1,
      5,
      6,
      7
    ]
  },
  "independent": {
    "id": "knapsack-022-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 54
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1983,
  "elapsedNanos": 2976522,
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
  "id": "knapsack-022-r4",
  "caseId": "knapsack-022",
  "round": 4,
  "family": "knapsack",
  "description": "Pesos en progresión y valores no monótonos",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 19,
    "items": [
      {
        "weight": 2,
        "value": 8
      },
      {
        "weight": 4,
        "value": 3
      },
      {
        "weight": 6,
        "value": 15
      },
      {
        "weight": 10,
        "value": 23
      },
      {
        "weight": 12,
        "value": 9
      },
      {
        "weight": 14,
        "value": 30
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
      "value": 46,
      "weight": 18,
      "selected": [
        0,
        2,
        3
      ]
    },
    "operations": 953,
    "elapsedNanos": 48001
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 46,
    "weight": 18,
    "selected": [
      0,
      2,
      3
    ]
  },
  "independent": {
    "id": "knapsack-022-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 46
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 953,
  "elapsedNanos": 210190,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
