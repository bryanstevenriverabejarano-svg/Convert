# schedule-009

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-009-r1

### identifier

```json
"schedule-009-r1"
```

### objective

```json
"Solapamiento escalonado"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "c22af2e0aa70b580c6158a366ebf18083222dcd1f074550cfe30aaf87158c3aa",
  "familyState": {
    "schema": 1,
    "revision": 60,
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
            "value": 3
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 2,
            "end": 6,
            "value": 8
          },
          {
            "start": 6,
            "end": 8,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 3,
            "value": 4
          },
          {
            "start": 0,
            "end": 5,
            "value": 8
          },
          {
            "start": 0,
            "end": 7,
            "value": 9
          },
          {
            "start": 3,
            "end": 5,
            "value": 5
          },
          {
            "start": 5,
            "end": 7,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 6,
            "value": 8
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 4,
            "end": 6,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 3
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 25,
      "selected": [
        1,
        4,
        7
      ]
    },
    "operations": 867,
    "elapsedNanos": 499808
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 25,
  "selected": [
    1,
    4,
    7
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
    "id": "schedule-009-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 25
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
  "fullJournalSha256": "d516050e644b0d141a653582c4551b39a0d8fdf04a08a02447c3afb2315c5549",
  "familyState": {
    "schema": 1,
    "revision": 61,
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
            "end": 3,
            "value": 4
          },
          {
            "start": 0,
            "end": 5,
            "value": 8
          },
          {
            "start": 0,
            "end": 7,
            "value": 9
          },
          {
            "start": 3,
            "end": 5,
            "value": 5
          },
          {
            "start": 5,
            "end": 7,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 6,
            "value": 8
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 4,
            "end": 6,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 3
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
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
            "start": 1,
            "end": 4,
            "value": 8
          },
          {
            "start": 2,
            "end": 5,
            "value": 13
          },
          {
            "start": 3,
            "end": 6,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 12
          },
          {
            "start": 5,
            "end": 8,
            "value": 6
          },
          {
            "start": 6,
            "end": 9,
            "value": 11
          },
          {
            "start": 7,
            "end": 10,
            "value": 5
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-009-r1",
  "caseId": "schedule-009",
  "round": 1,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 3
      },
      {
        "start": 1,
        "end": 4,
        "value": 8
      },
      {
        "start": 2,
        "end": 5,
        "value": 13
      },
      {
        "start": 3,
        "end": 6,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 12
      },
      {
        "start": 5,
        "end": 8,
        "value": 6
      },
      {
        "start": 6,
        "end": 9,
        "value": 11
      },
      {
        "start": 7,
        "end": 10,
        "value": 5
      }
    ]
  }
}
```

### operations

```json
867
```

### elapsedNanos

