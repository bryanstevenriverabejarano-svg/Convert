# schedule-015

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-015-r1

### identifier

```json
"schedule-015-r1"
```

### objective

```json
"Elecciones simétricas con óptimos empatados"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "6b80d4d5f3ab8ae8b1ca61de022fe13d0d9f9d6ec744dd9425ea83f90c2f50a9",
  "familyState": {
    "schema": 1,
    "revision": 66,
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
            "value": 0
          },
          {
            "start": 2,
            "end": 5,
            "value": 0
          },
          {
            "start": 1,
            "end": 4,
            "value": 0
          },
          {
            "start": 5,
            "end": 6,
            "value": 0
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
            "end": 4,
            "value": 5
          },
          {
            "start": 3,
            "end": 9,
            "value": 11
          },
          {
            "start": 8,
            "end": 10,
            "value": 7
          },
          {
            "start": 10,
            "end": 12,
            "value": 8
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
            "start": 100,
            "end": 101,
            "value": 8
          },
          {
            "start": 102,
            "end": 104,
            "value": 4
          },
          {
            "start": 101,
            "end": 103,
            "value": 7
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-004-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 3
      },
      {
        "id": "schedule-005-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":11,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 11,
      "selected": [
        0,
        1,
        4
      ]
    },
    "operations": 507,
    "elapsedNanos": 428443
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 11,
  "selected": [
    0,
    1,
    4
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
    "id": "schedule-015-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 11
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
  "fullJournalSha256": "8e2d68a6d4be07d25b010da6a89b8661eb44795d97c946374409c073dd2358c9",
  "familyState": {
    "schema": 1,
    "revision": 67,
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
            "value": 8
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 3,
            "end": 9,
            "value": 11
          },
          {
            "start": 8,
            "end": 10,
            "value": 7
          },
          {
            "start": 10,
            "end": 12,
            "value": 8
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
            "start": 100,
            "end": 101,
            "value": 8
          },
          {
            "start": 102,
            "end": 104,
            "value": 4
          },
          {
            "start": 101,
            "end": 103,
            "value": 7
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 5
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 0,
            "end": 1,
            "value": 5
          },
          {
            "start": 1,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 1
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-004-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 3
      },
      {
        "id": "schedule-005-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r1",
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
  "id": "schedule-015-r1",
  "caseId": "schedule-015",
  "round": 1,
  "family": "schedule",
  "description": "Elecciones simétricas con óptimos empatados",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 5
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 0,
        "end": 1,
        "value": 5
      },
      {
        "start": 1,
        "end": 4,
        "value": 5
      },
      {
        "start": 4,
        "end": 5,
        "value": 1
      }
    ]
  }
}
```

### operations

```json
507
```

### elapsedNanos

