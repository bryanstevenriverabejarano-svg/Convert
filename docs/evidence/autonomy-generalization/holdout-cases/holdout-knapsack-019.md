# holdout-knapsack-019

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-019

### identifier

```json
"holdout-knapsack-019"
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
  "fullJournalSha256": "88c105e0a5723850dfd2643ac21f42fe3512d6e510a7ae7bb4dea3f5098bb34b",
  "familyState": {
    "schema": 1,
    "revision": 431,
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
      },
      {
        "capacity": 28,
        "items": [
          {
            "weight": 20,
            "value": 0
          },
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 20,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 16,
            "value": 65
          },
          {
            "weight": 2,
            "value": 0
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 10,
            "value": 76
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
      },
      {
        "id": "holdout-knapsack-004",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":4,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 125,
      "weight": 17,
      "selected": [
        2,
        3
      ]
    },
    "operations": 2005,
    "elapsedNanos": 88911
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 125,
  "weight": 17,
  "selected": [
    2,
    3
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
    "id": "holdout-knapsack-019",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 125
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
  "fullJournalSha256": "e462e871419fb44b56e49581a411ab32d82248c7e67e0b87eb4562953e044358",
  "familyState": {
    "schema": 1,
    "revision": 432,
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
      },
      {
        "capacity": 28,
        "items": [
          {
            "weight": 20,
            "value": 0
          },
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 20,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 16,
            "value": 65
          },
          {
            "weight": 2,
            "value": 0
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 10,
            "value": 76
          }
        ]
      },
      {
        "capacity": 18,
        "items": [
          {
            "weight": 19,
            "value": 53
          },
          {
            "weight": 19,
            "value": 6
          },
          {
            "weight": 8,
            "value": 28
          },
          {
            "weight": 9,
            "value": 97
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
      },
      {
        "id": "holdout-knapsack-004",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-019",
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
  "id": "holdout-knapsack-019",
  "family": "knapsack",
  "description": "Mezcla de alternativas factibles y recursos que exceden el presupuesto",
  "input": {
    "capacity": 18,
    "items": [
      {
        "weight": 19,
        "value": 53
      },
      {
        "weight": 19,
        "value": 6
      },
      {
        "weight": 8,
        "value": 28
      },
      {
        "weight": 9,
        "value": 97
      }
    ]
  }
}
```

### operations

```json
2005
```

### elapsedNanos

```json
282076
```

### adapted

```json
false
```

### reused

```json
true
```
