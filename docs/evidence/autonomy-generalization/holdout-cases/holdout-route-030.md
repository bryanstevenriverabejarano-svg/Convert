# holdout-route-030

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-030

### identifier

```json
"holdout-route-030"
```

### objective

```json
"Grafo dirigido aleatorio con ciclos de coste no negativo"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "6cfa5a3df96680dd9199b57117157ab6bb8be5ec63618faa16603739db086831",
  "familyState": {
    "schema": 1,
    "revision": 503,
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
            2,
            9,
            -5
          ],
          [
            2,
            0,
            -4
          ],
          [
            2,
            6,
            8
          ],
          [
            2,
            7,
            -7
          ],
          [
            2,
            1,
            6
          ],
          [
            9,
            10,
            23
          ],
          [
            9,
            4,
            6
          ],
          [
            9,
            3,
            18
          ],
          [
            9,
            1,
            11
          ],
          [
            10,
            0,
            -5
          ],
          [
            0,
            4,
            10
          ],
          [
            0,
            7,
            6
          ],
          [
            0,
            3,
            17
          ],
          [
            4,
            5,
            0
          ],
          [
            5,
            8,
            -1
          ],
          [
            5,
            6,
            -1
          ],
          [
            8,
            6,
            10
          ],
          [
            8,
            7,
            1
          ],
          [
            8,
            3,
            -3
          ],
          [
            8,
            1,
            19
          ],
          [
            6,
            7,
            9
          ],
          [
            7,
            3,
            -8
          ],
          [
            3,
            1,
            10
          ]
        ],
        "source": 2,
        "target": 1
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            28
          ],
          [
            3,
            4,
            1
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 12,
        "edges": [
          [
            0,
            6,
            17
          ],
          [
            1,
            2,
            -4
          ],
          [
            1,
            7,
            3
          ],
          [
            2,
            1,
            30
          ],
          [
            2,
            8,
            27
          ],
          [
            3,
            5,
            4
          ],
          [
            3,
            10,
            25
          ],
          [
            4,
            0,
            7
          ],
          [
            4,
            7,
            17
          ],
          [
            5,
            0,
            41
          ],
          [
            5,
            7,
            18
          ],
          [
            5,
            10,
            38
          ],
          [
            6,
            0,
            6
          ],
          [
            6,
            1,
            -11
          ],
          [
            6,
            11,
            -15
          ],
          [
            7,
            4,
            24
          ],
          [
            7,
            9,
            12
          ],
          [
            8,
            1,
            33
          ],
          [
            9,
            1,
            30
          ],
          [
            9,
            7,
            34
          ],
          [
            9,
            8,
            18
          ],
          [
            9,
            11,
            33
          ],
          [
            11,
            2,
            6
          ],
          [
            11,
            7,
            5
          ],
          [
            11,
            9,
            -7
          ],
          [
            0,
            1,
            3
          ],
          [
            2,
            3,
            28
          ],
          [
            3,
            4,
            44
          ],
          [
            4,
            5,
            -24
          ],
          [
            5,
            6,
            50
          ],
          [
            6,
            7,
            -20
          ],
          [
            7,
            8,
            -7
          ],
          [
            8,
            9,
            12
          ],
          [
            9,
            10,
            37
          ],
          [
            10,
            11,
            22
          ],
          [
            11,
            0,
            22
          ]
        ],
        "source": 0,
        "target": 11
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-017",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-018",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-031",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-007",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-005",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-027",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-028",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 23,
      "path": [
        0,
        6,
        7,
        8
      ]
    },
    "operations": 6434,
    "elapsedNanos": 269869
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 23,
  "path": [
    0,
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
    "id": "holdout-route-030",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 23,
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
  "fullJournalSha256": "4d022900a2d82c8fbdf26059b3c95af4663e8ab9aab6c6f3cea1dda07bb98138",
  "familyState": {
    "schema": 1,
    "revision": 504,
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
            28
          ],
          [
            3,
            4,
            1
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 12,
        "edges": [
          [
            0,
            6,
            17
          ],
          [
            1,
            2,
            -4
          ],
          [
            1,
            7,
            3
          ],
          [
            2,
            1,
            30
          ],
          [
            2,
            8,
            27
          ],
          [
            3,
            5,
            4
          ],
          [
            3,
            10,
            25
          ],
          [
            4,
            0,
            7
          ],
          [
            4,
            7,
            17
          ],
          [
            5,
            0,
            41
          ],
          [
            5,
            7,
            18
          ],
          [
            5,
            10,
            38
          ],
          [
            6,
            0,
            6
          ],
          [
            6,
            1,
            -11
          ],
          [
            6,
            11,
            -15
          ],
          [
            7,
            4,
            24
          ],
          [
            7,
            9,
            12
          ],
          [
            8,
            1,
            33
          ],
          [
            9,
            1,
            30
          ],
          [
            9,
            7,
            34
          ],
          [
            9,
            8,
            18
          ],
          [
            9,
            11,
            33
          ],
          [
            11,
            2,
            6
          ],
          [
            11,
            7,
            5
          ],
          [
            11,
            9,
            -7
          ],
          [
            0,
            1,
            3
          ],
          [
            2,
            3,
            28
          ],
          [
            3,
            4,
            44
          ],
          [
            4,
            5,
            -24
          ],
          [
            5,
            6,
            50
          ],
          [
            6,
            7,
            -20
          ],
          [
            7,
            8,
            -7
          ],
          [
            8,
            9,
            12
          ],
          [
            9,
            10,
            37
          ],
          [
            10,
            11,
            22
          ],
          [
            11,
            0,
            22
          ]
        ],
        "source": 0,
        "target": 11
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            3,
            32
          ],
          [
            0,
            6,
            8
          ],
          [
            3,
            8,
            2
          ],
          [
            4,
            3,
            16
          ],
          [
            5,
            2,
            31
          ],
          [
            5,
            3,
            30
          ],
          [
            0,
            1,
            21
          ],
          [
            1,
            2,
            1
          ],
          [
            2,
            3,
            23
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            5,
            19
          ],
          [
            5,
            6,
            9
          ],
          [
            6,
            7,
            11
          ],
          [
            7,
            8,
            4
          ],
          [
            8,
            0,
            7
          ]
        ],
        "source": 0,
        "target": 8
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-018",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-031",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-007",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-005",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-027",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-028",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-030",
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
  "id": "holdout-route-030",
  "family": "route",
  "description": "Grafo dirigido aleatorio con ciclos de coste no negativo",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        3,
        32
      ],
      [
        0,
        6,
        8
      ],
      [
        3,
        8,
        2
      ],
      [
        4,
        3,
        16
      ],
      [
        5,
        2,
        31
      ],
      [
        5,
        3,
        30
      ],
      [
        0,
        1,
        21
      ],
      [
        1,
        2,
        1
      ],
      [
        2,
        3,
        23
      ],
      [
        3,
        4,
        2
      ],
      [
        4,
        5,
        19
      ],
      [
        5,
        6,
        9
      ],
      [
        6,
        7,
        11
      ],
      [
        7,
        8,
        4
      ],
      [
        8,
        0,
        7
      ]
    ],
    "source": 0,
    "target": 8
  }
}
```

### operations

```json
6434
```

### elapsedNanos

```json
604663
```

### adapted

```json
false
```

### reused

```json
true
```