```json
989320
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-015-r2

### identifier

```json
"schedule-015-r2"
```

### objective

```json
"Elecciones simétricas con óptimos empatados"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "784c9adcace33c7665152d013627415cfa038b883553e5637d9b1c0a684636fe",
  "familyState": {
    "schema": 1,
    "revision": 170,
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
            "value": 0
          },
          {
            "start": 2,
            "end": 5,
            "value": 0
          },
          {
            "start": 1,
            "end": 4,
            "value": 0
          },
          {
            "start": 5,
            "end": 6,
            "value": 0
          },
          {
            "start": -1,
            "end": 7,
            "value": 1
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
            "end": 4,
            "value": 5
          },
          {
            "start": 3,
            "end": 9,
            "value": 11
          },
          {
            "start": 8,
            "end": 10,
            "value": 7
          },
          {
            "start": 10,
            "end": 12,
            "value": 8
          },
          {
            "start": -1,
            "end": 13,
            "value": 20
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
            "start": 100,
            "end": 101,
            "value": 8
          },
          {
            "start": 102,
            "end": 104,
            "value": 4
          },
          {
            "start": 101,
            "end": 103,
            "value": 7
          },
          {
            "start": -1,
            "end": 105,
            "value": 11
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
      },
      {
        "id": "schedule-002-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-004-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-005-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":14,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 11,
      "selected": [
        0,
        1,
        4
      ]
    },
    "operations": 717,
    "elapsedNanos": 280114
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 11,
  "selected": [
    0,
    1,
    4
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
    "id": "schedule-015-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 11
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
  "fullJournalSha256": "d0b4a8578f954a46bc9305cf8a221ef477116217aba1635749ba4945b8ac76b9",
  "familyState": {
    "schema": 1,
    "revision": 171,
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
            "value": 8
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 3,
            "end": 9,
            "value": 11
          },
          {
            "start": 8,
            "end": 10,
            "value": 7
          },
          {
            "start": 10,
            "end": 12,
            "value": 8
          },
          {
            "start": -1,
            "end": 13,
            "value": 20
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
            "start": 100,
            "end": 101,
            "value": 8
          },
          {
            "start": 102,
            "end": 104,
            "value": 4
          },
          {
            "start": 101,
            "end": 103,
            "value": 7
          },
          {
            "start": -1,
            "end": 105,
            "value": 11
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 2,
            "value": 5
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 0,
            "end": 1,
            "value": 5
          },
          {
            "start": 1,
            "end": 4,
            "value": 5
          },
          {
            "start": 4,
            "end": 5,
            "value": 1
          },
          {
            "start": -1,
            "end": 6,
            "value": 11
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
      },
      {
        "id": "schedule-002-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-004-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-005-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r2",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r2",
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
  "id": "schedule-015-r2",
  "caseId": "schedule-015",
  "round": 2,
  "family": "schedule",
  "description": "Elecciones simétricas con óptimos empatados",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 5
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 0,
        "end": 1,
        "value": 5
      },
      {
        "start": 1,
        "end": 4,
        "value": 5
      },
      {
        "start": 4,
        "end": 5,
        "value": 1
      },
      {
        "start": -1,
        "end": 6,
        "value": 11
      }
    ]
  }
}
```

### operations

```json
717
```

### elapsedNanos

```json
713543
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-015-r3

### identifier

```json
"schedule-015-r3"
```

### objective

```json
"Elecciones simétricas con óptimos empatados"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "ba344e12e0ffdf4ce237da5b90458b1f18f803b1fa9dc32a89a84e755529070d",
  "familyState": {
    "schema": 1,
    "revision": 274,
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
            "end": 10,
            "value": 0
          },
          {
            "start": 2,
            "end": 8,
            "value": 0
          },
          {
            "start": 10,
            "end": 12,
            "value": 0
          },
          {
            "start": 0,
            "end": 2,
            "value": 0
          },
          {
            "start": 2,
            "end": 4,
            "value": 1
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 8,
            "value": 5
          },
          {
            "start": 6,
            "end": 18,
            "value": 11
          },
          {
            "start": 16,
            "end": 20,
            "value": 7
          },
          {
            "start": 20,
            "end": 24,
            "value": 8
          },
          {
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 200,
            "end": 202,
            "value": 8
          },
          {
            "start": 204,
            "end": 208,
            "value": 4
          },
          {
            "start": 202,
            "end": 206,
            "value": 7
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
      },
      {
        "id": "schedule-002-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-004-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-005-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":14,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 15,
      "selected": [
        1,
        5,
        0,
        3
      ]
    },
    "operations": 728,
    "elapsedNanos": 81501
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 15,
  "selected": [
    1,
    5,
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
    "id": "schedule-015-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 15
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
  "fullJournalSha256": "133f22ae8b38a1b48e6f9645771f602e502304351d89d741bf1064c1915519cb",
  "familyState": {
    "schema": 1,
    "revision": 275,
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
            "value": 5
          },
          {
            "start": 6,
            "end": 18,
            "value": 11
          },
          {
            "start": 16,
            "end": 20,
            "value": 7
          },
          {
            "start": 20,
            "end": 24,
            "value": 8
          },
          {
            "start": 0,
            "end": 2,
            "value": 4
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 200,
            "end": 202,
            "value": 8
          },
          {
            "start": 204,
            "end": 208,
            "value": 4
          },
          {
            "start": 202,
            "end": 206,
            "value": 7
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
            "start": 4,
            "end": 8,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 5
          },
          {
            "start": 2,
            "end": 8,
            "value": 5
          },
          {
            "start": 8,
            "end": 10,
            "value": 1
          },
          {
            "start": 0,
            "end": 2,
            "value": 2
          },
          {
            "start": 2,
            "end": 4,
            "value": 4
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
      },
      {
        "id": "schedule-002-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-004-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-005-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r3",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r3",
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
  "id": "schedule-015-r3",
  "caseId": "schedule-015",
  "round": 3,
  "family": "schedule",
  "description": "Elecciones simétricas con óptimos empatados",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 4,
        "end": 8,
        "value": 5
      },
      {
        "start": 0,
        "end": 2,
        "value": 5
      },
      {
        "start": 2,
        "end": 8,
        "value": 5
      },
      {
        "start": 8,
        "end": 10,
        "value": 1
      },
      {
        "start": 0,
        "end": 2,
        "value": 2
      },
      {
        "start": 2,
        "end": 4,
        "value": 4
      }
    ]
  }
}
```

