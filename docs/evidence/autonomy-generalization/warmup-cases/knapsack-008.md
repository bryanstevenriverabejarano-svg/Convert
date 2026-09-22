# knapsack-008

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-008-r1

### identifier

```json
"knapsack-008-r1"
```

### objective

```json
"Todo cabe exactamente"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "7fc701f9f94d8bba7adc38802b56933f1cebbf75d325f0fc8a0dc373e503b00b",
  "familyState": {
    "schema": 1,
    "revision": 33,
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
      },
      {
        "capacity": 2,
        "items": [
          {
            "weight": 3,
            "value": 20
          },
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 8,
            "value": 30
          },
          {
            "weight": 4,
            "value": 9
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
      },
      {
        "id": "knapsack-007-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":6,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 23,
      "weight": 15,
      "selected": [
        0,
        1,
        2,
        3,
        4
      ]
    },
    "operations": 435,
    "elapsedNanos": 376435
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 23,
  "weight": 15,
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
    "id": "knapsack-008-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 23
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
  "fullJournalSha256": "7bb1811b0b1cd5d20ad60be67d8e5f36030cd46d18c80f8f6977e66e8a693ced",
  "familyState": {
    "schema": 1,
    "revision": 34,
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
      },
      {
        "capacity": 2,
        "items": [
          {
            "weight": 3,
            "value": 20
          },
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 8,
            "value": 30
          },
          {
            "weight": 4,
            "value": 9
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 3,
            "value": 3
          },
          {
            "weight": 4,
            "value": 8
          },
          {
            "weight": 5,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-008-r1",
  "caseId": "knapsack-008",
  "round": 1,
  "family": "knapsack",
  "description": "Todo cabe exactamente",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 15,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 2,
        "value": 4
      },
      {
        "weight": 3,
        "value": 3
      },
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 6
      }
    ]
  }
}
```

### operations

```json
435
```

### elapsedNanos

```json
800261
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-008-r2

### identifier

```json
"knapsack-008-r2"
```

### objective

```json
"Todo cabe exactamente"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "c4efa501430030b58e39f5aaa0ca9cb202d5a4086b771a837641be8c51575446",
  "familyState": {
    "schema": 1,
    "revision": 137,
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
      },
      {
        "capacity": 2,
        "items": [
          {
            "weight": 3,
            "value": 20
          },
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 8,
            "value": 30
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 1,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 26,
      "weight": 15,
      "selected": [
        0,
        1,
        2,
        3,
        5
      ]
    },
    "operations": 580,
    "elapsedNanos": 297229
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 26,
  "weight": 15,
  "selected": [
    0,
    1,
    2,
    3,
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
    "id": "knapsack-008-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 26
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
  "fullJournalSha256": "7d7f70820fc3a841c93f297661aa0012397f60ef5825f767ae1110e8bfafa4e3",
  "familyState": {
    "schema": 1,
    "revision": 138,
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
      },
      {
        "capacity": 2,
        "items": [
          {
            "weight": 3,
            "value": 20
          },
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 8,
            "value": 30
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 1,
            "value": 31
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 3,
            "value": 3
          },
          {
            "weight": 4,
            "value": 8
          },
          {
            "weight": 5,
            "value": 6
          },
          {
            "weight": 5,
            "value": 9
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-008-r2",
  "caseId": "knapsack-008",
  "round": 2,
  "family": "knapsack",
  "description": "Todo cabe exactamente",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 15,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 2,
        "value": 4
      },
      {
        "weight": 3,
        "value": 3
      },
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 6
      },
      {
        "weight": 5,
        "value": 9
      }
    ]
  }
}
```

### operations

```json
580
```

### elapsedNanos

```json
777457
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-008-r3

### identifier

```json
"knapsack-008-r3"
```

### objective

```json
"Todo cabe exactamente"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "096e19980e261c8792fecbee0c728c45b2ba925c96448b9dfaaa25390a4be9cd",
  "familyState": {
    "schema": 1,
    "revision": 241,
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
      },
      {
        "capacity": 3,
        "items": [
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 8,
            "value": 30
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 1,
            "value": 10
          },
          {
            "weight": 2,
            "value": 11
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 24,
      "weight": 16,
      "selected": [
        0,
        1,
        2,
        3,
        4,
        5
      ]
    },
    "operations": 611,
    "elapsedNanos": 114358
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 24,
  "weight": 16,
  "selected": [
    0,
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
    "id": "knapsack-008-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 24
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
  "fullJournalSha256": "7276f9d2c1c797d679c4f96c9111f0a43a1c5040984a458fe888b08babdf6d32",
  "familyState": {
    "schema": 1,
    "revision": 242,
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
      },
      {
        "capacity": 3,
        "items": [
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 8,
            "value": 30
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 1,
            "value": 10
          },
          {
            "weight": 2,
            "value": 11
          }
        ]
      },
      {
        "capacity": 16,
        "items": [
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 3,
            "value": 3
          },
          {
            "weight": 4,
            "value": 8
          },
          {
            "weight": 5,
            "value": 6
          },
          {
            "weight": 1,
            "value": 1
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-008-r3",
  "caseId": "knapsack-008",
  "round": 3,
  "family": "knapsack",
  "description": "Todo cabe exactamente",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 16,
    "items": [
      {
        "weight": 2,
        "value": 4
      },
      {
        "weight": 3,
        "value": 3
      },
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 6
      },
      {
        "weight": 1,
        "value": 1
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
611
```

### elapsedNanos

```json
348104
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-008-r4

### identifier

```json
"knapsack-008-r4"
```

### objective

```json
"Todo cabe exactamente"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "33391702df894d3e5d476fd5a26bfde0c66067c707b8481db7820b4449605c78",
  "familyState": {
    "schema": 1,
    "revision": 345,
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
      },
      {
        "capacity": 0,
        "items": [
          {
            "weight": 3,
            "value": 20
          },
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 4,
            "value": 9
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 20,
      "weight": 12,
      "selected": [
        0,
        1,
        2,
        3
      ]
    },
    "operations": 312,
    "elapsedNanos": 48792
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 20,
  "weight": 12,
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
    "id": "knapsack-008-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 20
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
  "fullJournalSha256": "c451e0e593fc51ad6832a5a6dc50270ff5d56d94896d07f5041b735a34718bfa",
  "familyState": {
    "schema": 1,
    "revision": 346,
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
      },
      {
        "capacity": 0,
        "items": [
          {
            "weight": 3,
            "value": 20
          },
          {
            "weight": 5,
            "value": 1
          },
          {
            "weight": 4,
            "value": 9
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 4,
            "value": 8
          },
          {
            "weight": 5,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-008-r4",
  "caseId": "knapsack-008",
  "round": 4,
  "family": "knapsack",
  "description": "Todo cabe exactamente",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 13,
    "items": [
      {
        "weight": 1,
        "value": 2
      },
      {
        "weight": 2,
        "value": 4
      },
      {
        "weight": 4,
        "value": 8
      },
      {
        "weight": 5,
        "value": 6
      }
    ]
  }
}
```

### operations

```json
312
```

### elapsedNanos

```json
253474
```

### adapted

```json
false
```

### reused

```json
true
```
