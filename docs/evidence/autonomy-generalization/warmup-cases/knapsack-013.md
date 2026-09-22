# knapsack-013

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-013-r1

### identifier

```json
"knapsack-013-r1"
```

### objective

```json
"Un objeto voluminoso compite con muchas piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "ce6455d3185781da53eb0c268937b47b3639c88dc76ce5a51879c3e49c631081",
  "familyState": {
    "schema": 1,
    "revision": 38,
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
            "value": 8
          },
          {
            "weight": 5,
            "value": 7
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 7,
            "value": 17
          }
        ]
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":11,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 30,
      "weight": 12,
      "selected": [
        1,
        2,
        3,
        4,
        5,
        6
      ]
    },
    "operations": 734,
    "elapsedNanos": 458216
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 30,
  "weight": 12,
  "selected": [
    1,
    2,
    3,
    4,
    5,
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
    "id": "knapsack-013-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 30
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-013-r1",
  "caseId": "knapsack-013",
  "round": 1,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
}
```

### operations

```json
734
```

### elapsedNanos

```json
1072052
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-013-r2

### identifier

```json
"knapsack-013-r2"
```

### objective

```json
"Un objeto voluminoso compite con muchas piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "b2b97e57529f6d323106fbc26761996fd2eb0bc9de072cc2ce67b888bd41742b",
  "familyState": {
    "schema": 1,
    "revision": 142,
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
            "value": 8
          },
          {
            "weight": 5,
            "value": 7
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 7,
            "value": 17
          },
          {
            "weight": 4,
            "value": 18
          }
        ]
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 51,
      "weight": 12,
      "selected": [
        1,
        2,
        3,
        4,
        7
      ]
    },
    "operations": 1022,
    "elapsedNanos": 161028
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 51,
  "weight": 12,
  "selected": [
    1,
    2,
    3,
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
    "id": "knapsack-013-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 51
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-013-r2",
  "caseId": "knapsack-013",
  "round": 2,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
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
}
```

### operations

```json
1022
```

### elapsedNanos

```json
513338
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-013-r3

### identifier

```json
"knapsack-013-r3"
```

### objective

```json
"Un objeto voluminoso compite con muchas piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "736a15768cd50ec2a13b2b08ead6cbd4971ebcb0f1c83c91aa585539e7d9a5e6",
  "familyState": {
    "schema": 1,
    "revision": 246,
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
            "weight": 5,
            "value": 7
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 7,
            "value": 17
          },
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 2,
            "value": 5
          }
        ]
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 35,
      "weight": 14,
      "selected": [
        0,
        1,
        2,
        3,
        6
      ]
    },
    "operations": 1069,
    "elapsedNanos": 78927
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 35,
  "weight": 14,
  "selected": [
    0,
    1,
    2,
    3,
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
    "id": "knapsack-013-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 35
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-013-r3",
  "caseId": "knapsack-013",
  "round": 3,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
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
}
```

### operations

```json
1069
```

### elapsedNanos

```json
309006
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-013-r4

### identifier

```json
"knapsack-013-r4"
```

### objective

```json
"Un objeto voluminoso compite con muchas piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "82aa68eb65ef2a48a6b148a72c867132f303069275bc8639184cf7d50b208198",
  "familyState": {
    "schema": 1,
    "revision": 350,
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
            "value": 8
          },
          {
            "weight": 5,
            "value": 7
          },
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 7,
            "value": 17
          }
        ]
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 25,
      "weight": 10,
      "selected": [
        1,
        2,
        3,
        4,
        5
      ]
    },
    "operations": 509,
    "elapsedNanos": 62932
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 25,
  "weight": 10,
  "selected": [
    1,
    2,
    3,
    4,
    5
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
    "id": "knapsack-013-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 25
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-013-r4",
  "caseId": "knapsack-013",
  "round": 4,
  "family": "knapsack",
  "description": "Un objeto voluminoso compite con muchas piezas",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
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
}
```

### operations

```json
509
```

### elapsedNanos

```json
282326
```

### adapted

```json
false
```

### reused

```json
true
```
