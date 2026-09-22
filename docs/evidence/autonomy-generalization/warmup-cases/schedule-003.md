# schedule-003

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-003-r1

### identifier

```json
"schedule-003-r1"
```

### objective

```json
"Un trabajo largo pierde frente a dos cortos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "27677c0a6e155366757ff64845618c75bdb583ec8c75e05cafe8294b941fcb9f",
  "familyState": {
    "schema": 1,
    "revision": 54,
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
            "end": 2,
            "value": 3
          },
          {
            "start": 2,
            "end": 3,
            "value": 4
          },
          {
            "start": 3,
            "end": 4,
            "value": 2
          },
          {
            "start": 4,
            "end": 5,
            "value": 3
          },
          {
            "start": 5,
            "end": 6,
            "value": 4
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
      },
      {
        "id": "schedule-002-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"EARLIEST_FINISH\",\"programa_sha256\":\"4dfc415d8c8ed79e60e86190087963c87221e8e020c11788eca2b75cf18391ea\",\"version\":1,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
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
    "regressionChecks": 2,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 18,
      "selected": [
        1,
        2
      ]
    },
    "operations": 293,
    "elapsedNanos": 226114
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 18,
  "selected": [
    1,
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
    "checks": 2,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "schedule-003-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 18
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
  "fullJournalSha256": "25f6cd2730756730a5ae25914ebf63b763490762947a654fbb1396c9915f6999",
  "familyState": {
    "schema": 1,
    "revision": 55,
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
            "end": 2,
            "value": 3
          },
          {
            "start": 2,
            "end": 3,
            "value": 4
          },
          {
            "start": 3,
            "end": 4,
            "value": 2
          },
          {
            "start": 4,
            "end": 5,
            "value": 3
          },
          {
            "start": 5,
            "end": 6,
            "value": 4
          }
        ]
      },
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
      },
      {
        "id": "schedule-002-r1",
        "family": "schedule",
        "strategy": "EARLIEST_FINISH",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "schedule-003-r1",
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
  "id": "schedule-003-r1",
  "caseId": "schedule-003",
  "round": 1,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
  }
}
```

### operations

```json
293
```

### elapsedNanos

```json
877054
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-003-r2

### identifier

```json
"schedule-003-r2"
```

### objective

```json
"Un trabajo largo pierde frente a dos cortos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "17140bbce45ffcc108a6a775a3fc489c2620acc99a0a21ece33159e6bdc66dfa",
  "familyState": {
    "schema": 1,
    "revision": 158,
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
            "end": 2,
            "value": 3
          },
          {
            "start": 2,
            "end": 3,
            "value": 4
          },
          {
            "start": 3,
            "end": 4,
            "value": 2
          },
          {
            "start": 4,
            "end": 5,
            "value": 3
          },
          {
            "start": 5,
            "end": 6,
            "value": 4
          },
          {
            "start": -1,
            "end": 7,
            "value": 10
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
      "value": 18,
      "selected": [
        1,
        2
      ]
    },
    "operations": 17329,
    "elapsedNanos": 582419
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 18,
  "selected": [
    1,
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
    "id": "schedule-003-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 18
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
  "fullJournalSha256": "a66b9584250fe27cefb8d72051f60f4968eae0b164cb06d6b615c650990bdb4b",
  "familyState": {
    "schema": 1,
    "revision": 159,
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
            "value": 7
          },
          {
            "start": -1,
            "end": 4,
            "value": 4
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
            "end": 2,
            "value": 3
          },
          {
            "start": 2,
            "end": 3,
            "value": 4
          },
          {
            "start": 3,
            "end": 4,
            "value": 2
          },
          {
            "start": 4,
            "end": 5,
            "value": 3
          },
          {
            "start": 5,
            "end": 6,
            "value": 4
          },
          {
            "start": -1,
            "end": 7,
            "value": 10
          }
        ]
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-003-r2",
  "caseId": "schedule-003",
  "round": 2,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
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
  }
}
```

### operations

```json
17329
```

### elapsedNanos

```json
949371
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-003-r3

### identifier

```json
"schedule-003-r3"
```

### objective

```json
"Un trabajo largo pierde frente a dos cortos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "5f04192809046d7ba3acd7106e5148401ef82c757bb0f9175d6f9a383ad0b02e",
  "familyState": {
    "schema": 1,
    "revision": 262,
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
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 4,
            "value": 3
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 2
          },
          {
            "start": 8,
            "end": 10,
            "value": 3
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
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
      "value": 18,
      "selected": [
        0,
        1
      ]
    },
    "operations": 17337,
    "elapsedNanos": 385750
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 18,
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
    "id": "schedule-003-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 18
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
  "fullJournalSha256": "ce39b57991983c3703ad814d6a1b2681a7f487ef80e87a434dde825ff3e6d2f3",
  "familyState": {
    "schema": 1,
    "revision": 263,
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
            "value": 3
          },
          {
            "start": 3,
            "end": 6,
            "value": 5
          }
        ]
      },
      {
        "jobs": [
          {
            "start": 2,
            "end": 4,
            "value": 3
          },
          {
            "start": 4,
            "end": 6,
            "value": 4
          },
          {
            "start": 6,
            "end": 8,
            "value": 2
          },
          {
            "start": 8,
            "end": 10,
            "value": 3
          },
          {
            "start": 10,
            "end": 12,
            "value": 4
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-003-r3",
  "caseId": "schedule-003",
  "round": 3,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
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
  }
}
```

### operations

```json
17337
```

### elapsedNanos

```json
829815
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-003-r4

### identifier

```json
"schedule-003-r4"
```

### objective

```json
"Un trabajo largo pierde frente a dos cortos"
```

### initialState

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "f1e3eec71ae93d393425c7990492c8a6d33d5d2dcff918171f5115e8387f2054",
  "familyState": {
    "schema": 1,
    "revision": 366,
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
      },
      {
        "jobs": [
          {
            "start": 5,
            "end": 6,
            "value": 4
          },
          {
            "start": 4,
            "end": 5,
            "value": 3
          },
          {
            "start": 2,
            "end": 3,
            "value": 4
          },
          {
            "start": 1,
            "end": 2,
            "value": 3
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
      "value": 15,
      "selected": [
        1
      ]
    },
    "operations": 17086,
    "elapsedNanos": 264710
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 15,
  "selected": [
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
    "id": "schedule-003-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
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
  "fullJournalSha256": "c8f03e1e7003ded1aafe3d0e50d19fbad161eda2e34a59d36836745807fe12a5",
  "familyState": {
    "schema": 1,
    "revision": 367,
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
        "jobs": []
      },
      {
        "jobs": [
          {
            "start": 5,
            "end": 6,
            "value": 4
          },
          {
            "start": 4,
            "end": 5,
            "value": 3
          },
          {
            "start": 2,
            "end": 3,
            "value": 4
          },
          {
            "start": 1,
            "end": 2,
            "value": 3
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-003-r4",
  "caseId": "schedule-003",
  "round": 4,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
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
  }
}
```

### operations

```json
17086
```

### elapsedNanos

```json
460270
```

### adapted

```json
false
```

### reused

```json
true
```
