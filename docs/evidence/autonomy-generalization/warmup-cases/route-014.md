# route-014

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-014-r1

### identifier

```json
"route-014-r1"
```

### objective

```json
"Dos concentradores con atajos cruzados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "67628f8b197bafdb838ef8e703ad340e1b583aa532de1955246d75a508b39b83",
  "familyState": {
    "schema": 1,
    "revision": 13,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            0
          ],
          [
            2,
            3,
            0
          ],
          [
            3,
            1,
            0
          ],
          [
            3,
            5,
            1
          ],
          [
            0,
            4,
            1
          ],
          [
            4,
            5,
            8
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            5
          ],
          [
            2,
            1,
            -7
          ],
          [
            1,
            3,
            3
          ],
          [
            3,
            5,
            1
          ],
          [
            2,
            4,
            0
          ],
          [
            4,
            5,
            2
          ]
        ],
        "source": 0,
        "target": 5
      }
    ],
    "receipts": [
      {
        "id": "route-001-r1",
        "family": "route",
        "strategy": "FEWEST_EDGES",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 2
      },
      {
        "id": "route-003-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":2}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 7,
      "path": [
        0,
        2,
        4,
        6,
        7
      ]
    },
    "operations": 2133,
    "elapsedNanos": 1212951
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 7,
  "path": [
    0,
    2,
    4,
    6,
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-014-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 7,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "b6d8a0fe3f3c7bf65a795e939b568865e801815ce00f116477f7f49d5e789cc1",
  "familyState": {
    "schema": 1,
    "revision": 14,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            0
          ],
          [
            2,
            3,
            0
          ],
          [
            3,
            1,
            0
          ],
          [
            3,
            5,
            1
          ],
          [
            0,
            4,
            1
          ],
          [
            4,
            5,
            8
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            5
          ],
          [
            2,
            1,
            -7
          ],
          [
            1,
            3,
            3
          ],
          [
            3,
            5,
            1
          ],
          [
            2,
            4,
            0
          ],
          [
            4,
            5,
            2
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            9
          ],
          [
            1,
            4,
            4
          ],
          [
            2,
            4,
            1
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            1
          ],
          [
            4,
            6,
            2
          ],
          [
            5,
            7,
            8
          ],
          [
            6,
            7,
            1
          ]
        ],
        "source": 0,
        "target": 7
      }
    ],
    "receipts": [
      {
        "id": "route-001-r1",
        "family": "route",
        "strategy": "FEWEST_EDGES",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 2
      },
      {
        "id": "route-003-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 3
      },
      {
        "id": "route-014-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-014-r1",
  "caseId": "route-014",
  "round": 1,
  "family": "route",
  "description": "Dos concentradores con atajos cruzados",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        9
      ],
      [
        1,
        4,
        4
      ],
      [
        2,
        4,
        1
      ],
      [
        2,
        5,
        2
      ],
      [
        3,
        6,
        1
      ],
      [
        4,
        6,
        2
      ],
      [
        5,
        7,
        8
      ],
      [
        6,
        7,
        1
      ]
    ],
    "source": 0,
    "target": 7
  }
}
```

### operations

```json
2133
```

### elapsedNanos

```json
1706929
```

### adapted

```json
false
```

### reused

```json
true
```

## route-014-r2

### identifier

```json
"route-014-r2"
```

### objective

