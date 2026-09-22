# holdout-knapsack-015

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-015

### identifier

```json
"holdout-knapsack-015"
```

### objective

```json
"Mezcla de alternativas factibles y recursos que exceden el presupuesto"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "bc79cf8f734d5bc963efe8ec79670b5dad61ba6a0c65ca1f0eb28ac2097cc1df",
  "familyState": {
    "schema": 1,
    "revision": 517,
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
      },
      {
        "capacity": 25,
        "items": [
          {
            "weight": 12,
            "value": 43
          },
          {
            "weight": 12,
            "value": 18
          },
          {
            "weight": 26,
            "value": 97
          },
          {
            "weight": 12,
            "value": 4
          },
          {
            "weight": 12,
            "value": 61
          },
          {
            "weight": 12,
            "value": 18
          },
          {
            "weight": 12,
            "value": 32
          }
        ]
      },
      {
        "capacity": 59,
        "items": [
          {
            "weight": 5,
            "value": 9
          },
          {
            "weight": 33,
            "value": 102
          },
          {
            "weight": 32,
            "value": 96
          },
          {
            "weight": 40,
            "value": 43
          },
          {
            "weight": 29,
            "value": 35
          },
          {
            "weight": 29,
            "value": 88
          },
          {
            "weight": 2,
            "value": 7
          },
          {
            "weight": 17,
            "value": 37
          },
          {
            "weight": 14,
            "value": 47
          },
          {
            "weight": 29,
            "value": 32
          },
          {
            "weight": 18,
            "value": 37
          }
        ]
      }
    ],
    "receipts": [
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
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 135,
      "weight": 36,
      "selected": [
        3,
        9
      ]
    },
    "operations": 12489,
    "elapsedNanos": 147828
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 135,
  "weight": 36,
  "selected": [
    3,
    9
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
    "id": "holdout-knapsack-015",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4096,
      "optimum": 135
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
  "fullJournalSha256": "f938548c2b0239e2e5d9e8840b9bb6497337080344c5af259399a2731cdcd35f",
  "familyState": {
    "schema": 1,
    "revision": 518,
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
        "capacity": 25,
        "items": [
          {
            "weight": 12,
            "value": 43
          },
          {
            "weight": 12,
            "value": 18
          },
          {
            "weight": 26,
            "value": 97
          },
          {
            "weight": 12,
            "value": 4
          },
          {
            "weight": 12,
            "value": 61
          },
          {
            "weight": 12,
            "value": 18
          },
          {
            "weight": 12,
            "value": 32
          }
        ]
      },
      {
        "capacity": 59,
        "items": [
          {
            "weight": 5,
            "value": 9
          },
          {
            "weight": 33,
            "value": 102
          },
          {
            "weight": 32,
            "value": 96
          },
          {
            "weight": 40,
            "value": 43
          },
          {
            "weight": 29,
            "value": 35
          },
          {
            "weight": 29,
            "value": 88
          },
          {
            "weight": 2,
            "value": 7
          },
          {
            "weight": 17,
            "value": 37
          },
          {
            "weight": 14,
            "value": 47
          },
          {
            "weight": 29,
            "value": 32
          },
          {
            "weight": 18,
            "value": 37
          }
        ]
      },
      {
        "capacity": 36,
        "items": [
          {
            "weight": 37,
            "value": 26
          },
          {
            "weight": 37,
            "value": 35
          },
          {
            "weight": 24,
            "value": 43
          },
          {
            "weight": 18,
            "value": 41
          },
          {
            "weight": 18,
            "value": 26
          },
          {
            "weight": 26,
            "value": 81
          },
          {
            "weight": 37,
            "value": 9
          },
          {
            "weight": 30,
            "value": 39
          },
          {
            "weight": 29,
            "value": 35
          },
          {
            "weight": 18,
            "value": 94
          },
          {
            "weight": 37,
            "value": 64
          },
          {
            "weight": 37,
            "value": 58
          }
        ]
      }
    ],
    "receipts": [
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
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-knapsack-015",
  "family": "knapsack",
  "description": "Mezcla de alternativas factibles y recursos que exceden el presupuesto",
  "input": {
    "capacity": 36,
    "items": [
      {
        "weight": 37,
        "value": 26
      },
      {
        "weight": 37,
        "value": 35
      },
      {
        "weight": 24,
        "value": 43
      },
      {
        "weight": 18,
        "value": 41
      },
      {
        "weight": 18,
        "value": 26
      },
      {
        "weight": 26,
        "value": 81
      },
      {
        "weight": 37,
        "value": 9
      },
      {
        "weight": 30,
        "value": 39
      },
      {
        "weight": 29,
        "value": 35
      },
      {
        "weight": 18,
        "value": 94
      },
      {
        "weight": 37,
        "value": 64
      },
      {
        "weight": 37,
        "value": 58
      }
    ]
  }
}
```

### operations

```json
12489
```

### elapsedNanos

```json
444406
```

### adapted

```json
false
```

### reused

```json
true
```
