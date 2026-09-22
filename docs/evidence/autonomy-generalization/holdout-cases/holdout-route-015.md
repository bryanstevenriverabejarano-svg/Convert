# holdout-route-015

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-015

### identifier

```json
"holdout-route-015"
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
  "fullJournalSha256": "f1f42ec80f8587d2355701467607883aff060e5f9ac35fa9d5a5f882140dd843",
  "familyState": {
    "schema": 1,
    "revision": 453,
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
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            6,
            80
          ],
          [
            1,
            5,
            -5
          ],
          [
            2,
            0,
            -33
          ],
          [
            2,
            3,
            -40
          ],
          [
            2,
            7,
            -26
          ],
          [
            2,
            9,
            -37
          ],
          [
            3,
            12,
            30
          ],
          [
            4,
            1,
            35
          ],
          [
            4,
            5,
            10
          ],
          [
            4,
            11,
            34
          ],
          [
            5,
            8,
            16
          ],
          [
            6,
            0,
            -18
          ],
          [
            6,
            2,
            33
          ],
          [
            7,
            4,
            28
          ],
          [
            7,
            6,
            28
          ],
          [
            8,
            12,
            42
          ],
          [
            9,
            0,
            23
          ],
          [
            9,
            1,
            58
          ],
          [
            9,
            11,
            65
          ],
          [
            9,
            12,
            37
          ],
          [
            10,
            0,
            -42
          ],
          [
            10,
            1,
            17
          ],
          [
            10,
            2,
            18
          ],
          [
            10,
            7,
            -9
          ],
          [
            10,
            12,
            5
          ],
          [
            11,
            3,
            -2
          ],
          [
            12,
            2,
            30
          ],
          [
            12,
            7,
            3
          ],
          [
            0,
            1,
            64
          ],
          [
            1,
            2,
            25
          ],
          [
            3,
            4,
            39
          ],
          [
            5,
            6,
            37
          ],
          [
            6,
            7,
            4
          ],
          [
            7,
            8,
            -14
          ],
          [
            8,
            9,
            23
          ],
          [
            9,
            10,
            64
          ],
          [
            10,
            11,
            11
          ],
          [
            11,
            12,
            2
          ],
          [
            12,
            0,
            -20
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 5,
        "edges": [
          [
            3,
            2,
            45
          ],
          [
            4,
            0,
            -21
          ],
          [
            0,
            1,
            61
          ],
          [
            1,
            2,
            15
          ],
          [
            2,
            3,
            -9
          ],
          [
            3,
            4,
            13
          ]
        ],
        "source": 0,
        "target": 4
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-route-016",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-020",
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
      "status": "unreachable",
      "path": []
    },
    "operations": 6892,
    "elapsedNanos": 258852
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
    "id": "holdout-route-015",
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
  "fullJournalSha256": "ddd33c800e8aa64bb1531728a01924b838c4edae74a76a38a62a45042a7e281e",
  "familyState": {
    "schema": 1,
    "revision": 454,
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
            6,
            80
          ],
          [
            1,
            5,
            -5
          ],
          [
            2,
            0,
            -33
          ],
          [
            2,
            3,
            -40
          ],
          [
            2,
            7,
            -26
          ],
          [
            2,
            9,
            -37
          ],
          [
            3,
            12,
            30
          ],
          [
            4,
            1,
            35
          ],
          [
            4,
            5,
            10
          ],
          [
            4,
            11,
            34
          ],
          [
            5,
            8,
            16
          ],
          [
            6,
            0,
            -18
          ],
          [
            6,
            2,
            33
          ],
          [
            7,
            4,
            28
          ],
          [
            7,
            6,
            28
          ],
          [
            8,
            12,
            42
          ],
          [
            9,
            0,
            23
          ],
          [
            9,
            1,
            58
          ],
          [
            9,
            11,
            65
          ],
          [
            9,
            12,
            37
          ],
          [
            10,
            0,
            -42
          ],
          [
            10,
            1,
            17
          ],
          [
            10,
            2,
            18
          ],
          [
            10,
            7,
            -9
          ],
          [
            10,
            12,
            5
          ],
          [
            11,
            3,
            -2
          ],
          [
            12,
            2,
            30
          ],
          [
            12,
            7,
            3
          ],
          [
            0,
            1,
            64
          ],
          [
            1,
            2,
            25
          ],
          [
            3,
            4,
            39
          ],
          [
            5,
            6,
            37
          ],
          [
            6,
            7,
            4
          ],
          [
            7,
            8,
            -14
          ],
          [
            8,
            9,
            23
          ],
          [
            9,
            10,
            64
          ],
          [
            10,
            11,
            11
          ],
          [
            11,
            12,
            2
          ],
          [
            12,
            0,
            -20
          ]
        ],
        "source": 0,
        "target": 12
      },
      {
        "nodes": 5,
        "edges": [
          [
            3,
            2,
            45
          ],
          [
            4,
            0,
            -21
          ],
          [
            0,
            1,
            61
          ],
          [
            1,
            2,
            15
          ],
          [
            2,
            3,
            -9
          ],
          [
            3,
            4,
            13
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            1,
            18
          ],
          [
            0,
            3,
            14
          ],
          [
            0,
            5,
            14
          ],
          [
            0,
            6,
            17
          ],
          [
            0,
            8,
            10
          ],
          [
            1,
            2,
            24
          ],
          [
            1,
            8,
            26
          ],
          [
            2,
            0,
            11
          ],
          [
            2,
            3,
            29
          ],
          [
            2,
            7,
            4
          ],
          [
            3,
            0,
            28
          ],
          [
            3,
            4,
            15
          ],
          [
            3,
            5,
            3
          ],
          [
            3,
            6,
            15
          ],
          [
            4,
            5,
            29
          ],
          [
            5,
            2,
            10
          ],
          [
            5,
            8,
            35
          ],
          [
            6,
            5,
            30
          ],
          [
            7,
            0,
            26
          ],
          [
            7,
            1,
            22
          ],
          [
            7,
            3,
            13
          ],
          [
            7,
            8,
            35
          ],
          [
            8,
            0,
            27
          ],
          [
            8,
            2,
            18
          ],
          [
            8,
            3,
            15
          ],
          [
            8,
            4,
            6
          ],
          [
            9,
            10,
            15
          ],
          [
            10,
            9,
            4
          ],
          [
            11,
            9,
            30
          ],
          [
            11,
            12,
            9
          ],
          [
            12,
            11,
            4
          ]
        ],
        "source": 0,
        "target": 12
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-route-016",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-020",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-route-015",
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
  "id": "holdout-route-015",
  "family": "route",
  "description": "Dos componentes aleatorias con destino inalcanzable",
  "input": {
    "nodes": 13,
    "edges": [
      [
        0,
        1,
        18
      ],
      [
        0,
        3,
        14
      ],
      [
        0,
        5,
        14
      ],
      [
        0,
        6,
        17
      ],
      [
        0,
        8,
        10
      ],
      [
        1,
        2,
        24
      ],
      [
        1,
        8,
        26
      ],
      [
        2,
        0,
        11
      ],
      [
        2,
        3,
        29
      ],
      [
        2,
        7,
        4
      ],
      [
        3,
        0,
        28
      ],
      [
        3,
        4,
        15
      ],
      [
        3,
        5,
        3
      ],
      [
        3,
        6,
        15
      ],
      [
        4,
        5,
        29
      ],
      [
        5,
        2,
        10
      ],
      [
        5,
        8,
        35
      ],
      [
        6,
        5,
        30
      ],
      [
        7,
        0,
        26
      ],
      [
        7,
        1,
        22
      ],
      [
        7,
        3,
        13
      ],
      [
        7,
        8,
        35
      ],
      [
        8,
        0,
        27
      ],
      [
        8,
        2,
        18
      ],
      [
        8,
        3,
        15
      ],
      [
        8,
        4,
        6
      ],
      [
        9,
        10,
        15
      ],
      [
        10,
        9,
        4
      ],
      [
        11,
        9,
        30
      ],
      [
        11,
        12,
        9
      ],
      [
        12,
        11,
        4
      ]
    ],
    "source": 0,
    "target": 12
  }
}
```

### operations

```json
6892
```

### elapsedNanos

```json
488120
```

### adapted

```json
false
```

### reused

```json
true
```
