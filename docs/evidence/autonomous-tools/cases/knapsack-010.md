# knapsack-010: Dominancia parcial entre objetos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-010-r1",
  "caseId": "knapsack-010",
  "round": 1,
  "family": "knapsack",
  "description": "Dominancia parcial entre objetos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 12,
    "items": [
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 7
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 7,
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
      "value": 26,
      "weight": 10,
      "selected": [
        2,
        4
      ]
    },
    "operations": 588,
    "elapsedNanos": 541970
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 26,
    "weight": 10,
    "selected": [
      2,
      4
    ]
  },
  "independent": {
    "id": "knapsack-010-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 26
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 588,
  "elapsedNanos": 854692,
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
  "id": "knapsack-010-r2",
  "caseId": "knapsack-010",
  "round": 2,
  "family": "knapsack",
  "description": "Dominancia parcial entre objetos",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 12,
    "items": [
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 7
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 7,
        "value": 17
      },
      {
        "weight": 4,
        "value": 18
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
      "weight": 11,
      "selected": [
        0,
        2,
        5
      ]
    },
    "operations": 802,
    "elapsedNanos": 161248
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 35,
    "weight": 11,
    "selected": [
      0,
      2,
      5
    ]
  },
  "independent": {
    "id": "knapsack-010-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
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
  "operations": 802,
  "elapsedNanos": 540958,
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
  "id": "knapsack-010-r3",
  "caseId": "knapsack-010",
  "round": 3,
  "family": "knapsack",
  "description": "Dominancia parcial entre objetos",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 13,
    "items": [
      {
        "weight": 5,
        "value": 7
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 7,
        "value": 17
      },
      {
        "weight": 2,
        "value": 4
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
      "value": 31,
      "weight": 12,
      "selected": [
        1,
        3,
        5
      ]
    },
    "operations": 840,
    "elapsedNanos": 1277546
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 31,
    "weight": 12,
    "selected": [
      1,
      3,
      5
    ]
  },
  "independent": {
    "id": "knapsack-010-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
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
  "operations": 840,
  "elapsedNanos": 2860962,
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
  "id": "knapsack-010-r4",
  "caseId": "knapsack-010",
  "round": 4,
  "family": "knapsack",
  "description": "Dominancia parcial entre objetos",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 10,
    "items": [
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 7
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 7,
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
      "value": 17,
      "weight": 7,
      "selected": [
        3
      ]
    },
    "operations": 413,
    "elapsedNanos": 38556
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 17,
    "weight": 7,
    "selected": [
      3
    ]
  },
  "independent": {
    "id": "knapsack-010-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 17
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 413,
  "elapsedNanos": 181178,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