```json
1104010
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-009-r2

### identifier

```json
"schedule-009-r2"
```

### objective

```json
"Solapamiento escalonado"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "2a73d2f81393f47b011c3e85867902e4ec8b8d455b4f9796ab4e59171aa9fba0",
  "familyState": {
    "schema": 1,
    "revision": 164,
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
            "value": 3
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 2,
            "end": 6,
            "value": 8
          },
          {
            "start": 6,
            "end": 8,
            "value": 2
          },
          {
            "start": 1,
            "end": 9,
            "value": 13
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 3,
            "value": 4
          },
          {
            "start": 0,
            "end": 5,
            "value": 8
          },
          {
            "start": 0,
            "end": 7,
            "value": 9
          },
          {
            "start": 3,
            "end": 5,
            "value": 5
          },
          {
            "start": 5,
            "end": 7,
            "value": 4
          },
          {
            "start": -1,
            "end": 8,
            "value": 16
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 6,
            "value": 8
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 4,
            "end": 6,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 3
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": -1,
            "end": 9,
            "value": 17
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 33,
      "selected": [
        8
      ]
    },
    "operations": 1323,
    "elapsedNanos": 211082
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 33,
  "selected": [
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
    "id": "schedule-009-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 33
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
  "fullJournalSha256": "9278b7cd83b8705b9abe96258fb478ffc44ff2fd7d9ad3cfd445392c5d9fec2a",
  "familyState": {
    "schema": 1,
    "revision": 165,
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
            "end": 3,
            "value": 4
          },
          {
            "start": 0,
            "end": 5,
            "value": 8
          },
          {
            "start": 0,
            "end": 7,
            "value": 9
          },
          {
            "start": 3,
            "end": 5,
            "value": 5
          },
          {
            "start": 5,
            "end": 7,
            "value": 4
          },
          {
            "start": -1,
            "end": 8,
            "value": 16
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 6,
            "value": 8
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 4,
            "end": 6,
            "value": 5
          },
          {
            "start": 0,
            "end": 2,
            "value": 3
          },
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": -1,
            "end": 9,
            "value": 17
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
            "start": 1,
            "end": 4,
            "value": 8
          },
          {
            "start": 2,
            "end": 5,
            "value": 13
          },
          {
            "start": 3,
            "end": 6,
            "value": 7
          },
          {
            "start": 4,
            "end": 7,
            "value": 12
          },
          {
            "start": 5,
            "end": 8,
            "value": 6
          },
          {
            "start": 6,
            "end": 9,
            "value": 11
          },
          {
            "start": 7,
            "end": 10,
            "value": 5
          },
          {
            "start": -1,
            "end": 11,
            "value": 33
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-009-r2",
  "caseId": "schedule-009",
  "round": 2,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 3
      },
      {
        "start": 1,
        "end": 4,
        "value": 8
      },
      {
        "start": 2,
        "end": 5,
        "value": 13
      },
      {
        "start": 3,
        "end": 6,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 12
      },
      {
        "start": 5,
        "end": 8,
        "value": 6
      },
      {
        "start": 6,
        "end": 9,
        "value": 11
      },
      {
        "start": 7,
        "end": 10,
        "value": 5
      },
      {
        "start": -1,
        "end": 11,
        "value": 33
      }
    ]
  }
}
```

### operations

```json
1323
```

### elapsedNanos

```json
557933
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-009-r3

### identifier

```json
"schedule-009-r3"
```

### objective

```json
"Solapamiento escalonado"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "a3a311db547b35987180a0a3f1c654ea9a67b73cbd47785db62662ab96e3026b",
  "familyState": {
    "schema": 1,
    "revision": 268,
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
            "end": 12,
            "value": 12
          },
          {
            "start": 4,
            "end": 12,
            "value": 8
          },
          {
            "start": 12,
            "end": 16,
            "value": 2
          },
          {
            "start": 4,
            "end": 8,
            "value": 1
          },
          {
            "start": 8,
            "end": 12,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 10,
            "value": 8
          },
          {
            "start": 0,
            "end": 14,
            "value": 9
          },
          {
            "start": 6,
            "end": 10,
            "value": 5
          },
          {
            "start": 10,
            "end": 14,
            "value": 4
          },
          {
            "start": 0,
            "end": 3,
            "value": 2
          },
          {
            "start": 3,
            "end": 6,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 12,
            "value": 12
          },
          {
            "start": 8,
            "end": 12,
            "value": 5
          },
          {
            "start": 0,
            "end": 4,
            "value": 3
          },
          {
            "start": 12,
            "end": 16,
            "value": 4
          },
          {
            "start": 0,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 12,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 25,
      "selected": [
        0,
        3,
        6
      ]
    },
    "operations": 1343,
    "elapsedNanos": 124784
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 25,
  "selected": [
    0,
    3,
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
    "id": "schedule-009-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 25
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
  "fullJournalSha256": "8c72c85e23249214c272f7279fff24d37df244c746a337a7d840f513eb7db19f",
  "familyState": {
    "schema": 1,
    "revision": 269,
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
            "end": 10,
            "value": 8
          },
          {
            "start": 0,
            "end": 14,
            "value": 9
          },
          {
            "start": 6,
            "end": 10,
            "value": 5
          },
          {
            "start": 10,
            "end": 14,
            "value": 4
          },
          {
            "start": 0,
            "end": 3,
            "value": 2
          },
          {
            "start": 3,
            "end": 6,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 12,
            "value": 12
          },
          {
            "start": 8,
            "end": 12,
            "value": 5
          },
          {
            "start": 0,
            "end": 4,
            "value": 3
          },
          {
            "start": 12,
            "end": 16,
            "value": 4
          },
          {
            "start": 0,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 12,
            "value": 5
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 8,
            "value": 8
          },
          {
            "start": 4,
            "end": 10,
            "value": 13
          },
          {
            "start": 6,
            "end": 12,
            "value": 7
          },
          {
            "start": 8,
            "end": 14,
            "value": 12
          },
          {
            "start": 10,
            "end": 16,
            "value": 6
          },
          {
            "start": 12,
            "end": 18,
            "value": 11
          },
          {
            "start": 14,
            "end": 20,
            "value": 5
          },
          {
            "start": 0,
            "end": 3,
            "value": 1
          },
          {
            "start": 3,
            "end": 6,
            "value": 3
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-009-r3",
  "caseId": "schedule-009",
  "round": 3,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 2,
        "end": 8,
        "value": 8
      },
      {
        "start": 4,
        "end": 10,
        "value": 13
      },
      {
        "start": 6,
        "end": 12,
        "value": 7
      },
      {
        "start": 8,
        "end": 14,
        "value": 12
      },
      {
        "start": 10,
        "end": 16,
        "value": 6
      },
      {
        "start": 12,
        "end": 18,
        "value": 11
      },
      {
        "start": 14,
        "end": 20,
        "value": 5
      },
      {
        "start": 0,
        "end": 3,
        "value": 1
      },
      {
        "start": 3,
        "end": 6,
        "value": 3
      }
    ]
  }
}
```

