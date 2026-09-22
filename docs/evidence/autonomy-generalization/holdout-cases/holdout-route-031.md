# holdout-route-031

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-031

### identifier

```json
"holdout-route-031"
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
  "fullJournalSha256": "fea3e448168bc3e33f5eefb3eb256f389a2f7928fa9096f0d060144210e75df4",
  "familyState": {
    "schema": 1,
    "revision": 493,
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
      },
      {
        "nodes": 14,
        "edges": [
          [
            13,
            7,
            -2
          ],
          [
            13,
            5,
            -1
          ],
          [
            13,
            12,
            3
          ],
          [
            13,
            1,
            -3
          ],
          [
            7,
            0,
            20
          ],
          [
            7,
            5,
            12
          ],
          [
            7,
            4,
            1
          ],
          [
            7,
            6,
            -2
          ],
          [
            0,
            10,
            14
          ],
          [
            0,
            6,
            -9
          ],
          [
            0,
            9,
            -9
          ],
          [
            0,
            1,
            21
          ],
          [
            10,
            5,
            -4
          ],
          [
            10,
            3,
            5
          ],
          [
            10,
            4,
            -8
          ],
          [
            10,
            9,
            3
          ],
          [
            10,
            12,
            13
          ],
          [
            10,
            1,
            -3
          ],
          [
            5,
            3,
            -9
          ],
          [
            5,
            9,
            -4
          ],
          [
            5,
            11,
            -2
          ],
          [
            5,
            1,
            20
          ],
          [
            3,
            4,
            6
          ],
          [
            3,
            2,
            9
          ],
          [
            3,
            11,
            -5
          ],
          [
            3,
            1,
            19
          ],
          [
            4,
            6,
            18
          ],
          [
            4,
            8,
            -3
          ],
          [
            4,
            9,
            22
          ],
          [
            6,
            2,
            15
          ],
          [
            6,
            9,
            13
          ],
          [
            6,
            12,
            10
          ],
          [
            2,
            8,
            11
          ],
          [
            2,
            1,
            -2
          ],
          [
            8,
            9,
            -4
          ],
          [
            8,
            12,
            5
          ],
          [
            9,
            11,
            7
          ],
          [
            11,
            12,
            1
          ],
          [
            12,
            1,
            1
          ]
        ],
        "source": 13,
        "target": 1
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            4,
            19
          ],
          [
            1,
            2,
            8
          ],
          [
            1,
            3,
            24
          ],
          [
            1,
            4,
            1
          ],
          [
            1,
            5,
            7
          ],
          [
            1,
            6,
            1
          ],
          [
            2,
            0,
            21
          ],
          [
            2,
            3,
            18
          ],
          [
            3,
            0,
            8
          ],
          [
            3,
            2,
            20
          ],
          [
            3,
            4,
            7
          ],
          [
            3,
            6,
            1
          ],
          [
            5,
            0,
            14
          ],
          [
            5,
            4,
            26
          ],
          [
            6,
            4,
            7
          ],
          [
            0,
            1,
            24
          ],
          [
            4,
            5,
            12
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            0,
            23
          ]
        ],
        "source": 0,
        "target": 6
      }
    ],
    "receipts": [
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
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":6,\"candidatos_descartados_en_recibos\":0}",
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
    "operations": 8436,
    "elapsedNanos": 276829
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
    "id": "holdout-route-031",
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
  "fullJournalSha256": "5dfb99552ab06d2fc3a294023f83d4ded79f3e6884fa4de554d1ec2eb98a0a97",
  "familyState": {
    "schema": 1,
    "revision": 494,
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
            13,
            7,
            -2
          ],
          [
            13,
            5,
            -1
          ],
          [
            13,
            12,
            3
          ],
          [
            13,
            1,
            -3
          ],
          [
            7,
            0,
            20
          ],
          [
            7,
            5,
            12
          ],
          [
            7,
            4,
            1
          ],
          [
            7,
            6,
            -2
          ],
          [
            0,
            10,
            14
          ],
          [
            0,
            6,
            -9
          ],
          [
            0,
            9,
            -9
          ],
          [
            0,
            1,
            21
          ],
          [
            10,
            5,
            -4
          ],
          [
            10,
            3,
            5
          ],
          [
            10,
            4,
            -8
          ],
          [
            10,
            9,
            3
          ],
          [
            10,
            12,
            13
          ],
          [
            10,
            1,
            -3
          ],
          [
            5,
            3,
            -9
          ],
          [
            5,
            9,
            -4
          ],
          [
            5,
            11,
            -2
          ],
          [
            5,
            1,
            20
          ],
          [
            3,
            4,
            6
          ],
          [
            3,
            2,
            9
          ],
          [
            3,
            11,
            -5
          ],
          [
            3,
            1,
            19
          ],
          [
            4,
            6,
            18
          ],
          [
            4,
            8,
            -3
          ],
          [
            4,
            9,
            22
          ],
          [
            6,
            2,
            15
          ],
          [
            6,
            9,
            13
          ],
          [
            6,
            12,
            10
          ],
          [
            2,
            8,
            11
          ],
          [
            2,
            1,
            -2
          ],
          [
            8,
            9,
            -4
          ],
          [
            8,
            12,
            5
          ],
          [
            9,
            11,
            7
          ],
          [
            11,
            12,
            1
          ],
          [
            12,
            1,
            1
          ]
        ],
        "source": 13,
        "target": 1
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            4,
            19
          ],
          [
            1,
            2,
            8
          ],
          [
            1,
            3,
            24
          ],
          [
            1,
            4,
            1
          ],
          [
            1,
            5,
            7
          ],
          [
            1,
            6,
            1
          ],
          [
            2,
            0,
            21
          ],
          [
            2,
            3,
            18
          ],
          [
            3,
            0,
            8
          ],
          [
            3,
            2,
            20
          ],
          [
            3,
            4,
            7
          ],
          [
            3,
            6,
            1
          ],
          [
            5,
            0,
            14
          ],
          [
            5,
            4,
            26
          ],
          [
            6,
            4,
            7
          ],
          [
            0,
            1,
            24
          ],
          [
            4,
            5,
            12
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            0,
            23
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 11,
        "edges": [
          [
            2,
            3,
            28
          ],
          [
            2,
            5,
            13
          ],
          [
            2,
            7,
            25
          ],
          [
            2,
            8,
            29
          ],
          [
            2,
            9,
            20
          ],
          [
            3,
            4,
            11
          ],
          [
            3,
            7,
            16
          ],
          [
            3,
            9,
            20
          ],
          [
            4,
            2,
            28
          ],
          [
            4,
            7,
            11
          ],
          [
            4,
            9,
            21
          ],
          [
            5,
            3,
            9
          ],
          [
            5,
            4,
            13
          ],
          [
            6,
            4,
            3
          ],
          [
            7,
            3,
            34
          ],
          [
            8,
            4,
            10
          ],
          [
            8,
            6,
            31
          ],
          [
            8,
            7,
            33
          ],
          [
            9,
            3,
            7
          ],
          [
            9,
            6,
            8
          ],
          [
            9,
            7,
            12
          ],
          [
            10,
            2,
            34
          ],
          [
            10,
            3,
            24
          ],
          [
            10,
            6,
            19
          ],
          [
            10,
            7,
            17
          ],
          [
            10,
            8,
            9
          ]
        ],
        "source": 0,
        "target": 10
      }
    ],
    "receipts": [
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
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-031",
  "family": "route",
  "description": "Dos componentes aleatorias con destino inalcanzable",
  "input": {
    "nodes": 11,
    "edges": [
      [
        2,
        3,
        28
      ],
      [
        2,
        5,
        13
      ],
      [
        2,
        7,
        25
      ],
      [
        2,
        8,
        29
      ],
      [
        2,
        9,
        20
      ],
      [
        3,
        4,
        11
      ],
      [
        3,
        7,
        16
      ],
      [
        3,
        9,
        20
      ],
      [
        4,
        2,
        28
      ],
      [
        4,
        7,
        11
      ],
      [
        4,
        9,
        21
      ],
      [
        5,
        3,
        9
      ],
      [
        5,
        4,
        13
      ],
      [
        6,
        4,
        3
      ],
      [
        7,
        3,
        34
      ],
      [
        8,
        4,
        10
      ],
      [
        8,
        6,
        31
      ],
      [
        8,
        7,
        33
      ],
      [
        9,
        3,
        7
      ],
      [
        9,
        6,
        8
      ],
      [
        9,
        7,
        12
      ],
      [
        10,
        2,
        34
      ],
      [
        10,
        3,
        24
      ],
      [
        10,
        6,
        19
      ],
      [
        10,
        7,
        17
      ],
      [
        10,
        8,
        9
      ]
    ],
    "source": 0,
    "target": 10
  }
}
```

### operations

```json
8436
```

### elapsedNanos

```json
515451
```

### adapted

```json
false
```

### reused

```json
true
```
