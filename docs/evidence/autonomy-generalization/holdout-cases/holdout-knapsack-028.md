# holdout-knapsack-028

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-knapsack-028

### identifier

```json
"holdout-knapsack-028"
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
  "fullJournalSha256": "0dc6e7b1411981a7c3d00c519635b2e461c36a6e14de84f26bcbf6fea14a65d2",
  "familyState": {
    "schema": 1,
    "revision": 540,
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
      "value": 194,
      "weight": 62,
      "selected": [
        0,
        1,
        3,
        8
      ]
    },
    "operations": 19161,
    "elapsedNanos": 163642
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 194,
  "weight": 62,
  "selected": [
    0,
    1,
    3,
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
    "id": "holdout-knapsack-028",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 194
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
  "fullJournalSha256": "724e7e4162340be48fc2fccbd6deba11f9761ae9dd7f932a0820828ef368300a",
  "familyState": {
    "schema": 1,
    "revision": 541,
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
      },
      {
        "capacity": 84,
        "items": [
          {
            "weight": 18,
            "value": 78
          },
          {
            "weight": 20,
            "value": 49
          },
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 2,
            "value": 47
          },
          {
            "weight": 24,
            "value": 0
          },
          {
            "weight": 22,
            "value": 0
          },
          {
            "weight": 14,
            "value": 0
          },
          {
            "weight": 22,
            "value": 0
          },
          {
            "weight": 22,
            "value": 20
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-knapsack-028",
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
  "id": "holdout-knapsack-028",
  "family": "knapsack",
  "description": "Pesos pares, recompensas dispersas y valores cero",
  "input": {
    "capacity": 84,
    "items": [
      {
        "weight": 18,
        "value": 78
      },
      {
        "weight": 20,
        "value": 49
      },
      {
        "weight": 14,
        "value": 0
      },
      {
        "weight": 2,
        "value": 47
      },
      {
        "weight": 24,
        "value": 0
      },
      {
        "weight": 22,
        "value": 0
      },
      {
        "weight": 14,
        "value": 0
      },
      {
        "weight": 22,
        "value": 0
      },
      {
        "weight": 22,
        "value": 20
      }
    ]
  }
}
```

### operations

```json
19161
```

### elapsedNanos

```json
432478
```

### adapted

```json
false
```

### reused

```json
true
```
