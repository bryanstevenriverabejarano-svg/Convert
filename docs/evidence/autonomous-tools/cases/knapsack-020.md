# knapsack-020: Muchas piezas pequeñas y dos alternativas medianas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-020-r1",
  "caseId": "knapsack-020",
  "round": 1,
  "family": "knapsack",
  "description": "Muchas piezas pequeñas y dos alternativas medianas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 9,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 3
      },
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 5
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 4,
        "value": 15
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
      "value": 31,
      "weight": 9,
      "selected": [
        0,
        1,
        3,
        4,
        5,
        7
      ]
    },
    "operations": 793,
    "elapsedNanos": 378419
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 31,
    "weight": 9,
    "selected": [
      0,
      1,
      3,
      4,
      5,
      7
    ]
  },
  "independent": {
    "id": "knapsack-020-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 31
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 793,
  "elapsedNanos": 916773,
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
  "id": "knapsack-020-r2",
  "caseId": "knapsack-020",
  "round": 2,
  "family": "knapsack",
  "description": "Muchas piezas pequeñas y dos alternativas medianas",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 9,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 3
      },
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 5
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 4,
        "value": 15
      },
      {
        "weight": 3,
        "value": 17
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
      "value": 41,
      "weight": 9,
      "selected": [
        3,
        5,
        7,
        8
      ]
    },
    "operations": 1182,
    "elapsedNanos": 144032
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 41,
    "weight": 9,
    "selected": [
      3,
      5,
      7,
      8
    ]
  },
  "independent": {
    "id": "knapsack-020-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 41
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1182,
  "elapsedNanos": 513668,
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
  "id": "knapsack-020-r3",
  "caseId": "knapsack-020",
  "round": 3,
  "family": "knapsack",
  "description": "Muchas piezas pequeñas y dos alternativas medianas",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 10,
    "items": [
      {
        "weight": 1,
        "value": 3
      },
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 5
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 4,
        "value": 15
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
      "value": 36,
      "weight": 10,
      "selected": [
        4,
        5,
        6
      ]
    },
    "operations": 1223,
    "elapsedNanos": 1158069
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 36,
    "weight": 10,
    "selected": [
      4,
      5,
      6
    ]
  },
  "independent": {
    "id": "knapsack-020-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 36
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1223,
  "elapsedNanos": 2811649,
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
  "id": "knapsack-020-r4",
  "caseId": "knapsack-020",
  "round": 4,
  "family": "knapsack",
  "description": "Muchas piezas pequeñas y dos alternativas medianas",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 7,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 3
      },
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 1,
        "value": 5
      },
      {
        "weight": 5,
        "value": 16
      },
      {
        "weight": 4,
        "value": 15
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
      "value": 27,
      "weight": 7,
      "selected": [
        1,
        3,
        4,
        6
      ]
    },
    "operations": 520,
    "elapsedNanos": 44045
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 27,
    "weight": 7,
    "selected": [
      1,
      3,
      4,
      6
    ]
  },
  "independent": {
    "id": "knapsack-020-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 27
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 520,
  "elapsedNanos": 188318,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
