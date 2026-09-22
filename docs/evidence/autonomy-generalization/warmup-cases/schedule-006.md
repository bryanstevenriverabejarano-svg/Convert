# schedule-006

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-006-r1

### identifier

```json
"schedule-006-r1"
```

### objective

```json
"Ventanas idénticas con diferentes recompensas"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "8c81b3791201f7e352fe522791b79b8f1a72f9486cb0e232bd96c8598e596a08",
  "familyState": {
    "schema": 1,
    "revision": 57,
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
            "value": 15
          },
          {
            "start": 0,
            "end": 5,
            "value": 9
          },
          {
            "start": 5,
            "end": 10,
            "value": 9
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 1
          },
          {
            "start": 0,
            "end": 2,
            "value": 9
          },
          {
            "start": 2,
            "end": 3,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 12,
            "value": 10
          },
          {
            "start": 1,
            "end": 11,
            "value": 12
          },
          {
            "start": 2,
            "end": 10,
            "value": 8
          },
          {
            "start": 3,
            "end": 9,
            "value": 14
          },
          {
            "start": 4,
            "end": 8,
            "value": 6
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "schedule-003-r1",
        "family": "schedule",
        "strategy": "EARLIEST_FINISH",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":2}",
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
      "value": 14,
      "selected": [
        1,
        3
      ]
    },
    "operations": 426,
    "elapsedNanos": 395884
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 14,
  "selected": [
    1,
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
    "id": "schedule-006-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 14
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
  "fullJournalSha256": "97820eadee403935b291e1355f78a5a3a9879cc8aac5fb6df8df636911072b8b",
  "familyState": {
    "schema": 1,
    "revision": 58,
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
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 1
          },
          {
            "start": 0,
            "end": 2,
            "value": 9
          },
          {
            "start": 2,
            "end": 3,
            "value": 2
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 12,
            "value": 10
          },
          {
            "start": 1,
            "end": 11,
            "value": 12
          },
          {
            "start": 2,
            "end": 10,
            "value": 8
          },
          {
            "start": 3,
            "end": 9,
            "value": 14
          },
          {
            "start": 4,
            "end": 8,
            "value": 6
          }
        ]
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-006-r1",
  "caseId": "schedule-006",
  "round": 1,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
  }
}
```

### operations

```json
426
```

### elapsedNanos

```json
1093935
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-006-r2

### identifier

```json
"schedule-006-r2"
```

### objective

```json
"Ventanas idénticas con diferentes recompensas"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "0feb3de1092ab7aab2a291d44727e15e24f6c14f5e40988eaae2627b3aa3c7ed",
  "familyState": {
    "schema": 1,
    "revision": 161,
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
            "value": 15
          },
          {
            "start": 0,
            "end": 5,
            "value": 9
          },
          {
            "start": 5,
            "end": 10,
            "value": 9
          },
          {
            "start": -1,
            "end": 11,
            "value": 17
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 1,
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 1
          },
          {
            "start": 0,
            "end": 2,
            "value": 9
          },
          {
            "start": 2,
            "end": 3,
            "value": 2
          },
          {
            "start": -1,
            "end": 4,
            "value": 7
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 12,
            "value": 10
          },
          {
            "start": 1,
            "end": 11,
            "value": 12
          },
          {
            "start": 2,
            "end": 10,
            "value": 8
          },
          {
            "start": 3,
            "end": 9,
            "value": 14
          },
          {
            "start": 4,
            "end": 8,
            "value": 6
          },
          {
            "start": -1,
            "end": 13,
            "value": 26
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
      "value": 14,
      "selected": [
        1,
        3
      ]
    },
    "operations": 606,
    "elapsedNanos": 589139
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 14,
  "selected": [
    1,
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
    "id": "schedule-006-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 14
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
  "fullJournalSha256": "fef28cd71181c556261e3cac30c5ba0e9cdd201c83524ff3e1a76d3a00d15616",
  "familyState": {
    "schema": 1,
    "revision": 162,
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
            "value": 1
          },
          {
            "start": 1,
            "end": 2,
            "value": 1
          },
          {
            "start": 0,
            "end": 2,
            "value": 9
          },
          {
            "start": 2,
            "end": 3,
            "value": 2
          },
          {
            "start": -1,
            "end": 4,
            "value": 7
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 0,
            "end": 12,
            "value": 10
          },
          {
            "start": 1,
            "end": 11,
            "value": 12
          },
          {
            "start": 2,
            "end": 10,
            "value": 8
          },
          {
            "start": 3,
            "end": 9,
            "value": 14
          },
          {
            "start": 4,
            "end": 8,
            "value": 6
          },
          {
            "start": -1,
            "end": 13,
            "value": 26
          }
        ]
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-006-r2",
  "caseId": "schedule-006",
  "round": 2,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
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
  }
}
```

### operations

```json
606
```

### elapsedNanos

```json
1210597
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-006-r3

