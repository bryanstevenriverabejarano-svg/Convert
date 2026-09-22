# holdout-dependencies-010

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-010

### identifier

```json
"holdout-dependencies-010"
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
  "fullJournalSha256": "0aa9a7abcd2aeaed2a7223fe5ac9020947f0f563f3daf842407745b56a6259f2",
  "familyState": {
    "schema": 1,
    "revision": 499,
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
        "nodes": 10,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            7
          ],
          [
            2,
            0
          ],
          [
            2,
            4
          ],
          [
            2,
            7
          ],
          [
            2,
            9
          ],
          [
            3,
            0
          ],
          [
            3,
            1
          ],
          [
            3,
            6
          ],
          [
            4,
            0
          ],
          [
            4,
            1
          ],
          [
            4,
            6
          ],
          [
            4,
            7
          ],
          [
            5,
            4
          ],
          [
            5,
            7
          ],
          [
            5,
            9
          ],
          [
            6,
            0
          ],
          [
            6,
            1
          ],
          [
            8,
            0
          ],
          [
            8,
            1
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
            5
          ],
          [
            8,
            6
          ],
          [
            8,
            7
          ],
          [
            9,
            1
          ],
          [
            9,
            4
          ],
          [
            9,
            7
          ]
        ]
      },
      {
        "nodes": 14,
        "edges": [
          [
            1,
            0
          ],
          [
            2,
            0
          ],
          [
            2,
            1
          ],
          [
            2,
            3
          ],
          [
            2,
            4
          ],
          [
            3,
            4
          ],
          [
            3,
            10
          ],
          [
            4,
            0
          ],
          [
            4,
            7
          ],
          [
            5,
            0
          ],
          [
            5,
            1
          ],
          [
            5,
            2
          ],
          [
            5,
            3
          ],
          [
            5,
            4
          ],
          [
            5,
            7
          ],
          [
            5,
            9
          ],
          [
            6,
            0
          ],
          [
            6,
            2
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
            8
          ],
          [
            6,
            9
          ],
          [
            6,
            11
          ],
          [
            6,
            12
          ],
          [
            7,
            0
          ],
          [
            7,
            1
          ],
          [
            8,
            0
          ],
          [
            8,
            1
          ],
          [
            8,
            2
          ],
          [
            8,
            4
          ],
          [
            8,
            5
          ],
          [
            8,
            7
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
            8,
            11
          ],
          [
            8,
            12
          ],
          [
            8,
            13
          ],
          [
            9,
            1
          ],
          [
            9,
            3
          ],
          [
            9,
            4
          ],
          [
            9,
            7
          ],
          [
            10,
            0
          ],
          [
            10,
            7
          ],
          [
            11,
            0
          ],
          [
            11,
            1
          ],
          [
            11,
            3
          ],
          [
            11,
            4
          ],
          [
            11,
            5
          ],
          [
            11,
            9
          ],
          [
            11,
            10
          ],
          [
            12,
            0
          ],
          [
            12,
            3
          ],
          [
            12,
            7
          ],
          [
            13,
            0
          ],
          [
            13,
            2
          ],
          [
            13,
            3
          ],
          [
            13,
            4
          ],
          [
            13,
            5
          ],
          [
            13,
            7
          ],
          [
            13,
            10
          ],
          [
            13,
            11
          ],
          [
            13,
            12
          ]
        ]
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            6
          ],
          [
            1,
            0
          ],
          [
            1,
            6
          ],
          [
            2,
            12
          ],
          [
            3,
            4
          ],
          [
            3,
            7
          ],
          [
            3,
            11
          ],
          [
            3,
            12
          ],
          [
            4,
            4
          ],
          [
            5,
            3
          ],
          [
            5,
            6
          ],
          [
            5,
            8
          ],
          [
            6,
            2
          ],
          [
            6,
            4
          ],
          [
            6,
            9
          ],
          [
            8,
            1
          ],
          [
            8,
            6
          ],
          [
            11,
            8
          ],
          [
            12,
            0
          ],
          [
            12,
            10
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-012",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-004",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-026",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-018",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-020",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
        9,
        3,
        10,
        8,
        0,
        1,
        4,
        2,
        7,
        5,
        6
      ],
      "layers": [
        [
          9
        ],
        [
          3
        ],
        [
          10
        ],
        [
          8
        ],
        [
          0
        ],
        [
          1,
          4
        ],
        [
          2,
          7
        ],
        [
          5
        ],
        [
          6
        ]
      ]
    },
    "operations": 10144,
    "elapsedNanos": 358138
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    9,
    3,
    10,
    8,
    0,
    1,
    4,
    2,
    7,
    5,
    6
  ],
  "layers": [
    [
      9
    ],
    [
      3
    ],
    [
      10
    ],
    [
      8
    ],
    [
      0
    ],
    [
      1,
      4
    ],
    [
      2,
      7
    ],
    [
      5
    ],
    [
      6
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
    "id": "holdout-dependencies-010",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 9
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
  "fullJournalSha256": "4a6524c5e7df01e68ce0c72f1035f9fa16c57ebbf09782678a53873498601505",
  "familyState": {
    "schema": 1,
    "revision": 500,
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
            1,
            0
          ],
          [
            2,
            0
          ],
          [
            2,
            1
          ],
          [
            2,
            3
          ],
          [
            2,
            4
          ],
          [
            3,
            4
          ],
          [
            3,
            10
          ],
          [
            4,
            0
          ],
          [
            4,
            7
          ],
          [
            5,
            0
          ],
          [
            5,
            1
          ],
          [
            5,
            2
          ],
          [
            5,
            3
          ],
          [
            5,
            4
          ],
          [
            5,
            7
          ],
          [
            5,
            9
          ],
          [
            6,
            0
          ],
          [
            6,
            2
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
            8
          ],
          [
            6,
            9
          ],
          [
            6,
            11
          ],
          [
            6,
            12
          ],
          [
            7,
            0
          ],
          [
            7,
            1
          ],
          [
            8,
            0
          ],
          [
            8,
            1
          ],
          [
            8,
            2
          ],
          [
            8,
            4
          ],
          [
            8,
            5
          ],
          [
            8,
            7
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
            8,
            11
          ],
          [
            8,
            12
          ],
          [
            8,
            13
          ],
          [
            9,
            1
          ],
          [
            9,
            3
          ],
          [
            9,
            4
          ],
          [
            9,
            7
          ],
          [
            10,
            0
          ],
          [
            10,
            7
          ],
          [
            11,
            0
          ],
          [
            11,
            1
          ],
          [
            11,
            3
          ],
          [
            11,
            4
          ],
          [
            11,
            5
          ],
          [
            11,
            9
          ],
          [
            11,
            10
          ],
          [
            12,
            0
          ],
          [
            12,
            3
          ],
          [
            12,
            7
          ],
          [
            13,
            0
          ],
          [
            13,
            2
          ],
          [
            13,
            3
          ],
          [
            13,
            4
          ],
          [
            13,
            5
          ],
          [
            13,
            7
          ],
          [
            13,
            10
          ],
          [
            13,
            11
          ],
          [
            13,
            12
          ]
        ]
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            6
          ],
          [
            1,
            0
          ],
          [
            1,
            6
          ],
          [
            2,
            12
          ],
          [
            3,
            4
          ],
          [
            3,
            7
          ],
          [
            3,
            11
          ],
          [
            3,
            12
          ],
          [
            4,
            4
          ],
          [
            5,
            3
          ],
          [
            5,
            6
          ],
          [
            5,
            8
          ],
          [
            6,
            2
          ],
          [
            6,
            4
          ],
          [
            6,
            9
          ],
          [
            8,
            1
          ],
          [
            8,
            6
          ],
          [
            11,
            8
          ],
          [
            12,
            0
          ],
          [
            12,
            10
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
            4
          ],
          [
            0,
            5
          ],
          [
            1,
            2
          ],
          [
            2,
            5
          ],
          [
            2,
            6
          ],
          [
            3,
            0
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
            6
          ],
          [
            3,
            8
          ],
          [
            3,
            10
          ],
          [
            4,
            2
          ],
          [
            4,
            5
          ],
          [
            4,
            7
          ],
          [
            5,
            6
          ],
          [
            7,
            5
          ],
          [
            7,
            6
          ],
          [
            8,
            0
          ],
          [
            8,
            1
          ],
          [
            8,
            4
          ],
          [
            8,
            5
          ],
          [
            8,
            6
          ],
          [
            8,
            7
          ],
          [
            9,
            0
          ],
          [
            9,
            1
          ],
          [
            9,
            3
          ],
          [
            9,
            4
          ],
          [
            9,
            5
          ],
          [
            9,
            6
          ],
          [
            9,
            7
          ],
          [
            9,
            8
          ],
          [
            9,
            10
          ],
          [
            10,
            1
          ],
          [
            10,
            2
          ],
          [
            10,
            6
          ],
          [
            10,
            7
          ],
          [
            10,
            8
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-004",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-026",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-018",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-020",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-010",
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
  "id": "holdout-dependencies-010",
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
        4
      ],
      [
        0,
        5
      ],
      [
        1,
        2
      ],
      [
        2,
        5
      ],
      [
        2,
        6
      ],
      [
        3,
        0
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
        6
      ],
      [
        3,
        8
      ],
      [
        3,
        10
      ],
      [
        4,
        2
      ],
      [
        4,
        5
      ],
      [
        4,
        7
      ],
      [
        5,
        6
      ],
      [
        7,
        5
      ],
      [
        7,
        6
      ],
      [
        8,
        0
      ],
      [
        8,
        1
      ],
      [
        8,
        4
      ],
      [
        8,
        5
      ],
      [
        8,
        6
      ],
      [
        8,
        7
      ],
      [
        9,
        0
      ],
      [
        9,
        1
      ],
      [
        9,
        3
      ],
      [
        9,
        4
      ],
      [
        9,
        5
      ],
      [
        9,
        6
      ],
      [
        9,
        7
      ],
      [
        9,
        8
      ],
      [
        9,
        10
      ],
      [
        10,
        1
      ],
      [
        10,
        2
      ],
      [
        10,
        6
      ],
      [
        10,
        7
      ],
      [
        10,
        8
      ]
    ]
  }
}
```

### operations

```json
10144
```

### elapsedNanos

```json
662658
```

### adapted

```json
false
```

### reused

```json
true
```
