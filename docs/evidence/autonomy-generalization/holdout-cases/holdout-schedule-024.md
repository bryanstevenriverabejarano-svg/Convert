# holdout-schedule-024

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-024

### identifier

```json
"holdout-schedule-024"
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
  "fullJournalSha256": "6cce57131cb096a7b2ceaaa9473c7ab74a9e63f92b1e24b28c85be366f31f29f",
  "familyState": {
    "schema": 1,
    "revision": 511,
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
            "start": -16,
            "end": 7,
            "value": 52
          },
          {
            "start": 0,
            "end": 19,
            "value": 94
          },
          {
            "start": -19,
            "end": 13,
            "value": 1
          },
          {
            "start": -4,
            "end": 12,
            "value": 95
          },
          {
            "start": -4,
            "end": 14,
            "value": 12
          },
          {
            "start": -9,
            "end": 19,
            "value": 44
          },
          {
            "start": 0,
            "end": 7,
            "value": 58
          },
          {
            "start": -2,
            "end": 23,
            "value": 70
          },
          {
            "start": -1,
            "end": 3,
            "value": 11
          },
          {
            "start": -15,
            "end": 3,
            "value": 10
          },
          {
            "start": -15,
            "end": 23,
            "value": 42
          },
          {
            "start": -9,
            "end": 10,
            "value": 13
          },
          {
            "start": -4,
            "end": 10,
            "value": 87
          },
          {
            "start": -10,
            "end": 17,
            "value": 14
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 18,
            "value": 0
          },
          {
            "start": 15,
            "end": 25,
            "value": 48
          },
          {
            "start": 0,
            "end": 3,
            "value": 88
          },
          {
            "start": 15,
            "end": 20,
            "value": 85
          },
          {
            "start": 15,
            "end": 20,
            "value": 67
          },
          {
            "start": -5,
            "end": -1,
            "value": 53
          },
          {
            "start": 15,
            "end": 25,
            "value": 62
          },
          {
            "start": 15,
            "end": 21,
            "value": 77
          },
          {
            "start": -5,
            "end": -1,
            "value": 63
          },
          {
            "start": 7,
            "end": 18,
            "value": 89
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -11,
            "end": 10,
            "value": 82
          },
          {
            "start": -10,
            "end": 24,
            "value": 3
          },
          {
            "start": -17,
            "end": 3,
            "value": 82
          },
          {
            "start": -17,
            "end": 21,
            "value": 69
          }
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-schedule-018",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-007",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-010",
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
      "value": 358,
      "selected": [
        8,
        1,
        0,
        6,
        3,
        7,
        10
      ]
    },
    "operations": 22822,
    "elapsedNanos": 549832
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 358,
  "selected": [
    8,
    1,
    0,
    6,
    3,
    7,
    10
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
    "id": "holdout-schedule-024",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4096,
      "optimum": 358
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
  "fullJournalSha256": "e3b2598323e1d80510dbfed588696507fea1a05fe9ab2052a8fcd05818e96b29",
  "familyState": {
    "schema": 1,
    "revision": 512,
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
            "end": 18,
            "value": 0
          },
          {
            "start": 15,
            "end": 25,
            "value": 48
          },
          {
            "start": 0,
            "end": 3,
            "value": 88
          },
          {
            "start": 15,
            "end": 20,
            "value": 85
          },
          {
            "start": 15,
            "end": 20,
            "value": 67
          },
          {
            "start": -5,
            "end": -1,
            "value": 53
          },
          {
            "start": 15,
            "end": 25,
            "value": 62
          },
          {
            "start": 15,
            "end": 21,
            "value": 77
          },
          {
            "start": -5,
            "end": -1,
            "value": 63
          },
          {
            "start": 7,
            "end": 18,
            "value": 89
          }
        ]
      },
      {
        "jobs": [
          {
            "start": -11,
            "end": 10,
            "value": 82
          },
          {
            "start": -10,
            "end": 24,
            "value": 3
          },
          {
            "start": -17,
            "end": 3,
            "value": 82
          },
          {
            "start": -17,
            "end": 21,
            "value": 69
          }
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "holdout-schedule-026",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-018",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-schedule-007",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-schedule-024",
  "family": "schedule",
  "description": "Grupos temporales separados con conflictos locales",
  "input": {
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
  }
}
```

### operations

```json
22822
```

### elapsedNanos

```json
826380
```

### adapted

```json
false
```

### reused

```json
true
```
