# knapsack-006

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-006-r1

### identifier

```json
"knapsack-006-r1"
```

### objective

```json
"Capacidad cero"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "f0e7649ac84afa818e37fbd1c5ef7a6262a8a3c257bcbec8e6819bcff79f22b8",
  "familyState": {
    "schema": 1,
    "revision": 31,
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
        "capacity": 8,
        "items": [
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 9
          }
        ]
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 3,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 7
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          }
        ]
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 4,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r1",
        "family": "knapsack",
        "strategy": "RATIO_GREEDY",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":4,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 0,
      "weight": 0,
      "selected": []
    },
    "operations": 399,
    "elapsedNanos": 383726
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 0,
  "weight": 0,
  "selected": []
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
    "id": "knapsack-006-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 0
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
  "fullJournalSha256": "d5d832383d1a4a7e01ce4c6e2cd421c3f626cd20bc3db09dbc56ae99e6ee55d9",
  "familyState": {
    "schema": 1,
    "revision": 32,
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
            "weight": 3,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 7
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          }
        ]
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 4,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          }
        ]
      },
      {
        "capacity": 0,
        "items": [
          {
            "weight": 1,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
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
        "id": "knapsack-001-r1",
        "family": "knapsack",
        "strategy": "RATIO_GREEDY",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-006-r1",
  "caseId": "knapsack-006",
  "round": 1,
  "family": "knapsack",
  "description": "Capacidad cero",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 0,
    "items": [
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 6,
        "value": 11
      }
    ]
  }
}
```

### operations

```json
399
```

### elapsedNanos

```json
844186
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-006-r2

### identifier

```json
"knapsack-006-r2"
```

### objective

```json
"Capacidad cero"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "adca17a340b733d7b5ee4d9ceb21f01ffd902096bc730ae7df17847d041acdca",
  "familyState": {
    "schema": 1,
    "revision": 135,
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
        "capacity": 8,
        "items": [
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 2,
            "value": 16
          }
        ]
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 3,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 7
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          },
          {
            "weight": 3,
            "value": 10
          }
        ]
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 4,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 3,
            "value": 7
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
      "value": 0,
      "weight": 0,
      "selected": []
    },
    "operations": 536,
    "elapsedNanos": 219344
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 0,
  "weight": 0,
  "selected": []
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
    "id": "knapsack-006-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 0
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
  "fullJournalSha256": "f6451fb1b0589cef278b57d20d317604f31d32e306f1289dfda56698156d6fed",
  "familyState": {
    "schema": 1,
    "revision": 136,
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
            "weight": 3,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 7
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          },
          {
            "weight": 3,
            "value": 10
          }
        ]
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 4,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 3,
            "value": 7
          }
        ]
      },
      {
        "capacity": 0,
        "items": [
          {
            "weight": 1,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 1,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-006-r2",
  "caseId": "knapsack-006",
  "round": 2,
  "family": "knapsack",
  "description": "Capacidad cero",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 0,
    "items": [
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 1,
        "value": 12
      }
    ]
  }
}
```

### operations

```json
536
```

### elapsedNanos

```json
707975
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-006-r3

### identifier

```json
"knapsack-006-r3"
```

### objective

```json
"Capacidad cero"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "911b65c80e2f79a4a7389c3f795246e07b0eb7b6c6dcd41260532b476285db7c",
  "familyState": {
    "schema": 1,
    "revision": 239,
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
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 9
          }
        ]
      },
      {
        "capacity": 10,
        "items": [
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 7
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          },
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 3
          }
        ]
      },
      {
        "capacity": 10,
        "items": [
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 4,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 4,
            "value": 4
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
      "value": 3,
      "weight": 1,
      "selected": [
        3
      ]
    },
    "operations": 568,
    "elapsedNanos": 71846
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 3,
  "weight": 1,
  "selected": [
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
    "id": "knapsack-006-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 3
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
  "fullJournalSha256": "da39ce6117e777fd9891ebf06f39d1963c5c78a263443bc1243b1dea9ca5c828",
  "familyState": {
    "schema": 1,
    "revision": 240,
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
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 7
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          },
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 3
          }
        ]
      },
      {
        "capacity": 10,
        "items": [
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 4,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 4,
            "value": 4
          }
        ]
      },
      {
        "capacity": 1,
        "items": [
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 6,
            "value": 11
          },
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 1,
            "value": 3
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-006-r3",
  "caseId": "knapsack-006",
  "round": 3,
  "family": "knapsack",
  "description": "Capacidad cero",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 1,
    "items": [
      {
        "weight": 3,
        "value": 9
      },
      {
        "weight": 6,
        "value": 11
      },
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 1,
        "value": 3
      }
    ]
  }
}
```

### operations

```json
568
```

### elapsedNanos

```json
705141
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-006-r4

### identifier

```json
"knapsack-006-r4"
```

### objective

```json
"Capacidad cero"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "1f3a6bb7f6252e298a0629db413244f301202afbda1f364ed8a78c88c07ed242",
  "familyState": {
    "schema": 1,
    "revision": 343,
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
        "capacity": 6,
        "items": [
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 4,
            "value": 9
          }
        ]
      },
      {
        "capacity": 7,
        "items": [
          {
            "weight": 3,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          }
        ]
      },
      {
        "capacity": 7,
        "items": [
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
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
      "value": 0,
      "weight": 0,
      "selected": []
    },
    "operations": 277,
    "elapsedNanos": 61160
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 0,
  "weight": 0,
  "selected": []
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
    "id": "knapsack-006-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
      "optimum": 0
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
  "fullJournalSha256": "d11b7112c074b6d0f27c519eafe949712c3cc4eb6109d8e4dcd23ecc28491132",
  "familyState": {
    "schema": 1,
    "revision": 344,
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
            "weight": 3,
            "value": 4
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 3,
            "value": 1
          },
          {
            "weight": 3,
            "value": 8
          }
        ]
      },
      {
        "capacity": 7,
        "items": [
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 2,
            "value": 6
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 6,
            "value": 6
          }
        ]
      },
      {
        "capacity": 0,
        "items": [
          {
            "weight": 1,
            "value": 4
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-006-r4",
  "caseId": "knapsack-006",
  "round": 4,
  "family": "knapsack",
  "description": "Capacidad cero",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 0,
    "items": [
      {
        "weight": 1,
        "value": 4
      },
      {
        "weight": 6,
        "value": 11
      }
    ]
  }
}
```

### operations

```json
277
```

### elapsedNanos

```json
266704
```

### adapted

```json
false
```

### reused

```json
true
```
