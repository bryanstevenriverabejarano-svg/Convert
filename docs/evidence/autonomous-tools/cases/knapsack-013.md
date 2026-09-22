# knapsack-013: Un objeto voluminoso compite con muchas piezas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-013-r1",
  "caseId": "knapsack-013",
  "round": 1,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 13,
    "items": [
      {
        "weight": 13,
        "value": 30
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
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
      "value": 30,
      "weight": 12,
      "selected": [
        1,
        2,
        3,
        4,
        5,
        6
      ]
    },
    "operations": 734,
    "elapsedNanos": 1933934
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 30,
    "weight": 12,
    "selected": [
      1,
      2,
      3,
      4,
      5,
      6
    ]
  },
  "independent": {
    "id": "knapsack-013-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 30
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 734,
  "elapsedNanos": 3745718,
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
  "id": "knapsack-013-r2",
  "caseId": "knapsack-013",
  "round": 2,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 13,
    "items": [
      {
        "weight": 13,
        "value": 30
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 4,
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
      "value": 51,
      "weight": 12,
      "selected": [
        1,
        2,
        3,
        4,
        7
      ]
    },
    "operations": 1022,
    "elapsedNanos": 198182
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 51,
    "weight": 12,
    "selected": [
      1,
      2,
      3,
      4,
      7
    ]
  },
  "independent": {
    "id": "knapsack-013-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 51
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1022,
  "elapsedNanos": 582519,
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
  "id": "knapsack-013-r3",
  "caseId": "knapsack-013",
  "round": 3,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 14,
    "items": [
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 6,
        "value": 15
      },
      {
        "weight": 7,
        "value": 16
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
      "value": 35,
      "weight": 14,
      "selected": [
        0,
        1,
        2,
        3,
        6
      ]
    },
    "operations": 1069,
    "elapsedNanos": 1105422
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 35,
    "weight": 14,
    "selected": [
      0,
      1,
      2,
      3,
      6
    ]
  },
  "independent": {
    "id": "knapsack-013-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 35
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1069,
  "elapsedNanos": 2665063,
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
  "id": "knapsack-013-r4",
  "caseId": "knapsack-013",
  "round": 4,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 11,
    "items": [
      {
        "weight": 13,
        "value": 30
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 2,
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
      "value": 25,
      "weight": 10,
      "selected": [
        1,
        2,
        3,
        4,
        5
      ]
    },
    "operations": 509,
    "elapsedNanos": 40760
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 25,
    "weight": 10,
    "selected": [
      1,
      2,
      3,
      4,
      5
    ]
  },
  "independent": {
    "id": "knapsack-013-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 25
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 509,
  "elapsedNanos": 187207,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