```json
"Dos concentradores con atajos cruzados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "bd788fa04136d9938947f652bc762c26ba21d4dbf17775dce838c9028be5ed5f",
  "familyState": {
    "schema": 1,
    "revision": 117,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            4,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            0
          ],
          [
            2,
            3,
            0
          ],
          [
            3,
            5,
            1
          ],
          [
            0,
            4,
            1
          ],
          [
            4,
            5,
            8
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            5
          ],
          [
            2,
            1,
            -7
          ],
          [
            3,
            5,
            1
          ],
          [
            2,
            4,
            0
          ],
          [
            4,
            5,
            2
          ]
        ],
        "source": 0,
        "target": 5
      }
    ],
    "receipts": [
      {
        "id": "route-001-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 7,
      "path": [
        0,
        2,
        4,
        6,
        7
      ]
    },
    "operations": 2036,
    "elapsedNanos": 195749
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 7,
  "path": [
    0,
    2,
    4,
    6,
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-014-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 7,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "fa3a054c33cb878a68cfec95121a444407fe744e02140814fdac219de109054e",
  "familyState": {
    "schema": 1,
    "revision": 118,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            0
          ],
          [
            2,
            3,
            0
          ],
          [
            3,
            5,
            1
          ],
          [
            0,
            4,
            1
          ],
          [
            4,
            5,
            8
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            5
          ],
          [
            2,
            1,
            -7
          ],
          [
            3,
            5,
            1
          ],
          [
            2,
            4,
            0
          ],
          [
            4,
            5,
            2
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            9
          ],
          [
            1,
            4,
            4
          ],
          [
            2,
            4,
            1
          ],
          [
            3,
            6,
            1
          ],
          [
            4,
            6,
            2
          ],
          [
            5,
            7,
            8
          ],
          [
            6,
            7,
            1
          ]
        ],
        "source": 0,
        "target": 7
      }
    ],
    "receipts": [
      {
        "id": "route-001-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-014-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-014-r2",
  "caseId": "route-014",
  "round": 2,
  "family": "route",
  "description": "Dos concentradores con atajos cruzados",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        9
      ],
      [
        1,
        4,
        4
      ],
      [
        2,
        4,
        1
      ],
      [
        3,
        6,
        1
      ],
      [
        4,
        6,
        2
      ],
      [
        5,
        7,
        8
      ],
      [
        6,
        7,
        1
      ]
    ],
    "source": 0,
    "target": 7
  }
}
```

### operations

```json
2036
```

### elapsedNanos

```json
642358
```

### adapted

```json
false
```

### reused

```json
true
```

## route-014-r3

### identifier

```json
"route-014-r3"
```

### objective

```json
"Dos concentradores con atajos cruzados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "d8d335d1a32eee167f3dd404a811604aead9058efc3b141784b5fa0e7c4a548b",
  "familyState": {
    "schema": 1,
    "revision": 221,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            1,
            2,
            7
          ],
          [
            3,
            4,
            7
          ],
          [
            4,
            5,
            7
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            1,
            2,
            6
          ],
          [
            2,
            3,
            -11
          ],
          [
            3,
            1,
            5
          ],
          [
            3,
            5,
            13
          ],
          [
            0,
            4,
            -9
          ],
          [
            4,
            5,
            14
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            0,
            2,
            0
          ],
          [
            2,
            1,
            1
          ],
          [
            1,
            3,
            -2
          ],
          [
            3,
            5,
            13
          ],
          [
            2,
            4,
            -5
          ],
          [
            4,
            5,
            8
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 5
      }
    ],
    "receipts": [
      {
        "id": "route-001-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": -3,
      "path": [
        0,
        3,
        6,
        7
      ]
    },
    "operations": 2209,
    "elapsedNanos": 278731
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": -3,
  "path": [
    0,
    3,
    6,
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-014-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -3,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "398de97a4b2c2b39ab90f45e7dfb43ad1dd12707c1fe1f7bc9f33f0dfdac11dd",
  "familyState": {
    "schema": 1,
    "revision": 222,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            1,
            2,
            6
          ],
          [
            2,
            3,
            -11
          ],
          [
            3,
            1,
            5
          ],
          [
            3,
            5,
            13
          ],
          [
            0,
            4,
            -9
          ],
          [
            4,
            5,
            14
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            0,
            2,
            0
          ],
          [
            2,
            1,
            1
          ],
          [
            1,
            3,
            -2
          ],
          [
            3,
            5,
            13
          ],
          [
            2,
            4,
            -5
          ],
          [
            4,
            5,
            8
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            0,
            2,
            -2
          ],
          [
            1,
            3,
            4
          ],
          [
            1,
            4,
            5
          ],
          [
            2,
            4,
            -4
          ],
          [
            2,
            5,
            3
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            6,
            -3
          ],
          [
            5,
            7,
            3
          ],
          [
            6,
            7,
            7
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 7
      }
    ],
    "receipts": [
      {
        "id": "route-001-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-014-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-014-r3",
  "caseId": "route-014",
  "round": 3,
  "family": "route",
  "description": "Dos concentradores con atajos cruzados",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        -10
      ],
      [
        0,
        2,
        -2
      ],
      [
        1,
        3,
        4
      ],
      [
        1,
        4,
        5
      ],
      [
        2,
        4,
        -4
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        6,
        2
      ],
      [
        4,
        6,
        -3
      ],
      [
        5,
        7,
        3
      ],
      [
        6,
        7,
        7
      ],
      [
        0,
        3,
        -12
      ]
    ],
    "source": 0,
    "target": 7
  }
}
```

