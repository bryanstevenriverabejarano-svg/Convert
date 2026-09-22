# knapsack-024

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-024-r1

### identifier

```json
"knapsack-024-r1"
```

### objective

```json
"Capacidad impar con pesos pares"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "9ec78881755cd0b47b979c7aeb54f0de90edbcfa1dd179e21ff653c957ec6b0c",
  "familyState": {
    "schema": 1,
    "revision": 49,
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
        "capacity": 100,
        "items": [
          {
            "weight": 51,
            "value": 89
          },
          {
            "weight": 49,
            "value": 91
          },
          {
            "weight": 34,
            "value": 67
          },
          {
            "weight": 33,
            "value": 64
          },
          {
            "weight": 33,
            "value": 65
          }
        ]
      },
      {
        "capacity": 21,
        "items": [
          {
            "weight": 2,
            "value": 8
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
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
      },
      {
        "id": "knapsack-015-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":22,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 29,
      "weight": 16,
      "selected": [
        2,
        4
      ]
    },
    "operations": 1214,
    "elapsedNanos": 444756
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 29,
  "weight": 16,
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
    "id": "knapsack-024-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 29
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
  "fullJournalSha256": "c30fa6aa30dc49663fceacd14dfcb7be5e56f12e8f4c60dcc7e0995335787de1",
  "familyState": {
    "schema": 1,
    "revision": 50,
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
        "capacity": 21,
        "items": [
          {
            "weight": 2,
            "value": 8
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          }
        ]
      },
      {
        "capacity": 17,
        "items": [
          {
            "weight": 2,
            "value": 2
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 8,
            "value": 14
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
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
      },
      {
        "id": "knapsack-015-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-024-r1",
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
  "id": "knapsack-024-r1",
  "caseId": "knapsack-024",
  "round": 1,
  "family": "knapsack",
  "description": "Capacidad impar con pesos pares",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 17,
    "items": [
      {
        "weight": 2,
        "value": 2
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 8,
        "value": 14
      },
      {
        "weight": 10,
        "value": 19
      },
      {
        "weight": 12,
        "value": 20
      }
    ]
  }
}
```

### operations

```json
1214
```

### elapsedNanos

