# knapsack-002

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-002-r1

### identifier

```json
"knapsack-002-r1"
```

### objective

```json
"Densidad voraz pierde frente a dos piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "9c6b0b4270bb7eb84a5b5db7a4c3bc779db48a7dc937affc31f8a8ad7f115f1a",
  "familyState": {
    "schema": 1,
    "revision": 27,
    "tools": {
      "version": 1,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 5,
        "items": [
          {
            "weight": 5,
            "value": 8
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r1",
        "family": "knapsack",
        "strategy": "RATIO_GREEDY",
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
    "strategy": "RATIO_GREEDY",
    "programSha256": "24de8eaed44b3c41554e32ac5b00ae07e7fec98b03846ad68af40e9cee90483b",
    "programReference": "actionsExecuted[0].program"
  },
  {
    "strategy": "VALUE_GREEDY",
    "programSha256": "7a3b08f4e91338f85ff729d70c8258d67fba28061d660c43cc842aaaa423707e",
    "programReference": "actionsExecuted[1].program"
  },
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "programReference": "actionsExecuted[2].program"
  }
]
```

### toolsUsed

```json
[
  "RATIO_GREEDY",
  "VALUE_GREEDY",
  "DYNAMIC_PROGRAMMING"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"RATIO_GREEDY\",\"programa_sha256\":\"24de8eaed44b3c41554e32ac5b00ae07e7fec98b03846ad68af40e9cee90483b\",\"version\":1,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "RATIO_GREEDY",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "RATIO_GREEDY"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "24de8eaed44b3c41554e32ac5b00ae07e7fec98b03846ad68af40e9cee90483b",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "value": 13,
      "weight": 6,
      "selected": [
        0
      ]
    },
    "operations": 57,
    "elapsedNanos": 126737
  },
  {
    "strategy": "VALUE_GREEDY",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "VALUE_GREEDY"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "7a3b08f4e91338f85ff729d70c8258d67fba28061d660c43cc842aaaa423707e",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "value": 13,
      "weight": 6,
      "selected": [
        0
      ]
    },
    "operations": 57,
    "elapsedNanos": 121740
  },
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 1,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 20,
      "weight": 10,
      "selected": [
        1,
        2
      ]
    },
    "operations": 116,
    "elapsedNanos": 181658
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 20,
  "weight": 10,
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
  "status": "not_established",
  "observedFeedback": [
    "El testigo o su objetivo no supera la verificación independiente.",
    "El testigo o su objetivo no supera la verificación independiente."
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
    "RATIO_GREEDY",
    "VALUE_GREEDY",
    "DYNAMIC_PROGRAMMING"
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
    "strategy": "VALUE_GREEDY",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "VALUE_GREEDY"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "7a3b08f4e91338f85ff729d70c8258d67fba28061d660c43cc842aaaa423707e",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "value": 13,
      "weight": 6,
      "selected": [
        0
      ]
    },
    "operations": 57,
    "elapsedNanos": 121740
  },
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 1,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 20,
      "weight": 10,
      "selected": [
        1,
        2
      ]
    },
    "operations": 116,
    "elapsedNanos": 181658
  }
]
```

### newResult

```json
{
  "status": "ok",
  "value": 20,
  "weight": 10,
  "selected": [
    1,
    2
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
    "strategy": "RATIO_GREEDY",
    "checks": 0,
    "passed": false
  },
  {
    "strategy": "VALUE_GREEDY",
    "checks": 0,
    "passed": false
  },
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "checks": 1,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "knapsack-002-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 20
    }
  },
  "decisionSummary": "Descarté candidatos que fallaron el contrato y verifiqué una alternativa con regresiones.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "8f76898a0cbd2521d606ed00ca46d0ba8dc8839243f5bf4cf745b1d74f2db3df",
  "familyState": {
    "schema": 1,
    "revision": 28,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 5,
        "items": [
          {
            "weight": 5,
            "value": 8
          }
        ]
      },
      {
        "capacity": 10,
        "items": [
          {
            "weight": 6,
            "value": 13
          },
          {
            "weight": 5,
            "value": 10
          },
          {
            "weight": 5,
            "value": 10
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r1",
        "family": "knapsack",
        "strategy": "RATIO_GREEDY",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r1",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
  "id": "knapsack-002-r1",
  "caseId": "knapsack-002",
  "round": 1,
  "family": "knapsack",
  "description": "Densidad voraz pierde frente a dos piezas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 10,
    "items": [
      {
        "weight": 6,
        "value": 13
      },
      {
        "weight": 5,
        "value": 10
      },
      {
        "weight": 5,
        "value": 10
      }
    ]
  }
}
```

### operations

```json
230
```

### elapsedNanos

```json
1437922
```

### adapted

```json
true
```

### reused

```json
false
```

## knapsack-002-r2

### identifier

```json
"knapsack-002-r2"
```

### objective

