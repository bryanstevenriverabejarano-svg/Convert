# holdout-schedule-005

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-005

### identifier

```json
"holdout-schedule-005"
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
  "fullJournalSha256": "4f87a81407d9dfea1b76918cfa4dc4e4adbbc99358e7c0c42e6e74166fefc803",
  "familyState": {
    "schema": 1,
    "revision": 420,
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
      },
      {
        "jobs": [
          {
            "start": 57,
            "end": 66,
            "value": 21
          },
          {
            "start": 51,
            "end": 53,
            "value": 99
          },
          {
            "start": 62,
            "end": 64,
            "value": 4
          },
          {
            "start": 58,
            "end": 63,
            "value": 99
          },
          {
            "start": 104,
            "end": 109,
            "value": 21
          },
          {
            "start": 60,
            "end": 64,
            "value": 53
          },
          {
            "start": 1,
            "end": 8,
            "value": 42
          },
          {
            "start": 109,
            "end": 110,
            "value": 27
          },
          {
            "start": 57,
            "end": 60,
            "value": 85
          },
          {
            "start": 107,
            "end": 111,
            "value": 17
          },
          {
            "start": 108,
            "end": 114,
            "value": 71
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 112,
            "end": 121,
            "value": 70
          },
          {
            "start": 6,
            "end": 13,
            "value": 45
          },
          {
            "start": 59,
            "end": 60,
            "value": 62
          },
          {
            "start": 105,
            "end": 108,
            "value": 42
          },
          {
            "start": 103,
            "end": 110,
            "value": 3
          },
          {
            "start": 112,
            "end": 117,
            "value": 86
          },
          {
            "start": 51,
            "end": 57,
            "value": 18
          },
          {
            "start": 110,
            "end": 112,
            "value": 73
          },
          {
            "start": 0,
            "end": 2,
            "value": 98
          },
          {
            "start": 62,
            "end": 71,
            "value": 11
          },
          {
            "start": 110,
            "end": 119,
            "value": 80
          },
          {
            "start": 108,
            "end": 114,
            "value": 11
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 278,
      "selected": [
        8,
        0,
        4,
        2
      ]
    },
    "operations": 17921,
    "elapsedNanos": 428443
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 278,
  "selected": [
    8,
    0,
    4,
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
    "id": "holdout-schedule-005",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2048,
      "optimum": 278
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
  "fullJournalSha256": "db3cdd2307975b4e7a4d8389e077be229dca8b63809b91bf200c2c482f47fb27",
  "familyState": {
    "schema": 1,
    "revision": 421,
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
            "start": 57,
            "end": 66,
            "value": 21
          },
          {
            "start": 51,
            "end": 53,
            "value": 99
          },
          {
            "start": 62,
            "end": 64,
            "value": 4
          },
          {
            "start": 58,
            "end": 63,
            "value": 99
          },
          {
            "start": 104,
            "end": 109,
            "value": 21
          },
          {
            "start": 60,
            "end": 64,
            "value": 53
          },
          {
            "start": 1,
            "end": 8,
            "value": 42
          },
          {
            "start": 109,
            "end": 110,
            "value": 27
          },
          {
            "start": 57,
            "end": 60,
            "value": 85
          },
          {
            "start": 107,
            "end": 111,
            "value": 17
          },
          {
            "start": 108,
            "end": 114,
            "value": 71
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 112,
            "end": 121,
            "value": 70
          },
          {
            "start": 6,
            "end": 13,
            "value": 45
          },
          {
            "start": 59,
            "end": 60,
            "value": 62
          },
          {
            "start": 105,
            "end": 108,
            "value": 42
          },
          {
            "start": 103,
            "end": 110,
            "value": 3
          },
          {
            "start": 112,
            "end": 117,
            "value": 86
          },
          {
            "start": 51,
            "end": 57,
            "value": 18
          },
          {
            "start": 110,
            "end": 112,
            "value": 73
          },
          {
            "start": 0,
            "end": 2,
            "value": 98
          },
          {
            "start": 62,
            "end": 71,
            "value": 11
          },
          {
            "start": 110,
            "end": 119,
            "value": 80
          },
          {
            "start": 108,
            "end": 114,
            "value": 11
          }
        ]
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-schedule-005",
  "family": "schedule",
  "description": "Calendario aleatorio con distintos grados de solapamiento",
  "input": {
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
  }
}
```

### operations

```json
17921
```

### elapsedNanos

```json
655267
```

### adapted

```json
false
```

### reused

```json
true
```
