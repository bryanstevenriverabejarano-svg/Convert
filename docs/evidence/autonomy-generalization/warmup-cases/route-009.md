# route-009

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-009-r1

### identifier

```json
"route-009-r1"
```

### objective

```json
"Grafo denso dirigido"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "d8f807a2dfe95fa1448dcba12424a1a25143db72d19ab4b9fb655730a9b0cdbc",
  "familyState": {
    "schema": 1,
    "revision": 8,
    "tools": {
      "version": 2,
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
            "strategy": "DIJKSTRA"
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
            "strategy": "FEWEST_EDGES"
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
            2
          ],
          [
            2,
            3,
            2
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            0,
            2
          ],
          [
            2,
            5,
            4
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
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
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
          ],
          [
            6,
            7,
            1
          ],
          [
            0,
            4,
            2
          ],
          [
            1,
            5,
            2
          ],
          [
            2,
            6,
            2
          ],
          [
            3,
            7,
            2
          ],
          [
            5,
            2,
            1
          ]
        ],
        "source": 0,
        "target": 7
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
            1,
            2,
            2
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            2
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            2
          ],
          [
            0,
            3,
            2
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            2
          ],
          [
            5,
            8,
            2
          ]
        ],
        "source": 0,
        "target": 8
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
    "strategy": "DIJKSTRA",
    "programSha256": "68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "DIJKSTRA"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"DIJKSTRA\",\"programa_sha256\":\"68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3\",\"version\":2,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":1}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "DIJKSTRA",
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
          "strategy": "DIJKSTRA"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 5,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 3084,
    "elapsedNanos": 1889008
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 5,
  "path": [
    0,
    2,
    5
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
    "DIJKSTRA"
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
    "strategy": "DIJKSTRA",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-009-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 5,
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
  "fullJournalSha256": "dd118f0c357f49eb2d6036f0581bf351ca6f0f9343b397ef4d477319d6f00725",
  "familyState": {
    "schema": 1,
    "revision": 9,
    "tools": {
      "version": 2,
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
            "strategy": "DIJKSTRA"
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
            "strategy": "FEWEST_EDGES"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
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
          ],
          [
            6,
            7,
            1
          ],
          [
            0,
            4,
            2
          ],
          [
            1,
            5,
            2
          ],
          [
            2,
            6,
            2
          ],
          [
            3,
            7,
            2
          ],
          [
            5,
            2,
            1
          ]
        ],
        "source": 0,
        "target": 7
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
            1,
            2,
            2
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            2
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            2
          ],
          [
            0,
            3,
            2
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            2
          ],
          [
            5,
            8,
            2
          ]
        ],
        "source": 0,
        "target": 8
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            8
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            9
          ],
          [
            0,
            4,
            3
          ],
          [
            0,
            5,
            10
          ],
          [
            1,
            0,
            4
          ],
          [
            1,
            2,
            5
          ],
          [
            1,
            3,
            12
          ],
          [
            1,
            4,
            6
          ],
          [
            1,
            5,
            13
          ],
          [
            2,
            0,
            7
          ],
          [
            2,
            1,
            1
          ],
          [
            2,
            3,
            2
          ],
          [
            2,
            4,
            9
          ],
          [
            2,
            5,
            3
          ],
          [
            3,
            0,
            10
          ],
          [
            3,
            1,
            4
          ],
          [
            3,
            2,
            11
          ],
          [
            3,
            4,
            12
          ],
          [
            3,
            5,
            6
          ],
          [
            4,
            0,
            13
          ],
          [
            4,
            1,
            7
          ],
          [
            4,
            2,
            1
          ],
          [
            4,
            3,
            8
          ],
          [
            4,
            5,
            9
          ],
          [
            5,
            0,
            3
          ],
          [
            5,
            1,
            10
          ],
          [
            5,
            2,
            4
          ],
          [
            5,
            3,
            11
          ],
          [
            5,
            4,
            5
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-009-r1",
  "caseId": "route-009",
  "round": 1,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        8
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        9
      ],
      [
        0,
        4,
        3
      ],
      [
        0,
        5,
        10
      ],
      [
        1,
        0,
        4
      ],
      [
        1,
        2,
        5
      ],
      [
        1,
        3,
        12
      ],
      [
        1,
        4,
        6
      ],
      [
        1,
        5,
        13
      ],
      [
        2,
        0,
        7
      ],
      [
        2,
        1,
        1
      ],
      [
        2,
        3,
        2
      ],
      [
        2,
        4,
        9
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        0,
        10
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        11
      ],
      [
        3,
        4,
        12
      ],
      [
        3,
        5,
        6
      ],
      [
        4,
        0,
        13
      ],
      [
        4,
        1,
        7
      ],
      [
        4,
        2,
        1
      ],
      [
        4,
        3,
        8
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        0,
        3
      ],
      [
        5,
        1,
        10
      ],
      [
        5,
        2,
        4
      ],
      [
        5,
        3,
        11
      ],
      [
        5,
        4,
        5
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
3084
```

