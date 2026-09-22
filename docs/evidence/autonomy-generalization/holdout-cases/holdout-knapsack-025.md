# holdout-knapsack-025

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-025

### identifier

```json
"holdout-knapsack-025"
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
  "fullJournalSha256": "a56701e2e4df5f84315f7dc0e2bcd3fdc58f654ffd9c799d265f5a4af509e943",
  "familyState": {
    "schema": 1,
    "revision": 535,
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
        "capacity": 67,
        "items": [
          {
            "weight": 40,
            "value": 78
          },
          {
            "weight": 17,
            "value": 67
          },
          {
            "weight": 26,
            "value": 35
          },
          {
            "weight": 15,
            "value": 76
          },
          {
            "weight": 23,
            "value": 73
          },
          {
            "weight": 35,
            "value": 35
          },
          {
            "weight": 17,
            "value": 83
          },
          {
            "weight": 7,
            "value": 44
          },
          {
            "weight": 36,
            "value": 78
          },
          {
            "weight": 29,
            "value": 28
          },
          {
            "weight": 13,
            "value": 31
          },
          {
            "weight": 33,
            "value": 22
          }
        ]
      },
      {
        "capacity": 43,
        "items": [
          {
            "weight": 10,
            "value": 10
          },
          {
            "weight": 13,
            "value": 18
          },
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 28,
            "value": 32
          },
          {
            "weight": 33,
            "value": 34
          }
        ]
      },
      {
        "capacity": 40,
        "items": [
          {
            "weight": 7,
            "value": 45
          },
          {
            "weight": 9,
            "value": 73
          },
          {
            "weight": 20,
            "value": 41
          },
          {
            "weight": 41,
            "value": 55
          },
          {
            "weight": 20,
            "value": 96
          },
          {
            "weight": 41,
            "value": 16
          },
          {
            "weight": 2,
            "value": 3
          },
          {
            "weight": 31,
            "value": 25
          },
          {
            "weight": 21,
            "value": 67
          },
          {
            "weight": 41,
            "value": 90
          },
          {
            "weight": 41,
            "value": 99
          },
          {
            "weight": 20,
            "value": 55
          },
          {
            "weight": 20,
            "value": 5
          },
          {
            "weight": 34,
            "value": 85
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-023",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-006",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-015",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-030",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-031",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-005",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-026",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-027",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 349,
      "weight": 65,
      "selected": [
        1,
        3,
        5,
        6,
        7
      ]
    },
    "operations": 22673,
    "elapsedNanos": 183220
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 349,
  "weight": 65,
  "selected": [
    1,
    3,
    5,
    6,
    7
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
    "id": "holdout-knapsack-025",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 349
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
  "fullJournalSha256": "b8dc66cd7e9876fdcf0975d2a71defc2ef8584b31cb36960271072e4a5827a11",
  "familyState": {
    "schema": 1,
    "revision": 536,
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
        "capacity": 43,
        "items": [
          {
            "weight": 10,
            "value": 10
          },
          {
            "weight": 13,
            "value": 18
          },
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 28,
            "value": 32
          },
          {
            "weight": 33,
            "value": 34
          }
        ]
      },
      {
        "capacity": 40,
        "items": [
          {
            "weight": 7,
            "value": 45
          },
          {
            "weight": 9,
            "value": 73
          },
          {
            "weight": 20,
            "value": 41
          },
          {
            "weight": 41,
            "value": 55
          },
          {
            "weight": 20,
            "value": 96
          },
          {
            "weight": 41,
            "value": 16
          },
          {
            "weight": 2,
            "value": 3
          },
          {
            "weight": 31,
            "value": 25
          },
          {
            "weight": 21,
            "value": 67
          },
          {
            "weight": 41,
            "value": 90
          },
          {
            "weight": 41,
            "value": 99
          },
          {
            "weight": 20,
            "value": 55
          },
          {
            "weight": 20,
            "value": 5
          },
          {
            "weight": 34,
            "value": 85
          }
        ]
      },
      {
        "capacity": 65,
        "items": [
          {
            "weight": 31,
            "value": 84
          },
          {
            "weight": 30,
            "value": 82
          },
          {
            "weight": 31,
            "value": 54
          },
          {
            "weight": 2,
            "value": 91
          },
          {
            "weight": 24,
            "value": 64
          },
          {
            "weight": 10,
            "value": 81
          },
          {
            "weight": 19,
            "value": 4
          },
          {
            "weight": 4,
            "value": 91
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-knapsack-023",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-006",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-015",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-030",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-031",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-005",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-026",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-027",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-025",
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
  "id": "holdout-knapsack-025",
  "family": "knapsack",
  "description": "Recursos aleatorios con valores y pesos independientes",
  "input": {
    "capacity": 65,
    "items": [
      {
        "weight": 31,
        "value": 84
      },
      {
        "weight": 30,
        "value": 82
      },
      {
        "weight": 31,
        "value": 54
      },
      {
        "weight": 2,
        "value": 91
      },
      {
        "weight": 24,
        "value": 64
      },
      {
        "weight": 10,
        "value": 81
      },
      {
        "weight": 19,
        "value": 4
      },
      {
        "weight": 4,
        "value": 91
      }
    ]
  }
}
```

### operations

```json
22673
```

### elapsedNanos

```json
408272
```

### adapted

```json
false
```

### reused

```json
true
```