```json
"Densidad voraz pierde frente a dos piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "18760f9cb009f0dde5fa2da266b2e45a8f494bc9cd89d843d78464ae0e866155",
  "familyState": {
    "schema": 1,
    "revision": 131,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 23,
        "items": [
          {
            "weight": 1,
            "value": 0
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 22
          },
          {
            "weight": 4,
            "value": 10
          },
          {
            "weight": 5,
            "value": 21
          },
          {
            "weight": 6,
            "value": 9
          },
          {
            "weight": 7,
            "value": 20
          },
          {
            "weight": 8,
            "value": 8
          },
          {
            "weight": 9,
            "value": 19
          },
          {
            "weight": 10,
            "value": 7
          },
          {
            "weight": 11,
            "value": 18
          },
          {
            "weight": 12,
            "value": 6
          }
        ]
      },
      {
        "capacity": 26,
        "items": [
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 15
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 5,
            "value": 16
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 7,
            "value": 4
          },
          {
            "weight": 1,
            "value": 17
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 5
          },
          {
            "weight": 4,
            "value": 18
          },
          {
            "weight": 5,
            "value": 12
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 7,
            "value": 19
          }
        ]
      },
      {
        "capacity": 5,
        "items": [
          {
            "weight": 5,
            "value": 8
          },
          {
            "weight": 1,
            "value": 9
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "DYNAMIC_PROGRAMMING"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 27,
      "weight": 9,
      "selected": [
        0,
        3
      ]
    },
    "operations": 21498,
    "elapsedNanos": 729096
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 27,
  "weight": 9,
  "selected": [
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
    "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "knapsack-002-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 27
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "54023ee2024974450612a4a54fc4e709a39088507d6de70cc2e78a67c2e0afdb",
  "familyState": {
    "schema": 1,
    "revision": 132,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 26,
        "items": [
          {
            "weight": 1,
            "value": 2
          },
          {
            "weight": 2,
            "value": 15
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 5,
            "value": 16
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 7,
            "value": 4
          },
          {
            "weight": 1,
            "value": 17
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 5
          },
          {
            "weight": 4,
            "value": 18
          },
          {
            "weight": 5,
            "value": 12
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 7,
            "value": 19
          }
        ]
      },
      {
        "capacity": 5,
        "items": [
          {
            "weight": 5,
            "value": 8
          },
          {
            "weight": 1,
            "value": 9
          }
        ]
      },
      {
        "capacity": 10,
        "items": [
          {
            "weight": 6,
            "value": 13
          },
          {
            "weight": 5,
            "value": 10
          },
          {
            "weight": 5,
            "value": 10
          },
          {
            "weight": 3,
            "value": 14
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r2",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
  "id": "knapsack-002-r2",
  "caseId": "knapsack-002",
  "round": 2,
  "family": "knapsack",
  "description": "Densidad voraz pierde frente a dos piezas",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 10,
    "items": [
      {
        "weight": 6,
        "value": 13
      },
      {
        "weight": 5,
        "value": 10
      },
      {
        "weight": 5,
        "value": 10
      },
      {
        "weight": 3,
        "value": 14
      }
    ]
  }
}
```

### operations

```json
21498
```

### elapsedNanos

