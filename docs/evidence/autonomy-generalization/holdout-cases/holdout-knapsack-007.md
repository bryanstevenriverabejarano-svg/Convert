# holdout-knapsack-007

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-007

### identifier

```json
"holdout-knapsack-007"
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
  "fullJournalSha256": "f308b765540a790c508b37b3622067b5e9c4397f8899443bed53df02bc92361e",
  "familyState": {
    "schema": 1,
    "revision": 449,
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
            "weight": 6,
            "value": 71
          },
          {
            "weight": 6,
            "value": 36
          },
          {
            "weight": 18,
            "value": 0
          },
          {
            "weight": 12,
            "value": 0
          },
          {
            "weight": 18,
            "value": 0
          },
          {
            "weight": 8,
            "value": 44
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 12,
            "value": 64
          },
          {
            "weight": 22,
            "value": 0
          }
        ]
      },
      {
        "capacity": 7,
        "items": [
          {
            "weight": 1,
            "value": 3
          },
          {
            "weight": 5,
            "value": 5
          },
          {
            "weight": 5,
            "value": 17
          },
          {
            "weight": 27,
            "value": 88
          },
          {
            "weight": 31,
            "value": 36
          },
          {
            "weight": 30,
            "value": 36
          },
          {
            "weight": 39,
            "value": 123
          },
          {
            "weight": 12,
            "value": 24
          }
        ]
      },
      {
        "capacity": 51,
        "items": [
          {
            "weight": 8,
            "value": 50
          },
          {
            "weight": 2,
            "value": 66
          },
          {
            "weight": 22,
            "value": 0
          },
          {
            "weight": 16,
            "value": 0
          },
          {
            "weight": 18,
            "value": 39
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 2,
            "value": 0
          },
          {
            "weight": 18,
            "value": 73
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-knapsack-020",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-002",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-024",
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
      "value": 180,
      "weight": 7,
      "selected": [
        2,
        6,
        10
      ]
    },
    "operations": 4623,
    "elapsedNanos": 182870
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 180,
  "weight": 7,
  "selected": [
    2,
    6,
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
    "id": "holdout-knapsack-007",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2048,
      "optimum": 180
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
  "fullJournalSha256": "07599291f0626925b0c4eb372093c0e6aa6a78f526e4f1ea57bc99b09fa268a5",
  "familyState": {
    "schema": 1,
    "revision": 450,
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
            "weight": 1,
            "value": 3
          },
          {
            "weight": 5,
            "value": 5
          },
          {
            "weight": 5,
            "value": 17
          },
          {
            "weight": 27,
            "value": 88
          },
          {
            "weight": 31,
            "value": 36
          },
          {
            "weight": 30,
            "value": 36
          },
          {
            "weight": 39,
            "value": 123
          },
          {
            "weight": 12,
            "value": 24
          }
        ]
      },
      {
        "capacity": 51,
        "items": [
          {
            "weight": 8,
            "value": 50
          },
          {
            "weight": 2,
            "value": 66
          },
          {
            "weight": 22,
            "value": 0
          },
          {
            "weight": 16,
            "value": 0
          },
          {
            "weight": 18,
            "value": 39
          },
          {
            "weight": 10,
            "value": 0
          },
          {
            "weight": 2,
            "value": 0
          },
          {
            "weight": 18,
            "value": 73
          }
        ]
      },
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
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-knapsack-020",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-002",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-024",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-knapsack-007",
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
  "id": "holdout-knapsack-007",
  "family": "knapsack",
  "description": "Mezcla de alternativas factibles y recursos que exceden el presupuesto",
  "input": {
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
  }
}
```

### operations

```json
4623
```

### elapsedNanos

```json
423444
```

### adapted

```json
false
```

### reused

```json
true
```
