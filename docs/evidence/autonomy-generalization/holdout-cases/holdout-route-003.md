# holdout-route-003

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-003

### identifier

```json
"holdout-route-003"
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
  "fullJournalSha256": "35249f1488d5ec1ff3982ee7c8adc57929075490f0e84c6d91303fe1fd5e4fe3",
  "familyState": {
    "schema": 1,
    "revision": 513,
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
      },
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
    "operations": 6760,
    "elapsedNanos": 319562
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
    "id": "holdout-route-003",
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
  "fullJournalSha256": "3450ca235c66429d7757c3f9b286149fe967c10f73c603745980a248937badb8",
  "familyState": {
    "schema": 1,
    "revision": 514,
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
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-003",
  "family": "route",
  "description": "Dos componentes aleatorias con destino inalcanzable",
  "input": {
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
  }
}
```

### operations

```json
6760
```

### elapsedNanos

```json
538305
```

### adapted

```json
false
```

### reused

```json
true
```
