# holdout-route-023

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-023

### identifier

```json
"holdout-route-023"
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
  "fullJournalSha256": "8294e2a63c2ef27b12c15dccd6d61079294180fd53f5f4904aa6066bc08cb550",
  "familyState": {
    "schema": 1,
    "revision": 515,
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
            6,
            5,
            -3
          ],
          [
            6,
            2,
            24
          ],
          [
            6,
            3,
            -5
          ],
          [
            6,
            0,
            -4
          ],
          [
            6,
            1,
            15
          ],
          [
            5,
            9,
            7
          ],
          [
            5,
            2,
            11
          ],
          [
            5,
            4,
            6
          ],
          [
            5,
            7,
            5
          ],
          [
            9,
            2,
            16
          ],
          [
            2,
            3,
            19
          ],
          [
            2,
            0,
            -8
          ],
          [
            2,
            10,
            24
          ],
          [
            3,
            0,
            0
          ],
          [
            0,
            10,
            14
          ],
          [
            0,
            8,
            23
          ],
          [
            0,
            4,
            7
          ],
          [
            10,
            8,
            21
          ],
          [
            8,
            4,
            -8
          ],
          [
            4,
            7,
            9
          ],
          [
            7,
            1,
            7
          ]
        ],
        "source": 6,
        "target": 1
      },
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
      }
    ],
    "receipts": [
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":9,\"candidatos_descartados_en_recibos\":0}",
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
    "operations": 5473,
    "elapsedNanos": 223801
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
    "id": "holdout-route-023",
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
  "fullJournalSha256": "09b6dec6d20dc25a093af67f79eb7727e3dc7c89fe7e54a8c58e88018b5aa92f",
  "familyState": {
    "schema": 1,
    "revision": 516,
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-023",
  "family": "route",
  "description": "Dos componentes aleatorias con destino inalcanzable",
  "input": {
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
}
```

### operations

```json
5473
```

### elapsedNanos

```json
524574
```

### adapted

```json
false
```

### reused

```json
true
```
