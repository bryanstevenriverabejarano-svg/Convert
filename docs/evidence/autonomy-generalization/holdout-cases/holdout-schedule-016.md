# holdout-schedule-016

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-016

### identifier

```json
"holdout-schedule-016"
```

### objective

```json
"Grupos temporales separados con conflictos locales"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "591435e5bffd52efca859e2fd97eda94e75ecfa61849fe8e6cccb9914f95953f",
  "familyState": {
    "schema": 1,
    "revision": 425,
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
            "start": -8,
            "end": -3,
            "value": 6
          },
          {
            "start": -15,
            "end": -1,
            "value": 19
          },
          {
            "start": 23,
            "end": 35,
            "value": 81
          },
          {
            "start": 27,
            "end": 42,
            "value": 31
          },
          {
            "start": 9,
            "end": 17,
            "value": 94
          },
          {
            "start": 13,
            "end": 14,
            "value": 4
          },
          {
            "start": 30,
            "end": 42,
            "value": 39
          },
          {
            "start": 17,
            "end": 24,
            "value": 25
          },
          {
            "start": -14,
            "end": -8,
            "value": 97
          },
          {
            "start": -14,
            "end": -11,
            "value": 65
          },
          {
            "start": 23,
            "end": 30,
            "value": 15
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 9,
            "end": 17,
            "value": 84
          },
          {
            "start": 6,
            "end": 15,
            "value": 97
          },
          {
            "start": 4,
            "end": 9,
            "value": 2
          },
          {
            "start": 107,
            "end": 116,
            "value": 58
          },
          {
            "start": 62,
            "end": 70,
            "value": 41
          },
          {
            "start": 3,
            "end": 11,
            "value": 10
          },
          {
            "start": 58,
            "end": 66,
            "value": 77
          },
          {
            "start": 55,
            "end": 63,
            "value": 22
          },
          {
            "start": 7,
            "end": 11,
            "value": 62
          },
          {
            "start": 3,
            "end": 11,
            "value": 95
          },
          {
            "start": 2,
            "end": 4,
            "value": 53
          },
          {
            "start": 8,
            "end": 14,
            "value": 60
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -15,
            "end": 4,
            "value": 28
          },
          {
            "start": -10,
            "end": 18,
            "value": 37
          },
          {
            "start": -4,
            "end": 21,
            "value": 70
          },
          {
            "start": -8,
            "end": 23,
            "value": 15
          },
          {
            "start": -17,
            "end": 6,
            "value": 50
          },
          {
            "start": -7,
            "end": 11,
            "value": 31
          },
          {
            "start": -18,
            "end": 12,
            "value": 11
          },
          {
            "start": -17,
            "end": 15,
            "value": 45
          },
          {
            "start": -5,
            "end": 13,
            "value": 31
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-008",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-032",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-005",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-020",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-006",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 411,
      "selected": [
        7,
        2,
        1,
        6,
        0
      ]
    },
    "operations": 8434,
    "elapsedNanos": 269948
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 411,
  "selected": [
    7,
    2,
    1,
    6,
    0
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
    "id": "holdout-schedule-016",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 411
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
  "fullJournalSha256": "123ef40306e7bb5ce0f4c3a85114308ef08637bbcb3da3d3b6825f023acab86b",
  "familyState": {
    "schema": 1,
    "revision": 426,
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
            "start": 9,
            "end": 17,
            "value": 84
          },
          {
            "start": 6,
            "end": 15,
            "value": 97
          },
          {
            "start": 4,
            "end": 9,
            "value": 2
          },
          {
            "start": 107,
            "end": 116,
            "value": 58
          },
          {
            "start": 62,
            "end": 70,
            "value": 41
          },
          {
            "start": 3,
            "end": 11,
            "value": 10
          },
          {
            "start": 58,
            "end": 66,
            "value": 77
          },
          {
            "start": 55,
            "end": 63,
            "value": 22
          },
          {
            "start": 7,
            "end": 11,
            "value": 62
          },
          {
            "start": 3,
            "end": 11,
            "value": 95
          },
          {
            "start": 2,
            "end": 4,
            "value": 53
          },
          {
            "start": 8,
            "end": 14,
            "value": 60
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -15,
            "end": 4,
            "value": 28
          },
          {
            "start": -10,
            "end": 18,
            "value": 37
          },
          {
            "start": -4,
            "end": 21,
            "value": 70
          },
          {
            "start": -8,
            "end": 23,
            "value": 15
          },
          {
            "start": -17,
            "end": 6,
            "value": 50
          },
          {
            "start": -7,
            "end": 11,
            "value": 31
          },
          {
            "start": -18,
            "end": 12,
            "value": 11
          },
          {
            "start": -17,
            "end": 15,
            "value": 45
          },
          {
            "start": -5,
            "end": 13,
            "value": 31
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 106,
            "end": 107,
            "value": 86
          },
          {
            "start": 50,
            "end": 54,
            "value": 92
          },
          {
            "start": 8,
            "end": 15,
            "value": 66
          },
          {
            "start": 60,
            "end": 68,
            "value": 72
          },
          {
            "start": 53,
            "end": 55,
            "value": 1
          },
          {
            "start": 12,
            "end": 18,
            "value": 12
          },
          {
            "start": 59,
            "end": 62,
            "value": 84
          },
          {
            "start": 5,
            "end": 6,
            "value": 83
          },
          {
            "start": 3,
            "end": 7,
            "value": 4
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-008",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-032",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-005",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-020",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-006",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-016",
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
  "id": "holdout-schedule-016",
  "family": "schedule",
  "description": "Grupos temporales separados con conflictos locales",
  "input": {
    "jobs": [
      {
        "start": 106,
        "end": 107,
        "value": 86
      },
      {
        "start": 50,
        "end": 54,
        "value": 92
      },
      {
        "start": 8,
        "end": 15,
        "value": 66
      },
      {
        "start": 60,
        "end": 68,
        "value": 72
      },
      {
        "start": 53,
        "end": 55,
        "value": 1
      },
      {
        "start": 12,
        "end": 18,
        "value": 12
      },
      {
        "start": 59,
        "end": 62,
        "value": 84
      },
      {
        "start": 5,
        "end": 6,
        "value": 83
      },
      {
        "start": 3,
        "end": 7,
        "value": 4
      }
    ]
  }
}
```

### operations

```json
8434
```

### elapsedNanos

```json
510834
```

### adapted

```json
false
```

### reused

```json
true
```
