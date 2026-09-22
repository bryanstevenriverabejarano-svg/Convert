# holdout-schedule-026

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-026

### identifier

```json
"holdout-schedule-026"
```

### objective

```json
"Intervalos aleatorios que comparten una región central"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "f7a82b28edfc2f58f178b6f1d641fcd2331b389bfb36b235a8eea2a336500fd3",
  "familyState": {
    "schema": 1,
    "revision": 489,
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
            "start": -5,
            "end": 1,
            "value": 99
          },
          {
            "start": 7,
            "end": 14,
            "value": 13
          },
          {
            "start": 15,
            "end": 19,
            "value": 10
          },
          {
            "start": 7,
            "end": 14,
            "value": 17
          },
          {
            "start": 7,
            "end": 14,
            "value": 70
          },
          {
            "start": 0,
            "end": 7,
            "value": 21
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -15,
            "end": 24,
            "value": 85
          },
          {
            "start": -7,
            "end": 7,
            "value": 97
          },
          {
            "start": -12,
            "end": 1,
            "value": 76
          },
          {
            "start": -5,
            "end": 23,
            "value": 84
          },
          {
            "start": -3,
            "end": 20,
            "value": 62
          },
          {
            "start": 0,
            "end": 16,
            "value": 60
          },
          {
            "start": -4,
            "end": 11,
            "value": 67
          },
          {
            "start": -17,
            "end": 14,
            "value": 27
          },
          {
            "start": -13,
            "end": 18,
            "value": 96
          },
          {
            "start": -3,
            "end": 17,
            "value": 61
          },
          {
            "start": -11,
            "end": 1,
            "value": 46
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 15,
            "end": 17,
            "value": 82
          },
          {
            "start": 0,
            "end": 7,
            "value": 3
          },
          {
            "start": 7,
            "end": 17,
            "value": 68
          },
          {
            "start": 0,
            "end": 7,
            "value": 57
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-031",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-014",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-015",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 95,
      "selected": [
        6
      ]
    },
    "operations": 19600,
    "elapsedNanos": 316127
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 95,
  "selected": [
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
    "id": "holdout-schedule-026",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 95
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
  "fullJournalSha256": "ba19b9a825dfdf4defcda1dc9afa6a5107839bda2201ab162e03299c245bc2b2",
  "familyState": {
    "schema": 1,
    "revision": 490,
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
            "start": -15,
            "end": 24,
            "value": 85
          },
          {
            "start": -7,
            "end": 7,
            "value": 97
          },
          {
            "start": -12,
            "end": 1,
            "value": 76
          },
          {
            "start": -5,
            "end": 23,
            "value": 84
          },
          {
            "start": -3,
            "end": 20,
            "value": 62
          },
          {
            "start": 0,
            "end": 16,
            "value": 60
          },
          {
            "start": -4,
            "end": 11,
            "value": 67
          },
          {
            "start": -17,
            "end": 14,
            "value": 27
          },
          {
            "start": -13,
            "end": 18,
            "value": 96
          },
          {
            "start": -3,
            "end": 17,
            "value": 61
          },
          {
            "start": -11,
            "end": 1,
            "value": 46
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 15,
            "end": 17,
            "value": 82
          },
          {
            "start": 0,
            "end": 7,
            "value": 3
          },
          {
            "start": 7,
            "end": 17,
            "value": 68
          },
          {
            "start": 0,
            "end": 7,
            "value": 57
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -6,
            "end": 8,
            "value": 36
          },
          {
            "start": -15,
            "end": 8,
            "value": 46
          },
          {
            "start": -18,
            "end": 5,
            "value": 11
          },
          {
            "start": -13,
            "end": 3,
            "value": 3
          },
          {
            "start": -18,
            "end": 16,
            "value": 91
          },
          {
            "start": -19,
            "end": 17,
            "value": 53
          },
          {
            "start": -13,
            "end": 14,
            "value": 95
          },
          {
            "start": -6,
            "end": 1,
            "value": 83
          },
          {
            "start": -3,
            "end": 13,
            "value": 48
          },
          {
            "start": -9,
            "end": 5,
            "value": 91
          },
          {
            "start": -5,
            "end": 6,
            "value": 88
          },
          {
            "start": -18,
            "end": 11,
            "value": 8
          },
          {
            "start": -16,
            "end": 12,
            "value": 40
          },
          {
            "start": -3,
            "end": 24,
            "value": 62
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-031",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-014",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-015",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-026",
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
  "id": "holdout-schedule-026",
  "family": "schedule",
  "description": "Intervalos aleatorios que comparten una región central",
  "input": {
    "jobs": [
      {
        "start": -6,
        "end": 8,
        "value": 36
      },
      {
        "start": -15,
        "end": 8,
        "value": 46
      },
      {
        "start": -18,
        "end": 5,
        "value": 11
      },
      {
        "start": -13,
        "end": 3,
        "value": 3
      },
      {
        "start": -18,
        "end": 16,
        "value": 91
      },
      {
        "start": -19,
        "end": 17,
        "value": 53
      },
      {
        "start": -13,
        "end": 14,
        "value": 95
      },
      {
        "start": -6,
        "end": 1,
        "value": 83
      },
      {
        "start": -3,
        "end": 13,
        "value": 48
      },
      {
        "start": -9,
        "end": 5,
        "value": 91
      },
      {
        "start": -5,
        "end": 6,
        "value": 88
      },
      {
        "start": -18,
        "end": 11,
        "value": 8
      },
      {
        "start": -16,
        "end": 12,
        "value": 40
      },
      {
        "start": -3,
        "end": 24,
        "value": 62
      }
    ]
  }
}
```

### operations

```json
19600
```

### elapsedNanos

```json
550993
```

### adapted

```json
false
```

### reused

```json
true
```