### operations

```json
2209
```

### elapsedNanos

```json
962040
```

### adapted

```json
false
```

### reused

```json
true
```

## route-014-r4

### identifier

```json
"route-014-r4"
```

### objective

```json
"Dos concentradores con atajos cruzados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "98bd6fc87c1b403d0f0f708ff51a5f39edefe6ae5a31178d5e84137e5757da89",
  "familyState": {
    "schema": 1,
    "revision": 325,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            1
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            0
          ],
          [
            2,
            3,
            0
          ],
          [
            3,
            1,
            0
          ],
          [
            3,
            5,
            1
          ],
          [
            0,
            4,
            1
          ],
          [
            4,
            5,
            8
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            5
          ],
          [
            2,
            1,
            -7
          ],
          [
            1,
            3,
            3
          ],
          [
            3,
            5,
            1
          ],
          [
            2,
            4,
            0
          ],
          [
            4,
            5,
            2
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      }
    ],
    "receipts": [
      {
        "id": "route-001-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 8,
      "path": [
        0,
        2,
        4,
        6,
        7,
        8
      ]
    },
    "operations": 2979,
    "elapsedNanos": 108039
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 8,
  "path": [
    0,
    2,
    4,
    6,
    7,
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-014-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 8,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "319330c64d48620d89e12cdb852caeaf1e66de7023fdf1110a7cb3e42e04180c",
  "familyState": {
    "schema": 1,
    "revision": 326,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            0
          ],
          [
            2,
            3,
            0
          ],
          [
            3,
            1,
            0
          ],
          [
            3,
            5,
            1
          ],
          [
            0,
            4,
            1
          ],
          [
            4,
            5,
            8
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            5
          ],
          [
            2,
            1,
            -7
          ],
          [
            1,
            3,
            3
          ],
          [
            3,
            5,
            1
          ],
          [
            2,
            4,
            0
          ],
          [
            4,
            5,
            2
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            9
          ],
          [
            1,
            4,
            4
          ],
          [
            2,
            4,
            1
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            1
          ],
          [
            4,
            6,
            2
          ],
          [
            5,
            7,
            8
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            1
          ]
        ],
        "source": 0,
        "target": 8
      }
    ],
    "receipts": [
      {
        "id": "route-001-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-004-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-007-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-008-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-009-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-010-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-011-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-012-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-013-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-014-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-014-r4",
  "caseId": "route-014",
  "round": 4,
  "family": "route",
  "description": "Dos concentradores con atajos cruzados",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        0,
        2,
        3
      ],
      [
        1,
        3,
        9
      ],
      [
        1,
        4,
        4
      ],
      [
        2,
        4,
        1
      ],
      [
        2,
        5,
        2
      ],
      [
        3,
        6,
        1
      ],
      [
        4,
        6,
        2
      ],
      [
        5,
        7,
        8
      ],
      [
        6,
        7,
        1
      ],
      [
        7,
        8,
        1
      ]
    ],
    "source": 0,
    "target": 8
  }
}
```

### operations

```json
2979
```

### elapsedNanos

```json
336016
```

### adapted

```json
false
```

### reused

```json
true
```