### elapsedNanos

```json
2454052
```

### adapted

```json
false
```

### reused

```json
true
```

## route-009-r2

### identifier

```json
"route-009-r2"
```

### objective

```json
"Grafo denso dirigido"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "bfbc96df2eead240918f346fdf8797fc4484aa89b4268f18fb2bc98a7ac4ab87",
  "familyState": {
    "schema": 1,
    "revision": 112,
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
            2
          ],
          [
            2,
            3,
            2
          ],
          [
            4,
            0,
            2
          ],
          [
            2,
            5,
            4
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
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
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
          ],
          [
            0,
            4,
            2
          ],
          [
            1,
            5,
            2
          ],
          [
            2,
            6,
            2
          ],
          [
            3,
            7,
            2
          ],
          [
            5,
            2,
            1
          ]
        ],
        "source": 0,
        "target": 7
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
            1,
            2,
            2
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            2
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            2
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            2
          ],
          [
            5,
            8,
            2
          ]
        ],
        "source": 0,
        "target": 8
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 5,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 2894,
    "elapsedNanos": 349296
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 5,
  "path": [
    0,
    2,
    5
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
    "id": "route-009-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 5,
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
  "fullJournalSha256": "bf53bfcca86b71e0534d1cff57215c04f7c218bf5b7710b7ababf663d94aa045",
  "familyState": {
    "schema": 1,
    "revision": 113,
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
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
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
          ],
          [
            0,
            4,
            2
          ],
          [
            1,
            5,
            2
          ],
          [
            2,
            6,
            2
          ],
          [
            3,
            7,
            2
          ],
          [
            5,
            2,
            1
          ]
        ],
        "source": 0,
        "target": 7
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
            1,
            2,
            2
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            2
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            2
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            2
          ],
          [
            5,
            8,
            2
          ]
        ],
        "source": 0,
        "target": 8
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            8
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            9
          ],
          [
            0,
            4,
            3
          ],
          [
            0,
            5,
            10
          ],
          [
            1,
            0,
            4
          ],
          [
            1,
            2,
            5
          ],
          [
            1,
            3,
            12
          ],
          [
            1,
            4,
            6
          ],
          [
            1,
            5,
            13
          ],
          [
            2,
            0,
            7
          ],
          [
            2,
            1,
            1
          ],
          [
            2,
            3,
            2
          ],
          [
            2,
            4,
            9
          ],
          [
            2,
            5,
            3
          ],
          [
            3,
            1,
            4
          ],
          [
            3,
            2,
            11
          ],
          [
            3,
            4,
            12
          ],
          [
            3,
            5,
            6
          ],
          [
            4,
            0,
            13
          ],
          [
            4,
            1,
            7
          ],
          [
            4,
            2,
            1
          ],
          [
            4,
            3,
            8
          ],
          [
            4,
            5,
            9
          ],
          [
            5,
            0,
            3
          ],
          [
            5,
            1,
            10
          ],
          [
            5,
            2,
            4
          ],
          [
            5,
            3,
            11
          ],
          [
            5,
            4,
            5
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-009-r2",
  "caseId": "route-009",
  "round": 2,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        8
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        9
      ],
      [
        0,
        4,
        3
      ],
      [
        0,
        5,
        10
      ],
      [
        1,
        0,
        4
      ],
      [
        1,
        2,
        5
      ],
      [
        1,
        3,
        12
      ],
      [
        1,
        4,
        6
      ],
      [
        1,
        5,
        13
      ],
      [
        2,
        0,
        7
      ],
      [
        2,
        1,
        1
      ],
      [
        2,
        3,
        2
      ],
      [
        2,
        4,
        9
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        11
      ],
      [
        3,
        4,
        12
      ],
      [
        3,
        5,
        6
      ],
      [
        4,
        0,
        13
      ],
      [
        4,
        1,
        7
      ],
      [
        4,
        2,
        1
      ],
      [
        4,
        3,
        8
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        0,
        3
      ],
      [
        5,
        1,
        10
      ],
      [
        5,
        2,
        4
      ],
      [
        5,
        3,
        11
      ],
      [
        5,
        4,
        5
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
2894
```

