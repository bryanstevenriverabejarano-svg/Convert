# holdout-knapsack-017

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-017

### identifier

```json
"holdout-knapsack-017"
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
  "fullJournalSha256": "7dc13f096d6055bed96a54293cded963e2edaf60497c45d3a10fc67676e95097",
  "familyState": {
    "schema": 1,
    "revision": 505,
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
      },
      {
        "capacity": 72,
        "items": [
          {
            "weight": 27,
            "value": 2
          },
          {
            "weight": 17,
            "value": 3
          },
          {
            "weight": 6,
            "value": 86
          },
          {
            "weight": 25,
            "value": 9
          },
          {
            "weight": 39,
            "value": 62
          },
          {
            "weight": 31,
            "value": 0
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
      },
      {
        "id": "holdout-knapsack-001",
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
      "value": 343,
      "weight": 91,
      "selected": [
        2,
        3,
        5,
        6,
        8,
        10
      ]
    },
    "operations": 11128,
    "elapsedNanos": 269458
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 343,
  "weight": 91,
  "selected": [
    2,
    3,
    5,
    6,
    8,
    10
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
    "id": "holdout-knapsack-017",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4096,
      "optimum": 343
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
  "fullJournalSha256": "bffd602d0fd8aed86c6da2f6f826b1cba8a23fa5609c2289d0932891c7897541",
  "familyState": {
    "schema": 1,
    "revision": 506,
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
      },
      {
        "capacity": 72,
        "items": [
          {
            "weight": 27,
            "value": 2
          },
          {
            "weight": 17,
            "value": 3
          },
          {
            "weight": 6,
            "value": 86
          },
          {
            "weight": 25,
            "value": 9
          },
          {
            "weight": 39,
            "value": 62
          },
          {
            "weight": 31,
            "value": 0
          }
        ]
      },
      {
        "capacity": 92,
        "items": [
          {
            "weight": 12,
            "value": 27
          },
          {
            "weight": 38,
            "value": 97
          },
          {
            "weight": 21,
            "value": 85
          },
          {
            "weight": 5,
            "value": 47
          },
          {
            "weight": 40,
            "value": 99
          },
          {
            "weight": 28,
            "value": 100
          },
          {
            "weight": 15,
            "value": 56
          },
          {
            "weight": 24,
            "value": 22
          },
          {
            "weight": 5,
            "value": 17
          },
          {
            "weight": 39,
            "value": 5
          },
          {
            "weight": 17,
            "value": 38
          },
          {
            "weight": 15,
            "value": 31
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-knapsack-001",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-017",
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
  "id": "holdout-knapsack-017",
  "family": "knapsack",
  "description": "Recursos aleatorios con valores y pesos independientes",
  "input": {
    "capacity": 92,
    "items": [
      {
        "weight": 12,
        "value": 27
      },
      {
        "weight": 38,
        "value": 97
      },
      {
        "weight": 21,
        "value": 85
      },
      {
        "weight": 5,
        "value": 47
      },
      {
        "weight": 40,
        "value": 99
      },
      {
        "weight": 28,
        "value": 100
      },
      {
        "weight": 15,
        "value": 56
      },
      {
        "weight": 24,
        "value": 22
      },
      {
        "weight": 5,
        "value": 17
      },
      {
        "weight": 39,
        "value": 5
      },
      {
        "weight": 17,
        "value": 38
      },
      {
        "weight": 15,
        "value": 31
      }
    ]
  }
}
```

### operations

```json
11128
```

### elapsedNanos

```json
646905
```

### adapted

```json
false
```

### reused

```json
true
```
