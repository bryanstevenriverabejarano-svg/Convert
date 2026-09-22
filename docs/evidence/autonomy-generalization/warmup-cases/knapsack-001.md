# knapsack-001

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## knapsack-001-r1

### identifier

```json
"knapsack-001-r1"
```

### objective

```json
"Una elección indivisible"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "6f67a53d50e9f34ddc87fad933adb95adf3b37715b3ffd9638195ea9c577fc38",
  "familyState": {
    "schema": 1,
    "revision": 26,
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
    "strategy": "RATIO_GREEDY",
    "programSha256": "24de8eaed44b3c41554e32ac5b00ae07e7fec98b03846ad68af40e9cee90483b",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "RATIO_GREEDY"
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
    "passed": true,
    "regressionChecks": 0,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 8,
      "weight": 5,
      "selected": [
        0
      ]
    },
    "operations": 29,
    "elapsedNanos": 2247758
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 8,
  "weight": 5,
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
    "RATIO_GREEDY"
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
    "strategy": "RATIO_GREEDY",
    "checks": 0,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "knapsack-001-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2,
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-001-r1",
  "caseId": "knapsack-001",
  "round": 1,
  "family": "knapsack",
  "description": "Una elección indivisible",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "capacity": 5,
    "items": [
      {
        "weight": 5,
        "value": 8
      }
    ]
  }
}
```

### operations

```json
29
```

### elapsedNanos

```json
2776247
```

### adapted

```json
false
```

### reused

```json
false
```

## knapsack-001-r2

### identifier

```json
"knapsack-001-r2"
```

### objective

```json
"Una elección indivisible"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "8d597d303bbaefbe8404fc50322602d3f10725782fdd184e92be9a709a19997f",
  "familyState": {
    "schema": 1,
    "revision": 130,
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
        "capacity": 17,
        "items": [
          {
            "weight": 2,
            "value": 2
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 8,
            "value": 14
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
          }
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "knapsack-026-r1",
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
      "value": 9,
      "weight": 1,
      "selected": [
        1
      ]
    },
    "operations": 21612,
    "elapsedNanos": 623110
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 9,
  "weight": 1,
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
    "id": "knapsack-001-r2",
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-001-r2",
  "caseId": "knapsack-001",
  "round": 2,
  "family": "knapsack",
  "description": "Una elección indivisible",
  "mutation": "Aparece una nueva alternativa de alta recompensa; recalcular la selección.",
  "input": {
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
}
```

### operations

```json
21612
```

### elapsedNanos

```json
861481
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-001-r3

### identifier

```json
"knapsack-001-r3"
```

### objective

```json
"Una elección indivisible"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "bbdc6cdf8bb20d404b97d00889fed8dcf7b8c95503ef5396bf4ba5d9db50c37b",
  "familyState": {
    "schema": 1,
    "revision": 234,
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
        "capacity": 17,
        "items": [
          {
            "weight": 2,
            "value": 2
          },
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 8,
            "value": 14
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
          },
          {
            "weight": 5,
            "value": 21
          }
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "knapsack-026-r2",
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
      "value": 9,
      "weight": 5,
      "selected": [
        0,
        1
      ]
    },
    "operations": 25819,
    "elapsedNanos": 419960
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 9,
  "weight": 5,
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
    "id": "knapsack-001-r3",
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-001-r3",
  "caseId": "knapsack-001",
  "round": 3,
  "family": "knapsack",
  "description": "Una elección indivisible",
  "mutation": "Un recurso se sustituye por dos módulos indivisibles y cambia el presupuesto.",
  "input": {
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
}
```

### operations

```json
25819
```

### elapsedNanos

```json
829414
```

### adapted

```json
false
```

### reused

```json
true
```

## knapsack-001-r4

### identifier

```json
"knapsack-001-r4"
```

### objective

```json
"Una elección indivisible"
```

### initialState

```json
{
  "family": "knapsack",
  "persisted": true,
  "fullJournalSha256": "00d1e9959354ea444dc3534d59b8b8803287aee674a1c7b250ca08624a010c70",
  "familyState": {
    "schema": 1,
    "revision": 338,
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
        "capacity": 18,
        "items": [
          {
            "weight": 4,
            "value": 7
          },
          {
            "weight": 6,
            "value": 10
          },
          {
            "weight": 8,
            "value": 14
          },
          {
            "weight": 10,
            "value": 19
          },
          {
            "weight": 12,
            "value": 20
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
      }
    ],
    "receipts": [
      {
        "id": "knapsack-026-r3",
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
      "value": 0,
      "weight": 0,
      "selected": []
    },
    "operations": 25848,
    "elapsedNanos": 571744
  }
]
```

### result

```json
{
  "status": "ok",
  "value": 0,
  "weight": 0,
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
    "id": "knapsack-001-r4",
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

### challenge

```json
{
  "schema": 1,
  "id": "knapsack-001-r4",
  "caseId": "knapsack-001",
  "round": 4,
  "family": "knapsack",
  "description": "Una elección indivisible",
  "mutation": "Un recurso deja de estar disponible y disminuye el presupuesto.",
  "input": {
    "capacity": 3,
    "items": []
  }
}
```

### operations

```json
25848
```

### elapsedNanos

```json
795494
```

### adapted

```json
false
```

### reused

```json
true
```