### operations

```json
728
```

### elapsedNanos

```json
295526
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-015-r4

### identifier

```json
"schedule-015-r4"
```

### objective

```json
"Elecciones simétricas con óptimos empatados"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "e29dc12bb3a9f9899dd58d37728aa211143ff333bf1e9a9cbaea9fde60d58055",
  "familyState": {
    "schema": 1,
    "revision": 378,
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
            "end": 6,
            "value": 0
          },
          {
            "start": 2,
            "end": 5,
            "value": 0
          },
          {
            "start": 0,
            "end": 2,
            "value": 0
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 10,
            "end": 12,
            "value": 8
          },
          {
            "start": 8,
            "end": 10,
            "value": 7
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 8
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 101,
            "end": 103,
            "value": 7
          },
          {
            "start": 100,
            "end": 101,
            "value": 8
          },
          {
            "start": 0,
            "end": 1,
            "value": 2
          }
        ]
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
      },
      {
        "id": "schedule-002-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-004-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-005-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r4",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":14,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 11,
      "selected": [
        3,
        2,
        0
      ]
    },
    "operations": 355,
    "elapsedNanos": 73779
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 11,
  "selected": [
    3,
    2,
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
    "id": "schedule-015-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 11
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
  "fullJournalSha256": "58fa28f80e8abe849a8a8be8b902779aafc76eb74086cb93531cc1cbe1475331",
  "familyState": {
    "schema": 1,
    "revision": 379,
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
            "start": 10,
            "end": 12,
            "value": 8
          },
          {
            "start": 8,
            "end": 10,
            "value": 7
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 8
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 101,
            "end": 103,
            "value": 7
          },
          {
            "start": 100,
            "end": 101,
            "value": 8
          },
          {
            "start": 0,
            "end": 1,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 5,
            "value": 1
          },
          {
            "start": 1,
            "end": 4,
            "value": 5
          },
          {
            "start": 2,
            "end": 4,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 5
          }
        ]
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
      },
      {
        "id": "schedule-002-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-004-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-005-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-006-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-007-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-008-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-009-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-010-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-011-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-012-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-013-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-014-r4",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-015-r4",
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
  "id": "schedule-015-r4",
  "caseId": "schedule-015",
  "round": 4,
  "family": "schedule",
  "description": "Elecciones simétricas con óptimos empatados",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 4,
        "end": 5,
        "value": 1
      },
      {
        "start": 1,
        "end": 4,
        "value": 5
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 0,
        "end": 2,
        "value": 5
      }
    ]
  }
}
```

### operations

```json
355
```

### elapsedNanos

```json
339441
```

### adapted

```json
false
```

### reused

```json
true
```
