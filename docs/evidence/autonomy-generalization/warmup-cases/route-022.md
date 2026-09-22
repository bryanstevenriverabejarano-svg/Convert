# route-022

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-022-r1

### identifier

```json
"route-022-r1"
```

### objective

```json
"Tres caminos óptimos empatados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "ed791ad7f18c78d8acbefac7e0d3391ccefc3e7143bdada75020946d27c2df2d",
  "familyState": {
    "schema": 1,
    "revision": 21,
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
            5,
            2
          ],
          [
            5,
            6,
            2
          ],
          [
            6,
            7,
            2
          ],
          [
            7,
            8,
            2
          ],
          [
            8,
            9,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            3,
            7,
            2
          ],
          [
            1,
            6,
            2
          ],
          [
            6,
            9,
            1
          ],
          [
            7,
            9,
            8
          ]
        ],
        "source": 0,
        "target": 9
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            1
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            7,
            2
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            1
          ],
          [
            4,
            10,
            2
          ],
          [
            5,
            11,
            3
          ],
          [
            5,
            12,
            1
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            3
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
            2,
            6,
            3
          ],
          [
            3,
            7,
            3
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            3
          ],
          [
            2,
            7,
            1
          ],
          [
            7,
            9,
            1
          ],
          [
            4,
            8,
            1
          ]
        ],
        "source": 0,
        "target": 9
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
      },
      {
        "id": "route-015-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":9,\"candidatos_descartados_en_recibos\":2}",
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
        3,
        6
      ]
    },
    "operations": 6273,
    "elapsedNanos": 1514946
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
    3,
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
    "id": "route-022-r1",
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
  "fullJournalSha256": "af37685f8e5ae3416d008781828890a261d8ce1356f31651a31ef2c5b9fba75d",
  "familyState": {
    "schema": 1,
    "revision": 22,
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
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            1
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            7,
            2
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            1
          ],
          [
            4,
            10,
            2
          ],
          [
            5,
            11,
            3
          ],
          [
            5,
            12,
            1
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            3
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
            2,
            6,
            3
          ],
          [
            3,
            7,
            3
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            3
          ],
          [
            2,
            7,
            1
          ],
          [
            7,
            9,
            1
          ],
          [
            4,
            8,
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
            1
          ],
          [
            1,
            4,
            1
          ],
          [
            4,
            6,
            1
          ],
          [
            0,
            2,
            1
          ],
          [
            2,
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
            3,
            2
          ],
          [
            3,
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
      },
      {
        "id": "route-015-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r1",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-022-r1",
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
  "id": "route-022-r1",
  "caseId": "route-022",
  "round": 1,
  "family": "route",
  "description": "Tres caminos óptimos empatados",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
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
        4,
        1
      ],
      [
        4,
        6,
        1
      ],
      [
        0,
        2,
        1
      ],
      [
        2,
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
        3,
        2
      ],
      [
        3,
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
6273
```

### elapsedNanos

```json
2018630
```

### adapted

```json
false
```

### reused

```json
true
```

## route-022-r2

### identifier

```json
"route-022-r2"
```

### objective

```json
"Tres caminos óptimos empatados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "00415ccdaa7145eafd7d1d93a96a2bb6314ad7b3b8a8b28437744bd83f5207ee",
  "familyState": {
    "schema": 1,
    "revision": 125,
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
            5,
            2
          ],
          [
            5,
            6,
            2
          ],
          [
            6,
            7,
            2
          ],
          [
            8,
            9,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            3,
            7,
            2
          ],
          [
            1,
            6,
            2
          ],
          [
            6,
            9,
            1
          ],
          [
            7,
            9,
            8
          ]
        ],
        "source": 0,
        "target": 9
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            1
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            1
          ],
          [
            4,
            10,
            2
          ],
          [
            5,
            11,
            3
          ],
          [
            5,
            12,
            1
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            3
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
            2,
            6,
            3
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            3
          ],
          [
            2,
            7,
            1
          ],
          [
            7,
            9,
            1
          ],
          [
            4,
            8,
            1
          ]
        ],
        "source": 0,
        "target": 9
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
      },
      {
        "id": "route-015-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
        3,
        6
      ]
    },
    "operations": 6253,
    "elapsedNanos": 299251
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
    3,
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
    "id": "route-022-r2",
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
  "fullJournalSha256": "fb57a37b8eb71ef5367fef07bba257cb4df4be0d289d0543a8e149dac14db248",
  "familyState": {
    "schema": 1,
    "revision": 126,
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
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            1
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            1
          ],
          [
            4,
            10,
            2
          ],
          [
            5,
            11,
            3
          ],
          [
            5,
            12,
            1
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            3
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
            2,
            6,
            3
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            3
          ],
          [
            2,
            7,
            1
          ],
          [
            7,
            9,
            1
          ],
          [
            4,
            8,
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
            1
          ],
          [
            1,
            4,
            1
          ],
          [
            4,
            6,
            1
          ],
          [
            0,
            2,
            1
          ],
          [
            5,
            6,
            1
          ],
          [
            0,
            3,
            2
          ],
          [
            3,
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
      },
      {
        "id": "route-015-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-022-r2",
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
  "id": "route-022-r2",
  "caseId": "route-022",
  "round": 2,
  "family": "route",
  "description": "Tres caminos óptimos empatados",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
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
        4,
        1
      ],
      [
        4,
        6,
        1
      ],
      [
        0,
        2,
        1
      ],
      [
        5,
        6,
        1
      ],
      [
        0,
        3,
        2
      ],
      [
        3,
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
6253
```

