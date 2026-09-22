# holdout-route-014

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-014

### identifier

```json
"holdout-route-014"
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
  "fullJournalSha256": "eb3036f83837484d09641c269034e5f51893e3d5d8e0314eddcfea8548a58bd9",
  "familyState": {
    "schema": 1,
    "revision": 470,
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
            -24
          ],
          [
            0,
            2,
            -30
          ],
          [
            0,
            6,
            -41
          ],
          [
            0,
            9,
            23
          ],
          [
            0,
            12,
            -40
          ],
          [
            0,
            13,
            16
          ],
          [
            1,
            0,
            46
          ],
          [
            1,
            2,
            0
          ],
          [
            1,
            3,
            7
          ],
          [
            1,
            4,
            53
          ],
          [
            1,
            5,
            49
          ],
          [
            1,
            6,
            6
          ],
          [
            1,
            9,
            70
          ],
          [
            1,
            11,
            68
          ],
          [
            1,
            12,
            22
          ],
          [
            2,
            1,
            17
          ],
          [
            2,
            3,
            15
          ],
          [
            2,
            4,
            58
          ],
          [
            2,
            6,
            9
          ],
          [
            2,
            7,
            81
          ],
          [
            2,
            9,
            59
          ],
          [
            2,
            10,
            -6
          ],
          [
            2,
            12,
            24
          ],
          [
            2,
            13,
            54
          ],
          [
            3,
            0,
            87
          ],
          [
            3,
            1,
            36
          ],
          [
            3,
            2,
            14
          ],
          [
            3,
            5,
            59
          ],
          [
            3,
            6,
            29
          ],
          [
            3,
            7,
            55
          ],
          [
            3,
            8,
            71
          ],
          [
            3,
            9,
            66
          ],
          [
            3,
            10,
            2
          ],
          [
            3,
            11,
            64
          ],
          [
            3,
            12,
            33
          ],
          [
            4,
            0,
            26
          ],
          [
            4,
            1,
            -11
          ],
          [
            4,
            5,
            19
          ],
          [
            4,
            8,
            10
          ],
          [
            4,
            9,
            32
          ],
          [
            4,
            10,
            -20
          ],
          [
            4,
            11,
            14
          ],
          [
            4,
            12,
            -15
          ],
          [
            5,
            0,
            24
          ],
          [
            5,
            1,
            -35
          ],
          [
            5,
            3,
            -28
          ],
          [
            5,
            4,
            20
          ],
          [
            5,
            8,
            2
          ],
          [
            5,
            9,
            28
          ],
          [
            6,
            1,
            37
          ],
          [
            6,
            2,
            31
          ],
          [
            6,
            3,
            27
          ],
          [
            6,
            5,
            77
          ],
          [
            6,
            10,
            0
          ],
          [
            6,
            11,
            51
          ],
          [
            6,
            12,
            13
          ],
          [
            7,
            0,
            31
          ],
          [
            7,
            3,
            -40
          ],
          [
            7,
            6,
            -42
          ],
          [
            7,
            8,
            2
          ],
          [
            8,
            0,
            16
          ],
          [
            8,
            1,
            6
          ],
          [
            8,
            3,
            -23
          ],
          [
            8,
            9,
            22
          ],
          [
            8,
            11,
            35
          ],
          [
            8,
            12,
            -31
          ],
          [
            9,
            0,
            22
          ],
          [
            9,
            1,
            -8
          ],
          [
            9,
            3,
            -31
          ],
          [
            9,
            4,
            2
          ],
          [
            9,
            6,
            -19
          ],
          [
            9,
            8,
            5
          ],
          [
            9,
            10,
            -25
          ],
          [
            9,
            11,
            -2
          ],
          [
            9,
            13,
            6
          ],
          [
            10,
            0,
            84
          ],
          [
            10,
            1,
            32
          ],
          [
            10,
            2,
            12
          ],
          [
            10,
            3,
            26
          ],
          [
            10,
            8,
            69
          ],
          [
            10,
            9,
            76
          ],
          [
            10,
            11,
            62
          ],
          [
            10,
            12,
            23
          ],
          [
            11,
            1,
            0
          ],
          [
            11,
            2,
            -33
          ],
          [
            11,
            3,
            -17
          ],
          [
            11,
            5,
            31
          ],
          [
            11,
            6,
            -42
          ],
          [
            11,
            7,
            6
          ],
          [
            11,
            8,
            18
          ],
          [
            11,
            9,
            12
          ],
          [
            11,
            10,
            -15
          ],
          [
            11,
            12,
            -39
          ],
          [
            12,
            0,
            84
          ],
          [
            12,
            3,
            27
          ],
          [
            12,
            4,
            47
          ],
          [
            12,
            6,
            13
          ],
          [
            12,
            7,
            55
          ],
          [
            12,
            10,
            32
          ],
          [
            12,
            13,
            44
          ],
          [
            13,
            0,
            33
          ],
          [
            13,
            2,
            -6
          ],
          [
            13,
            3,
            -16
          ],
          [
            13,
            4,
            10
          ],
          [
            13,
            10,
            -21
          ],
          [
            3,
            4,
            54
          ],
          [
            5,
            6,
            -46
          ],
          [
            6,
            7,
            54
          ]
        ],
        "source": 0,
        "target": 13
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            2,
            -8
          ],
          [
            2,
            4,
            -1
          ],
          [
            2,
            3,
            20
          ],
          [
            4,
            1,
            13
          ],
          [
            1,
            3,
            12
          ]
        ],
        "source": 0,
        "target": 3
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            7,
            29
          ],
          [
            0,
            10,
            9
          ],
          [
            1,
            0,
            17
          ],
          [
            1,
            2,
            11
          ],
          [
            1,
            6,
            12
          ],
          [
            1,
            7,
            24
          ],
          [
            1,
            9,
            17
          ],
          [
            1,
            10,
            1
          ],
          [
            2,
            0,
            32
          ],
          [
            2,
            1,
            9
          ],
          [
            2,
            6,
            35
          ],
          [
            2,
            7,
            1
          ],
          [
            3,
            1,
            12
          ],
          [
            3,
            4,
            16
          ],
          [
            4,
            0,
            27
          ],
          [
            4,
            2,
            30
          ],
          [
            4,
            5,
            14
          ],
          [
            4,
            8,
            14
          ],
          [
            4,
            10,
            0
          ],
          [
            5,
            1,
            13
          ],
          [
            5,
            4,
            35
          ],
          [
            5,
            6,
            15
          ],
          [
            5,
            7,
            25
          ],
          [
            6,
            2,
            33
          ],
          [
            7,
            1,
            32
          ],
          [
            7,
            3,
            5
          ],
          [
            7,
            5,
            13
          ],
          [
            8,
            1,
            7
          ],
          [
            8,
            3,
            21
          ],
          [
            8,
            6,
            22
          ],
          [
            9,
            2,
            8
          ],
          [
            9,
            3,
            3
          ],
          [
            9,
            4,
            11
          ],
          [
            9,
            7,
            8
          ],
          [
            10,
            3,
            25
          ],
          [
            10,
            5,
            34
          ],
          [
            10,
            9,
            23
          ],
          [
            0,
            1,
            6
          ],
          [
            2,
            3,
            8
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            8
          ],
          [
            8,
            9,
            4
          ],
          [
            9,
            10,
            6
          ],
          [
            10,
            0,
            16
          ]
        ],
        "source": 0,
        "target": 10
      }
    ],
    "receipts": [
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
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 34,
      "path": [
        0,
        1,
        3,
        7,
        8
      ]
    },
    "operations": 7584,
    "elapsedNanos": 446689
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 34,
  "path": [
    0,
    1,
    3,
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
    "id": "holdout-route-014",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 34,
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
  "fullJournalSha256": "3132f6832c35a95d26c8532ff9957ac1ec0cd55916c880a7901b881802cb637b",
  "familyState": {
    "schema": 1,
    "revision": 471,
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
            2,
            -8
          ],
          [
            2,
            4,
            -1
          ],
          [
            2,
            3,
            20
          ],
          [
            4,
            1,
            13
          ],
          [
            1,
            3,
            12
          ]
        ],
        "source": 0,
        "target": 3
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            7,
            29
          ],
          [
            0,
            10,
            9
          ],
          [
            1,
            0,
            17
          ],
          [
            1,
            2,
            11
          ],
          [
            1,
            6,
            12
          ],
          [
            1,
            7,
            24
          ],
          [
            1,
            9,
            17
          ],
          [
            1,
            10,
            1
          ],
          [
            2,
            0,
            32
          ],
          [
            2,
            1,
            9
          ],
          [
            2,
            6,
            35
          ],
          [
            2,
            7,
            1
          ],
          [
            3,
            1,
            12
          ],
          [
            3,
            4,
            16
          ],
          [
            4,
            0,
            27
          ],
          [
            4,
            2,
            30
          ],
          [
            4,
            5,
            14
          ],
          [
            4,
            8,
            14
          ],
          [
            4,
            10,
            0
          ],
          [
            5,
            1,
            13
          ],
          [
            5,
            4,
            35
          ],
          [
            5,
            6,
            15
          ],
          [
            5,
            7,
            25
          ],
          [
            6,
            2,
            33
          ],
          [
            7,
            1,
            32
          ],
          [
            7,
            3,
            5
          ],
          [
            7,
            5,
            13
          ],
          [
            8,
            1,
            7
          ],
          [
            8,
            3,
            21
          ],
          [
            8,
            6,
            22
          ],
          [
            9,
            2,
            8
          ],
          [
            9,
            3,
            3
          ],
          [
            9,
            4,
            11
          ],
          [
            9,
            7,
            8
          ],
          [
            10,
            3,
            25
          ],
          [
            10,
            5,
            34
          ],
          [
            10,
            9,
            23
          ],
          [
            0,
            1,
            6
          ],
          [
            2,
            3,
            8
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            8
          ],
          [
            8,
            9,
            4
          ],
          [
            9,
            10,
            6
          ],
          [
            10,
            0,
            16
          ]
        ],
        "source": 0,
        "target": 10
      },
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
      }
    ],
    "receipts": [
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
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-014",
  "family": "route",
  "description": "Grafo dirigido aleatorio con ciclos de coste no negativo",
  "input": {
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
  }
}
```

### operations

```json
7584
```

### elapsedNanos

```json
753502
```

### adapted

```json
false
```

### reused

```json
true
```
