# knapsack-019: Recompensas altas cerca de una restricción estrecha

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "knapsack-019-r1",
  "caseId": "knapsack-019",
  "round": 1,
  "family": "knapsack",
  "description": "Recompensas altas cerca de una restricción estrecha",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 7,
    "items": [
      {
        "weight": 6,
        "value": 1000
      },
      {
        "weight": 4,
        "value": 700
      },
      {
        "weight": 3,
        "value": 600
      },
      {
        "weight": 2,
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
      "value": 1300,
      "weight": 7,
      "selected": [
        1,
        2
      ]
    },
    "operations": 582,
    "elapsedNanos": 532646
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 1300,
    "weight": 7,
    "selected": [
      1,
      2
    ]
  },
  "independent": {
    "id": "knapsack-019-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 1300
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 582,
  "elapsedNanos": 1190677,
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
  "id": "knapsack-019-r2",
  "caseId": "knapsack-019",
  "round": 2,
  "family": "knapsack",
  "description": "Recompensas altas cerca de una restricción estrecha",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 7,
    "items": [
      {
        "weight": 6,
        "value": 1000
      },
      {
        "weight": 4,
        "value": 700
      },
      {
        "weight": 3,
        "value": 600
      },
      {
        "weight": 2,
        "value": 1
      },
      {
        "weight": 2,
        "value": 1001
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
      "value": 1701,
      "weight": 6,
      "selected": [
        1,
        4
      ]
    },
    "operations": 786,
    "elapsedNanos": 126747
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 1701,
    "weight": 6,
    "selected": [
      1,
      4
    ]
  },
  "independent": {
    "id": "knapsack-019-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 1701
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 786,
  "elapsedNanos": 549250,
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
  "id": "knapsack-019-r3",
  "caseId": "knapsack-019",
  "round": 3,
  "family": "knapsack",
  "description": "Recompensas altas cerca de una restricción estrecha",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 8,
    "items": [
      {
        "weight": 4,
        "value": 700
      },
      {
        "weight": 3,
        "value": 600
      },
      {
        "weight": 2,
        "value": 1
      },
      {
        "weight": 3,
        "value": 500
      },
      {
        "weight": 3,
        "value": 501
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
      "value": 1300,
      "weight": 7,
      "selected": [
        0,
        1
      ]
    },
    "operations": 829,
    "elapsedNanos": 930494
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 1300,
    "weight": 7,
    "selected": [
      0,
      1
    ]
  },
  "independent": {
    "id": "knapsack-019-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 1300
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 829,
  "elapsedNanos": 2610953,
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
  "id": "knapsack-019-r4",
  "caseId": "knapsack-019",
  "round": 4,
  "family": "knapsack",
  "description": "Recompensas altas cerca de una restricción estrecha",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 5,
    "items": [
      {
        "weight": 6,
        "value": 1000
      },
      {
        "weight": 4,
        "value": 700
      },
      {
        "weight": 2,
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
      "value": 700,
      "weight": 4,
      "selected": [
        1
      ]
    },
    "operations": 410,
    "elapsedNanos": 40270
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 700,
    "weight": 4,
    "selected": [
      1
    ]
  },
  "independent": {
    "id": "knapsack-019-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 700
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 410,
  "elapsedNanos": 187357,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
