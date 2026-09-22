# knapsack-016: Frontera de Pareto con múltiples empates

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-016-r1",
  "caseId": "knapsack-016",
  "round": 1,
  "family": "knapsack",
  "description": "Frontera de Pareto con múltiples empates",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 16,
    "items": [
      {
        "weight": 2,
        "value": 3
      },
      {
        "weight": 4,
        "value": 6
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 8,
        "value": 12
      },
      {
        "weight": 10,
        "value": 15
      },
      {
        "weight": 12,
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
      "value": 24,
      "weight": 16,
      "selected": [
        0,
        2,
        3
      ]
    },
    "operations": 985,
    "elapsedNanos": 1334530
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 24,
    "weight": 16,
    "selected": [
      0,
      2,
      3
    ]
  },
  "independent": {
    "id": "knapsack-016-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 24
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 985,
  "elapsedNanos": 2903014,
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
  "id": "knapsack-016-r2",
  "caseId": "knapsack-016",
  "round": 2,
  "family": "knapsack",
  "description": "Frontera de Pareto con múltiples empates",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 16,
    "items": [
      {
        "weight": 2,
        "value": 3
      },
      {
        "weight": 4,
        "value": 6
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 8,
        "value": 12
      },
      {
        "weight": 10,
        "value": 15
      },
      {
        "weight": 12,
        "value": 18
      },
      {
        "weight": 5,
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
      "value": 34,
      "weight": 15,
      "selected": [
        1,
        2,
        6
      ]
    },
    "operations": 1366,
    "elapsedNanos": 152274
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 34,
    "weight": 15,
    "selected": [
      1,
      2,
      6
    ]
  },
  "independent": {
    "id": "knapsack-016-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 34
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1366,
  "elapsedNanos": 554018,
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
  "id": "knapsack-016-r3",
  "caseId": "knapsack-016",
  "round": 3,
  "family": "knapsack",
  "description": "Frontera de Pareto con múltiples empates",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 17,
    "items": [
      {
        "weight": 4,
        "value": 6
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 8,
        "value": 12
      },
      {
        "weight": 10,
        "value": 15
      },
      {
        "weight": 12,
        "value": 18
      },
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 1,
        "value": 3
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
      "weight": 17,
      "selected": [
        1,
        3,
        6
      ]
    },
    "operations": 1419,
    "elapsedNanos": 1030421
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 27,
    "weight": 17,
    "selected": [
      1,
      3,
      6
    ]
  },
  "independent": {
    "id": "knapsack-016-r3",
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
  "operations": 1419,
  "elapsedNanos": 2806592,
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
  "id": "knapsack-016-r4",
  "caseId": "knapsack-016",
  "round": 4,
  "family": "knapsack",
  "description": "Frontera de Pareto con múltiples empates",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 14,
    "items": [
      {
        "weight": 2,
        "value": 3
      },
      {
        "weight": 4,
        "value": 6
      },
      {
        "weight": 6,
        "value": 9
      },
      {
        "weight": 10,
        "value": 15
      },
      {
        "weight": 12,
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
      "value": 21,
      "weight": 14,
      "selected": [
        1,
        3
      ]
    },
    "operations": 702,
    "elapsedNanos": 51376
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 21,
    "weight": 14,
    "selected": [
      1,
      3
    ]
  },
  "independent": {
    "id": "knapsack-016-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 21
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 702,
  "elapsedNanos": 196390,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
