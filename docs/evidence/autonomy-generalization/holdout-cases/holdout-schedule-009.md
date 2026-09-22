# holdout-schedule-009

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-009

### identifier

```json
"holdout-schedule-009"
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
  "fullJournalSha256": "8ad692e45abbda7be39ed67787b6790deb15c07357ad89fdd18d4d8b8d1f2ff5",
  "familyState": {
    "schema": 1,
    "revision": 542,
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
            "start": 6,
            "end": 9,
            "value": 87
          },
          {
            "start": 15,
            "end": 22,
            "value": 20
          },
          {
            "start": -19,
            "end": -10,
            "value": 32
          },
          {
            "start": 22,
            "end": 34,
            "value": 65
          },
          {
            "start": -4,
            "end": 3,
            "value": 48
          },
          {
            "start": 20,
            "end": 27,
            "value": 10
          },
          {
            "start": 19,
            "end": 23,
            "value": 52
          },
          {
            "start": -14,
            "end": -5,
            "value": 82
          },
          {
            "start": 8,
            "end": 10,
            "value": 92
          },
          {
            "start": -13,
            "end": -8,
            "value": 47
          },
          {
            "start": 23,
            "end": 28,
            "value": 87
          },
          {
            "start": 7,
            "end": 14,
            "value": 25
          },
          {
            "start": -7,
            "end": 8,
            "value": 55
          },
          {
            "start": 10,
            "end": 16,
            "value": 95
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 8,
            "value": 61
          },
          {
            "start": -11,
            "end": 3,
            "value": 62
          },
          {
            "start": -1,
            "end": 7,
            "value": 30
          },
          {
            "start": 14,
            "end": 28,
            "value": 78
          },
          {
            "start": -19,
            "end": -14,
            "value": 43
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 62,
            "end": 68,
            "value": 75
          },
          {
            "start": 7,
            "end": 15,
            "value": 22
          },
          {
            "start": 61,
            "end": 64,
            "value": 8
          },
          {
            "start": 103,
            "end": 108,
            "value": 99
          },
          {
            "start": 58,
            "end": 60,
            "value": 81
          },
          {
            "start": 55,
            "end": 60,
            "value": 35
          },
          {
            "start": 55,
            "end": 56,
            "value": 15
          },
          {
            "start": 4,
            "end": 8,
            "value": 15
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-017",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-013",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-004",
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
      "value": 423,
      "selected": [
        4,
        10,
        13,
        2,
        11,
        8
      ]
    },
    "operations": 34425,
    "elapsedNanos": 571173
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 423,
  "selected": [
    4,
    10,
    13,
    2,
    11,
    8
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
    "id": "holdout-schedule-009",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 423
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
  "fullJournalSha256": "ecf2d4daa795a3aae99dd13cda451c60a6dcc427f755ff9af215113c27d3acdb",
  "familyState": {
    "schema": 1,
    "revision": 543,
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
            "end": 8,
            "value": 61
          },
          {
            "start": -11,
            "end": 3,
            "value": 62
          },
          {
            "start": -1,
            "end": 7,
            "value": 30
          },
          {
            "start": 14,
            "end": 28,
            "value": 78
          },
          {
            "start": -19,
            "end": -14,
            "value": 43
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 62,
            "end": 68,
            "value": 75
          },
          {
            "start": 7,
            "end": 15,
            "value": 22
          },
          {
            "start": 61,
            "end": 64,
            "value": 8
          },
          {
            "start": 103,
            "end": 108,
            "value": 99
          },
          {
            "start": 58,
            "end": 60,
            "value": 81
          },
          {
            "start": 55,
            "end": 60,
            "value": 35
          },
          {
            "start": 55,
            "end": 56,
            "value": 15
          },
          {
            "start": 4,
            "end": 8,
            "value": 15
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 3,
            "end": 16,
            "value": 89
          },
          {
            "start": 20,
            "end": 24,
            "value": 83
          },
          {
            "start": 16,
            "end": 21,
            "value": 63
          },
          {
            "start": 21,
            "end": 34,
            "value": 5
          },
          {
            "start": -10,
            "end": -4,
            "value": 53
          },
          {
            "start": 3,
            "end": 6,
            "value": 58
          },
          {
            "start": -8,
            "end": 2,
            "value": 11
          },
          {
            "start": 23,
            "end": 26,
            "value": 75
          },
          {
            "start": 35,
            "end": 49,
            "value": 92
          },
          {
            "start": -18,
            "end": -5,
            "value": 17
          },
          {
            "start": 1,
            "end": 4,
            "value": 88
          },
          {
            "start": 22,
            "end": 31,
            "value": 98
          },
          {
            "start": 13,
            "end": 16,
            "value": 1
          },
          {
            "start": 4,
            "end": 15,
            "value": 29
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-017",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-013",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-004",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-009",
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
  "id": "holdout-schedule-009",
  "family": "schedule",
  "description": "Calendario aleatorio con distintos grados de solapamiento",
  "input": {
    "jobs": [
      {
        "start": 3,
        "end": 16,
        "value": 89
      },
      {
        "start": 20,
        "end": 24,
        "value": 83
      },
      {
        "start": 16,
        "end": 21,
        "value": 63
      },
      {
        "start": 21,
        "end": 34,
        "value": 5
      },
      {
        "start": -10,
        "end": -4,
        "value": 53
      },
      {
        "start": 3,
        "end": 6,
        "value": 58
      },
      {
        "start": -8,
        "end": 2,
        "value": 11
      },
      {
        "start": 23,
        "end": 26,
        "value": 75
      },
      {
        "start": 35,
        "end": 49,
        "value": 92
      },
      {
        "start": -18,
        "end": -5,
        "value": 17
      },
      {
        "start": 1,
        "end": 4,
        "value": 88
      },
      {
        "start": 22,
        "end": 31,
        "value": 98
      },
      {
        "start": 13,
        "end": 16,
        "value": 1
      },
      {
        "start": 4,
        "end": 15,
        "value": 29
      }
    ]
  }
}
```

### operations

```json
34425
```

### elapsedNanos

```json
863354
```

### adapted

```json
false
```

### reused

```json
true
```
