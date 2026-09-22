# knapsack-014

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-014-r1

### identifier

```json
"knapsack-014-r1"
```

### objective

```json
"Potencias de dos en pesos"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "2ce23bd0c8b1261eb96d748087150f1a6b2fdda85b573803546ad3b4f9e4b993",
  "familyState": {
    "schema": 1,
    "revision": 39,
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
        "capacity": 19,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 7,
            "value": 12
          },
          {
            "weight": 9,
            "value": 16
          },
          {
            "weight": 11,
            "value": 20
          }
        ]
      },
      {
        "capacity": 12,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-002-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 3
      },
      {
        "id": "knapsack-003-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 64,
      "weight": 31,
      "selected": [
        0,
        1,
        2,
        3,
        4
      ]
    },
    "operations": 888,
    "elapsedNanos": 442423
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 64,
  "weight": 31,
  "selected": [
    0,
    1,
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
    "id": "knapsack-014-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 64
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
  "fullJournalSha256": "598408b8c52599c985b1e2228d7da8a8c4c1b13e656a51c946ab25de0da68776",
  "familyState": {
    "schema": 1,
    "revision": 40,
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
        "capacity": 12,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      },
      {
        "capacity": 31,
        "items": [
          {
            "weight": 1,
            "value": 1
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 16,
            "value": 34
          },
          {
            "weight": 32,
            "value": 100
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-002-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 3
      },
      {
        "id": "knapsack-003-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-014-r1",
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
  "id": "knapsack-014-r1",
  "caseId": "knapsack-014",
  "round": 1,
  "family": "knapsack",
  "description": "Potencias de dos en pesos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 31,
    "items": [
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 16,
        "value": 34
      },
      {
        "weight": 32,
        "value": 100
      }
    ]
  }
}
```

### operations

```json
888
```

### elapsedNanos

```json
1115707
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-014-r2

### identifier

```json
"knapsack-014-r2"
```

### objective

```json
"Potencias de dos en pesos"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "b29edf2940416cdca3b597eaa1b2a81b4f29561b37f69497f1efb74ddab19834",
  "familyState": {
    "schema": 1,
    "revision": 143,
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
        "capacity": 19,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 7,
            "value": 12
          },
          {
            "weight": 9,
            "value": 16
          },
          {
            "weight": 11,
            "value": 20
          },
          {
            "weight": 6,
            "value": 21
          }
        ]
      },
      {
        "capacity": 12,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 4,
            "value": 12
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 4,
            "value": 31
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-003-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 145,
      "weight": 31,
      "selected": [
        0,
        2,
        4,
        6
      ]
    },
    "operations": 1219,
    "elapsedNanos": 170822
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 145,
  "weight": 31,
  "selected": [
    0,
    2,
    4,
    6
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
    "id": "knapsack-014-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 145
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
  "fullJournalSha256": "a1458cfdc9d5efe3511890be065e163825ccf7a5a09bc452a4be3490989be882",
  "familyState": {
    "schema": 1,
    "revision": 144,
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
        "capacity": 12,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 4,
            "value": 12
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 4,
            "value": 31
          }
        ]
      },
      {
        "capacity": 31,
        "items": [
          {
            "weight": 1,
            "value": 1
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 16,
            "value": 34
          },
          {
            "weight": 32,
            "value": 100
          },
          {
            "weight": 10,
            "value": 101
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-003-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-014-r2",
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
  "id": "knapsack-014-r2",
  "caseId": "knapsack-014",
  "round": 2,
  "family": "knapsack",
  "description": "Potencias de dos en pesos",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 31,
    "items": [
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 16,
        "value": 34
      },
      {
        "weight": 32,
        "value": 100
      },
      {
        "weight": 10,
        "value": 101
      }
    ]
  }
}
```

### operations

```json
1219
```

### elapsedNanos

```json
503313
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-014-r3

### identifier

```json
"knapsack-014-r3"
```

### objective

