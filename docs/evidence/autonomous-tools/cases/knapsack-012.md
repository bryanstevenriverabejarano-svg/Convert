# knapsack-012: Duplicados seleccionables por índice

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-012-r1",
  "caseId": "knapsack-012",
  "round": 1,
  "family": "knapsack",
  "description": "Duplicados seleccionables por índice",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 12,
    "items": [
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 6,
        "value": 11
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
      "value": 22,
      "weight": 12,
      "selected": [
        3,
        4
      ]
    },
    "operations": 626,
    "elapsedNanos": 273604
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 22,
    "weight": 12,
    "selected": [
      3,
      4
    ]
  },
  "independent": {
    "id": "knapsack-012-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 22
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 626,
  "elapsedNanos": 1244717,
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
  "id": "knapsack-012-r2",
  "caseId": "knapsack-012",
  "round": 2,
  "family": "knapsack",
  "description": "Duplicados seleccionables por índice",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 12,
    "items": [
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 4,
        "value": 12
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
      "weight": 12,
      "selected": [
        0,
        1,
        5
      ]
    },
    "operations": 851,
    "elapsedNanos": 142280
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 26,
    "weight": 12,
    "selected": [
      0,
      1,
      5
    ]
  },
  "independent": {
    "id": "knapsack-012-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
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
  "operations": 851,
  "elapsedNanos": 534028,
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
  "id": "knapsack-012-r3",
  "caseId": "knapsack-012",
  "round": 3,
  "family": "knapsack",
  "description": "Duplicados seleccionables por índice",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 13,
    "items": [
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 2,
        "value": 3
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
      "value": 23,
      "weight": 12,
      "selected": [
        0,
        2,
        5
      ]
    },
    "operations": 895,
    "elapsedNanos": 873068
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 23,
    "weight": 12,
    "selected": [
      0,
      2,
      5
    ]
  },
  "independent": {
    "id": "knapsack-012-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 23
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 895,
  "elapsedNanos": 2373944,
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
  "id": "knapsack-012-r4",
  "caseId": "knapsack-012",
  "round": 4,
  "family": "knapsack",
  "description": "Duplicados seleccionables por índice",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 10,
    "items": [
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 6,
        "value": 11
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
      "value": 18,
      "weight": 10,
      "selected": [
        0,
        2
      ]
    },
    "operations": 439,
    "elapsedNanos": 63794
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 18,
    "weight": 10,
    "selected": [
      0,
      2
    ]
  },
  "independent": {
    "id": "knapsack-012-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 18
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 439,
  "elapsedNanos": 229569,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
