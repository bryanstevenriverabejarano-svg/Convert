# route-011

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-011-r1

### identifier

```json
"route-011-r1"
```

### objective

```json
"Dos componentes sin puente"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "89a9821d3bb42c5b8cf0e97815e85106c47223726873648ee67df77253c57ae5",
  "familyState": {
    "schema": 1,
    "revision": 10,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"DIJKSTRA\",\"programa_sha256\":\"68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3\",\"version\":2,\"recibos_verificados_conservados\":9,\"candidatos_descartados_en_recibos\":1}",
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
      "status": "unreachable",
      "path": []
    },
    "operations": 3041,
    "elapsedNanos": 2139578
  }
]
```

### result

```json
{
  "status": "unreachable",
  "path": []
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
    "id": "route-011-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "reachable": false
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
  "fullJournalSha256": "a7ff8fbad55ffb97e7120ea61d90587fdcd5acf7b27626e98dba25b8d3973629",
  "familyState": {
    "schema": 1,
    "revision": 11,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-011-r1",
  "caseId": "route-011",
  "round": 1,
  "family": "route",
  "description": "Dos componentes sin puente",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
  }
}
```

### operations

```json
3041
```

### elapsedNanos

```json
3010233
```

### adapted

```json
false
```

### reused

```json
true
```

## route-011-r2

### identifier

```json
"route-011-r2"
```

### objective

```json
"Dos componentes sin puente"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "2ff2961403a104a8e7d7b0976b13c8d212f3ebcfe0cbde313fb61c52bd4f4885",
  "familyState": {
    "schema": 1,
    "revision": 114,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":10,\"candidatos_descartados_en_recibos\":0}",
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
      "status": "unreachable",
      "path": []
    },
    "operations": 2815,
    "elapsedNanos": 361914
  }
]
```

### result

```json
{
  "status": "unreachable",
  "path": []
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
    "id": "route-011-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "reachable": false
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
  "fullJournalSha256": "672d0edd24b0082492c07b4b09e0ee1dd076e2b1b5e4d8ca2f4263fb3502c80d",
  "familyState": {
    "schema": 1,
    "revision": 115,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-011-r2",
  "caseId": "route-011",
  "round": 2,
  "family": "route",
  "description": "Dos componentes sin puente",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
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
  }
}
```

### operations

```json
2815
```

### elapsedNanos

```json
731770
```

### adapted

```json
false
```

### reused

```json
true
```

## route-011-r3

### identifier

```json
"route-011-r3"
```

### objective

```json
"Dos componentes sin puente"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "a8a9e57f916d547455bfb224c0da73c28741a68049f7fa7ee88a3afaa1e2e120",
  "familyState": {
    "schema": 1,
    "revision": 218,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":10,\"candidatos_descartados_en_recibos\":0}",
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
      "status": "unreachable",
      "path": []
    },
    "operations": 3258,
    "elapsedNanos": 448381
  }
]
```

### result

```json
{
  "status": "unreachable",
  "path": []
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
    "id": "route-011-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "reachable": false
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
  "fullJournalSha256": "0f7f484dd444105de89a86870373dbb76c75f24ce976c6ae6e5add84efbc9420",
  "familyState": {
    "schema": 1,
    "revision": 219,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-011-r3",
  "caseId": "route-011",
  "round": 3,
  "family": "route",
  "description": "Dos componentes sin puente",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
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
  }
}
```

### operations

```json
3258
```

### elapsedNanos

```json
964603
```

### adapted

```json
false
```

### reused

```json
true
```

## route-011-r4

### identifier

```json
"route-011-r4"
```

### objective

```json
"Dos componentes sin puente"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "a5a8d6c7c715d1ba2d20178eb5eced2c31e4133ba9914df48ed8dfa707812615",
  "familyState": {
    "schema": 1,
    "revision": 322,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":10,\"candidatos_descartados_en_recibos\":0}",
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
      "status": "unreachable",
      "path": []
    },
    "operations": 3804,
    "elapsedNanos": 168859
  }
]
```

### result

```json
{
  "status": "unreachable",
  "path": []
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
    "id": "route-011-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "reachable": false
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
  "fullJournalSha256": "48cf24fd9458488570655b88edd8e25048dc2b10b568459dee362a6473c99d66",
  "familyState": {
    "schema": 1,
    "revision": 323,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "route-011-r4",
  "caseId": "route-011",
  "round": 4,
  "family": "route",
  "description": "Dos componentes sin puente",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
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
  }
}
```

### operations

```json
3804
```

### elapsedNanos

```json
437126
```

### adapted

```json
false
```

### reused

```json
true
```
