# holdout-route-021

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-021

### identifier

```json
"holdout-route-021"
```

### objective

```json
"DAG aleatorio con etiquetas permutadas y costes negativos"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "0b8f69f37a61236c1be77c3dfe2eceab5e27e692cfb96c649b64828e64f4ef08",
  "familyState": {
    "schema": 1,
    "revision": 443,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": -29,
      "path": [
        3,
        2,
        6,
        5,
        8,
        9
      ]
    },
    "operations": 3494,
    "elapsedNanos": 129711
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": -29,
  "path": [
    3,
    2,
    6,
    5,
    8,
    9
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
    "id": "holdout-route-021",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -29,
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
  "fullJournalSha256": "9a9c13f1689a72f449161dc3cad0be9f75c9841779f8e1088284c5f610ebf3af",
  "familyState": {
    "schema": 1,
    "revision": 444,
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
      },
      {
        "nodes": 10,
        "edges": [
          [
            3,
            1,
            -9
          ],
          [
            3,
            2,
            -9
          ],
          [
            3,
            4,
            -6
          ],
          [
            3,
            8,
            -2
          ],
          [
            3,
            7,
            7
          ],
          [
            1,
            2,
            2
          ],
          [
            1,
            6,
            16
          ],
          [
            1,
            4,
            14
          ],
          [
            2,
            6,
            -6
          ],
          [
            6,
            5,
            -4
          ],
          [
            5,
            0,
            17
          ],
          [
            5,
            8,
            -4
          ],
          [
            0,
            4,
            5
          ],
          [
            4,
            8,
            12
          ],
          [
            8,
            7,
            14
          ],
          [
            8,
            9,
            -6
          ],
          [
            7,
            9,
            3
          ]
        ],
        "source": 3,
        "target": 9
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
      },
      {
        "id": "holdout-route-021",
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
  "id": "holdout-route-021",
  "family": "route",
  "description": "DAG aleatorio con etiquetas permutadas y costes negativos",
  "input": {
    "nodes": 10,
    "edges": [
      [
        3,
        1,
        -9
      ],
      [
        3,
        2,
        -9
      ],
      [
        3,
        4,
        -6
      ],
      [
        3,
        8,
        -2
      ],
      [
        3,
        7,
        7
      ],
      [
        1,
        2,
        2
      ],
      [
        1,
        6,
        16
      ],
      [
        1,
        4,
        14
      ],
      [
        2,
        6,
        -6
      ],
      [
        6,
        5,
        -4
      ],
      [
        5,
        0,
        17
      ],
      [
        5,
        8,
        -4
      ],
      [
        0,
        4,
        5
      ],
      [
        4,
        8,
        12
      ],
      [
        8,
        7,
        14
      ],
      [
        8,
        9,
        -6
      ],
      [
        7,
        9,
        3
      ]
    ],
    "source": 3,
    "target": 9
  }
}
```

### operations

```json
3494
```

### elapsedNanos

```json
339821
```

### adapted

```json
false
```

### reused

```json
true
```
