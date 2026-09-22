# holdout-schedule-025

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-025

### identifier

```json
"holdout-schedule-025"
```

### objective

```json
"Calendario aleatorio con distintos grados de solapamiento"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "815acc37340712ad155c8ab48f7dbf81bf510c5e50265e9f3d21e758d85cd98f",
  "familyState": {
    "schema": 1,
    "revision": 465,
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
            "start": -2,
            "end": 8,
            "value": 50
          },
          {
            "start": 29,
            "end": 44,
            "value": 52
          },
          {
            "start": -1,
            "end": 3,
            "value": 42
          },
          {
            "start": 2,
            "end": 7,
            "value": 80
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -1,
            "end": 19,
            "value": 11
          },
          {
            "start": -12,
            "end": 24,
            "value": 55
          },
          {
            "start": -11,
            "end": 16,
            "value": 81
          },
          {
            "start": -13,
            "end": 1,
            "value": 56
          },
          {
            "start": -11,
            "end": 10,
            "value": 22
          },
          {
            "start": -5,
            "end": 11,
            "value": 40
          },
          {
            "start": -16,
            "end": 14,
            "value": 94
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 9,
            "value": 88
          },
          {
            "start": -5,
            "end": -4,
            "value": 60
          },
          {
            "start": 0,
            "end": 11,
            "value": 93
          },
          {
            "start": -5,
            "end": 2,
            "value": 31
          },
          {
            "start": -5,
            "end": -1,
            "value": 50
          },
          {
            "start": 15,
            "end": 22,
            "value": 79
          },
          {
            "start": 15,
            "end": 19,
            "value": 8
          },
          {
            "start": 7,
            "end": 8,
            "value": 63
          },
          {
            "start": 0,
            "end": 7,
            "value": 38
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-001",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      "value": 246,
      "selected": [
        1,
        6,
        3,
        0
      ]
    },
    "operations": 3583,
    "elapsedNanos": 130032
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 246,
  "selected": [
    1,
    6,
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
    "id": "holdout-schedule-025",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2048,
      "optimum": 246
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
  "fullJournalSha256": "18533631013b7f0ae1e3f3e090c859b4459c5933dfe7bfe2c587744b80fd1505",
  "familyState": {
    "schema": 1,
    "revision": 466,
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
            "start": -1,
            "end": 19,
            "value": 11
          },
          {
            "start": -12,
            "end": 24,
            "value": 55
          },
          {
            "start": -11,
            "end": 16,
            "value": 81
          },
          {
            "start": -13,
            "end": 1,
            "value": 56
          },
          {
            "start": -11,
            "end": 10,
            "value": 22
          },
          {
            "start": -5,
            "end": 11,
            "value": 40
          },
          {
            "start": -16,
            "end": 14,
            "value": 94
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 9,
            "value": 88
          },
          {
            "start": -5,
            "end": -4,
            "value": 60
          },
          {
            "start": 0,
            "end": 11,
            "value": 93
          },
          {
            "start": -5,
            "end": 2,
            "value": 31
          },
          {
            "start": -5,
            "end": -1,
            "value": 50
          },
          {
            "start": 15,
            "end": 22,
            "value": 79
          },
          {
            "start": 15,
            "end": 19,
            "value": 8
          },
          {
            "start": 7,
            "end": 8,
            "value": 63
          },
          {
            "start": 0,
            "end": 7,
            "value": 38
          }
        ]
      },
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
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-001",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-schedule-025",
  "family": "schedule",
  "description": "Calendario aleatorio con distintos grados de solapamiento",
  "input": {
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
  }
}
```

### operations

```json
3583
```

### elapsedNanos

```json
377857
```

### adapted

```json
false
```

### reused

```json
true
```
