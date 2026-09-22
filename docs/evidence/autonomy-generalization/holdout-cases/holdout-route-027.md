# holdout-route-027

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-027

### identifier

```json
"holdout-route-027"
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
  "fullJournalSha256": "5d57863d9c2c7c39c69843b9607619b3be4604337395a8a13e52b1a9840cbc9a",
  "familyState": {
    "schema": 1,
    "revision": 501,
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
      },
      {
        "nodes": 10,
        "edges": [
          [
            0,
            2,
            24
          ],
          [
            0,
            3,
            13
          ],
          [
            1,
            2,
            8
          ],
          [
            3,
            0,
            26
          ],
          [
            3,
            5,
            24
          ],
          [
            3,
            6,
            14
          ],
          [
            4,
            3,
            35
          ],
          [
            4,
            6,
            0
          ],
          [
            5,
            2,
            1
          ],
          [
            5,
            4,
            3
          ],
          [
            6,
            1,
            22
          ],
          [
            6,
            3,
            33
          ],
          [
            6,
            5,
            24
          ],
          [
            7,
            9,
            14
          ],
          [
            8,
            9,
            31
          ]
        ],
        "source": 0,
        "target": 9
      },
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
      }
    ],
    "receipts": [
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
    "operations": 5666,
    "elapsedNanos": 466008
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
    "id": "holdout-route-027",
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
  "fullJournalSha256": "29d9b72ad7679e997d915f66bf0879a7143f4528c8a7d41aaa89a796f0c4ba1a",
  "familyState": {
    "schema": 1,
    "revision": 502,
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
            0,
            2,
            24
          ],
          [
            0,
            3,
            13
          ],
          [
            1,
            2,
            8
          ],
          [
            3,
            0,
            26
          ],
          [
            3,
            5,
            24
          ],
          [
            3,
            6,
            14
          ],
          [
            4,
            3,
            35
          ],
          [
            4,
            6,
            0
          ],
          [
            5,
            2,
            1
          ],
          [
            5,
            4,
            3
          ],
          [
            6,
            1,
            22
          ],
          [
            6,
            3,
            33
          ],
          [
            6,
            5,
            24
          ],
          [
            7,
            9,
            14
          ],
          [
            8,
            9,
            31
          ]
        ],
        "source": 0,
        "target": 9
      },
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
      }
    ],
    "receipts": [
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-027",
  "family": "route",
  "description": "Dos componentes aleatorias con destino inalcanzable",
  "input": {
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
  }
}
```

### operations

```json
5666
```

### elapsedNanos

```json
715085
```

### adapted

```json
false
```

### reused

```json
true
```