```json
980588
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-024-r2

### identifier

```json
"knapsack-024-r2"
```

### objective

```json
"Capacidad impar con pesos pares"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "69f84442a6616108a71f7975b879dcbdbc8d3bc8ca64f4ae5eb28dfd7907a948",
  "familyState": {
    "schema": 1,
    "revision": 153,
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
        "capacity": 100,
        "items": [
          {
            "weight": 51,
            "value": 89
          },
          {
            "weight": 49,
            "value": 91
          },
          {
            "weight": 34,
            "value": 67
          },
          {
            "weight": 33,
            "value": 64
          },
          {
            "weight": 33,
            "value": 65
          },
          {
            "weight": 33,
            "value": 92
          }
        ]
      },
      {
        "capacity": 21,
        "items": [
          {
            "weight": 2,
            "value": 8
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          },
          {
            "weight": 7,
            "value": 31
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 19
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "knapsack-015-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 42,
      "weight": 17,
      "selected": [
        1,
        3,
        6
      ]
    },
    "operations": 1614,
    "elapsedNanos": 184682
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 42,
  "weight": 17,
  "selected": [
    1,
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
    "id": "knapsack-024-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 42
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
  "fullJournalSha256": "9077ac59229d895645c3a9aeffde01fd62b61e186cdbac89d1853ab35a0eeba0",
  "familyState": {
    "schema": 1,
    "revision": 154,
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
        "capacity": 21,
        "items": [
          {
            "weight": 2,
            "value": 8
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          },
          {
            "weight": 7,
            "value": 31
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 19
          }
        ]
      },
      {
        "capacity": 17,
        "items": [
          {
            "weight": 2,
            "value": 2
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 8,
            "value": 14
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
          },
          {
            "weight": 5,
            "value": 21
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "knapsack-015-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-024-r2",
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
  "id": "knapsack-024-r2",
  "caseId": "knapsack-024",
  "round": 2,
  "family": "knapsack",
  "description": "Capacidad impar con pesos pares",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 17,
    "items": [
      {
        "weight": 2,
        "value": 2
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 8,
        "value": 14
      },
      {
        "weight": 10,
        "value": 19
      },
      {
        "weight": 12,
        "value": 20
      },
      {
        "weight": 5,
        "value": 21
      }
    ]
  }
}
```

### operations

```json
1614
```

### elapsedNanos

```json
486948
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-024-r3

### identifier

```json
"knapsack-024-r3"
```

### objective

```json
"Capacidad impar con pesos pares"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "19be0b80d22f5a6a3d4be86383b492c9bc3bac00fe2ab8a4635497af2a922c0b",
  "familyState": {
    "schema": 1,
    "revision": 257,
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
        "capacity": 100,
        "items": [
          {
            "weight": 49,
            "value": 91
          },
          {
            "weight": 34,
            "value": 67
          },
          {
            "weight": 33,
            "value": 64
          },
          {
            "weight": 33,
            "value": 65
          },
          {
            "weight": 25,
            "value": 44
          },
          {
            "weight": 26,
            "value": 46
          }
        ]
      },
      {
        "capacity": 22,
        "items": [
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          },
          {
            "weight": 1,
            "value": 4
          },
          {
            "weight": 1,
            "value": 5
          }
        ]
      },
      {
        "capacity": 16,
        "items": [
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 10
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "knapsack-015-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 33,
      "weight": 18,
      "selected": [
        2,
        3
      ]
    },
    "operations": 1688,
    "elapsedNanos": 87669
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 33,
  "weight": 18,
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
    "id": "knapsack-024-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 33
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
  "fullJournalSha256": "248ed15fa9ac8e38f62637d78440ff14f071f6bbd0e0e531fb926a3b6f83f8ba",
  "familyState": {
    "schema": 1,
    "revision": 258,
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
        "capacity": 22,
        "items": [
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 8,
            "value": 6
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          },
          {
            "weight": 1,
            "value": 4
          },
          {
            "weight": 1,
            "value": 5
          }
        ]
      },
      {
        "capacity": 16,
        "items": [
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 10
          }
        ]
      },
      {
        "capacity": 18,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 8,
            "value": 14
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
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
      },
      {
        "id": "knapsack-015-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-024-r3",
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
  "id": "knapsack-024-r3",
  "caseId": "knapsack-024",
  "round": 3,
  "family": "knapsack",
  "description": "Capacidad impar con pesos pares",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 18,
    "items": [
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 8,
        "value": 14
      },
      {
        "weight": 10,
        "value": 19
      },
      {
        "weight": 12,
        "value": 20
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
1688
```

### elapsedNanos

```json
347633
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-024-r4

### identifier

```json
"knapsack-024-r4"
```

### objective

```json
"Capacidad impar con pesos pares"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "e305affdb0ca38bc0ace1763868b711b298103b145eec83a06931e5df2fb476f",
  "familyState": {
    "schema": 1,
    "revision": 361,
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
        "capacity": 98,
        "items": [
          {
            "weight": 51,
            "value": 89
          },
          {
            "weight": 49,
            "value": 91
          },
          {
            "weight": 33,
            "value": 64
          },
          {
            "weight": 33,
            "value": 65
          }
        ]
      },
      {
        "capacity": 19,
        "items": [
          {
            "weight": 2,
            "value": 8
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "knapsack-015-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r4",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
      "weight": 14,
      "selected": [
        1,
        3
      ]
    },
    "operations": 900,
    "elapsedNanos": 85226
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 26,
  "weight": 14,
  "selected": [
    1,
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
    "id": "knapsack-024-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
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
  "fullJournalSha256": "0451d4e67eb7d81a654b39b0d186edff2c79311831f97f82f21a027f10771117",
  "familyState": {
    "schema": 1,
    "revision": 362,
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
            "weight": 2,
            "value": 8
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 6,
            "value": 15
          },
          {
            "weight": 10,
            "value": 23
          },
          {
            "weight": 12,
            "value": 9
          },
          {
            "weight": 14,
            "value": 30
          }
        ]
      },
      {
        "capacity": 13,
        "items": [
          {
            "weight": 8,
            "value": 18
          },
          {
            "weight": 7,
            "value": 15
          },
          {
            "weight": 5,
            "value": 13
          },
          {
            "weight": 5,
            "value": 13
          }
        ]
      },
      {
        "capacity": 15,
        "items": [
          {
            "weight": 2,
            "value": 2
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "knapsack-015-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-016-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-017-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-018-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-019-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-020-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-021-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-022-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-023-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-024-r4",
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
  "id": "knapsack-024-r4",
  "caseId": "knapsack-024",
  "round": 4,
  "family": "knapsack",
  "description": "Capacidad impar con pesos pares",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 15,
    "items": [
      {
        "weight": 2,
        "value": 2
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 6,
        "value": 10
      },
      {
        "weight": 10,
        "value": 19
      },
      {
        "weight": 12,
        "value": 20
      }
    ]
  }
}
```

### operations

```json
900
```

### elapsedNanos

```json
295836
```

### adapted

```json
false
```

### reused

```json
true
```
