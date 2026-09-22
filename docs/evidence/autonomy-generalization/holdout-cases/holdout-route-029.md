# holdout-route-029

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-route-029

### identifier

```json
"holdout-route-029"
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
  "fullJournalSha256": "4c80f990c4d9d22f75bfd0ce4e3a0285e8c2d9dd47fd66acb03c709a81374120",
  "familyState": {
    "schema": 1,
    "revision": 436,
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
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            2,
            0,
            1
          ],
          [
            3,
            4,
            1
          ],
          [
            0,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 14,
        "edges": [
          [
            0,
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
          ],
          [
            3,
            4,
            3
          ],
          [
            4,
            5,
            3
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            3
          ],
          [
            8,
            9,
            3
          ],
          [
            9,
            10,
            3
          ],
          [
            10,
            11,
            3
          ],
          [
            11,
            12,
            3
          ],
          [
            12,
            13,
            3
          ],
          [
            1,
            11,
            1
          ],
          [
            0,
            13,
            40
          ],
          [
            8,
            3,
            2
          ],
          [
            0,
            2,
            4
          ]
        ],
        "source": 13,
        "target": 0
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            16
          ],
          [
            1,
            0,
            23
          ],
          [
            3,
            2,
            7
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 0,
      "path": [
        2,
        0,
        1,
        3,
        5,
        4
      ]
    },
    "operations": 4463,
    "elapsedNanos": 122010
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 0,
  "path": [
    2,
    0,
    1,
    3,
    5,
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
    "id": "holdout-route-029",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 0,
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
  "fullJournalSha256": "707455652d46a5210fd5af1b7e4486a2828a498265510e0b214bd3e66a3c8312",
  "familyState": {
    "schema": 1,
    "revision": 437,
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
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
          ],
          [
            3,
            4,
            3
          ],
          [
            4,
            5,
            3
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            3
          ],
          [
            8,
            9,
            3
          ],
          [
            9,
            10,
            3
          ],
          [
            10,
            11,
            3
          ],
          [
            11,
            12,
            3
          ],
          [
            12,
            13,
            3
          ],
          [
            1,
            11,
            1
          ],
          [
            0,
            13,
            40
          ],
          [
            8,
            3,
            2
          ],
          [
            0,
            2,
            4
          ]
        ],
        "source": 13,
        "target": 0
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            16
          ],
          [
            1,
            0,
            23
          ],
          [
            3,
            2,
            7
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 6,
        "edges": [
          [
            2,
            0,
            -8
          ],
          [
            2,
            3,
            -8
          ],
          [
            2,
            4,
            2
          ],
          [
            0,
            1,
            -3
          ],
          [
            0,
            3,
            -4
          ],
          [
            0,
            5,
            15
          ],
          [
            1,
            3,
            -6
          ],
          [
            3,
            5,
            6
          ],
          [
            5,
            4,
            11
          ]
        ],
        "source": 2,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-route-029",
  "family": "route",
  "description": "DAG aleatorio con etiquetas permutadas y costes negativos",
  "input": {
    "nodes": 6,
    "edges": [
      [
        2,
        0,
        -8
      ],
      [
        2,
        3,
        -8
      ],
      [
        2,
        4,
        2
      ],
      [
        0,
        1,
        -3
      ],
      [
        0,
        3,
        -4
      ],
      [
        0,
        5,
        15
      ],
      [
        1,
        3,
        -6
      ],
      [
        3,
        5,
        6
      ],
      [
        5,
        4,
        11
      ]
    ],
    "source": 2,
    "target": 4
  }
}
```

### operations

```json
4463
```

### elapsedNanos

```json
393190
```

### adapted

```json
false
```

### reused

```json
true
```
