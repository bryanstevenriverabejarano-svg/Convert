# holdout-schedule-013

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-schedule-013

### identifier

```json
"holdout-schedule-013"
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
  "fullJournalSha256": "a9d07a552bb89439dbe48bd6c7d77e4bc01a055dd5b48ce1408e563971d1722b",
  "familyState": {
    "schema": 1,
    "revision": 537,
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
      },
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
      "value": 244,
      "selected": [
        4,
        1,
        0,
        3
      ]
    },
    "operations": 17927,
    "elapsedNanos": 298661
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 244,
  "selected": [
    4,
    1,
    0,
    3
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
    "id": "holdout-schedule-013",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 244
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
  "fullJournalSha256": "40e918e934b503e6285d9229bae44006fba70e52fc78fb56b2c0ff387f5e71d3",
  "familyState": {
    "schema": 1,
    "revision": 538,
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
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-schedule-013",
  "family": "schedule",
  "description": "Calendario aleatorio con distintos grados de solapamiento",
  "input": {
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
  }
}
```

### operations

```json
17927
```

### elapsedNanos

```json
619204
```

### adapted

```json
false
```

### reused

```json
true
```
