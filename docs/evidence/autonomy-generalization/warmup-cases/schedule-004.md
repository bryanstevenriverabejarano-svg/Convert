# schedule-004

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## schedule-004-r1

### identifier

```json
"schedule-004-r1"
```

### objective

```json
"Terminar antes no maximiza la recompensa"
```

### initialState

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
  },
  {
    "strategy": "GREATEST_VALUE",
    "programSha256": "32f36b2acd8dffd4878e738266f30448eb58db3e8960807adb8ebb3dcd3549b6",
    "programReference": "actionsExecuted[1].program"
  },
  {
    "strategy": "WEIGHTED_DP",
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "programReference": "actionsExecuted[2].program"
  }
]
```

### toolsUsed

```json
[
  "EARLIEST_FINISH",
  "GREATEST_VALUE",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"EARLIEST_FINISH\",\"programa_sha256\":\"4dfc415d8c8ed79e60e86190087963c87221e8e020c11788eca2b75cf18391ea\",\"version\":1,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":0}",
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
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "value": 4,
      "selected": [
        0,
        1,
        3
      ]
    },
    "operations": 101,
    "elapsedNanos": 116332
  },
  {
    "strategy": "GREATEST_VALUE",
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
          "strategy": "GREATEST_VALUE"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "32f36b2acd8dffd4878e738266f30448eb58db3e8960807adb8ebb3dcd3549b6",
    "passed": false,
    "regressionChecks": 3,
    "feedback": "El candidato falla una regresión previamente verificada",
    "result": {
      "status": "ok",
      "value": 11,
      "selected": [
        2,
        3
      ]
    },
    "operations": 477,
    "elapsedNanos": 1067636
  },
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
        2,
        3
      ]
    },
    "operations": 417,
    "elapsedNanos": 395544
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 11,
  "selected": [
    2,
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
  "status": "not_established",
  "observedFeedback": [
    "El testigo o su objetivo no supera la verificación independiente.",
    "El candidato falla una regresión previamente verificada"
  ],
  "detail": "Feedback de validación observable; no equivale a un diagnóstico causal completo."
}
```

### improvementHypothesis

```json
{
  "status": "not_recorded",
  "detail": "El laboratorio selecciona candidatos de estrategias disponibles; no genera una hipótesis causal explícita."
}
```

### modificationPerformed

```json
{
  "strategySequence": [
    "EARLIEST_FINISH",
    "GREATEST_VALUE",
    "WEIGHTED_DP"
  ],
  "promoted": true,
  "version": 2,
  "scope": "Cambios de programa declarativo y diario; no se modifica código fuente ni pesos del modelo."
}
```

### newExecution

```json
[
  {
    "strategy": "GREATEST_VALUE",
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
          "strategy": "GREATEST_VALUE"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "32f36b2acd8dffd4878e738266f30448eb58db3e8960807adb8ebb3dcd3549b6",
    "passed": false,
    "regressionChecks": 3,
    "feedback": "El candidato falla una regresión previamente verificada",
    "result": {
      "status": "ok",
      "value": 11,
      "selected": [
        2,
        3
      ]
    },
    "operations": 477,
    "elapsedNanos": 1067636
  },
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
        2,
        3
      ]
    },
    "operations": 417,
    "elapsedNanos": 395544
  }
]
```

### newResult

```json
{
  "status": "ok",
  "value": 11,
  "selected": [
    2,
    3
  ]
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
    "passed": false
  },
  {
    "strategy": "GREATEST_VALUE",
    "checks": 3,
    "passed": false
  },
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
    "id": "schedule-004-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 11
    }
  },
  "decisionSummary": "Descarté candidatos que fallaron el contrato y verifiqué una alternativa con regresiones.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "schedule",
  "persisted": true,
  "fullJournalSha256": "0a5d513c677db2863d801105549a029744bca9955b617ec34a187e476d07b618",
  "familyState": {
    "schema": 1,
    "revision": 56,
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
      }
    ],
    "receipts": [
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
      },
      {
        "id": "schedule-004-r1",
        "family": "schedule",
        "strategy": "WEIGHTED_DP",
        "version": 2,
        "verified": true,
        "attempts": 3
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-004-r1",
  "caseId": "schedule-004",
  "round": 1,
  "family": "schedule",
  "description": "Terminar antes no maximiza la recompensa",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
  }
}
```

### operations

```json
995
```

### elapsedNanos

```json
2464638
```

### adapted

```json
true
```

### reused

```json
false
```

## schedule-004-r2

### identifier

```json
"schedule-004-r2"
```

### objective

```json
"Terminar antes no maximiza la recompensa"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":0}",
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
        2,
        3
      ]
    },
    "operations": 607,
    "elapsedNanos": 209519
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 11,
  "selected": [
    2,
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
    "id": "schedule-004-r2",
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
  "fullJournalSha256": "994ef36df5791a25c0e2cae073f8f7c38f8c0f8a81fd8ae3086d9b48234ea30f",
  "familyState": {
    "schema": 1,
    "revision": 160,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-004-r2",
  "caseId": "schedule-004",
  "round": 2,
  "family": "schedule",
  "description": "Terminar antes no maximiza la recompensa",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
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
  }
}
```

### operations

```json
607
```

### elapsedNanos

```json
1164699
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-004-r3

### identifier

```json
"schedule-004-r3"
```

### objective

```json
"Terminar antes no maximiza la recompensa"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":0}",
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
        1,
        2
      ]
    },
    "operations": 615,
    "elapsedNanos": 87820
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 11,
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
    "id": "schedule-004-r3",
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
  "fullJournalSha256": "59f0395e87e7b90c37a2354fbcc833d22f430cee25f3b06dc9f2dcfbb4eb58ca",
  "familyState": {
    "schema": 1,
    "revision": 264,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-004-r3",
  "caseId": "schedule-004",
  "round": 3,
  "family": "schedule",
  "description": "Terminar antes no maximiza la recompensa",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
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
  }
}
```

### operations

```json
615
```

### elapsedNanos

```json
315897
```

### adapted

```json
false
```

### reused

```json
true
```

## schedule-004-r4

### identifier

```json
"schedule-004-r4"
```

### objective

```json
"Terminar antes no maximiza la recompensa"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"schedule\",\"estrategia\":\"WEIGHTED_DP\",\"programa_sha256\":\"6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076\",\"version\":2,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 4,
      "selected": [
        2,
        1,
        0
      ]
    },
    "operations": 281,
    "elapsedNanos": 47851
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 4,
  "selected": [
    2,
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
    "id": "schedule-004-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 4
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
  "fullJournalSha256": "e4dcdb91a5f2413ec8ef402cb3845a26e79fd7b22aacce6f23f02ad13c50d0d8",
  "familyState": {
    "schema": 1,
    "revision": 368,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "schedule-004-r4",
  "caseId": "schedule-004",
  "round": 4,
  "family": "schedule",
  "description": "Terminar antes no maximiza la recompensa",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
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
  }
}
```

### operations

```json
281
```

### elapsedNanos

```json
284209
```

### adapted

```json
false
```

### reused

```json
true
```