### identifier

```json
"schedule-006-r3"
```

### objective

```json
"Ventanas idénticas con diferentes recompensas"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "c9f52a2fd067fb75d81e871b9403136d04b7a7f813477e6fb3c694e4bbb9adc4",
  "familyState": {
    "schema": 1,
    "revision": 265,
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
            "value": 9
          },
          {
            "start": 10,
            "end": 20,
            "value": 9
          },
          {
            "start": 0,
            "end": 10,
            "value": 7
          },
          {
            "start": 10,
            "end": 20,
            "value": 9
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 4,
            "value": 1
          },
          {
            "start": 0,
            "end": 4,
            "value": 9
          },
          {
            "start": 4,
            "end": 6,
            "value": 2
          },
          {
            "start": 0,
            "end": 1,
            "value": 0
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
            "start": 2,
            "end": 22,
            "value": 12
          },
          {
            "start": 4,
            "end": 20,
            "value": 8
          },
          {
            "start": 6,
            "end": 18,
            "value": 14
          },
          {
            "start": 8,
            "end": 16,
            "value": 6
          },
          {
            "start": 0,
            "end": 12,
            "value": 5
          },
          {
            "start": 12,
            "end": 24,
            "value": 6
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
      "value": 14,
      "selected": [
        0,
        2
      ]
    },
    "operations": 610,
    "elapsedNanos": 313162
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 14,
  "selected": [
    0,
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
    "id": "schedule-006-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 14
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
  "fullJournalSha256": "d875a71041e82af1ec4a0a03a127bace28c3815c479900d9e119805f70e35c4d",
  "familyState": {
    "schema": 1,
    "revision": 266,
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
            "end": 4,
            "value": 1
          },
          {
            "start": 0,
            "end": 4,
            "value": 9
          },
          {
            "start": 4,
            "end": 6,
            "value": 2
          },
          {
            "start": 0,
            "end": 1,
            "value": 0
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
            "start": 2,
            "end": 22,
            "value": 12
          },
          {
            "start": 4,
            "end": 20,
            "value": 8
          },
          {
            "start": 6,
            "end": 18,
            "value": 14
          },
          {
            "start": 8,
            "end": 16,
            "value": 6
          },
          {
            "start": 0,
            "end": 12,
            "value": 5
          },
          {
            "start": 12,
            "end": 24,
            "value": 6
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-006-r3",
  "caseId": "schedule-006",
  "round": 3,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
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
  }
}
```

### operations

```json
610
```

### elapsedNanos

```json
550472
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-006-r4

### identifier

```json
"schedule-006-r4"
```

### objective

```json
"Ventanas idénticas con diferentes recompensas"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "c5bef05f2c76d9a9d590cdb39bb16a67e9b60affcfd9573cc890e7b6001de2a7",
  "familyState": {
    "schema": 1,
    "revision": 369,
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
            "end": 10,
            "value": 9
          },
          {
            "start": 0,
            "end": 10,
            "value": 15
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 3,
            "value": 2
          },
          {
            "start": 1,
            "end": 2,
            "value": 1
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 8,
            "value": 6
          },
          {
            "start": 3,
            "end": 9,
            "value": 14
          },
          {
            "start": 1,
            "end": 11,
            "value": 12
          },
          {
            "start": 0,
            "end": 12,
            "value": 10
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
      "value": 14,
      "selected": [
        1,
        0
      ]
    },
    "operations": 296,
    "elapsedNanos": 76313
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 14,
  "selected": [
    1,
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
    "id": "schedule-006-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 14
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
  "fullJournalSha256": "d17bf6c31b41e399d6033d29ac354d2bcbc255181edb16b9b327bebe72a7bc0b",
  "familyState": {
    "schema": 1,
    "revision": 370,
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
            "end": 3,
            "value": 2
          },
          {
            "start": 1,
            "end": 2,
            "value": 1
          },
          {
            "start": 0,
            "end": 1,
            "value": 1
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 4,
            "end": 8,
            "value": 6
          },
          {
            "start": 3,
            "end": 9,
            "value": 14
          },
          {
            "start": 1,
            "end": 11,
            "value": 12
          },
          {
            "start": 0,
            "end": 12,
            "value": 10
          }
        ]
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-006-r4",
  "caseId": "schedule-006",
  "round": 4,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
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
  }
}
```

### operations

```json
296
```

### elapsedNanos

```json
294555
```

### adapted

```json
false
```

### reused

```json
true
```
