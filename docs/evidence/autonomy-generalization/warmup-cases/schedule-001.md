# schedule-001

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-001-r1

### identifier

```json
"schedule-001-r1"
```

### objective

```json
"Un único intervalo"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "26393006b652dd98dcd8e67d268250cae85e849245f01320737b77f303c8e7ca",
  "familyState": {
    "schema": 1,
    "revision": 52,
    "receipts": []
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
    "strategy": "EARLIEST_FINISH",
    "programSha256": "4dfc415d8c8ed79e60e86190087963c87221e8e020c11788eca2b75cf18391ea",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "EARLIEST_FINISH"
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
  "proceduralContext": "",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "EARLIEST_FINISH",
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
          "strategy": "EARLIEST_FINISH"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "4dfc415d8c8ed79e60e86190087963c87221e8e020c11788eca2b75cf18391ea",
    "passed": true,
    "regressionChecks": 0,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 7,
      "selected": [
        0
      ]
    },
    "operations": 28,
    "elapsedNanos": 1367979
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 7,
  "selected": [
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
    "EARLIEST_FINISH"
  ],
  "promoted": true,
  "version": 1,
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
    "strategy": "EARLIEST_FINISH",
    "checks": 0,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "schedule-001-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2,
      "optimum": 7
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
  "fullJournalSha256": "7748ced7bcf4ee7dc2a2ef5c309177858b7f50db1920fed8ee018d5c9adeebb9",
  "familyState": {
    "schema": 1,
    "revision": 53,
    "tools": {
      "version": 1,
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
            "start": 0,
            "end": 3,
            "value": 7
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-001-r1",
        "family": "schedule",
        "strategy": "EARLIEST_FINISH",
        "version": 1,
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
  "id": "schedule-001-r1",
  "caseId": "schedule-001",
  "round": 1,
  "family": "schedule",
  "description": "Un único intervalo",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 7
      }
    ]
  }
}
```

### operations

```json
28
```

### elapsedNanos

```json
1897080
```

### adapted

```json
false
```

### reused

```json
false
```

## schedule-001-r2

### identifier

```json
"schedule-001-r2"
```

### objective

```json
"Un único intervalo"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "a5b987e0722169700ec64f570b833b575210492198eb12d10843abdd7e0ce104",
  "familyState": {
    "schema": 1,
    "revision": 156,
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
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 4
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 7,
            "end": 10,
            "value": 7
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 3
          },
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 6,
            "end": 8,
            "value": 12
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 12,
            "end": 14,
            "value": 3
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-026-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 7,
      "selected": [
        0
      ]
    },
    "operations": 22180,
    "elapsedNanos": 779030
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 7,
  "selected": [
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
    "id": "schedule-001-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
      "optimum": 7
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
  "fullJournalSha256": "5a50bc62ed97f71213c1bf5400c353ef459de5a9df14fd58e8c70f9a1c990fb6",
  "familyState": {
    "schema": 1,
    "revision": 157,
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
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 3
          },
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 6,
            "end": 8,
            "value": 12
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 12,
            "end": 14,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 3,
            "value": 7
          },
          {
            "start": -1,
            "end": 4,
            "value": 4
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-001-r2",
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
  "id": "schedule-001-r2",
  "caseId": "schedule-001",
  "round": 2,
  "family": "schedule",
  "description": "Un único intervalo",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 7
      },
      {
        "start": -1,
        "end": 4,
        "value": 4
      }
    ]
  }
}
```

### operations

```json
22180
```

### elapsedNanos

