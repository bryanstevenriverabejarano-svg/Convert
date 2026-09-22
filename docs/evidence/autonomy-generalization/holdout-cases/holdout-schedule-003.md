# holdout-schedule-003

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-003

### identifier

```json
"holdout-schedule-003"
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
  "fullJournalSha256": "f1b8e6dcd57658ba093e85691b0dbea129d04b0dc99f83b6ad6401c7bf1c2e6a",
  "familyState": {
    "schema": 1,
    "revision": 446,
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
            "start": 16,
            "end": 31,
            "value": 78
          },
          {
            "start": 0,
            "end": 13,
            "value": 13
          },
          {
            "start": 24,
            "end": 32,
            "value": 33
          },
          {
            "start": -19,
            "end": -18,
            "value": 73
          },
          {
            "start": 14,
            "end": 23,
            "value": 37
          },
          {
            "start": 30,
            "end": 39,
            "value": 79
          },
          {
            "start": 12,
            "end": 26,
            "value": 11
          },
          {
            "start": 14,
            "end": 24,
            "value": 28
          },
          {
            "start": 22,
            "end": 34,
            "value": 11
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -10,
            "end": 9,
            "value": 88
          },
          {
            "start": -16,
            "end": 6,
            "value": 54
          },
          {
            "start": -14,
            "end": 7,
            "value": 41
          },
          {
            "start": -3,
            "end": 1,
            "value": 62
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -15,
            "end": -2,
            "value": 42
          },
          {
            "start": 2,
            "end": 12,
            "value": 33
          },
          {
            "start": 25,
            "end": 39,
            "value": 56
          },
          {
            "start": 28,
            "end": 33,
            "value": 14
          },
          {
            "start": 9,
            "end": 15,
            "value": 77
          },
          {
            "start": 35,
            "end": 50,
            "value": 0
          },
          {
            "start": 12,
            "end": 17,
            "value": 69
          },
          {
            "start": 17,
            "end": 30,
            "value": 79
          },
          {
            "start": -7,
            "end": 4,
            "value": 7
          },
          {
            "start": 28,
            "end": 43,
            "value": 80
          },
          {
            "start": 14,
            "end": 16,
            "value": 36
          },
          {
            "start": -7,
            "end": 8,
            "value": 60
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-021",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-030",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-029",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":6,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 234,
      "selected": [
        8,
        1,
        3,
        2
      ]
    },
    "operations": 6699,
    "elapsedNanos": 263619
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 234,
  "selected": [
    8,
    1,
    3,
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
    "id": "holdout-schedule-003",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 1024,
      "optimum": 234
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
  "fullJournalSha256": "bf69cfe371bd0575f4dc0e46973c56e872d109989a809f09990477bebd84b613",
  "familyState": {
    "schema": 1,
    "revision": 447,
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
            "start": -10,
            "end": 9,
            "value": 88
          },
          {
            "start": -16,
            "end": 6,
            "value": 54
          },
          {
            "start": -14,
            "end": 7,
            "value": 41
          },
          {
            "start": -3,
            "end": 1,
            "value": 62
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -15,
            "end": -2,
            "value": 42
          },
          {
            "start": 2,
            "end": 12,
            "value": 33
          },
          {
            "start": 25,
            "end": 39,
            "value": 56
          },
          {
            "start": 28,
            "end": 33,
            "value": 14
          },
          {
            "start": 9,
            "end": 15,
            "value": 77
          },
          {
            "start": 35,
            "end": 50,
            "value": 0
          },
          {
            "start": 12,
            "end": 17,
            "value": 69
          },
          {
            "start": 17,
            "end": 30,
            "value": 79
          },
          {
            "start": -7,
            "end": 4,
            "value": 7
          },
          {
            "start": 28,
            "end": 43,
            "value": 80
          },
          {
            "start": 14,
            "end": 16,
            "value": 36
          },
          {
            "start": -7,
            "end": 8,
            "value": 60
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 14,
            "value": 57
          },
          {
            "start": 0,
            "end": 2,
            "value": 18
          },
          {
            "start": 15,
            "end": 24,
            "value": 45
          },
          {
            "start": 7,
            "end": 11,
            "value": 95
          },
          {
            "start": 15,
            "end": 25,
            "value": 15
          },
          {
            "start": 7,
            "end": 9,
            "value": 22
          },
          {
            "start": -5,
            "end": 6,
            "value": 12
          },
          {
            "start": 0,
            "end": 9,
            "value": 10
          },
          {
            "start": -5,
            "end": -2,
            "value": 76
          },
          {
            "start": -5,
            "end": 4,
            "value": 14
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-021",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-030",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-029",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-003",
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
  "id": "holdout-schedule-003",
  "family": "schedule",
  "description": "Empates de comienzo con duraciones y recompensas diferentes",
  "input": {
    "jobs": [
      {
        "start": 7,
        "end": 14,
        "value": 57
      },
      {
        "start": 0,
        "end": 2,
        "value": 18
      },
      {
        "start": 15,
        "end": 24,
        "value": 45
      },
      {
        "start": 7,
        "end": 11,
        "value": 95
      },
      {
        "start": 15,
        "end": 25,
        "value": 15
      },
      {
        "start": 7,
        "end": 9,
        "value": 22
      },
      {
        "start": -5,
        "end": 6,
        "value": 12
      },
      {
        "start": 0,
        "end": 9,
        "value": 10
      },
      {
        "start": -5,
        "end": -2,
        "value": 76
      },
      {
        "start": -5,
        "end": 4,
        "value": 14
      }
    ]
  }
}
```

### operations

```json
6699
```

### elapsedNanos

```json
624221
```

### adapted

```json
false
```

### reused

```json
true
```
