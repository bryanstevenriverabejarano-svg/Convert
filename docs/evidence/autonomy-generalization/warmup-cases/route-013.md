# route-013

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-013-r1

### identifier

```json
"route-013-r1"
```

### objective

```json
"DAG con descuento negativo que invalida Dijkstra ingenuo"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "e75da4fa535ae716e42f9953224b05ea65ab3604921a741813285eed3e73ca52",
  "familyState": {
    "schema": 1,
    "revision": 12,
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
            1
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            1,
            4,
            2
          ],
          [
            1,
            5,
            3
          ],
          [
            1,
            6,
            4
          ],
          [
            2,
            4,
            3
          ],
          [
            2,
            5,
            4
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            4,
            4
          ],
          [
            3,
            5,
            1
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            3
          ],
          [
            5,
            7,
            2
          ],
          [
            6,
            7,
            1
          ]
        ],
        "source": 0,
        "target": 7
      },
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
  },
  {
    "strategy": "FEWEST_EDGES",
    "programSha256": "13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04",
    "programReference": "actionsExecuted[1].program"
  },
  {
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[2].program"
  }
]
```

### toolsUsed

```json
[
  "DIJKSTRA",
  "FEWEST_EDGES",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"DIJKSTRA\",\"programa_sha256\":\"68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3\",\"version\":2,\"recibos_verificados_conservados\":11,\"candidatos_descartados_en_recibos\":1}",
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
    "passed": false,
    "regressionChecks": 0,
    "feedback": "Datos o precondiciones incompatibles con el candidato",
    "operations": 140,
    "elapsedNanos": 202238
  },
  {
    "strategy": "FEWEST_EDGES",
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
          "strategy": "FEWEST_EDGES"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        1,
        3,
        5
      ]
    },
    "operations": 452,
    "elapsedNanos": 358569
  },
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
      "cost": 2,
      "path": [
        0,
        2,
        1,
        3,
        5
      ]
    },
    "operations": 2155,
    "elapsedNanos": 1415479
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 2,
  "path": [
    0,
    2,
    1,
    3,
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
  "status": "not_established",
  "observedFeedback": [
    "Datos o precondiciones incompatibles con el candidato",
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
    "DIJKSTRA",
    "FEWEST_EDGES",
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
  "scope": "Cambios de programa declarativo y diario; no se modifica código fuente ni pesos del modelo."
}
```

### newExecution

```json
[
  {
    "strategy": "FEWEST_EDGES",
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
          "strategy": "FEWEST_EDGES"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        1,
        3,
        5
      ]
    },
    "operations": 452,
    "elapsedNanos": 358569
  },
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
      "cost": 2,
      "path": [
        0,
        2,
        1,
        3,
        5
      ]
    },
    "operations": 2155,
    "elapsedNanos": 1415479
  }
]
```

### newResult

```json
{
  "status": "ok",
  "cost": 2,
  "path": [
    0,
    2,
    1,
    3,
    5
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
    "strategy": "DIJKSTRA",
    "checks": 0,
    "passed": false
  },
  {
    "strategy": "FEWEST_EDGES",
    "checks": 0,
    "passed": false
  },
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
    "id": "route-013-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 2,
      "reachable": true
    }
  },
  "decisionSummary": "Descarté candidatos que fallaron el contrato y verifiqué una alternativa con regresiones.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

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

### challenge

```json
{
  "schema": 1,
  "id": "route-013-r1",
  "caseId": "route-013",
  "round": 1,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
}
```

### operations

```json
2747
```

### elapsedNanos

```json
2977033
```

### adapted

```json
true
```

### reused

```json
false
```

## route-013-r2

### identifier

```json
"route-013-r2"
```

### objective

```json
"DAG con descuento negativo que invalida Dijkstra ingenuo"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "3b213f11a767bffb7c1e1e02247fae31caf3164b6978267e727ecfcca58196d4",
  "familyState": {
    "schema": 1,
    "revision": 116,
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
            1
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            1,
            4,
            2
          ],
          [
            1,
            5,
            3
          ],
          [
            1,
            6,
            4
          ],
          [
            2,
            4,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            4,
            4
          ],
          [
            3,
            5,
            1
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            3
          ],
          [
            5,
            7,
            2
          ],
          [
            6,
            7,
            1
          ]
        ],
        "source": 0,
        "target": 7
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
        5
      ]
    },
    "operations": 2058,
    "elapsedNanos": 226214
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
    "id": "route-013-r2",
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

### challenge

```json
{
  "schema": 1,
  "id": "route-013-r2",
  "caseId": "route-013",
  "round": 2,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
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
}
```

### operations

```json
2058
```

### elapsedNanos

```json
681135
```

### adapted

```json
false
```

### reused

```json
true
```

## route-013-r3

### identifier

```json
"route-013-r3"
```

### objective

```json
"DAG con descuento negativo que invalida Dijkstra ingenuo"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "faf0016552407edd998dd8f4cfc2ff3f3a47f6c23d50798250c36b9d24292e50",
  "familyState": {
    "schema": 1,
    "revision": 220,
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
            -10
          ],
          [
            0,
            2,
            -3
          ],
          [
            0,
            3,
            -13
          ],
          [
            1,
            4,
            3
          ],
          [
            1,
            5,
            10
          ],
          [
            1,
            6,
            0
          ],
          [
            2,
            4,
            -2
          ],
          [
            2,
            5,
            5
          ],
          [
            2,
            6,
            -9
          ],
          [
            3,
            4,
            10
          ],
          [
            3,
            5,
            13
          ],
          [
            3,
            6,
            3
          ],
          [
            4,
            7,
            4
          ],
          [
            5,
            7,
            -3
          ],
          [
            6,
            7,
            7
          ],
          [
            0,
            4,
            -6
          ]
        ],
        "source": 0,
        "target": 7
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
        3,
        5
      ]
    },
    "operations": 2234,
    "elapsedNanos": 255517
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
    3,
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
    "id": "route-013-r3",
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

### challenge

```json
{
  "schema": 1,
  "id": "route-013-r3",
  "caseId": "route-013",
  "round": 3,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
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
}
```

### operations

```json
2234
```

### elapsedNanos

```json
678101
```

### adapted

```json
false
```

### reused

```json
true
```

## route-013-r4

### identifier

```json
"route-013-r4"
```

### objective

```json
"DAG con descuento negativo que invalida Dijkstra ingenuo"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "e52802d0ff55cc836f9ca8853595ae45a0c23e9e0ef14a5f591cf8a711103668",
  "familyState": {
    "schema": 1,
    "revision": 324,
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
            1
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            1,
            4,
            2
          ],
          [
            1,
            5,
            3
          ],
          [
            1,
            6,
            4
          ],
          [
            2,
            4,
            3
          ],
          [
            2,
            5,
            4
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            4,
            4
          ],
          [
            3,
            5,
            1
          ],
          [
            3,
            6,
            2
          ],
          [
            4,
            7,
            3
          ],
          [
            5,
            7,
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
            1
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 3,
      "path": [
        0,
        2,
        1,
        3,
        5,
        6
      ]
    },
    "operations": 3001,
    "elapsedNanos": 122280
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 3,
  "path": [
    0,
    2,
    1,
    3,
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
    "id": "route-013-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 3,
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

### challenge

```json
{
  "schema": 1,
  "id": "route-013-r4",
  "caseId": "route-013",
  "round": 4,
  "family": "route",
  "description": "DAG con descuento negativo que invalida Dijkstra ingenuo",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
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
}
```

### operations

```json
3001
```

### elapsedNanos

```json
370697
```

### adapted

```json
false
```

### reused

```json
true
```