### elapsedNanos

```json
691481
```

### adapted

```json
false
```

### reused

```json
true
```

## route-022-r3

### identifier

```json
"route-022-r3"
```

### objective

```json
"Tres caminos óptimos empatados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "f5256f9be7298c876f5aefacc2a8087303f8e1382a92200ea98f9f32c989fbea",
  "familyState": {
    "schema": 1,
    "revision": 229,
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
            5,
            8
          ],
          [
            5,
            6,
            -9
          ],
          [
            6,
            7,
            8
          ],
          [
            7,
            8,
            8
          ],
          [
            8,
            9,
            -9
          ],
          [
            0,
            3,
            -13
          ],
          [
            3,
            7,
            9
          ],
          [
            1,
            6,
            -2
          ],
          [
            6,
            9,
            2
          ],
          [
            7,
            9,
            3
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 9
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            0,
            2,
            -2
          ],
          [
            1,
            3,
            -4
          ],
          [
            1,
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
            -9
          ],
          [
            3,
            7,
            9
          ],
          [
            3,
            8,
            16
          ],
          [
            4,
            9,
            -3
          ],
          [
            4,
            10,
            4
          ],
          [
            5,
            11,
            5
          ],
          [
            5,
            12,
            -8
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            -8
          ],
          [
            0,
            2,
            -2
          ],
          [
            1,
            3,
            -2
          ],
          [
            1,
            4,
            4
          ],
          [
            2,
            5,
            4
          ],
          [
            2,
            6,
            -7
          ],
          [
            3,
            7,
            10
          ],
          [
            3,
            8,
            16
          ],
          [
            4,
            9,
            -1
          ],
          [
            2,
            7,
            -3
          ],
          [
            7,
            9,
            -4
          ],
          [
            4,
            8,
            8
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 9
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
      },
      {
        "id": "route-015-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": -12,
      "path": [
        0,
        3,
        6
      ]
    },
    "operations": 6715,
    "elapsedNanos": 170521
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": -12,
  "path": [
    0,
    3,
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
    "id": "route-022-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -12,
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
  "fullJournalSha256": "c1954414188bbac40a725a19a210b7426f073a294ec150afbe2768c60906d47f",
  "familyState": {
    "schema": 1,
    "revision": 230,
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
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            0,
            2,
            -2
          ],
          [
            1,
            3,
            -4
          ],
          [
            1,
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
            -9
          ],
          [
            3,
            7,
            9
          ],
          [
            3,
            8,
            16
          ],
          [
            4,
            9,
            -3
          ],
          [
            4,
            10,
            4
          ],
          [
            5,
            11,
            5
          ],
          [
            5,
            12,
            -8
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            1,
            -8
          ],
          [
            0,
            2,
            -2
          ],
          [
            1,
            3,
            -2
          ],
          [
            1,
            4,
            4
          ],
          [
            2,
            5,
            4
          ],
          [
            2,
            6,
            -7
          ],
          [
            3,
            7,
            10
          ],
          [
            3,
            8,
            16
          ],
          [
            4,
            9,
            -1
          ],
          [
            2,
            7,
            -3
          ],
          [
            7,
            9,
            -4
          ],
          [
            4,
            8,
            8
          ],
          [
            0,
            3,
            -12
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
            -10
          ],
          [
            1,
            4,
            2
          ],
          [
            4,
            6,
            -4
          ],
          [
            0,
            2,
            -4
          ],
          [
            2,
            5,
            2
          ],
          [
            5,
            6,
            -10
          ],
          [
            0,
            3,
            -14
          ],
          [
            3,
            6,
            2
          ],
          [
            0,
            4,
            -6
          ]
        ],
        "source": 0,
        "target": 6
      }
    ],
    "receipts": [
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
      },
      {
        "id": "route-015-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-022-r3",
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
  "id": "route-022-r3",
  "caseId": "route-022",
  "round": 3,
  "family": "route",
  "description": "Tres caminos óptimos empatados",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        -10
      ],
      [
        1,
        4,
        2
      ],
      [
        4,
        6,
        -4
      ],
      [
        0,
        2,
        -4
      ],
      [
        2,
        5,
        2
      ],
      [
        5,
        6,
        -10
      ],
      [
        0,
        3,
        -14
      ],
      [
        3,
        6,
        2
      ],
      [
        0,
        4,
        -6
      ]
    ],
    "source": 0,
    "target": 6
  }
}
```

