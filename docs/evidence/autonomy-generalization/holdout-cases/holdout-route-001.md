# holdout-route-001

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-001

### identifier

```json
"holdout-route-001"
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
  "fullJournalSha256": "507fc0fd042cc10ac96222cc2209003a2cbc393819bfd1293e55e43dd79b4373",
  "familyState": {
    "schema": 1,
    "revision": 478,
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
            1,
            3,
            15
          ],
          [
            1,
            4,
            27
          ],
          [
            1,
            7,
            29
          ],
          [
            2,
            3,
            6
          ],
          [
            2,
            8,
            16
          ],
          [
            3,
            0,
            35
          ],
          [
            3,
            7,
            10
          ],
          [
            4,
            1,
            8
          ],
          [
            4,
            2,
            17
          ],
          [
            4,
            6,
            4
          ],
          [
            4,
            7,
            30
          ],
          [
            5,
            0,
            18
          ],
          [
            5,
            8,
            31
          ],
          [
            7,
            1,
            25
          ],
          [
            7,
            5,
            35
          ],
          [
            7,
            6,
            24
          ],
          [
            8,
            1,
            33
          ],
          [
            8,
            3,
            30
          ],
          [
            8,
            5,
            16
          ],
          [
            0,
            1,
            8
          ],
          [
            1,
            2,
            13
          ],
          [
            3,
            4,
            16
          ],
          [
            4,
            5,
            10
          ],
          [
            5,
            6,
            18
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
          ],
          [
            8,
            0,
            19
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
            11
          ],
          [
            0,
            2,
            30
          ],
          [
            0,
            4,
            33
          ],
          [
            0,
            5,
            11
          ],
          [
            1,
            2,
            12
          ],
          [
            1,
            3,
            23
          ],
          [
            1,
            4,
            16
          ],
          [
            1,
            5,
            9
          ],
          [
            2,
            0,
            18
          ],
          [
            2,
            3,
            7
          ],
          [
            2,
            5,
            29
          ],
          [
            3,
            0,
            31
          ],
          [
            3,
            1,
            21
          ],
          [
            3,
            2,
            5
          ],
          [
            3,
            5,
            34
          ],
          [
            4,
            1,
            28
          ],
          [
            4,
            2,
            29
          ],
          [
            4,
            3,
            4
          ],
          [
            5,
            1,
            23
          ],
          [
            5,
            2,
            19
          ],
          [
            5,
            4,
            21
          ],
          [
            3,
            4,
            22
          ],
          [
            4,
            5,
            1
          ],
          [
            5,
            0,
            2
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 12,
        "edges": [
          [
            4,
            7,
            -2
          ],
          [
            4,
            6,
            -4
          ],
          [
            7,
            2,
            16
          ],
          [
            7,
            0,
            -5
          ],
          [
            7,
            6,
            16
          ],
          [
            2,
            8,
            -1
          ],
          [
            2,
            3,
            4
          ],
          [
            8,
            5,
            12
          ],
          [
            8,
            0,
            5
          ],
          [
            5,
            9,
            -9
          ],
          [
            5,
            11,
            16
          ],
          [
            5,
            6,
            24
          ],
          [
            9,
            1,
            12
          ],
          [
            9,
            3,
            23
          ],
          [
            9,
            10,
            -3
          ],
          [
            1,
            3,
            0
          ],
          [
            1,
            11,
            14
          ],
          [
            3,
            0,
            -9
          ],
          [
            3,
            10,
            15
          ],
          [
            0,
            11,
            -8
          ],
          [
            11,
            10,
            -8
          ],
          [
            10,
            6,
            15
          ]
        ],
        "source": 4,
        "target": 6
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-022",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-024",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-025",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-006",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-014",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-002",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-009",
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
      "cost": -10,
      "path": [
        0,
        5,
        3
      ]
    },
    "operations": 6181,
    "elapsedNanos": 243860
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": -10,
  "path": [
    0,
    5,
    3
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
    "id": "holdout-route-001",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -10,
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
  "fullJournalSha256": "79a6417f866445819d20f89565a3f5eded0da8ab9ac6e8ef06ddf3e1d8138265",
  "familyState": {
    "schema": 1,
    "revision": 479,
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
            11
          ],
          [
            0,
            2,
            30
          ],
          [
            0,
            4,
            33
          ],
          [
            0,
            5,
            11
          ],
          [
            1,
            2,
            12
          ],
          [
            1,
            3,
            23
          ],
          [
            1,
            4,
            16
          ],
          [
            1,
            5,
            9
          ],
          [
            2,
            0,
            18
          ],
          [
            2,
            3,
            7
          ],
          [
            2,
            5,
            29
          ],
          [
            3,
            0,
            31
          ],
          [
            3,
            1,
            21
          ],
          [
            3,
            2,
            5
          ],
          [
            3,
            5,
            34
          ],
          [
            4,
            1,
            28
          ],
          [
            4,
            2,
            29
          ],
          [
            4,
            3,
            4
          ],
          [
            5,
            1,
            23
          ],
          [
            5,
            2,
            19
          ],
          [
            5,
            4,
            21
          ],
          [
            3,
            4,
            22
          ],
          [
            4,
            5,
            1
          ],
          [
            5,
            0,
            2
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 12,
        "edges": [
          [
            4,
            7,
            -2
          ],
          [
            4,
            6,
            -4
          ],
          [
            7,
            2,
            16
          ],
          [
            7,
            0,
            -5
          ],
          [
            7,
            6,
            16
          ],
          [
            2,
            8,
            -1
          ],
          [
            2,
            3,
            4
          ],
          [
            8,
            5,
            12
          ],
          [
            8,
            0,
            5
          ],
          [
            5,
            9,
            -9
          ],
          [
            5,
            11,
            16
          ],
          [
            5,
            6,
            24
          ],
          [
            9,
            1,
            12
          ],
          [
            9,
            3,
            23
          ],
          [
            9,
            10,
            -3
          ],
          [
            1,
            3,
            0
          ],
          [
            1,
            11,
            14
          ],
          [
            3,
            0,
            -9
          ],
          [
            3,
            10,
            15
          ],
          [
            0,
            11,
            -8
          ],
          [
            11,
            10,
            -8
          ],
          [
            10,
            6,
            15
          ]
        ],
        "source": 4,
        "target": 6
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            5,
            -2
          ],
          [
            0,
            8,
            10
          ],
          [
            0,
            2,
            -8
          ],
          [
            0,
            6,
            17
          ],
          [
            5,
            4,
            7
          ],
          [
            5,
            3,
            -8
          ],
          [
            4,
            8,
            10
          ],
          [
            4,
            1,
            -2
          ],
          [
            4,
            6,
            22
          ],
          [
            4,
            3,
            9
          ],
          [
            8,
            1,
            21
          ],
          [
            8,
            7,
            20
          ],
          [
            8,
            2,
            -9
          ],
          [
            8,
            3,
            15
          ],
          [
            1,
            7,
            -5
          ],
          [
            1,
            2,
            21
          ],
          [
            7,
            2,
            19
          ],
          [
            2,
            6,
            13
          ],
          [
            6,
            3,
            6
          ]
        ],
        "source": 0,
        "target": 3
      }
    ],
    "receipts": [
      {
        "id": "holdout-route-022",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-024",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-025",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-006",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-014",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-002",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-009",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-001",
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
  "id": "holdout-route-001",
  "family": "route",
  "description": "DAG aleatorio con etiquetas permutadas y costes negativos",
  "input": {
    "nodes": 9,
    "edges": [
      [
        0,
        5,
        -2
      ],
      [
        0,
        8,
        10
      ],
      [
        0,
        2,
        -8
      ],
      [
        0,
        6,
        17
      ],
      [
        5,
        4,
        7
      ],
      [
        5,
        3,
        -8
      ],
      [
        4,
        8,
        10
      ],
      [
        4,
        1,
        -2
      ],
      [
        4,
        6,
        22
      ],
      [
        4,
        3,
        9
      ],
      [
        8,
        1,
        21
      ],
      [
        8,
        7,
        20
      ],
      [
        8,
        2,
        -9
      ],
      [
        8,
        3,
        15
      ],
      [
        1,
        7,
        -5
      ],
      [
        1,
        2,
        21
      ],
      [
        7,
        2,
        19
      ],
      [
        2,
        6,
        13
      ],
      [
        6,
        3,
        6
      ]
    ],
    "source": 0,
    "target": 3
  }
}
```

### operations

```json
6181
```

### elapsedNanos

```json
511225
```

### adapted

```json
false
```

### reused

```json
true
```
