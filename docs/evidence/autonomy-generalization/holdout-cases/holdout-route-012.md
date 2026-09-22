# holdout-route-012

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-012

### identifier

```json
"holdout-route-012"
```

### objective

```json
"Grafo cíclico aleatorio con potenciales: posibles arcos negativos sin ciclos negativos"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "135af7a6fa79c76436032a99ab1b920b0a083aa09c2d04127662feb9794c06a1",
  "familyState": {
    "schema": 1,
    "revision": 527,
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
            2,
            31
          ],
          [
            0,
            3,
            14
          ],
          [
            1,
            2,
            10
          ],
          [
            1,
            3,
            9
          ],
          [
            2,
            0,
            20
          ],
          [
            2,
            3,
            33
          ],
          [
            3,
            0,
            5
          ],
          [
            3,
            2,
            21
          ],
          [
            4,
            5,
            1
          ],
          [
            5,
            6,
            8
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
            3,
            32
          ],
          [
            0,
            5,
            23
          ],
          [
            0,
            8,
            10
          ],
          [
            1,
            0,
            31
          ],
          [
            1,
            2,
            8
          ],
          [
            1,
            5,
            4
          ],
          [
            1,
            7,
            13
          ],
          [
            1,
            8,
            16
          ],
          [
            2,
            4,
            23
          ],
          [
            2,
            7,
            34
          ],
          [
            2,
            8,
            23
          ],
          [
            3,
            0,
            3
          ],
          [
            3,
            4,
            1
          ],
          [
            3,
            7,
            22
          ],
          [
            4,
            2,
            23
          ],
          [
            4,
            6,
            1
          ],
          [
            4,
            7,
            21
          ],
          [
            5,
            0,
            19
          ],
          [
            5,
            1,
            5
          ],
          [
            5,
            3,
            31
          ],
          [
            5,
            4,
            32
          ],
          [
            5,
            6,
            17
          ],
          [
            5,
            7,
            24
          ],
          [
            5,
            8,
            32
          ],
          [
            6,
            2,
            34
          ],
          [
            6,
            3,
            29
          ],
          [
            6,
            7,
            24
          ],
          [
            7,
            1,
            30
          ],
          [
            7,
            2,
            4
          ],
          [
            7,
            3,
            6
          ],
          [
            7,
            8,
            7
          ],
          [
            8,
            0,
            9
          ],
          [
            8,
            2,
            26
          ],
          [
            8,
            3,
            20
          ],
          [
            8,
            5,
            27
          ],
          [
            8,
            6,
            35
          ],
          [
            0,
            1,
            7
          ],
          [
            2,
            3,
            9
          ],
          [
            4,
            5,
            10
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
            4
          ],
          [
            0,
            3,
            18
          ],
          [
            0,
            4,
            24
          ],
          [
            0,
            6,
            33
          ],
          [
            0,
            7,
            0
          ],
          [
            1,
            3,
            16
          ],
          [
            1,
            4,
            11
          ],
          [
            1,
            5,
            5
          ],
          [
            1,
            6,
            14
          ],
          [
            1,
            7,
            23
          ],
          [
            2,
            1,
            29
          ],
          [
            3,
            0,
            24
          ],
          [
            3,
            1,
            1
          ],
          [
            5,
            0,
            15
          ],
          [
            5,
            1,
            1
          ],
          [
            5,
            4,
            24
          ],
          [
            7,
            0,
            33
          ],
          [
            7,
            2,
            8
          ]
        ],
        "source": 0,
        "target": 9
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-030",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-013",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-003",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-010",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-023",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
        2,
        8,
        9
      ]
    },
    "operations": 4901,
    "elapsedNanos": 309587
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
    2,
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
    "id": "holdout-route-012",
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
  "fullJournalSha256": "dca14fd0bbb9b6f7f5b9716ee1fb36c80bf6f6d2fd1aa76537d76c29da5c7251",
  "familyState": {
    "schema": 1,
    "revision": 528,
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
            3,
            32
          ],
          [
            0,
            5,
            23
          ],
          [
            0,
            8,
            10
          ],
          [
            1,
            0,
            31
          ],
          [
            1,
            2,
            8
          ],
          [
            1,
            5,
            4
          ],
          [
            1,
            7,
            13
          ],
          [
            1,
            8,
            16
          ],
          [
            2,
            4,
            23
          ],
          [
            2,
            7,
            34
          ],
          [
            2,
            8,
            23
          ],
          [
            3,
            0,
            3
          ],
          [
            3,
            4,
            1
          ],
          [
            3,
            7,
            22
          ],
          [
            4,
            2,
            23
          ],
          [
            4,
            6,
            1
          ],
          [
            4,
            7,
            21
          ],
          [
            5,
            0,
            19
          ],
          [
            5,
            1,
            5
          ],
          [
            5,
            3,
            31
          ],
          [
            5,
            4,
            32
          ],
          [
            5,
            6,
            17
          ],
          [
            5,
            7,
            24
          ],
          [
            5,
            8,
            32
          ],
          [
            6,
            2,
            34
          ],
          [
            6,
            3,
            29
          ],
          [
            6,
            7,
            24
          ],
          [
            7,
            1,
            30
          ],
          [
            7,
            2,
            4
          ],
          [
            7,
            3,
            6
          ],
          [
            7,
            8,
            7
          ],
          [
            8,
            0,
            9
          ],
          [
            8,
            2,
            26
          ],
          [
            8,
            3,
            20
          ],
          [
            8,
            5,
            27
          ],
          [
            8,
            6,
            35
          ],
          [
            0,
            1,
            7
          ],
          [
            2,
            3,
            9
          ],
          [
            4,
            5,
            10
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
            4
          ],
          [
            0,
            3,
            18
          ],
          [
            0,
            4,
            24
          ],
          [
            0,
            6,
            33
          ],
          [
            0,
            7,
            0
          ],
          [
            1,
            3,
            16
          ],
          [
            1,
            4,
            11
          ],
          [
            1,
            5,
            5
          ],
          [
            1,
            6,
            14
          ],
          [
            1,
            7,
            23
          ],
          [
            2,
            1,
            29
          ],
          [
            3,
            0,
            24
          ],
          [
            3,
            1,
            1
          ],
          [
            5,
            0,
            15
          ],
          [
            5,
            1,
            1
          ],
          [
            5,
            4,
            24
          ],
          [
            7,
            0,
            33
          ],
          [
            7,
            2,
            8
          ]
        ],
        "source": 0,
        "target": 9
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            2,
            -13
          ],
          [
            0,
            9,
            11
          ],
          [
            1,
            4,
            -21
          ],
          [
            1,
            6,
            -15
          ],
          [
            2,
            0,
            37
          ],
          [
            2,
            1,
            43
          ],
          [
            2,
            7,
            44
          ],
          [
            2,
            8,
            27
          ],
          [
            4,
            0,
            60
          ],
          [
            4,
            7,
            54
          ],
          [
            4,
            8,
            63
          ],
          [
            5,
            0,
            63
          ],
          [
            5,
            3,
            59
          ],
          [
            5,
            7,
            50
          ],
          [
            5,
            8,
            61
          ],
          [
            5,
            9,
            61
          ],
          [
            6,
            1,
            51
          ],
          [
            6,
            4,
            -17
          ],
          [
            6,
            7,
            36
          ],
          [
            7,
            4,
            -46
          ],
          [
            7,
            5,
            -44
          ],
          [
            8,
            0,
            17
          ],
          [
            8,
            1,
            40
          ],
          [
            8,
            2,
            13
          ],
          [
            8,
            3,
            15
          ],
          [
            8,
            5,
            -28
          ],
          [
            9,
            1,
            44
          ],
          [
            9,
            2,
            4
          ],
          [
            9,
            5,
            -5
          ],
          [
            9,
            6,
            24
          ],
          [
            0,
            1,
            17
          ],
          [
            1,
            2,
            -19
          ],
          [
            2,
            3,
            6
          ],
          [
            3,
            4,
            -17
          ],
          [
            4,
            5,
            5
          ],
          [
            5,
            6,
            35
          ],
          [
            7,
            8,
            -5
          ],
          [
            8,
            9,
            -10
          ],
          [
            9,
            0,
            39
          ]
        ],
        "source": 0,
        "target": 9
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-013",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-003",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-010",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-023",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-012",
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
  "id": "holdout-route-012",
  "family": "route",
  "description": "Grafo cíclico aleatorio con potenciales: posibles arcos negativos sin ciclos negativos",
  "input": {
    "nodes": 10,
    "edges": [
      [
        0,
        2,
        -13
      ],
      [
        0,
        9,
        11
      ],
      [
        1,
        4,
        -21
      ],
      [
        1,
        6,
        -15
      ],
      [
        2,
        0,
        37
      ],
      [
        2,
        1,
        43
      ],
      [
        2,
        7,
        44
      ],
      [
        2,
        8,
        27
      ],
      [
        4,
        0,
        60
      ],
      [
        4,
        7,
        54
      ],
      [
        4,
        8,
        63
      ],
      [
        5,
        0,
        63
      ],
      [
        5,
        3,
        59
      ],
      [
        5,
        7,
        50
      ],
      [
        5,
        8,
        61
      ],
      [
        5,
        9,
        61
      ],
      [
        6,
        1,
        51
      ],
      [
        6,
        4,
        -17
      ],
      [
        6,
        7,
        36
      ],
      [
        7,
        4,
        -46
      ],
      [
        7,
        5,
        -44
      ],
      [
        8,
        0,
        17
      ],
      [
        8,
        1,
        40
      ],
      [
        8,
        2,
        13
      ],
      [
        8,
        3,
        15
      ],
      [
        8,
        5,
        -28
      ],
      [
        9,
        1,
        44
      ],
      [
        9,
        2,
        4
      ],
      [
        9,
        5,
        -5
      ],
      [
        9,
        6,
        24
      ],
      [
        0,
        1,
        17
      ],
      [
        1,
        2,
        -19
      ],
      [
        2,
        3,
        6
      ],
      [
        3,
        4,
        -17
      ],
      [
        4,
        5,
        5
      ],
      [
        5,
        6,
        35
      ],
      [
        7,
        8,
        -5
      ],
      [
        8,
        9,
        -10
      ],
      [
        9,
        0,
        39
      ]
    ],
    "source": 0,
    "target": 9
  }
}
```

### operations

```json
4901
```

### elapsedNanos

```json
608478
```

### adapted

```json
false
```

### reused

```json
true
```
