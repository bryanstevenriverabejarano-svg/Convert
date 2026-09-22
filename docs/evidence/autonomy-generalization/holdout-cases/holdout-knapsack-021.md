# holdout-knapsack-021

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-021

### identifier

```json
"holdout-knapsack-021"
```

### objective

```json
"Recursos aleatorios con valores y pesos independientes"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "db3cdd2307975b4e7a4d8389e077be229dca8b63809b91bf200c2c482f47fb27",
  "familyState": {
    "schema": 1,
    "revision": 421,
    "tools": {
      "version": 2,
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
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
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
      },
      {
        "capacity": 28,
        "items": [
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 18,
            "value": 19
          },
          {
            "weight": 18,
            "value": 0
          },
          {
            "weight": 4,
            "value": 0
          }
        ]
      },
      {
        "capacity": 24,
        "items": [
          {
            "weight": 35,
            "value": 72
          },
          {
            "weight": 9,
            "value": 33
          },
          {
            "weight": 16,
            "value": 54
          },
          {
            "weight": 28,
            "value": 57
          },
          {
            "weight": 23,
            "value": 46
          },
          {
            "weight": 22,
            "value": 45
          },
          {
            "weight": 29,
            "value": 61
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-016",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-018",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      }
    ]
  }
}
```

### capabilitiesUsed

```json
[
  "síntesis simbólica acotada",
  "ejecución de herramientas",
  "verificación de candidatos",
  "consulta del diario persistido"
]
```

### planCreated

```json
[
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "DYNAMIC_PROGRAMMING"
]
```

### modelsUsed

```json
{
  "status": "not_applicable",
  "detail": "Ningún LLM. Se ejecuta el laboratorio Java real con kernels simbólicos."
}
```

### memoriesRetrieved

```json
{
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

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
      "value": 390,
      "weight": 62,
      "selected": [
        0,
        1,
        2,
        3,
        4,
        7,
        8
      ]
    },
    "operations": 10179,
    "elapsedNanos": 358559
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 390,
  "weight": 62,
  "selected": [
    0,
    1,
    2,
    3,
    4,
    7,
    8
  ]
}
```

### passFail

```json
"PASS"
```

### failureRootCause

```json
{
  "status": "not_applicable",
  "detail": "No se observó este evento en la ejecución"
}
```

### improvementHypothesis

```json
{
  "status": "not_applicable",
  "detail": "No se observó este evento en la ejecución"
}
```

### modificationPerformed

```json
{
  "strategySequence": [
    "DYNAMIC_PROGRAMMING"
  ],
  "promoted": true,
  "version": 2,
  "scope": "Cambios de programa declarativo y diario; no se modifica código fuente ni pesos del modelo."
}
```

### newExecution

```json
{
  "status": "not_applicable",
  "detail": "No se observó este evento en la ejecución"
}
```

### newResult

```json
{
  "status": "not_applicable",
  "detail": "No se observó este evento en la ejecución"
}
```

### possibleSideEffects

```json
{
  "stateAfterReference": "stateAfter",
  "journalChanged": true,
  "unmeasured": "No se evalúan efectos externos; este proceso no usa red, apps ni sensores."
}
```

### regressionTests

```json
[
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "holdout-knapsack-021",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 390
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "5c960197c019ecc00126d3e9fc9b321e1e53f1274bdb5986fba8fa17d0e9dd44",
  "familyState": {
    "schema": 1,
    "revision": 422,
    "tools": {
      "version": 2,
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
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 28,
        "items": [
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 18,
            "value": 19
          },
          {
            "weight": 18,
            "value": 0
          },
          {
            "weight": 4,
            "value": 0
          }
        ]
      },
      {
        "capacity": 24,
        "items": [
          {
            "weight": 35,
            "value": 72
          },
          {
            "weight": 9,
            "value": 33
          },
          {
            "weight": 16,
            "value": 54
          },
          {
            "weight": 28,
            "value": 57
          },
          {
            "weight": 23,
            "value": 46
          },
          {
            "weight": 22,
            "value": 45
          },
          {
            "weight": 29,
            "value": 61
          }
        ]
      },
      {
        "capacity": 63,
        "items": [
          {
            "weight": 2,
            "value": 48
          },
          {
            "weight": 12,
            "value": 83
          },
          {
            "weight": 9,
            "value": 31
          },
          {
            "weight": 7,
            "value": 36
          },
          {
            "weight": 7,
            "value": 76
          },
          {
            "weight": 30,
            "value": 12
          },
          {
            "weight": 35,
            "value": 49
          },
          {
            "weight": 20,
            "value": 21
          },
          {
            "weight": 5,
            "value": 95
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-016",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-018",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-021",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-knapsack-021",
  "family": "knapsack",
  "description": "Recursos aleatorios con valores y pesos independientes",
  "input": {
    "capacity": 63,
    "items": [
      {
        "weight": 2,
        "value": 48
      },
      {
        "weight": 12,
        "value": 83
      },
      {
        "weight": 9,
        "value": 31
      },
      {
        "weight": 7,
        "value": 36
      },
      {
        "weight": 7,
        "value": 76
      },
      {
        "weight": 30,
        "value": 12
      },
      {
        "weight": 35,
        "value": 49
      },
      {
        "weight": 20,
        "value": 21
      },
      {
        "weight": 5,
        "value": 95
      }
    ]
  }
}
```

### operations

```json
10179
```

### elapsedNanos

```json
589330
```

### adapted

```json
false
```

### reused

```json
true
```
