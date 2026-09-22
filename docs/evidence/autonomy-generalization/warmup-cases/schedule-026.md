# schedule-026

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-026-r1

### identifier

```json
"schedule-026-r1"
```

### objective

```json
"Catorce trabajos y subproblemas repetidos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "d23f786b5783fd486816b841253770d948e999b28cb97e4abcb1b817145c8481",
  "familyState": {
    "schema": 1,
    "revision": 77,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 0,
            "end": 4,
            "value": 6
          },
          {
            "start": 4,
            "end": 7,
            "value": 5
          },
          {
            "start": 7,
            "end": 10,
            "value": 8
          },
          {
            "start": 3,
            "end": 8,
            "value": 14
          },
          {
            "start": 0,
            "end": 10,
            "value": 18
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 4
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 7,
            "end": 10,
            "value": 7
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-004-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 3
      },
      {
        "id": "schedule-005-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
    "strategy": "WEIGHTED_DP",
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "WEIGHTED_DP"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":22,\"candidatos_descartados_en_recibos\":2}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 82,
      "selected": [
        1,
        2,
        5,
        7,
        9,
        10,
        12
      ]
    },
    "operations": 22282,
    "elapsedNanos": 2682519
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 82,
  "selected": [
    1,
    2,
    5,
    7,
    9,
    10,
    12
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
    "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "schedule-026-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 82
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "ccd7ceb852ce56b2ae165508e0963abbc1655074150007da2bc6aa76fcf09fdc",
  "familyState": {
    "schema": 1,
    "revision": 78,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 4
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 7,
            "end": 10,
            "value": 7
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 3
          },
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 6,
            "end": 8,
            "value": 12
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 12,
            "end": 14,
            "value": 3
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-005-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-026-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
  "id": "schedule-026-r1",
  "caseId": "schedule-026",
  "round": 1,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 1,
        "value": 3
      },
      {
        "start": 0,
        "end": 2,
        "value": 8
      },
      {
        "start": 2,
        "end": 3,
        "value": 13
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 4,
        "end": 5,
        "value": 10
      },
      {
        "start": 4,
        "end": 6,
        "value": 15
      },
      {
        "start": 6,
        "end": 7,
        "value": 7
      },
      {
        "start": 6,
        "end": 8,
        "value": 12
      },
      {
        "start": 8,
        "end": 9,
        "value": 4
      },
      {
        "start": 8,
        "end": 10,
        "value": 9
      },
      {
        "start": 10,
        "end": 11,
        "value": 14
      },
      {
        "start": 10,
        "end": 12,
        "value": 6
      },
      {
        "start": 12,
        "end": 13,
        "value": 11
      },
      {
        "start": 12,
        "end": 14,
        "value": 3
      }
    ]
  }
}
```

### operations

```json
22282
```

### elapsedNanos

```json
3560395
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-026-r2

### identifier

```json
"schedule-026-r2"
```

### objective

```json
"Catorce trabajos y subproblemas repetidos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "ac9cce97db459f272b4c7fbe22802dcc41046f7a090c0117ceb0142e56976547",
  "familyState": {
    "schema": 1,
    "revision": 181,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 0,
            "end": 4,
            "value": 6
          },
          {
            "start": 4,
            "end": 7,
            "value": 5
          },
          {
            "start": 7,
            "end": 10,
            "value": 8
          },
          {
            "start": 3,
            "end": 8,
            "value": 14
          },
          {
            "start": 0,
            "end": 10,
            "value": 18
          },
          {
            "start": -1,
            "end": 11,
            "value": 26
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 4
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 7,
            "end": 10,
            "value": 7
          },
          {
            "start": -1,
            "end": 13,
            "value": 23
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          },
          {
            "start": -1,
            "end": 16,
            "value": 57
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-005-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
    "strategy": "WEIGHTED_DP",
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "WEIGHTED_DP"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 82,
      "selected": [
        0,
        1,
        4,
        6,
        8,
        9,
        11
      ]
    },
    "operations": 27045,
    "elapsedNanos": 640345
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 82,
  "selected": [
    0,
    1,
    4,
    6,
    8,
    9,
    11
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
    "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "schedule-026-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 82
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "e1b3c6558ee2829c9497334042f597e08894ed3c843b2220bec82b248a44c5ed",
  "familyState": {
    "schema": 1,
    "revision": 182,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 4
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 7,
            "end": 10,
            "value": 7
          },
          {
            "start": -1,
            "end": 13,
            "value": 23
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          },
          {
            "start": -1,
            "end": 16,
            "value": 57
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 6,
            "end": 8,
            "value": 12
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 12,
            "end": 14,
            "value": 3
          },
          {
            "start": -1,
            "end": 15,
            "value": 61
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-006-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-026-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
  "id": "schedule-026-r2",
  "caseId": "schedule-026",
  "round": 2,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 8
      },
      {
        "start": 2,
        "end": 3,
        "value": 13
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 4,
        "end": 5,
        "value": 10
      },
      {
        "start": 4,
        "end": 6,
        "value": 15
      },
      {
        "start": 6,
        "end": 7,
        "value": 7
      },
      {
        "start": 6,
        "end": 8,
        "value": 12
      },
      {
        "start": 8,
        "end": 9,
        "value": 4
      },
      {
        "start": 8,
        "end": 10,
        "value": 9
      },
      {
        "start": 10,
        "end": 11,
        "value": 14
      },
      {
        "start": 10,
        "end": 12,
        "value": 6
      },
      {
        "start": 12,
        "end": 13,
        "value": 11
      },
      {
        "start": 12,
        "end": 14,
        "value": 3
      },
      {
        "start": -1,
        "end": 15,
        "value": 61
      }
    ]
  }
}
```

### operations

```json
27045
```

### elapsedNanos

```json
1012805
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-026-r3

