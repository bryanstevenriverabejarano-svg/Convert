# holdout-route-032

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-032

### identifier

```json
"holdout-route-032"
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
  "fullJournalSha256": "0c3a59994d7ebbcfd0f396906a017d414884cc57180e8a6e02e79ca3c16a29d3",
  "familyState": {
    "schema": 1,
    "revision": 448,
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
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            6,
            21
          ],
          [
            0,
            10,
            21
          ],
          [
            0,
            11,
            30
          ],
          [
            1,
            0,
            11
          ],
          [
            1,
            3,
            21
          ],
          [
            1,
            4,
            7
          ],
          [
            1,
            6,
            20
          ],
          [
            1,
            9,
            24
          ],
          [
            1,
            10,
            7
          ],
          [
            2,
            0,
            34
          ],
          [
            2,
            3,
            15
          ],
          [
            2,
            4,
            13
          ],
          [
            2,
            7,
            10
          ],
          [
            2,
            10,
            32
          ],
          [
            2,
            12,
            12
          ],
          [
            3,
            1,
            2
          ],
          [
            3,
            2,
            5
          ],
          [
            3,
            4,
            5
          ],
          [
            3,
            5,
            28
          ],
          [
            3,
            6,
            0
          ],
          [
            3,
            10,
            2
          ],
          [
            3,
            11,
            35
          ],
          [
            4,
            3,
            12
          ],
          [
            4,
            5,
            8
          ],
          [
            4,
            9,
            6
          ],
          [
            4,
            10,
            3
          ],
          [
            5,
            0,
            18
          ],
          [
            5,
            2,
            12
          ],
          [
            5,
            3,
            22
          ],
          [
            5,
            4,
            17
          ],
          [
            5,
            8,
            9
          ],
          [
            5,
            9,
            27
          ],
          [
            5,
            12,
            7
          ],
          [
            6,
            1,
            17
          ],
          [
            6,
            2,
            18
          ],
          [
            6,
            3,
            4
          ],
          [
            6,
            4,
            19
          ],
          [
            6,
            9,
            24
          ],
          [
            6,
            12,
            7
          ],
          [
            7,
            1,
            10
          ],
          [
            7,
            2,
            15
          ],
          [
            7,
            4,
            29
          ],
          [
            7,
            6,
            3
          ],
          [
            7,
            11,
            33
          ],
          [
            8,
            2,
            20
          ],
          [
            8,
            3,
            28
          ],
          [
            8,
            4,
            16
          ],
          [
            8,
            11,
            9
          ],
          [
            9,
            0,
            12
          ],
          [
            9,
            2,
            35
          ],
          [
            9,
            5,
            8
          ],
          [
            9,
            8,
            9
          ],
          [
            9,
            10,
            13
          ],
          [
            9,
            11,
            22
          ],
          [
            10,
            2,
            31
          ],
          [
            10,
            5,
            31
          ],
          [
            10,
            11,
            22
          ],
          [
            10,
            12,
            0
          ],
          [
            11,
            2,
            2
          ],
          [
            11,
            7,
            6
          ],
          [
            11,
            8,
            3
          ],
          [
            11,
            10,
            27
          ],
          [
            11,
            12,
            1
          ],
          [
            12,
            1,
            4
          ],
          [
            12,
            2,
            32
          ],
          [
            12,
            4,
            33
          ],
          [
            12,
            7,
            17
          ],
          [
            0,
            1,
            14
          ],
          [
            1,
            2,
            8
          ],
          [
            5,
            6,
            7
          ],
          [
            6,
            7,
            10
          ],
          [
            7,
            8,
            3
          ],
          [
            8,
            9,
            25
          ],
          [
            12,
            0,
            17
          ]
        ],
        "source": 0,
        "target": 12
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
      },
      {
        "id": "holdout-route-026",
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
      "cost": 84,
      "path": [
        0,
        1,
        2,
        3,
        4
      ]
    },
    "operations": 6203,
    "elapsedNanos": 336867
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 84,
  "path": [
    0,
    1,
    2,
    3,
    4
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
    "id": "holdout-route-032",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 84,
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
  "fullJournalSha256": "f308b765540a790c508b37b3622067b5e9c4397f8899443bed53df02bc92361e",
  "familyState": {
    "schema": 1,
    "revision": 449,
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
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            6,
            21
          ],
          [
            0,
            10,
            21
          ],
          [
            0,
            11,
            30
          ],
          [
            1,
            0,
            11
          ],
          [
            1,
            3,
            21
          ],
          [
            1,
            4,
            7
          ],
          [
            1,
            6,
            20
          ],
          [
            1,
            9,
            24
          ],
          [
            1,
            10,
            7
          ],
          [
            2,
            0,
            34
          ],
          [
            2,
            3,
            15
          ],
          [
            2,
            4,
            13
          ],
          [
            2,
            7,
            10
          ],
          [
            2,
            10,
            32
          ],
          [
            2,
            12,
            12
          ],
          [
            3,
            1,
            2
          ],
          [
            3,
            2,
            5
          ],
          [
            3,
            4,
            5
          ],
          [
            3,
            5,
            28
          ],
          [
            3,
            6,
            0
          ],
          [
            3,
            10,
            2
          ],
          [
            3,
            11,
            35
          ],
          [
            4,
            3,
            12
          ],
          [
            4,
            5,
            8
          ],
          [
            4,
            9,
            6
          ],
          [
            4,
            10,
            3
          ],
          [
            5,
            0,
            18
          ],
          [
            5,
            2,
            12
          ],
          [
            5,
            3,
            22
          ],
          [
            5,
            4,
            17
          ],
          [
            5,
            8,
            9
          ],
          [
            5,
            9,
            27
          ],
          [
            5,
            12,
            7
          ],
          [
            6,
            1,
            17
          ],
          [
            6,
            2,
            18
          ],
          [
            6,
            3,
            4
          ],
          [
            6,
            4,
            19
          ],
          [
            6,
            9,
            24
          ],
          [
            6,
            12,
            7
          ],
          [
            7,
            1,
            10
          ],
          [
            7,
            2,
            15
          ],
          [
            7,
            4,
            29
          ],
          [
            7,
            6,
            3
          ],
          [
            7,
            11,
            33
          ],
          [
            8,
            2,
            20
          ],
          [
            8,
            3,
            28
          ],
          [
            8,
            4,
            16
          ],
          [
            8,
            11,
            9
          ],
          [
            9,
            0,
            12
          ],
          [
            9,
            2,
            35
          ],
          [
            9,
            5,
            8
          ],
          [
            9,
            8,
            9
          ],
          [
            9,
            10,
            13
          ],
          [
            9,
            11,
            22
          ],
          [
            10,
            2,
            31
          ],
          [
            10,
            5,
            31
          ],
          [
            10,
            11,
            22
          ],
          [
            10,
            12,
            0
          ],
          [
            11,
            2,
            2
          ],
          [
            11,
            7,
            6
          ],
          [
            11,
            8,
            3
          ],
          [
            11,
            10,
            27
          ],
          [
            11,
            12,
            1
          ],
          [
            12,
            1,
            4
          ],
          [
            12,
            2,
            32
          ],
          [
            12,
            4,
            33
          ],
          [
            12,
            7,
            17
          ],
          [
            0,
            1,
            14
          ],
          [
            1,
            2,
            8
          ],
          [
            5,
            6,
            7
          ],
          [
            6,
            7,
            10
          ],
          [
            7,
            8,
            3
          ],
          [
            8,
            9,
            25
          ],
          [
            12,
            0,
            17
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 5,
        "edges": [
          [
            1,
            0,
            10
          ],
          [
            2,
            0,
            16
          ],
          [
            2,
            3,
            3
          ],
          [
            3,
            0,
            36
          ],
          [
            3,
            1,
            22
          ],
          [
            3,
            4,
            69
          ],
          [
            4,
            0,
            -27
          ],
          [
            4,
            3,
            -35
          ],
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            11
          ]
        ],
        "source": 0,
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
      },
      {
        "id": "holdout-route-026",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-032",
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
  "id": "holdout-route-032",
  "family": "route",
  "description": "Grafo cíclico aleatorio con potenciales: posibles arcos negativos sin ciclos negativos",
  "input": {
    "nodes": 5,
    "edges": [
      [
        1,
        0,
        10
      ],
      [
        2,
        0,
        16
      ],
      [
        2,
        3,
        3
      ],
      [
        3,
        0,
        36
      ],
      [
        3,
        1,
        22
      ],
      [
        3,
        4,
        69
      ],
      [
        4,
        0,
        -27
      ],
      [
        4,
        3,
        -35
      ],
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        11
      ]
    ],
    "source": 0,
    "target": 4
  }
}
```

### operations

```json
6203
```

### elapsedNanos

```json
574218
```

### adapted

```json
false
```

### reused

```json
true
```
