# knapsack-003

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-003-r1

### identifier

```json
"knapsack-003-r1"
```

### objective

```json
"Valor individual voraz pierde frente a un conjunto"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":2}",
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
    "regressionChecks": 2,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 18,
      "weight": 8,
      "selected": [
        1,
        2
      ]
    },
    "operations": 189,
    "elapsedNanos": 226193
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 18,
  "weight": 8,
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
    "checks": 2,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "knapsack-003-r1",
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
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "6d2ff7aea1cab1fa1775a8dc3f09cc391fccfac58246b0e07fc77772844e2189",
  "familyState": {
    "schema": 1,
    "revision": 29,
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
      },
      {
        "capacity": 8,
        "items": [
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 9
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
      },
      {
        "id": "knapsack-003-r1",
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
  "id": "knapsack-003-r1",
  "caseId": "knapsack-003",
  "round": 1,
  "family": "knapsack",
  "description": "Valor individual voraz pierde frente a un conjunto",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 8,
    "items": [
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 4,
        "value": 9
      }
    ]
  }
}
```

### operations

```json
189
```

### elapsedNanos

```json
636459
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-003-r2

### identifier

```json
"knapsack-003-r2"
```

### objective

```json
"Valor individual voraz pierde frente a un conjunto"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 25,
      "weight": 6,
      "selected": [
        1,
        3
      ]
    },
    "operations": 17136,
    "elapsedNanos": 713483
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 25,
  "weight": 6,
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
    "id": "knapsack-003-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
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
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "e921439bfdc1c8f19f5225d417b893e4c2aa399cc2b071dbca86b9baa1b64892",
  "familyState": {
    "schema": 1,
    "revision": 133,
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
      },
      {
        "capacity": 8,
        "items": [
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 2,
            "value": 16
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
      },
      {
        "id": "knapsack-003-r2",
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
  "id": "knapsack-003-r2",
  "caseId": "knapsack-003",
  "round": 2,
  "family": "knapsack",
  "description": "Valor individual voraz pierde frente a un conjunto",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
    "capacity": 8,
    "items": [
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 2,
        "value": 16
      }
    ]
  }
}
```

### operations

```json
17136
```

### elapsedNanos

```json
1347238
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-003-r3

### identifier

```json
"knapsack-003-r3"
```

### objective

```json
"Valor individual voraz pierde frente a un conjunto"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 18,
      "weight": 8,
      "selected": [
        0,
        1
      ]
    },
    "operations": 17150,
    "elapsedNanos": 255818
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 18,
  "weight": 8,
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
    "id": "knapsack-003-r3",
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
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "7a7ee4954a33dd8c08c1730ec2969f65d3340ae153867d9f5445e3b8647b6341",
  "familyState": {
    "schema": 1,
    "revision": 237,
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
      },
      {
        "capacity": 9,
        "items": [
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 9
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 4,
            "value": 9
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
      },
      {
        "id": "knapsack-003-r3",
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
  "id": "knapsack-003-r3",
  "caseId": "knapsack-003",
  "round": 3,
  "family": "knapsack",
  "description": "Valor individual voraz pierde frente a un conjunto",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
    "capacity": 9,
    "items": [
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 4,
        "value": 9
      },
      {
        "weight": 4,
        "value": 7
      },
      {
        "weight": 4,
        "value": 9
      }
    ]
  }
}
```

### operations

```json
17150
```

### elapsedNanos

```json
490814
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-003-r4

### identifier

```json
"knapsack-003-r4"
```

### objective

```json
"Valor individual voraz pierde frente a un conjunto"
```

### initialState

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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"knapsack\",\"estrategia\":\"DYNAMIC_PROGRAMMING\",\"programa_sha256\":\"e40df907ff56c756013780b29a6dd0407a5750eec6583446e4465bb5d025bb57\",\"version\":2,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
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
      "value": 9,
      "weight": 4,
      "selected": [
        1
      ]
    },
    "operations": 17031,
    "elapsedNanos": 263158
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 9,
  "weight": 4,
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
    "id": "knapsack-003-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
      "optimum": 9
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
  "fullJournalSha256": "bc54c1d06c95ef9c18c5c27af0430ab6ebf177552b056bcddb6b87ca236cf750",
  "familyState": {
    "schema": 1,
    "revision": 341,
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
      },
      {
        "capacity": 6,
        "items": [
          {
            "weight": 8,
            "value": 15
          },
          {
            "weight": 4,
            "value": 9
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
      },
      {
        "id": "knapsack-003-r4",
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
  "id": "knapsack-003-r4",
  "caseId": "knapsack-003",
  "round": 4,
  "family": "knapsack",
  "description": "Valor individual voraz pierde frente a un conjunto",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 6,
    "items": [
      {
        "weight": 8,
        "value": 15
      },
      {
        "weight": 4,
        "value": 9
      }
    ]
  }
}
```

### operations

```json
17031
```

### elapsedNanos

```json
468591
```

### adapted

```json
false
```

### reused

```json
true
```
