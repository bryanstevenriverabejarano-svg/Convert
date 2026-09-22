# holdout-knapsack-009

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-009

### identifier

```json
"holdout-knapsack-009"
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
  "fullJournalSha256": "4a6524c5e7df01e68ce0c72f1035f9fa16c57ebbf09782678a53873498601505",
  "familyState": {
    "schema": 1,
    "revision": 500,
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
            "weight": 10,
            "value": 0
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 12,
            "value": 60
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 22,
            "value": 0
          },
          {
            "weight": 4,
            "value": 0
          }
        ]
      },
      {
        "capacity": 34,
        "items": [
          {
            "weight": 31,
            "value": 81
          },
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 9,
            "value": 74
          },
          {
            "weight": 27,
            "value": 31
          },
          {
            "weight": 35,
            "value": 85
          },
          {
            "weight": 29,
            "value": 7
          },
          {
            "weight": 33,
            "value": 58
          },
          {
            "weight": 1,
            "value": 36
          },
          {
            "weight": 12,
            "value": 56
          }
        ]
      },
      {
        "capacity": 68,
        "items": [
          {
            "weight": 21,
            "value": 45
          },
          {
            "weight": 22,
            "value": 66
          },
          {
            "weight": 40,
            "value": 84
          },
          {
            "weight": 32,
            "value": 67
          },
          {
            "weight": 16,
            "value": 32
          },
          {
            "weight": 36,
            "value": 114
          },
          {
            "weight": 36,
            "value": 115
          },
          {
            "weight": 6,
            "value": 12
          },
          {
            "weight": 38,
            "value": 38
          },
          {
            "weight": 4,
            "value": 10
          },
          {
            "weight": 3,
            "value": 8
          },
          {
            "weight": 20,
            "value": 65
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-014",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-003",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-032",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-012",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-013",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-010",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":6,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 269,
      "weight": 73,
      "selected": [
        0,
        2,
        3,
        4
      ]
    },
    "operations": 6588,
    "elapsedNanos": 114900
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 269,
  "weight": 73,
  "selected": [
    0,
    2,
    3,
    4
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
    "id": "holdout-knapsack-009",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 269
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
  "fullJournalSha256": "5d57863d9c2c7c39c69843b9607619b3be4604337395a8a13e52b1a9840cbc9a",
  "familyState": {
    "schema": 1,
    "revision": 501,
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
        "capacity": 34,
        "items": [
          {
            "weight": 31,
            "value": 81
          },
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 9,
            "value": 74
          },
          {
            "weight": 27,
            "value": 31
          },
          {
            "weight": 35,
            "value": 85
          },
          {
            "weight": 29,
            "value": 7
          },
          {
            "weight": 33,
            "value": 58
          },
          {
            "weight": 1,
            "value": 36
          },
          {
            "weight": 12,
            "value": 56
          }
        ]
      },
      {
        "capacity": 68,
        "items": [
          {
            "weight": 21,
            "value": 45
          },
          {
            "weight": 22,
            "value": 66
          },
          {
            "weight": 40,
            "value": 84
          },
          {
            "weight": 32,
            "value": 67
          },
          {
            "weight": 16,
            "value": 32
          },
          {
            "weight": 36,
            "value": 114
          },
          {
            "weight": 36,
            "value": 115
          },
          {
            "weight": 6,
            "value": 12
          },
          {
            "weight": 38,
            "value": 38
          },
          {
            "weight": 4,
            "value": 10
          },
          {
            "weight": 3,
            "value": 8
          },
          {
            "weight": 20,
            "value": 65
          }
        ]
      },
      {
        "capacity": 83,
        "items": [
          {
            "weight": 10,
            "value": 26
          },
          {
            "weight": 13,
            "value": 6
          },
          {
            "weight": 14,
            "value": 72
          },
          {
            "weight": 24,
            "value": 97
          },
          {
            "weight": 25,
            "value": 74
          },
          {
            "weight": 38,
            "value": 27
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-014",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-003",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-032",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-012",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-013",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-010",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-009",
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
  "id": "holdout-knapsack-009",
  "family": "knapsack",
  "description": "Recursos aleatorios con valores y pesos independientes",
  "input": {
    "capacity": 83,
    "items": [
      {
        "weight": 10,
        "value": 26
      },
      {
        "weight": 13,
        "value": 6
      },
      {
        "weight": 14,
        "value": 72
      },
      {
        "weight": 24,
        "value": 97
      },
      {
        "weight": 25,
        "value": 74
      },
      {
        "weight": 38,
        "value": 27
      }
    ]
  }
}
```

### operations

```json
6588
```

### elapsedNanos

```json
533517
```

### adapted

```json
false
```

### reused

```json
true
```