### identifier

```json
"schedule-026-r3"
```

### objective

```json
"Catorce trabajos y subproblemas repetidos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "85f07c344cbe0956b5e2fa72acad24e12f6136f7a012e7576e05ff691b953185",
  "familyState": {
    "schema": 1,
    "revision": 285,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 8,
            "end": 14,
            "value": 5
          },
          {
            "start": 14,
            "end": 20,
            "value": 8
          },
          {
            "start": 6,
            "end": 16,
            "value": 14
          },
          {
            "start": 0,
            "end": 20,
            "value": 18
          },
          {
            "start": 0,
            "end": 4,
            "value": 3
          },
          {
            "start": 4,
            "end": 8,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 12,
            "value": 4
          },
          {
            "start": 12,
            "end": 16,
            "value": 4
          },
          {
            "start": 16,
            "end": 20,
            "value": 4
          },
          {
            "start": 20,
            "end": 24,
            "value": 4
          },
          {
            "start": 2,
            "end": 8,
            "value": 7
          },
          {
            "start": 8,
            "end": 14,
            "value": 7
          },
          {
            "start": 14,
            "end": 20,
            "value": 7
          },
          {
            "start": 0,
            "end": 2,
            "value": 2
          },
          {
            "start": 2,
            "end": 4,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 6,
            "value": 9
          },
          {
            "start": 4,
            "end": 10,
            "value": 16
          },
          {
            "start": 6,
            "end": 14,
            "value": 6
          },
          {
            "start": 8,
            "end": 10,
            "value": 13
          },
          {
            "start": 10,
            "end": 14,
            "value": 3
          },
          {
            "start": 12,
            "end": 18,
            "value": 10
          },
          {
            "start": 14,
            "end": 22,
            "value": 17
          },
          {
            "start": 16,
            "end": 18,
            "value": 7
          },
          {
            "start": 18,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 26,
            "value": 4
          },
          {
            "start": 22,
            "end": 30,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 2
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-005-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
    "strategy": "WEIGHTED_DP",
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "WEIGHTED_DP"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 82,
      "selected": [
        0,
        1,
        4,
        6,
        8,
        9,
        11
      ]
    },
    "operations": 27066,
    "elapsedNanos": 487009
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 82,
  "selected": [
    0,
    1,
    4,
    6,
    8,
    9,
    11
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
    "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "schedule-026-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 82
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "5469a2ac590e7a6bab94fd26586f650d4497e6e5eb6c5136027b6ffa65dab5c2",
  "familyState": {
    "schema": 1,
    "revision": 286,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 4,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 12,
            "value": 4
          },
          {
            "start": 12,
            "end": 16,
            "value": 4
          },
          {
            "start": 16,
            "end": 20,
            "value": 4
          },
          {
            "start": 20,
            "end": 24,
            "value": 4
          },
          {
            "start": 2,
            "end": 8,
            "value": 7
          },
          {
            "start": 8,
            "end": 14,
            "value": 7
          },
          {
            "start": 14,
            "end": 20,
            "value": 7
          },
          {
            "start": 0,
            "end": 2,
            "value": 2
          },
          {
            "start": 2,
            "end": 4,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 6,
            "value": 9
          },
          {
            "start": 4,
            "end": 10,
            "value": 16
          },
          {
            "start": 6,
            "end": 14,
            "value": 6
          },
          {
            "start": 8,
            "end": 10,
            "value": 13
          },
          {
            "start": 10,
            "end": 14,
            "value": 3
          },
          {
            "start": 12,
            "end": 18,
            "value": 10
          },
          {
            "start": 14,
            "end": 22,
            "value": 17
          },
          {
            "start": 16,
            "end": 18,
            "value": 7
          },
          {
            "start": 18,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 26,
            "value": 4
          },
          {
            "start": 22,
            "end": 30,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 4,
            "value": 8
          },
          {
            "start": 4,
            "end": 6,
            "value": 13
          },
          {
            "start": 4,
            "end": 8,
            "value": 5
          },
          {
            "start": 8,
            "end": 10,
            "value": 10
          },
          {
            "start": 8,
            "end": 12,
            "value": 15
          },
          {
            "start": 12,
            "end": 14,
            "value": 7
          },
          {
            "start": 12,
            "end": 16,
            "value": 12
          },
          {
            "start": 16,
            "end": 18,
            "value": 4
          },
          {
            "start": 16,
            "end": 20,
            "value": 9
          },
          {
            "start": 20,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 24,
            "value": 6
          },
          {
            "start": 24,
            "end": 26,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 3
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-006-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-026-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
  "id": "schedule-026-r3",
  "caseId": "schedule-026",
  "round": 3,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 4,
        "value": 8
      },
      {
        "start": 4,
        "end": 6,
        "value": 13
      },
      {
        "start": 4,
        "end": 8,
        "value": 5
      },
      {
        "start": 8,
        "end": 10,
        "value": 10
      },
      {
        "start": 8,
        "end": 12,
        "value": 15
      },
      {
        "start": 12,
        "end": 14,
        "value": 7
      },
      {
        "start": 12,
        "end": 16,
        "value": 12
      },
      {
        "start": 16,
        "end": 18,
        "value": 4
      },
      {
        "start": 16,
        "end": 20,
        "value": 9
      },
      {
        "start": 20,
        "end": 22,
        "value": 14
      },
      {
        "start": 20,
        "end": 24,
        "value": 6
      },
      {
        "start": 24,
        "end": 26,
        "value": 11
      },
      {
        "start": 0,
        "end": 1,
        "value": 1
      },
      {
        "start": 1,
        "end": 2,
        "value": 3
      }
    ]
  }
}
```