```json
1274491
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-002-r3

### identifier

```json
"knapsack-002-r3"
```

### objective

```json
"Densidad voraz pierde frente a dos piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "19f56ca6f5d3f9f174b372889ea3f44e4fd04caf77399d45691f5bad92581770",
  "familyState": {
    "schema": 1,
    "revision": 235,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 23,
        "items": [
          {
            "weight": 1,
            "value": 0
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 22
          },
          {
            "weight": 4,
            "value": 10
          },
          {
            "weight": 5,
            "value": 21
          },
          {
            "weight": 6,
            "value": 9
          },
          {
            "weight": 7,
            "value": 20
          },
          {
            "weight": 8,
            "value": 8
          },
          {
            "weight": 9,
            "value": 19
          },
          {
            "weight": 10,
            "value": 7
          },
          {
            "weight": 11,
            "value": 18
          },
          {
            "weight": 12,
            "value": 6
          },
          {
            "weight": 7,
            "value": 23
          }
        ]
      },
      {
        "capacity": 26,
        "items": [
          {
            "weight": 2,
            "value": 15
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 5,
            "value": 16
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 7,
            "value": 4
          },
          {
            "weight": 1,
            "value": 17
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 5
          },
          {
            "weight": 4,
            "value": 18
          },
          {
            "weight": 5,
            "value": 12
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 7,
            "value": 19
          },
          {
            "weight": 8,
            "value": 20
          }
        ]
      },
      {
        "capacity": 6,
        "items": [
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 3,
            "value": 5
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "DYNAMIC_PROGRAMMING"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 24,
      "weight": 11,
      "selected": [
        0,
        2,
        3
      ]
    },
    "operations": 25627,
    "elapsedNanos": 393531
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 24,
  "weight": 11,
  "selected": [
    0,
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
    "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "knapsack-002-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
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
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "5aca80806b44839a892b65b0dc2bfb34c4228e2865b900a2a94c699d1376d79f",
  "familyState": {
    "schema": 1,
    "revision": 236,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 26,
        "items": [
          {
            "weight": 2,
            "value": 15
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 5,
            "value": 16
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 7,
            "value": 4
          },
          {
            "weight": 1,
            "value": 17
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 5
          },
          {
            "weight": 4,
            "value": 18
          },
          {
            "weight": 5,
            "value": 12
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 7,
            "value": 19
          },
          {
            "weight": 8,
            "value": 20
          }
        ]
      },
      {
        "capacity": 6,
        "items": [
          {
            "weight": 2,
            "value": 4
          },
          {
            "weight": 3,
            "value": 5
          }
        ]
      },
      {
        "capacity": 11,
        "items": [
          {
            "weight": 5,
            "value": 10
          },
          {
            "weight": 5,
            "value": 10
          },
          {
            "weight": 3,
            "value": 6
          },
          {
            "weight": 3,
            "value": 8
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r3",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
  "id": "knapsack-002-r3",
  "caseId": "knapsack-002",
  "round": 3,
  "family": "knapsack",
  "description": "Densidad voraz pierde frente a dos piezas",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 11,
    "items": [
      {
        "weight": 5,
        "value": 10
      },
      {
        "weight": 5,
        "value": 10
      },
      {
        "weight": 3,
        "value": 6
      },
      {
        "weight": 3,
        "value": 8
      }
    ]
  }
}
```

### operations

```json
25627
```

### elapsedNanos

```json
640214
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-002-r4

### identifier

```json
"knapsack-002-r4"
```

### objective

```json
"Densidad voraz pierde frente a dos piezas"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "5248f606e8e67f60333f8972542e4cf684f4f2b7db06f6f005db0b7c926d8574",
  "familyState": {
    "schema": 1,
    "revision": 339,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 24,
        "items": [
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 22
          },
          {
            "weight": 4,
            "value": 10
          },
          {
            "weight": 5,
            "value": 21
          },
          {
            "weight": 6,
            "value": 9
          },
          {
            "weight": 7,
            "value": 20
          },
          {
            "weight": 8,
            "value": 8
          },
          {
            "weight": 9,
            "value": 19
          },
          {
            "weight": 10,
            "value": 7
          },
          {
            "weight": 11,
            "value": 18
          },
          {
            "weight": 12,
            "value": 6
          },
          {
            "weight": 1,
            "value": 0
          },
          {
            "weight": 1,
            "value": 1
          }
        ]
      },
      {
        "capacity": 27,
        "items": [
          {
            "weight": 2,
            "value": 15
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 5,
            "value": 16
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 7,
            "value": 4
          },
          {
            "weight": 1,
            "value": 17
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 5
          },
          {
            "weight": 4,
            "value": 18
          },
          {
            "weight": 5,
            "value": 12
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 1,
            "value": 1
          },
          {
            "weight": 1,
            "value": 2
          }
        ]
      },
      {
        "capacity": 3,
        "items": []
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "DYNAMIC_PROGRAMMING"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "DYNAMIC_PROGRAMMING",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "knapsack",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DYNAMIC_PROGRAMMING"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 13,
      "weight": 6,
      "selected": [
        0
      ]
    },
    "operations": 25584,
    "elapsedNanos": 449243
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 13,
  "weight": 6,
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
    "DYNAMIC_PROGRAMMING"
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
    "strategy": "DYNAMIC_PROGRAMMING",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "knapsack-002-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
      "optimum": 13
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "5b1aa7efc93784f3ad591c406710df3886eb99c4cc093b35edbdf3cb29e45a43",
  "familyState": {
    "schema": 1,
    "revision": 340,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DYNAMIC_PROGRAMMING"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "knapsack",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "RATIO_GREEDY"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "capacity": 27,
        "items": [
          {
            "weight": 2,
            "value": 15
          },
          {
            "weight": 3,
            "value": 9
          },
          {
            "weight": 4,
            "value": 3
          },
          {
            "weight": 5,
            "value": 16
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 7,
            "value": 4
          },
          {
            "weight": 1,
            "value": 17
          },
          {
            "weight": 2,
            "value": 11
          },
          {
            "weight": 3,
            "value": 5
          },
          {
            "weight": 4,
            "value": 18
          },
          {
            "weight": 5,
            "value": 12
          },
          {
            "weight": 6,
            "value": 6
          },
          {
            "weight": 1,
            "value": 1
          },
          {
            "weight": 1,
            "value": 2
          }
        ]
      },
      {
        "capacity": 3,
        "items": []
      },
      {
        "capacity": 8,
        "items": [
          {
            "weight": 6,
            "value": 13
          },
          {
            "weight": 5,
            "value": 10
          }
        ]
      }
    ],
    "receipts": [
      {
        "id": "knapsack-001-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "knapsack-002-r4",
        "family": "knapsack",
        "strategy": "DYNAMIC_PROGRAMMING",
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
  "id": "knapsack-002-r4",
  "caseId": "knapsack-002",
  "round": 4,
  "family": "knapsack",
  "description": "Densidad voraz pierde frente a dos piezas",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 8,
    "items": [
      {
        "weight": 6,
        "value": 13
      },
      {
        "weight": 5,
        "value": 10
      }
    ]
  }
}
```

### operations

```json
25584
```

### elapsedNanos

```json
665462
```

### adapted

```json
false
```

### reused

```json
true
```