### elapsedNanos

```json
745891
```

### adapted

```json
false
```

### reused

```json
true
```

## route-009-r3

### identifier

```json
"route-009-r3"
```

### objective

```json
"Grafo denso dirigido"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "d51a74cf1251af94ff190086fb20e409ab93e29d0992d21de770c0e9a28f672e",
  "familyState": {
    "schema": 1,
    "revision": 216,
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
            8
          ],
          [
            2,
            3,
            -9
          ],
          [
            3,
            4,
            8
          ],
          [
            4,
            0,
            12
          ],
          [
            2,
            5,
            5
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
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            -8
          ],
          [
            1,
            2,
            9
          ],
          [
            2,
            3,
            -8
          ],
          [
            4,
            5,
            7
          ],
          [
            5,
            6,
            -10
          ],
          [
            6,
            7,
            7
          ],
          [
            0,
            4,
            -8
          ],
          [
            1,
            5,
            9
          ],
          [
            2,
            6,
            -8
          ],
          [
            3,
            7,
            9
          ],
          [
            5,
            2,
            0
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 7
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            1,
            2,
            8
          ],
          [
            3,
            4,
            7
          ],
          [
            4,
            5,
            8
          ],
          [
            6,
            7,
            7
          ],
          [
            7,
            8,
            8
          ],
          [
            0,
            3,
            -14
          ],
          [
            1,
            4,
            3
          ],
          [
            2,
            5,
            3
          ],
          [
            3,
            6,
            3
          ],
          [
            4,
            7,
            3
          ],
          [
            5,
            8,
            3
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 8
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 1,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 3266,
    "elapsedNanos": 682838
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 1,
  "path": [
    0,
    2,
    5
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
    "id": "route-009-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 1,
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
  "fullJournalSha256": "60d5f904f434586c9a0022a861a8fb54a9398b70439ada3e7332538a9b9d2f8b",
  "familyState": {
    "schema": 1,
    "revision": 217,
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
        "nodes": 8,
        "edges": [
          [
            0,
            1,
            -8
          ],
          [
            1,
            2,
            9
          ],
          [
            2,
            3,
            -8
          ],
          [
            4,
            5,
            7
          ],
          [
            5,
            6,
            -10
          ],
          [
            6,
            7,
            7
          ],
          [
            0,
            4,
            -8
          ],
          [
            1,
            5,
            9
          ],
          [
            2,
            6,
            -8
          ],
          [
            3,
            7,
            9
          ],
          [
            5,
            2,
            0
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 7
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            1,
            2,
            8
          ],
          [
            3,
            4,
            7
          ],
          [
            4,
            5,
            8
          ],
          [
            6,
            7,
            7
          ],
          [
            7,
            8,
            8
          ],
          [
            0,
            3,
            -14
          ],
          [
            1,
            4,
            3
          ],
          [
            2,
            5,
            3
          ],
          [
            3,
            6,
            3
          ],
          [
            4,
            7,
            3
          ],
          [
            5,
            8,
            3
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 8
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            -3
          ],
          [
            0,
            2,
            -3
          ],
          [
            0,
            3,
            -7
          ],
          [
            0,
            4,
            -7
          ],
          [
            0,
            5,
            6
          ],
          [
            1,
            0,
            15
          ],
          [
            1,
            2,
            11
          ],
          [
            1,
            3,
            7
          ],
          [
            1,
            4,
            7
          ],
          [
            1,
            5,
            20
          ],
          [
            2,
            0,
            12
          ],
          [
            2,
            1,
            -5
          ],
          [
            2,
            3,
            -9
          ],
          [
            2,
            4,
            4
          ],
          [
            2,
            5,
            4
          ],
          [
            3,
            0,
            26
          ],
          [
            3,
            1,
            9
          ],
          [
            3,
            2,
            22
          ],
          [
            3,
            4,
            18
          ],
          [
            3,
            5,
            18
          ],
          [
            4,
            0,
            23
          ],
          [
            4,
            1,
            6
          ],
          [
            4,
            2,
            6
          ],
          [
            4,
            3,
            2
          ],
          [
            4,
            5,
            15
          ],
          [
            5,
            0,
            7
          ],
          [
            5,
            1,
            3
          ],
          [
            5,
            2,
            3
          ],
          [
            5,
            3,
            -1
          ],
          [
            5,
            4,
            -1
          ],
          [
            5,
            6,
            -9
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-009-r3",
  "caseId": "route-009",
  "round": 3,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        -3
      ],
      [
        0,
        2,
        -3
      ],
      [
        0,
        3,
        -7
      ],
      [
        0,
        4,
        -7
      ],
      [
        0,
        5,
        6
      ],
      [
        1,
        0,
        15
      ],
      [
        1,
        2,
        11
      ],
      [
        1,
        3,
        7
      ],
      [
        1,
        4,
        7
      ],
      [
        1,
        5,
        20
      ],
      [
        2,
        0,
        12
      ],
      [
        2,
        1,
        -5
      ],
      [
        2,
        3,
        -9
      ],
      [
        2,
        4,
        4
      ],
      [
        2,
        5,
        4
      ],
      [
        3,
        0,
        26
      ],
      [
        3,
        1,
        9
      ],
      [
        3,
        2,
        22
      ],
      [
        3,
        4,
        18
      ],
      [
        3,
        5,
        18
      ],
      [
        4,
        0,
        23
      ],
      [
        4,
        1,
        6
      ],
      [
        4,
        2,
        6
      ],
      [
        4,
        3,
        2
      ],
      [
        4,
        5,
        15
      ],
      [
        5,
        0,
        7
      ],
      [
        5,
        1,
        3
      ],
      [
        5,
        2,
        3
      ],
      [
        5,
        3,
        -1
      ],
      [
        5,
        4,
        -1
      ],
      [
        5,
        6,
        -9
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
3266
```