### operations

```json
27066
```

### elapsedNanos

```json
873700
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-026-r4

### identifier

```json
"schedule-026-r4"
```

### objective

```json
"Catorce trabajos y subproblemas repetidos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "b9f704f83e60c0f26ce34aea5aafa005bff6bcf2a3680f7c30e80890cc49f031",
  "familyState": {
    "schema": 1,
    "revision": 389,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 0,
            "end": 10,
            "value": 18
          },
          {
            "start": 3,
            "end": 8,
            "value": 14
          },
          {
            "start": 4,
            "end": 7,
            "value": 5
          },
          {
            "start": 0,
            "end": 4,
            "value": 6
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 10,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 0,
            "end": 2,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 11,
            "end": 15,
            "value": 11
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 0,
            "end": 1,
            "value": 2
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-005-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
    "strategy": "WEIGHTED_DP",
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "WEIGHTED_DP"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 77,
      "selected": [
        11,
        10,
        7,
        6,
        4,
        3,
        1
      ]
    },
    "operations": 11608,
    "elapsedNanos": 287153
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 77,
  "selected": [
    11,
    10,
    7,
    6,
    4,
    3,
    1
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
    "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "schedule-026-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 77
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "528cd201128bc78f74c942937e9e2e726efaf3ef3c63e932609af6643fce795a",
  "familyState": {
    "schema": 1,
    "revision": 390,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "WEIGHTED_DP"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "schedule",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "EARLIEST_FINISH"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "jobs": [
          {
            "start": 7,
            "end": 10,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 0,
            "end": 2,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 11,
            "end": 15,
            "value": 11
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 0,
            "end": 1,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 12,
            "end": 14,
            "value": 3
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 0,
            "end": 1,
            "value": 3
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-006-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-016-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-017-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-018-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-019-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-020-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-021-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-022-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-023-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-024-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-025-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-026-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
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
  "id": "schedule-026-r4",
  "caseId": "schedule-026",
  "round": 4,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 12,
        "end": 14,
        "value": 3
      },
      {
        "start": 12,
        "end": 13,
        "value": 11
      },
      {
        "start": 10,
        "end": 12,
        "value": 6
      },
      {
        "start": 10,
        "end": 11,
        "value": 14
      },
      {
        "start": 8,
        "end": 10,
        "value": 9
      },
      {
        "start": 8,
        "end": 9,
        "value": 4
      },
      {
        "start": 6,
        "end": 7,
        "value": 7
      },
      {
        "start": 4,
        "end": 6,
        "value": 15
      },
      {
        "start": 4,
        "end": 5,
        "value": 10
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 2,
        "end": 3,
        "value": 13
      },
      {
        "start": 0,
        "end": 2,
        "value": 8
      },
      {
        "start": 0,
        "end": 1,
        "value": 3
      }
    ]
  }
}
```

### operations

```json
11608
```

### elapsedNanos

```json
799660
```

### adapted

```json
false
```

### reused

```json
true
```
