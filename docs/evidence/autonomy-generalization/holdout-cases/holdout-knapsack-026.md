# holdout-knapsack-026

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-026

### identifier

```json
"holdout-knapsack-026"
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
  "fullJournalSha256": "c46a970c9c254f2e2567b8d0b54e9728d6dcc33c2ab0c4c998d57296233c3737",
  "familyState": {
    "schema": 1,
    "revision": 533,
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
        "capacity": 9,
        "items": [
          {
            "weight": 40,
            "value": 81
          },
          {
            "weight": 16,
            "value": 32
          },
          {
            "weight": 3,
            "value": 10
          },
          {
            "weight": 36,
            "value": 111
          },
          {
            "weight": 36,
            "value": 109
          },
          {
            "weight": 20,
            "value": 45
          }
        ]
      },
      {
        "capacity": 82,
        "items": [
          {
            "weight": 41,
            "value": 100
          },
          {
            "weight": 41,
            "value": 30
          },
          {
            "weight": 25,
            "value": 43
          },
          {
            "weight": 83,
            "value": 94
          },
          {
            "weight": 41,
            "value": 39
          },
          {
            "weight": 83,
            "value": 18
          }
        ]
      },
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
      "value": 52,
      "weight": 41,
      "selected": [
        2,
        4
      ]
    },
    "operations": 5639,
    "elapsedNanos": 94079
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 52,
  "weight": 41,
  "selected": [
    2,
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
    "id": "holdout-knapsack-026",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 52
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
  "fullJournalSha256": "d89610db2478781493e3d3d2d94d33e15c4ca7cc910e58dbdcb5fbf38ba48af4",
  "familyState": {
    "schema": 1,
    "revision": 534,
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
        "capacity": 82,
        "items": [
          {
            "weight": 41,
            "value": 100
          },
          {
            "weight": 41,
            "value": 30
          },
          {
            "weight": 25,
            "value": 43
          },
          {
            "weight": 83,
            "value": 94
          },
          {
            "weight": 41,
            "value": 39
          },
          {
            "weight": 83,
            "value": 18
          }
        ]
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-knapsack-026",
  "family": "knapsack",
  "description": "Valores correlacionados con peso y perturbación aleatoria",
  "input": {
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
  }
}
```

### operations

```json
5639
```

### elapsedNanos

```json
357568
```

### adapted

```json
false
```

### reused

```json
true
```