### operations

```json
1343
```

### elapsedNanos

```json
390035
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-009-r4

### identifier

```json
"schedule-009-r4"
```

### objective

```json
"Solapamiento escalonado"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "5efd5ae8d3fd10d4e0fced1a7576212cf1962b7a026f9f44743c6807887f072f",
  "familyState": {
    "schema": 1,
    "revision": 372,
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
            "end": 8,
            "value": 2
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 2,
            "end": 6,
            "value": 3
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 5,
            "end": 7,
            "value": 4
          },
          {
            "start": 3,
            "end": 5,
            "value": 5
          },
          {
            "start": 0,
            "end": 5,
            "value": 8
          },
          {
            "start": 0,
            "end": 3,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 0,
            "end": 2,
            "value": 3
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 0,
            "end": 6,
            "value": 8
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 24,
      "selected": [
        4,
        1
      ]
    },
    "operations": 587,
    "elapsedNanos": 65356
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 24,
  "selected": [
    4,
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
    "id": "schedule-009-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 24
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
  "fullJournalSha256": "50927448741ed2cdc5ae6b99fb046f5ca2ab15f5a70fd1dae75c873019228d4f",
  "familyState": {
    "schema": 1,
    "revision": 373,
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
            "end": 7,
            "value": 4
          },
          {
            "start": 3,
            "end": 5,
            "value": 5
          },
          {
            "start": 0,
            "end": 5,
            "value": 8
          },
          {
            "start": 0,
            "end": 3,
            "value": 4
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 6,
            "end": 8,
            "value": 4
          },
          {
            "start": 0,
            "end": 2,
            "value": 3
          },
          {
            "start": 2,
            "end": 6,
            "value": 12
          },
          {
            "start": 0,
            "end": 6,
            "value": 8
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 7,
            "end": 10,
            "value": 5
          },
          {
            "start": 6,
            "end": 9,
            "value": 11
          },
          {
            "start": 5,
            "end": 8,
            "value": 6
          },
          {
            "start": 3,
            "end": 6,
            "value": 7
          },
          {
            "start": 2,
            "end": 5,
            "value": 13
          },
          {
            "start": 1,
            "end": 4,
            "value": 8
          },
          {
            "start": 0,
            "end": 3,
            "value": 3
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-009-r4",
  "caseId": "schedule-009",
  "round": 4,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 7,
        "end": 10,
        "value": 5
      },
      {
        "start": 6,
        "end": 9,
        "value": 11
      },
      {
        "start": 5,
        "end": 8,
        "value": 6
      },
      {
        "start": 3,
        "end": 6,
        "value": 7
      },
      {
        "start": 2,
        "end": 5,
        "value": 13
      },
      {
        "start": 1,
        "end": 4,
        "value": 8
      },
      {
        "start": 0,
        "end": 3,
        "value": 3
      }
    ]
  }
}
```

### operations

```json
587
```

### elapsedNanos

```json
445728
```

### adapted

```json
false
```

### reused

```json
true
```
