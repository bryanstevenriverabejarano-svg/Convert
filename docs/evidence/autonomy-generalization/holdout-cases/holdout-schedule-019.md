# holdout-schedule-019

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-019

### identifier

```json
"holdout-schedule-019"
```

### objective

```json
"Empates de comienzo con duraciones y recompensas diferentes"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "346d04c40439f90fc4bfbdf141b9d4393fe353df897f9c909a4dde6fbc5e7eea",
  "familyState": {
    "schema": 1,
    "revision": 474,
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
            "start": 33,
            "end": 46,
            "value": 46
          },
          {
            "start": -15,
            "end": -4,
            "value": 59
          },
          {
            "start": -18,
            "end": -10,
            "value": 39
          },
          {
            "start": 15,
            "end": 17,
            "value": 69
          },
          {
            "start": 13,
            "end": 19,
            "value": 39
          },
          {
            "start": -3,
            "end": 12,
            "value": 22
          },
          {
            "start": 11,
            "end": 14,
            "value": 72
          },
          {
            "start": 3,
            "end": 17,
            "value": 72
          },
          {
            "start": 16,
            "end": 17,
            "value": 55
          },
          {
            "start": 35,
            "end": 39,
            "value": 41
          },
          {
            "start": 34,
            "end": 44,
            "value": 35
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 60,
            "end": 62,
            "value": 90
          },
          {
            "start": 1,
            "end": 7,
            "value": 23
          },
          {
            "start": 110,
            "end": 113,
            "value": 26
          },
          {
            "start": 56,
            "end": 57,
            "value": 46
          },
          {
            "start": 11,
            "end": 19,
            "value": 56
          },
          {
            "start": 6,
            "end": 10,
            "value": 46
          },
          {
            "start": 3,
            "end": 6,
            "value": 0
          },
          {
            "start": 56,
            "end": 62,
            "value": 56
          },
          {
            "start": 55,
            "end": 60,
            "value": 84
          },
          {
            "start": 3,
            "end": 5,
            "value": 64
          },
          {
            "start": 12,
            "end": 16,
            "value": 78
          },
          {
            "start": 6,
            "end": 12,
            "value": 95
          },
          {
            "start": 10,
            "end": 17,
            "value": 62
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 8,
            "value": 28
          },
          {
            "start": 0,
            "end": 5,
            "value": 12
          },
          {
            "start": 7,
            "end": 16,
            "value": 17
          },
          {
            "start": 0,
            "end": 1,
            "value": 10
          },
          {
            "start": 15,
            "end": 19,
            "value": 63
          },
          {
            "start": 15,
            "end": 19,
            "value": 40
          },
          {
            "start": 7,
            "end": 17,
            "value": 49
          },
          {
            "start": -5,
            "end": 1,
            "value": 27
          },
          {
            "start": 7,
            "end": 10,
            "value": 26
          },
          {
            "start": -5,
            "end": 2,
            "value": 60
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-002",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-023",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-025",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-012",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-011",
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
      "value": 273,
      "selected": [
        1,
        10,
        6,
        2
      ]
    },
    "operations": 29262,
    "elapsedNanos": 505146
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 273,
  "selected": [
    1,
    10,
    6,
    2
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
    "id": "holdout-schedule-019",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 273
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
  "fullJournalSha256": "0e9114b87c1d17e138e406be5d8d379de6579cd574c69d47e9ce6d8f3f9ae8a7",
  "familyState": {
    "schema": 1,
    "revision": 475,
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
            "start": 60,
            "end": 62,
            "value": 90
          },
          {
            "start": 1,
            "end": 7,
            "value": 23
          },
          {
            "start": 110,
            "end": 113,
            "value": 26
          },
          {
            "start": 56,
            "end": 57,
            "value": 46
          },
          {
            "start": 11,
            "end": 19,
            "value": 56
          },
          {
            "start": 6,
            "end": 10,
            "value": 46
          },
          {
            "start": 3,
            "end": 6,
            "value": 0
          },
          {
            "start": 56,
            "end": 62,
            "value": 56
          },
          {
            "start": 55,
            "end": 60,
            "value": 84
          },
          {
            "start": 3,
            "end": 5,
            "value": 64
          },
          {
            "start": 12,
            "end": 16,
            "value": 78
          },
          {
            "start": 6,
            "end": 12,
            "value": 95
          },
          {
            "start": 10,
            "end": 17,
            "value": 62
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 8,
            "value": 28
          },
          {
            "start": 0,
            "end": 5,
            "value": 12
          },
          {
            "start": 7,
            "end": 16,
            "value": 17
          },
          {
            "start": 0,
            "end": 1,
            "value": 10
          },
          {
            "start": 15,
            "end": 19,
            "value": 63
          },
          {
            "start": 15,
            "end": 19,
            "value": 40
          },
          {
            "start": 7,
            "end": 17,
            "value": 49
          },
          {
            "start": -5,
            "end": 1,
            "value": 27
          },
          {
            "start": 7,
            "end": 10,
            "value": 26
          },
          {
            "start": -5,
            "end": 2,
            "value": 60
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -5,
            "end": 5,
            "value": 44
          },
          {
            "start": -5,
            "end": -1,
            "value": 80
          },
          {
            "start": 15,
            "end": 25,
            "value": 85
          },
          {
            "start": -5,
            "end": -1,
            "value": 43
          },
          {
            "start": 7,
            "end": 18,
            "value": 54
          },
          {
            "start": 0,
            "end": 9,
            "value": 97
          },
          {
            "start": 7,
            "end": 10,
            "value": 17
          },
          {
            "start": 15,
            "end": 17,
            "value": 5
          },
          {
            "start": -5,
            "end": 2,
            "value": 21
          },
          {
            "start": 7,
            "end": 16,
            "value": 23
          },
          {
            "start": 0,
            "end": 4,
            "value": 91
          },
          {
            "start": -5,
            "end": -4,
            "value": 71
          },
          {
            "start": -5,
            "end": -1,
            "value": 49
          },
          {
            "start": 0,
            "end": 4,
            "value": 24
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-002",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-023",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-025",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-012",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-011",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-019",
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
  "id": "holdout-schedule-019",
  "family": "schedule",
  "description": "Empates de comienzo con duraciones y recompensas diferentes",
  "input": {
    "jobs": [
      {
        "start": -5,
        "end": 5,
        "value": 44
      },
      {
        "start": -5,
        "end": -1,
        "value": 80
      },
      {
        "start": 15,
        "end": 25,
        "value": 85
      },
      {
        "start": -5,
        "end": -1,
        "value": 43
      },
      {
        "start": 7,
        "end": 18,
        "value": 54
      },
      {
        "start": 0,
        "end": 9,
        "value": 97
      },
      {
        "start": 7,
        "end": 10,
        "value": 17
      },
      {
        "start": 15,
        "end": 17,
        "value": 5
      },
      {
        "start": -5,
        "end": 2,
        "value": 21
      },
      {
        "start": 7,
        "end": 16,
        "value": 23
      },
      {
        "start": 0,
        "end": 4,
        "value": 91
      },
      {
        "start": -5,
        "end": -4,
        "value": 71
      },
      {
        "start": -5,
        "end": -1,
        "value": 49
      },
      {
        "start": 0,
        "end": 4,
        "value": 24
      }
    ]
  }
}
```

### operations

```json
29262
```

### elapsedNanos

```json
776436
```

### adapted

```json
false
```

### reused

```json
true
```