### elapsedNanos

```json
1266830
```

### adapted

```json
false
```

### reused

```json
true
```

## route-009-r4

### identifier

```json
"route-009-r4"
```

### objective

```json
"Grafo denso dirigido"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "e19aee77ef0f3bc56f2e3e0a05527c9b110babdb2d01d8e38bea80b49a1077b4",
  "familyState": {
    "schema": 1,
    "revision": 320,
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
            2
          ],
          [
            2,
            3,
            2
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            0,
            2
          ],
          [
            2,
            5,
            4
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
        "nodes": 9,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
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
          ],
          [
            6,
            7,
            1
          ],
          [
            0,
            4,
            2
          ],
          [
            1,
            5,
            2
          ],
          [
            2,
            6,
            2
          ],
          [
            3,
            7,
            2
          ],
          [
            5,
            2,
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
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            2
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            2
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            2
          ],
          [
            0,
            3,
            2
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            2
          ],
          [
            5,
            8,
            2
          ],
          [
            8,
            9,
            1
          ]
        ],
        "source": 0,
        "target": 9
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 6,
      "path": [
        0,
        2,
        5,
        6
      ]
    },
    "operations": 3900,
    "elapsedNanos": 174117
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 6,
  "path": [
    0,
    2,
    5,
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
    "id": "route-009-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 6,
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
  "fullJournalSha256": "0801bf5801a101b419867108f84489a14741991a6b2b9173ba53e5d0954e8049",
  "familyState": {
    "schema": 1,
    "revision": 321,
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
        "nodes": 9,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
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
          ],
          [
            6,
            7,
            1
          ],
          [
            0,
            4,
            2
          ],
          [
            1,
            5,
            2
          ],
          [
            2,
            6,
            2
          ],
          [
            3,
            7,
            2
          ],
          [
            5,
            2,
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
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            2
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            2
          ],
          [
            6,
            7,
            1
          ],
          [
            7,
            8,
            2
          ],
          [
            0,
            3,
            2
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            2
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            2
          ],
          [
            5,
            8,
            2
          ],
          [
            8,
            9,
            1
          ]
        ],
        "source": 0,
        "target": 9
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            8
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            9
          ],
          [
            0,
            4,
            3
          ],
          [
            0,
            5,
            10
          ],
          [
            1,
            0,
            4
          ],
          [
            1,
            2,
            5
          ],
          [
            1,
            3,
            12
          ],
          [
            1,
            4,
            6
          ],
          [
            1,
            5,
            13
          ],
          [
            2,
            0,
            7
          ],
          [
            2,
            1,
            1
          ],
          [
            2,
            3,
            2
          ],
          [
            2,
            4,
            9
          ],
          [
            2,
            5,
            3
          ],
          [
            3,
            0,
            10
          ],
          [
            3,
            1,
            4
          ],
          [
            3,
            2,
            11
          ],
          [
            3,
            4,
            12
          ],
          [
            3,
            5,
            6
          ],
          [
            4,
            0,
            13
          ],
          [
            4,
            1,
            7
          ],
          [
            4,
            2,
            1
          ],
          [
            4,
            3,
            8
          ],
          [
            4,
            5,
            9
          ],
          [
            5,
            0,
            3
          ],
          [
            5,
            1,
            10
          ],
          [
            5,
            2,
            4
          ],
          [
            5,
            3,
            11
          ],
          [
            5,
            4,
            5
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-009-r4",
  "caseId": "route-009",
  "round": 4,
  "family": "route",
  "description": "Grafo denso dirigido",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        8
      ],
      [
        0,
        2,
        2
      ],
      [
        0,
        3,
        9
      ],
      [
        0,
        4,
        3
      ],
      [
        0,
        5,
        10
      ],
      [
        1,
        0,
        4
      ],
      [
        1,
        2,
        5
      ],
      [
        1,
        3,
        12
      ],
      [
        1,
        4,
        6
      ],
      [
        1,
        5,
        13
      ],
      [
        2,
        0,
        7
      ],
      [
        2,
        1,
        1
      ],
      [
        2,
        3,
        2
      ],
      [
        2,
        4,
        9
      ],
      [
        2,
        5,
        3
      ],
      [
        3,
        0,
        10
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        11
      ],
      [
        3,
        4,
        12
      ],
      [
        3,
        5,
        6
      ],
      [
        4,
        0,
        13
      ],
      [
        4,
        1,
        7
      ],
      [
        4,
        2,
        1
      ],
      [
        4,
        3,
        8
      ],
      [
        4,
        5,
        9
      ],
      [
        5,
        0,
        3
      ],
      [
        5,
        1,
        10
      ],
      [
        5,
        2,
        4
      ],
      [
        5,
        3,
        11
      ],
      [
        5,
        4,
        5
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
}
```

### operations

```json
3900
```

### elapsedNanos

```json
425928
```

### adapted

```json
false
```

### reused

```json
true
```
