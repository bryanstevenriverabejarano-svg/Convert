# holdout-route-011

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-011

### identifier

```json
"holdout-route-011"
```

### objective

```json
"Dos componentes aleatorias con destino inalcanzable"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "45068e5ce817a0fc29f7df3a463ed33f41f5dfbba668a9f158c5edc3d11124a8",
  "familyState": {
    "schema": 1,
    "revision": 439,
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
            3,
            4,
            3
          ],
          [
            4,
            5,
            3
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            3
          ],
          [
            8,
            9,
            3
          ],
          [
            9,
            10,
            3
          ],
          [
            10,
            11,
            3
          ],
          [
            11,
            12,
            3
          ],
          [
            12,
            13,
            3
          ],
          [
            1,
            11,
            1
          ],
          [
            0,
            13,
            40
          ],
          [
            8,
            3,
            2
          ],
          [
            0,
            2,
            4
          ]
        ],
        "source": 13,
        "target": 0
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            16
          ],
          [
            1,
            0,
            23
          ],
          [
            3,
            2,
            7
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 6,
        "edges": [
          [
            2,
            0,
            -8
          ],
          [
            2,
            3,
            -8
          ],
          [
            2,
            4,
            2
          ],
          [
            0,
            1,
            -3
          ],
          [
            0,
            3,
            -4
          ],
          [
            0,
            5,
            15
          ],
          [
            1,
            3,
            -6
          ],
          [
            3,
            5,
            6
          ],
          [
            5,
            4,
            11
          ]
        ],
        "source": 2,
        "target": 4
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-019",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-029",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
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
    "operations": 4817,
    "elapsedNanos": 204101
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
    "id": "holdout-route-011",
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
  "fullJournalSha256": "f4c1e7b3ead9b9c4743c603310c94f55ab85782e0633b5ade3fef7447e75ce36",
  "familyState": {
    "schema": 1,
    "revision": 440,
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
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            16
          ],
          [
            1,
            0,
            23
          ],
          [
            3,
            2,
            7
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 6,
        "edges": [
          [
            2,
            0,
            -8
          ],
          [
            2,
            3,
            -8
          ],
          [
            2,
            4,
            2
          ],
          [
            0,
            1,
            -3
          ],
          [
            0,
            3,
            -4
          ],
          [
            0,
            5,
            15
          ],
          [
            1,
            3,
            -6
          ],
          [
            3,
            5,
            6
          ],
          [
            5,
            4,
            11
          ]
        ],
        "source": 2,
        "target": 4
      },
      {
        "nodes": 8,
        "edges": [
          [
            1,
            2,
            7
          ],
          [
            1,
            3,
            15
          ],
          [
            2,
            1,
            0
          ],
          [
            2,
            3,
            29
          ],
          [
            3,
            1,
            4
          ],
          [
            3,
            2,
            5
          ],
          [
            4,
            5,
            7
          ],
          [
            5,
            6,
            6
          ],
          [
            5,
            7,
            15
          ],
          [
            6,
            4,
            28
          ],
          [
            6,
            5,
            1
          ],
          [
            6,
            7,
            13
          ],
          [
            7,
            5,
            0
          ]
        ],
        "source": 0,
        "target": 7
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-019",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-029",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-011",
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
  "id": "holdout-route-011",
  "family": "route",
  "description": "Dos componentes aleatorias con destino inalcanzable",
  "input": {
    "nodes": 8,
    "edges": [
      [
        1,
        2,
        7
      ],
      [
        1,
        3,
        15
      ],
      [
        2,
        1,
        0
      ],
      [
        2,
        3,
        29
      ],
      [
        3,
        1,
        4
      ],
      [
        3,
        2,
        5
      ],
      [
        4,
        5,
        7
      ],
      [
        5,
        6,
        6
      ],
      [
        5,
        7,
        15
      ],
      [
        6,
        4,
        28
      ],
      [
        6,
        5,
        1
      ],
      [
        6,
        7,
        13
      ],
      [
        7,
        5,
        0
      ]
    ],
    "source": 0,
    "target": 7
  }
}
```

### operations

```json
4817
```

### elapsedNanos

```json
400190
```

### adapted

```json
false
```

### reused

```json
true
```