```json
1229215
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-001-r3

### identifier

```json
"schedule-001-r3"
```

### objective

```json
"Un único intervalo"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "9e4ce802852786e47026c31376faacf1a60eddf8b8468d9384b631a0b23772f0",
  "familyState": {
    "schema": 1,
    "revision": 260,
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
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 4
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
          },
          {
            "start": 1,
            "end": 4,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 7
          },
          {
            "start": 7,
            "end": 10,
            "value": 7
          },
          {
            "start": -1,
            "end": 13,
            "value": 23
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          },
          {
            "start": -1,
            "end": 16,
            "value": 57
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 6,
            "end": 8,
            "value": 12
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 12,
            "end": 14,
            "value": 3
          },
          {
            "start": -1,
            "end": 15,
            "value": 61
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-026-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 8,
      "selected": [
        0,
        1
      ]
    },
    "operations": 26884,
    "elapsedNanos": 481670
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 8,
  "selected": [
    0,
    1
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
    "id": "schedule-001-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
      "optimum": 8
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
  "fullJournalSha256": "5ed4fa3f5b021be1d45db8ba13c3df10b4da41149e52be48f795b799fdf4bbfd",
  "familyState": {
    "schema": 1,
    "revision": 261,
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
            "start": 0,
            "end": 1,
            "value": 2
          },
          {
            "start": 1,
            "end": 3,
            "value": 9
          },
          {
            "start": 2,
            "end": 5,
            "value": 16
          },
          {
            "start": 3,
            "end": 7,
            "value": 6
          },
          {
            "start": 4,
            "end": 5,
            "value": 13
          },
          {
            "start": 5,
            "end": 7,
            "value": 3
          },
          {
            "start": 6,
            "end": 9,
            "value": 10
          },
          {
            "start": 7,
            "end": 11,
            "value": 17
          },
          {
            "start": 8,
            "end": 9,
            "value": 7
          },
          {
            "start": 9,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 13,
            "value": 4
          },
          {
            "start": 11,
            "end": 15,
            "value": 11
          },
          {
            "start": -1,
            "end": 16,
            "value": 57
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 8
          },
          {
            "start": 2,
            "end": 3,
            "value": 13
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 10
          },
          {
            "start": 4,
            "end": 6,
            "value": 15
          },
          {
            "start": 6,
            "end": 7,
            "value": 7
          },
          {
            "start": 6,
            "end": 8,
            "value": 12
          },
          {
            "start": 8,
            "end": 9,
            "value": 4
          },
          {
            "start": 8,
            "end": 10,
            "value": 9
          },
          {
            "start": 10,
            "end": 11,
            "value": 14
          },
          {
            "start": 10,
            "end": 12,
            "value": 6
          },
          {
            "start": 12,
            "end": 13,
            "value": 11
          },
          {
            "start": 12,
            "end": 14,
            "value": 3
          },
          {
            "start": -1,
            "end": 15,
            "value": 61
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 3,
            "value": 3
          },
          {
            "start": 3,
            "end": 6,
            "value": 5
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-001-r3",
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
  "id": "schedule-001-r3",
  "caseId": "schedule-001",
  "round": 3,
  "family": "schedule",
  "description": "Un único intervalo",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 3
      },
      {
        "start": 3,
        "end": 6,
        "value": 5
      }
    ]
  }
}
```

### operations

```json
26884
```

### elapsedNanos

```json
724219
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-001-r4

### identifier

```json
"schedule-001-r4"
```

### objective

