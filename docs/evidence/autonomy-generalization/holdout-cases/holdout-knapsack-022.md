# holdout-knapsack-022

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-022

### identifier

```json
"holdout-knapsack-022"
```

### objective

```json
"Valores correlacionados con peso y perturbación aleatoria"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "acf5b369e828efe2a4cf915788ef1e657d4cf7439aea0393909ca90851cf3c9b",
  "familyState": {
    "schema": 1,
    "revision": 469,
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
        "capacity": 7,
        "items": [
          {
            "weight": 3,
            "value": 86
          },
          {
            "weight": 8,
            "value": 93
          },
          {
            "weight": 2,
            "value": 64
          },
          {
            "weight": 7,
            "value": 24
          },
          {
            "weight": 8,
            "value": 71
          },
          {
            "weight": 8,
            "value": 39
          },
          {
            "weight": 2,
            "value": 26
          },
          {
            "weight": 8,
            "value": 86
          },
          {
            "weight": 3,
            "value": 51
          },
          {
            "weight": 8,
            "value": 4
          },
          {
            "weight": 3,
            "value": 90
          }
        ]
      },
      {
        "capacity": 3,
        "items": [
          {
            "weight": 31,
            "value": 88
          },
          {
            "weight": 21,
            "value": 80
          },
          {
            "weight": 8,
            "value": 21
          },
          {
            "weight": 24,
            "value": 81
          },
          {
            "weight": 32,
            "value": 41
          },
          {
            "weight": 10,
            "value": 82
          },
          {
            "weight": 25,
            "value": 64
          },
          {
            "weight": 18,
            "value": 86
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 10,
            "value": 74
          },
          {
            "weight": 6,
            "value": 0
          },
          {
            "weight": 16,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 6,
            "value": 0
          },
          {
            "weight": 16,
            "value": 0
          },
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 20,
            "value": 0
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-007",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-029",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-008",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 67,
      "weight": 18,
      "selected": [
        3,
        4,
        6,
        8
      ]
    },
    "operations": 7016,
    "elapsedNanos": 163461
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 67,
  "weight": 18,
  "selected": [
    3,
    4,
    6,
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
    "id": "holdout-knapsack-022",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2048,
      "optimum": 67
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
  "fullJournalSha256": "eb3036f83837484d09641c269034e5f51893e3d5d8e0314eddcfea8548a58bd9",
  "familyState": {
    "schema": 1,
    "revision": 470,
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
        "capacity": 3,
        "items": [
          {
            "weight": 31,
            "value": 88
          },
          {
            "weight": 21,
            "value": 80
          },
          {
            "weight": 8,
            "value": 21
          },
          {
            "weight": 24,
            "value": 81
          },
          {
            "weight": 32,
            "value": 41
          },
          {
            "weight": 10,
            "value": 82
          },
          {
            "weight": 25,
            "value": 64
          },
          {
            "weight": 18,
            "value": 86
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 10,
            "value": 74
          },
          {
            "weight": 6,
            "value": 0
          },
          {
            "weight": 16,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 6,
            "value": 0
          },
          {
            "weight": 16,
            "value": 0
          },
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 20,
            "value": 0
          }
        ]
      },
      {
        "capacity": 18,
        "items": [
          {
            "weight": 36,
            "value": 111
          },
          {
            "weight": 27,
            "value": 28
          },
          {
            "weight": 28,
            "value": 33
          },
          {
            "weight": 1,
            "value": 5
          },
          {
            "weight": 2,
            "value": 12
          },
          {
            "weight": 5,
            "value": 18
          },
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 31,
            "value": 69
          },
          {
            "weight": 13,
            "value": 46
          },
          {
            "weight": 23,
            "value": 30
          },
          {
            "weight": 39,
            "value": 79
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-007",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-029",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-008",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-022",
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
  "id": "holdout-knapsack-022",
  "family": "knapsack",
  "description": "Valores correlacionados con peso y perturbación aleatoria",
  "input": {
    "capacity": 18,
    "items": [
      {
        "weight": 36,
        "value": 111
      },
      {
        "weight": 27,
        "value": 28
      },
      {
        "weight": 28,
        "value": 33
      },
      {
        "weight": 1,
        "value": 5
      },
      {
        "weight": 2,
        "value": 12
      },
      {
        "weight": 5,
        "value": 18
      },
      {
        "weight": 2,
        "value": 4
      },
      {
        "weight": 31,
        "value": 69
      },
      {
        "weight": 13,
        "value": 46
      },
      {
        "weight": 23,
        "value": 30
      },
      {
        "weight": 39,
        "value": 79
      }
    ]
  }
}
```

### operations

```json
7016
```

### elapsedNanos

```json
452528
```

### adapted

```json
false
```

### reused

```json
true
```
