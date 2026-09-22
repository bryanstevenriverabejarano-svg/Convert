# holdout-knapsack-004

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-004

### identifier

```json
"holdout-knapsack-004"
```

### objective

```json
"Pesos pares, recompensas dispersas y valores cero"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "c96a41a77d2e63eb6628cf46897e60d053f3ac4fd367dd7a3695c229ebf9177d",
  "familyState": {
    "schema": 1,
    "revision": 429,
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
      "value": 141,
      "weight": 26,
      "selected": [
        4,
        7
      ]
    },
    "operations": 2055,
    "elapsedNanos": 91926
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 141,
  "weight": 26,
  "selected": [
    4,
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
    "id": "holdout-knapsack-004",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 141
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
  "fullJournalSha256": "6083407e33877548c466fa5a57813959b987f5e1d2647604c668e119f6d2a3ea",
  "familyState": {
    "schema": 1,
    "revision": 430,
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-knapsack-004",
  "family": "knapsack",
  "description": "Pesos pares, recompensas dispersas y valores cero",
  "input": {
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
}
```

### operations

```json
2055
```

### elapsedNanos

```json
290609
```

### adapted

```json
false
```

### reused

```json
true
```