```json
"Un único intervalo"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "f687f2ee2dfa9c90f6bfe2534b76e14d912a0d6f5f33641f0f50147004e5b450",
  "familyState": {
    "schema": 1,
    "revision": 364,
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
            "start": 4,
            "end": 8,
            "value": 4
          },
          {
            "start": 8,
            "end": 12,
            "value": 4
          },
          {
            "start": 12,
            "end": 16,
            "value": 4
          },
          {
            "start": 16,
            "end": 20,
            "value": 4
          },
          {
            "start": 20,
            "end": 24,
            "value": 4
          },
          {
            "start": 2,
            "end": 8,
            "value": 7
          },
          {
            "start": 8,
            "end": 14,
            "value": 7
          },
          {
            "start": 14,
            "end": 20,
            "value": 7
          },
          {
            "start": 0,
            "end": 2,
            "value": 2
          },
          {
            "start": 2,
            "end": 4,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 6,
            "value": 9
          },
          {
            "start": 4,
            "end": 10,
            "value": 16
          },
          {
            "start": 6,
            "end": 14,
            "value": 6
          },
          {
            "start": 8,
            "end": 10,
            "value": 13
          },
          {
            "start": 10,
            "end": 14,
            "value": 3
          },
          {
            "start": 12,
            "end": 18,
            "value": 10
          },
          {
            "start": 14,
            "end": 22,
            "value": 17
          },
          {
            "start": 16,
            "end": 18,
            "value": 7
          },
          {
            "start": 18,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 26,
            "value": 4
          },
          {
            "start": 22,
            "end": 30,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 4,
            "value": 8
          },
          {
            "start": 4,
            "end": 6,
            "value": 13
          },
          {
            "start": 4,
            "end": 8,
            "value": 5
          },
          {
            "start": 8,
            "end": 10,
            "value": 10
          },
          {
            "start": 8,
            "end": 12,
            "value": 15
          },
          {
            "start": 12,
            "end": 14,
            "value": 7
          },
          {
            "start": 12,
            "end": 16,
            "value": 12
          },
          {
            "start": 16,
            "end": 18,
            "value": 4
          },
          {
            "start": 16,
            "end": 20,
            "value": 9
          },
          {
            "start": 20,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 24,
            "value": 6
          },
          {
            "start": 24,
            "end": 26,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 3
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-026-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 0,
      "selected": []
    },
    "operations": 26864,
    "elapsedNanos": 544654
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 0,
  "selected": []
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
    "id": "schedule-001-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 1,
      "optimum": 0
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
  "fullJournalSha256": "305e8a5e9c0166e85aecca56a61055f825e82f89bd2e940fa7905ca3d33d88b0",
  "familyState": {
    "schema": 1,
    "revision": 365,
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
            "start": 2,
            "end": 6,
            "value": 9
          },
          {
            "start": 4,
            "end": 10,
            "value": 16
          },
          {
            "start": 6,
            "end": 14,
            "value": 6
          },
          {
            "start": 8,
            "end": 10,
            "value": 13
          },
          {
            "start": 10,
            "end": 14,
            "value": 3
          },
          {
            "start": 12,
            "end": 18,
            "value": 10
          },
          {
            "start": 14,
            "end": 22,
            "value": 17
          },
          {
            "start": 16,
            "end": 18,
            "value": 7
          },
          {
            "start": 18,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 26,
            "value": 4
          },
          {
            "start": 22,
            "end": 30,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 4,
            "value": 8
          },
          {
            "start": 4,
            "end": 6,
            "value": 13
          },
          {
            "start": 4,
            "end": 8,
            "value": 5
          },
          {
            "start": 8,
            "end": 10,
            "value": 10
          },
          {
            "start": 8,
            "end": 12,
            "value": 15
          },
          {
            "start": 12,
            "end": 14,
            "value": 7
          },
          {
            "start": 12,
            "end": 16,
            "value": 12
          },
          {
            "start": 16,
            "end": 18,
            "value": 4
          },
          {
            "start": 16,
            "end": 20,
            "value": 9
          },
          {
            "start": 20,
            "end": 22,
            "value": 14
          },
          {
            "start": 20,
            "end": 24,
            "value": 6
          },
          {
            "start": 24,
            "end": 26,
            "value": 11
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 3
          }
        ]
      },
      {
        "jobs": []
      }
    ],
    "receipts": [
      {
        "id": "schedule-001-r4",
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
  "id": "schedule-001-r4",
  "caseId": "schedule-001",
  "round": 4,
  "family": "schedule",
  "description": "Un único intervalo",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": []
  }
}
```

### operations

```json
26864
```

### elapsedNanos

```json
848622
```

### adapted

```json
false
```

### reused

```json
true
```