```json
"Potencias de dos en pesos"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "c9dc0963b2f2f26d65a2f7344d4cf1d361f92266568f86e3251c3b12bf585ba9",
  "familyState": {
    "schema": 1,
    "revision": 247,
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
        "capacity": 20,
        "items": [
          {
            "weight": 7,
            "value": 12
          },
          {
            "weight": 9,
            "value": 16
          },
          {
            "weight": 11,
            "value": 20
          },
          {
            "weight": 2,
            "value": 3
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 2,
            "value": 3
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      },
      {
        "capacity": 14,
        "items": [
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 7,
            "value": 16
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-003-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 100,
      "weight": 32,
      "selected": [
        4
      ]
    },
    "operations": 1269,
    "elapsedNanos": 102080
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 100,
  "weight": 32,
  "selected": [
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
    "id": "knapsack-014-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 100
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
  "fullJournalSha256": "efb987a50524512bc1fe5b215cea0899ac578b4a224acde695c70945c7cc5aa8",
  "familyState": {
    "schema": 1,
    "revision": 248,
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
        "capacity": 13,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 2,
            "value": 3
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      },
      {
        "capacity": 14,
        "items": [
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 7,
            "value": 16
          }
        ]
      },
      {
        "capacity": 32,
        "items": [
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 16,
            "value": 34
          },
          {
            "weight": 32,
            "value": 100
          },
          {
            "weight": 1,
            "value": 0
          },
          {
            "weight": 1,
            "value": 2
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-003-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-014-r3",
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
  "id": "knapsack-014-r3",
  "caseId": "knapsack-014",
  "round": 3,
  "family": "knapsack",
  "description": "Potencias de dos en pesos",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 32,
    "items": [
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 16,
        "value": 34
      },
      {
        "weight": 32,
        "value": 100
      },
      {
        "weight": 1,
        "value": 0
      },
      {
        "weight": 1,
        "value": 2
      }
    ]
  }
}
```

### operations

```json
1269
```

### elapsedNanos

```json
324890
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-014-r4

### identifier

```json
"knapsack-014-r4"
```

### objective

```json
"Potencias de dos en pesos"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "26fb829ea1ff7226416da4fbf5073f62af5f5f0184e1aa12c6e15d2656fd45c9",
  "familyState": {
    "schema": 1,
    "revision": 351,
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
        "capacity": 17,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 7,
            "value": 12
          },
          {
            "weight": 11,
            "value": 20
          }
        ]
      },
      {
        "capacity": 10,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          }
        ]
      },
      {
        "capacity": 11,
        "items": [
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-003-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r4",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 49,
      "weight": 23,
      "selected": [
        0,
        1,
        2,
        3
      ]
    },
    "operations": 633,
    "elapsedNanos": 61210
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 49,
  "weight": 23,
  "selected": [
    0,
    1,
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
    "id": "knapsack-014-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 49
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
  "fullJournalSha256": "2cfa92ad4db97dbb63f4aa1ad7632a6ba214e114720f84704b81906ebf5ac624",
  "familyState": {
    "schema": 1,
    "revision": 352,
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
        "capacity": 10,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 6,
            "value": 11
          }
        ]
      },
      {
        "capacity": 11,
        "items": [
          {
            "weight": 13,
            "value": 30
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      },
      {
        "capacity": 29,
        "items": [
          {
            "weight": 1,
            "value": 1
          },
          {
            "weight": 2,
            "value": 5
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 16,
            "value": 34
          },
          {
            "weight": 32,
            "value": 100
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-003-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-004-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-005-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-006-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-007-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-008-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-009-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-010-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-011-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-012-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-013-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-014-r4",
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
  "id": "knapsack-014-r4",
  "caseId": "knapsack-014",
  "round": 4,
  "family": "knapsack",
  "description": "Potencias de dos en pesos",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 29,
    "items": [
      {
        "weight": 1,
        "value": 1
      },
      {
        "weight": 2,
        "value": 5
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 16,
        "value": 34
      },
      {
        "weight": 32,
        "value": 100
      }
    ]
  }
}
```

### operations

```json
633
```

### elapsedNanos

```json
363046
```

### adapted

```json
false
```

### reused

```json
true
```
