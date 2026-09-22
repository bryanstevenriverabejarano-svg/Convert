# holdout-schedule-015

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-015

### identifier

```json
"holdout-schedule-015"
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
  "fullJournalSha256": "7068f5597120d15ccc4856457b8cc32bb21417c5da0e49b14495cf0e08160898",
  "familyState": {
    "schema": 1,
    "revision": 487,
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
      },
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
      }
    ],
    "receipts": [
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
      "value": 139,
      "selected": [
        3,
        0
      ]
    },
    "operations": 19606,
    "elapsedNanos": 324559
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 139,
  "selected": [
    3,
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
    "id": "holdout-schedule-015",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 139
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
  "fullJournalSha256": "0ab2443537b6eac68c321111db10a65094bd6d9c7765a3a7207fd1dde83bb9d3",
  "familyState": {
    "schema": 1,
    "revision": 488,
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-schedule-015",
  "family": "schedule",
  "description": "Empates de comienzo con duraciones y recompensas diferentes",
  "input": {
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
}
```

### operations

```json
19606
```

### elapsedNanos

```json
532165
```

### adapted

```json
false
```

### reused

```json
true
```
