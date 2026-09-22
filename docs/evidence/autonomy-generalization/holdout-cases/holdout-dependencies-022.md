# holdout-dependencies-022

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-022

### identifier

```json
"holdout-dependencies-022"
```

### objective

```json
"DAG aleatorio denso con etiquetas permutadas"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "f4c1e7b3ead9b9c4743c603310c94f55ab85782e0633b5ade3fef7447e75ce36",
  "familyState": {
    "schema": 1,
    "revision": 440,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "dependencies",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "KAHN_LAYERS"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "dependencies",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "INPUT_ORDER"
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
            5
          ],
          [
            1,
            3
          ],
          [
            1,
            4
          ],
          [
            4,
            3
          ],
          [
            5,
            0
          ]
        ]
      },
      {
        "nodes": 14,
        "edges": [
          [
            0,
            2
          ],
          [
            0,
            8
          ],
          [
            1,
            9
          ],
          [
            1,
            11
          ],
          [
            2,
            1
          ],
          [
            2,
            5
          ],
          [
            2,
            8
          ],
          [
            2,
            9
          ],
          [
            3,
            5
          ],
          [
            3,
            7
          ],
          [
            3,
            8
          ],
          [
            3,
            9
          ],
          [
            3,
            11
          ],
          [
            3,
            13
          ],
          [
            5,
            1
          ],
          [
            5,
            7
          ],
          [
            6,
            12
          ],
          [
            8,
            3
          ],
          [
            8,
            4
          ],
          [
            8,
            11
          ],
          [
            9,
            0
          ],
          [
            10,
            1
          ],
          [
            10,
            6
          ],
          [
            11,
            1
          ],
          [
            11,
            12
          ],
          [
            12,
            0
          ],
          [
            12,
            2
          ],
          [
            12,
            6
          ],
          [
            12,
            13
          ],
          [
            13,
            0
          ]
        ]
      },
      {
        "nodes": 12,
        "edges": [
          [
            1,
            5
          ],
          [
            1,
            10
          ],
          [
            2,
            0
          ],
          [
            2,
            3
          ],
          [
            2,
            5
          ],
          [
            3,
            0
          ],
          [
            3,
            4
          ],
          [
            3,
            9
          ],
          [
            4,
            9
          ],
          [
            5,
            10
          ],
          [
            6,
            1
          ],
          [
            6,
            4
          ],
          [
            8,
            0
          ],
          [
            8,
            7
          ],
          [
            8,
            11
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-027",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-008",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-029",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-001",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-011",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-023",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-009",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
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
    "strategy": "KAHN_LAYERS",
    "programSha256": "82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "KAHN_LAYERS"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":7,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "KAHN_LAYERS",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "dependencies",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "KAHN_LAYERS"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "order": [
        0,
        6,
        8,
        7,
        10,
        1,
        3,
        5,
        2,
        9,
        4
      ],
      "layers": [
        [
          0
        ],
        [
          6,
          8
        ],
        [
          7,
          10
        ],
        [
          1,
          3
        ],
        [
          5
        ],
        [
          2
        ],
        [
          9
        ],
        [
          4
        ]
      ]
    },
    "operations": 7891,
    "elapsedNanos": 198673
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0,
    6,
    8,
    7,
    10,
    1,
    3,
    5,
    2,
    9,
    4
  ],
  "layers": [
    [
      0
    ],
    [
      6,
      8
    ],
    [
      7,
      10
    ],
    [
      1,
      3
    ],
    [
      5
    ],
    [
      2
    ],
    [
      9
    ],
    [
      4
    ]
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
    "KAHN_LAYERS"
  ],
  "promoted": true,
  "version": 2,
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
    "strategy": "KAHN_LAYERS",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "holdout-dependencies-022",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 8
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "53e2c0f56e1e67eeb7ef30bb645d9550d7d2e5bc8e818e028937d99a7e965366",
  "familyState": {
    "schema": 1,
    "revision": 441,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "dependencies",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "KAHN_LAYERS"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "dependencies",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "INPUT_ORDER"
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
            2
          ],
          [
            0,
            8
          ],
          [
            1,
            9
          ],
          [
            1,
            11
          ],
          [
            2,
            1
          ],
          [
            2,
            5
          ],
          [
            2,
            8
          ],
          [
            2,
            9
          ],
          [
            3,
            5
          ],
          [
            3,
            7
          ],
          [
            3,
            8
          ],
          [
            3,
            9
          ],
          [
            3,
            11
          ],
          [
            3,
            13
          ],
          [
            5,
            1
          ],
          [
            5,
            7
          ],
          [
            6,
            12
          ],
          [
            8,
            3
          ],
          [
            8,
            4
          ],
          [
            8,
            11
          ],
          [
            9,
            0
          ],
          [
            10,
            1
          ],
          [
            10,
            6
          ],
          [
            11,
            1
          ],
          [
            11,
            12
          ],
          [
            12,
            0
          ],
          [
            12,
            2
          ],
          [
            12,
            6
          ],
          [
            12,
            13
          ],
          [
            13,
            0
          ]
        ]
      },
      {
        "nodes": 12,
        "edges": [
          [
            1,
            5
          ],
          [
            1,
            10
          ],
          [
            2,
            0
          ],
          [
            2,
            3
          ],
          [
            2,
            5
          ],
          [
            3,
            0
          ],
          [
            3,
            4
          ],
          [
            3,
            9
          ],
          [
            4,
            9
          ],
          [
            5,
            10
          ],
          [
            6,
            1
          ],
          [
            6,
            4
          ],
          [
            8,
            0
          ],
          [
            8,
            7
          ],
          [
            8,
            11
          ]
        ]
      },
      {
        "nodes": 11,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            2
          ],
          [
            0,
            3
          ],
          [
            0,
            6
          ],
          [
            0,
            8
          ],
          [
            0,
            9
          ],
          [
            0,
            10
          ],
          [
            1,
            2
          ],
          [
            1,
            4
          ],
          [
            1,
            9
          ],
          [
            2,
            9
          ],
          [
            3,
            2
          ],
          [
            3,
            4
          ],
          [
            3,
            5
          ],
          [
            3,
            9
          ],
          [
            5,
            2
          ],
          [
            5,
            4
          ],
          [
            5,
            9
          ],
          [
            6,
            2
          ],
          [
            6,
            3
          ],
          [
            6,
            4
          ],
          [
            6,
            5
          ],
          [
            6,
            7
          ],
          [
            6,
            9
          ],
          [
            7,
            3
          ],
          [
            7,
            4
          ],
          [
            7,
            5
          ],
          [
            7,
            9
          ],
          [
            8,
            9
          ],
          [
            8,
            10
          ],
          [
            9,
            4
          ],
          [
            10,
            1
          ],
          [
            10,
            3
          ],
          [
            10,
            4
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-027",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-008",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-029",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-001",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-011",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-023",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-009",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-022",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
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
  "id": "holdout-dependencies-022",
  "family": "dependencies",
  "description": "DAG aleatorio denso con etiquetas permutadas",
  "input": {
    "nodes": 11,
    "edges": [
      [
        0,
        1
      ],
      [
        0,
        2
      ],
      [
        0,
        3
      ],
      [
        0,
        6
      ],
      [
        0,
        8
      ],
      [
        0,
        9
      ],
      [
        0,
        10
      ],
      [
        1,
        2
      ],
      [
        1,
        4
      ],
      [
        1,
        9
      ],
      [
        2,
        9
      ],
      [
        3,
        2
      ],
      [
        3,
        4
      ],
      [
        3,
        5
      ],
      [
        3,
        9
      ],
      [
        5,
        2
      ],
      [
        5,
        4
      ],
      [
        5,
        9
      ],
      [
        6,
        2
      ],
      [
        6,
        3
      ],
      [
        6,
        4
      ],
      [
        6,
        5
      ],
      [
        6,
        7
      ],
      [
        6,
        9
      ],
      [
        7,
        3
      ],
      [
        7,
        4
      ],
      [
        7,
        5
      ],
      [
        7,
        9
      ],
      [
        8,
        9
      ],
      [
        8,
        10
      ],
      [
        9,
        4
      ],
      [
        10,
        1
      ],
      [
        10,
        3
      ],
      [
        10,
        4
      ]
    ]
  }
}
```

### operations

```json
7891
```

### elapsedNanos

```json
472447
```

### adapted

```json
false
```

### reused

```json
true
```
