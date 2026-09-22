# holdout-route-024

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-024

### identifier

```json
"holdout-route-024"
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
  "fullJournalSha256": "abbea03021a6243a7a99b8ad478e58ce9df150b6e4fe6a21897a50f40d01644e",
  "familyState": {
    "schema": 1,
    "revision": 458,
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
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            3,
            2
          ],
          [
            0,
            8,
            28
          ],
          [
            0,
            10,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            1,
            3,
            3
          ],
          [
            1,
            6,
            18
          ],
          [
            1,
            8,
            35
          ],
          [
            2,
            1,
            22
          ],
          [
            2,
            3,
            21
          ],
          [
            2,
            4,
            3
          ],
          [
            2,
            5,
            29
          ],
          [
            2,
            7,
            0
          ],
          [
            2,
            8,
            11
          ],
          [
            2,
            9,
            0
          ],
          [
            3,
            0,
            23
          ],
          [
            3,
            1,
            19
          ],
          [
            3,
            2,
            21
          ],
          [
            3,
            7,
            25
          ],
          [
            3,
            10,
            29
          ],
          [
            4,
            0,
            18
          ],
          [
            4,
            1,
            23
          ],
          [
            4,
            3,
            30
          ],
          [
            4,
            7,
            23
          ],
          [
            4,
            9,
            34
          ],
          [
            4,
            10,
            7
          ],
          [
            5,
            0,
            33
          ],
          [
            5,
            1,
            3
          ],
          [
            5,
            3,
            2
          ],
          [
            5,
            4,
            28
          ],
          [
            5,
            8,
            8
          ],
          [
            6,
            3,
            22
          ],
          [
            6,
            4,
            25
          ],
          [
            6,
            8,
            4
          ],
          [
            6,
            10,
            14
          ],
          [
            7,
            0,
            15
          ],
          [
            7,
            2,
            20
          ],
          [
            7,
            3,
            7
          ],
          [
            7,
            4,
            27
          ],
          [
            7,
            5,
            10
          ],
          [
            7,
            6,
            16
          ],
          [
            7,
            8,
            4
          ],
          [
            8,
            0,
            10
          ],
          [
            8,
            1,
            18
          ],
          [
            8,
            3,
            7
          ],
          [
            8,
            6,
            30
          ],
          [
            8,
            7,
            19
          ],
          [
            8,
            10,
            10
          ],
          [
            9,
            0,
            21
          ],
          [
            9,
            3,
            30
          ],
          [
            10,
            3,
            29
          ],
          [
            10,
            5,
            15
          ],
          [
            10,
            6,
            30
          ],
          [
            10,
            8,
            25
          ],
          [
            0,
            1,
            9
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            5,
            8
          ],
          [
            5,
            6,
            17
          ],
          [
            6,
            7,
            5
          ],
          [
            8,
            9,
            4
          ],
          [
            9,
            10,
            18
          ],
          [
            10,
            0,
            15
          ]
        ],
        "source": 0,
        "target": 10
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
      },
      {
        "id": "holdout-route-022",
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
      "status": "ok",
      "cost": 4,
      "path": [
        0,
        12,
        13
      ]
    },
    "operations": 9595,
    "elapsedNanos": 655708
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
    12,
    13
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
    "id": "holdout-route-024",
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
  "fullJournalSha256": "d786adda8863c417edad722d0a141c28ed9c34ae54e6607a5a18de6a1c7eeeba",
  "familyState": {
    "schema": 1,
    "revision": 459,
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
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            3,
            2
          ],
          [
            0,
            8,
            28
          ],
          [
            0,
            10,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            1,
            3,
            3
          ],
          [
            1,
            6,
            18
          ],
          [
            1,
            8,
            35
          ],
          [
            2,
            1,
            22
          ],
          [
            2,
            3,
            21
          ],
          [
            2,
            4,
            3
          ],
          [
            2,
            5,
            29
          ],
          [
            2,
            7,
            0
          ],
          [
            2,
            8,
            11
          ],
          [
            2,
            9,
            0
          ],
          [
            3,
            0,
            23
          ],
          [
            3,
            1,
            19
          ],
          [
            3,
            2,
            21
          ],
          [
            3,
            7,
            25
          ],
          [
            3,
            10,
            29
          ],
          [
            4,
            0,
            18
          ],
          [
            4,
            1,
            23
          ],
          [
            4,
            3,
            30
          ],
          [
            4,
            7,
            23
          ],
          [
            4,
            9,
            34
          ],
          [
            4,
            10,
            7
          ],
          [
            5,
            0,
            33
          ],
          [
            5,
            1,
            3
          ],
          [
            5,
            3,
            2
          ],
          [
            5,
            4,
            28
          ],
          [
            5,
            8,
            8
          ],
          [
            6,
            3,
            22
          ],
          [
            6,
            4,
            25
          ],
          [
            6,
            8,
            4
          ],
          [
            6,
            10,
            14
          ],
          [
            7,
            0,
            15
          ],
          [
            7,
            2,
            20
          ],
          [
            7,
            3,
            7
          ],
          [
            7,
            4,
            27
          ],
          [
            7,
            5,
            10
          ],
          [
            7,
            6,
            16
          ],
          [
            7,
            8,
            4
          ],
          [
            8,
            0,
            10
          ],
          [
            8,
            1,
            18
          ],
          [
            8,
            3,
            7
          ],
          [
            8,
            6,
            30
          ],
          [
            8,
            7,
            19
          ],
          [
            8,
            10,
            10
          ],
          [
            9,
            0,
            21
          ],
          [
            9,
            3,
            30
          ],
          [
            10,
            3,
            29
          ],
          [
            10,
            5,
            15
          ],
          [
            10,
            6,
            30
          ],
          [
            10,
            8,
            25
          ],
          [
            0,
            1,
            9
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            5,
            8
          ],
          [
            5,
            6,
            17
          ],
          [
            6,
            7,
            5
          ],
          [
            8,
            9,
            4
          ],
          [
            9,
            10,
            18
          ],
          [
            10,
            0,
            15
          ]
        ],
        "source": 0,
        "target": 10
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-024",
  "family": "route",
  "description": "Grafo cíclico aleatorio con potenciales: posibles arcos negativos sin ciclos negativos",
  "input": {
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
  }
}
```

### operations

```json
9595
```

### elapsedNanos

```json
957753
```

### adapted

```json
false
```

### reused

```json
true
```