### operations

```json
6715
```

### elapsedNanos

```json
445969
```

### adapted

```json
false
```

### reused

```json
true
```

## route-022-r4

### identifier

```json
"route-022-r4"
```

### objective

```json
"Tres caminos óptimos empatados"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "dfdc34d1ee5ac41bb4d8e1a0276cb650a5533fcc0685fded7701c04ad46bbd46",
  "familyState": {
    "schema": 1,
    "revision": 333,
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
        "nodes": 11,
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
            5,
            2
          ],
          [
            5,
            6,
            2
          ],
          [
            6,
            7,
            2
          ],
          [
            7,
            8,
            2
          ],
          [
            8,
            9,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            3,
            7,
            2
          ],
          [
            1,
            6,
            2
          ],
          [
            6,
            9,
            1
          ],
          [
            7,
            9,
            8
          ],
          [
            9,
            10,
            1
          ]
        ],
        "source": 0,
        "target": 10
      },
      {
        "nodes": 14,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            1
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            7,
            2
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            1
          ],
          [
            4,
            10,
            2
          ],
          [
            5,
            11,
            3
          ],
          [
            5,
            12,
            1
          ],
          [
            12,
            13,
            1
          ]
        ],
        "source": 0,
        "target": 13
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            3
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
            2,
            6,
            3
          ],
          [
            3,
            7,
            3
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            3
          ],
          [
            2,
            7,
            1
          ],
          [
            7,
            9,
            1
          ],
          [
            4,
            8,
            1
          ],
          [
            9,
            10,
            1
          ]
        ],
        "source": 0,
        "target": 10
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
      },
      {
        "id": "route-015-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r4",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 4,
      "path": [
        0,
        3,
        6,
        7
      ]
    },
    "operations": 7947,
    "elapsedNanos": 164503
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 4,
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
    "id": "route-022-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 4,
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
  "fullJournalSha256": "3827f28774dc8100e349aab1005d78edb88556e658929c164dc94d45ed7b5689",
  "familyState": {
    "schema": 1,
    "revision": 334,
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
        "nodes": 14,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            1
          ],
          [
            1,
            4,
            2
          ],
          [
            2,
            5,
            3
          ],
          [
            2,
            6,
            1
          ],
          [
            3,
            7,
            2
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            1
          ],
          [
            4,
            10,
            2
          ],
          [
            5,
            11,
            3
          ],
          [
            5,
            12,
            1
          ],
          [
            12,
            13,
            1
          ]
        ],
        "source": 0,
        "target": 13
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            0,
            2,
            3
          ],
          [
            1,
            3,
            3
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
            2,
            6,
            3
          ],
          [
            3,
            7,
            3
          ],
          [
            3,
            8,
            3
          ],
          [
            4,
            9,
            3
          ],
          [
            2,
            7,
            1
          ],
          [
            7,
            9,
            1
          ],
          [
            4,
            8,
            1
          ],
          [
            9,
            10,
            1
          ]
        ],
        "source": 0,
        "target": 10
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
            1,
            4,
            1
          ],
          [
            4,
            6,
            1
          ],
          [
            0,
            2,
            1
          ],
          [
            2,
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
            3,
            2
          ],
          [
            3,
            6,
            1
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
      },
      {
        "id": "route-015-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-016-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-017-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-018-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-019-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-020-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-021-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-022-r4",
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
  "id": "route-022-r4",
  "caseId": "route-022",
  "round": 4,
  "family": "route",
  "description": "Tres caminos óptimos empatados",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 8,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        4,
        1
      ],
      [
        4,
        6,
        1
      ],
      [
        0,
        2,
        1
      ],
      [
        2,
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
        3,
        2
      ],
      [
        3,
        6,
        1
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
7947
```

### elapsedNanos

```json
387371
```

### adapted

```json
false
```

### reused

```json
true
```
