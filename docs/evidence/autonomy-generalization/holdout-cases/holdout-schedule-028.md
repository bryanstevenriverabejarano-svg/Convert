# holdout-schedule-028

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-028

### identifier

```json
"holdout-schedule-028"
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
  "fullJournalSha256": "a0cd9eaa387957c7c6db3358cac78d6cb7e190a99b30fc4a4dbd561cd6bc0ffc",
  "familyState": {
    "schema": 1,
    "revision": 524,
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
            "start": 55,
            "end": 58,
            "value": 76
          },
          {
            "start": 10,
            "end": 16,
            "value": 100
          },
          {
            "start": 7,
            "end": 12,
            "value": 36
          },
          {
            "start": 105,
            "end": 107,
            "value": 57
          },
          {
            "start": 105,
            "end": 113,
            "value": 44
          },
          {
            "start": 9,
            "end": 16,
            "value": 44
          },
          {
            "start": 102,
            "end": 105,
            "value": 38
          },
          {
            "start": 110,
            "end": 111,
            "value": 11
          },
          {
            "start": 6,
            "end": 10,
            "value": 42
          },
          {
            "start": 108,
            "end": 117,
            "value": 24
          },
          {
            "start": 111,
            "end": 112,
            "value": 34
          },
          {
            "start": 108,
            "end": 117,
            "value": 11
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 10,
            "value": 73
          },
          {
            "start": -5,
            "end": 7,
            "value": 67
          },
          {
            "start": 0,
            "end": 9,
            "value": 97
          },
          {
            "start": 0,
            "end": 1,
            "value": 54
          },
          {
            "start": -5,
            "end": 7,
            "value": 100
          },
          {
            "start": 7,
            "end": 10,
            "value": 46
          },
          {
            "start": 0,
            "end": 7,
            "value": 52
          },
          {
            "start": -5,
            "end": 6,
            "value": 32
          },
          {
            "start": 0,
            "end": 9,
            "value": 52
          },
          {
            "start": 0,
            "end": 1,
            "value": 54
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -12,
            "end": 14,
            "value": 12
          },
          {
            "start": -3,
            "end": 10,
            "value": 70
          },
          {
            "start": -4,
            "end": 20,
            "value": 85
          },
          {
            "start": -14,
            "end": 23,
            "value": 53
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-010",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-024",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-027",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-022",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":4,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 317,
      "selected": [
        7,
        2,
        1,
        5,
        3,
        4
      ]
    },
    "operations": 6702,
    "elapsedNanos": 256509
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 317,
  "selected": [
    7,
    2,
    1,
    5,
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
    "id": "holdout-schedule-028",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 317
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
  "fullJournalSha256": "b698100977c6ba148020639a93f82a54356844aef003874a7d4bcaa435fe0f64",
  "familyState": {
    "schema": 1,
    "revision": 525,
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
            "value": 73
          },
          {
            "start": -5,
            "end": 7,
            "value": 67
          },
          {
            "start": 0,
            "end": 9,
            "value": 97
          },
          {
            "start": 0,
            "end": 1,
            "value": 54
          },
          {
            "start": -5,
            "end": 7,
            "value": 100
          },
          {
            "start": 7,
            "end": 10,
            "value": 46
          },
          {
            "start": 0,
            "end": 7,
            "value": 52
          },
          {
            "start": -5,
            "end": 6,
            "value": 32
          },
          {
            "start": 0,
            "end": 9,
            "value": 52
          },
          {
            "start": 0,
            "end": 1,
            "value": 54
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -12,
            "end": 14,
            "value": 12
          },
          {
            "start": -3,
            "end": 10,
            "value": 70
          },
          {
            "start": -4,
            "end": 20,
            "value": 85
          },
          {
            "start": -14,
            "end": 23,
            "value": 53
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 5,
            "end": 8,
            "value": 60
          },
          {
            "start": 53,
            "end": 55,
            "value": 85
          },
          {
            "start": 7,
            "end": 12,
            "value": 69
          },
          {
            "start": 101,
            "end": 104,
            "value": 50
          },
          {
            "start": 112,
            "end": 120,
            "value": 69
          },
          {
            "start": 55,
            "end": 56,
            "value": 8
          },
          {
            "start": 112,
            "end": 121,
            "value": 3
          },
          {
            "start": 4,
            "end": 7,
            "value": 36
          },
          {
            "start": 106,
            "end": 115,
            "value": 47
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-010",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-024",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-027",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-022",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-028",
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
  "id": "holdout-schedule-028",
  "family": "schedule",
  "description": "Grupos temporales separados con conflictos locales",
  "input": {
    "jobs": [
      {
        "start": 5,
        "end": 8,
        "value": 60
      },
      {
        "start": 53,
        "end": 55,
        "value": 85
      },
      {
        "start": 7,
        "end": 12,
        "value": 69
      },
      {
        "start": 101,
        "end": 104,
        "value": 50
      },
      {
        "start": 112,
        "end": 120,
        "value": 69
      },
      {
        "start": 55,
        "end": 56,
        "value": 8
      },
      {
        "start": 112,
        "end": 121,
        "value": 3
      },
      {
        "start": 4,
        "end": 7,
        "value": 36
      },
      {
        "start": 106,
        "end": 115,
        "value": 47
      }
    ]
  }
}
```

### operations

```json
6702
```

### elapsedNanos

```json
481551
```

### adapted

```json
false
```

### reused

```json
true
```
