# holdout-dependencies-018

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-018

### identifier

```json
"holdout-dependencies-018"
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
  "fullJournalSha256": "5dfb99552ab06d2fc3a294023f83d4ded79f3e6884fa4de554d1ec2eb98a0a97",
  "familyState": {
    "schema": 1,
    "revision": 494,
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
        "nodes": 4,
        "edges": [
          [
            0,
            0
          ]
        ]
      },
      {
        "nodes": 12,
        "edges": [
          [
            0,
            7
          ],
          [
            0,
            10
          ],
          [
            1,
            6
          ],
          [
            1,
            9
          ],
          [
            3,
            3
          ],
          [
            3,
            5
          ],
          [
            3,
            6
          ],
          [
            4,
            1
          ],
          [
            4,
            5
          ],
          [
            4,
            6
          ],
          [
            5,
            8
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
            7,
            0
          ],
          [
            7,
            3
          ],
          [
            8,
            0
          ],
          [
            8,
            9
          ],
          [
            9,
            1
          ],
          [
            9,
            2
          ],
          [
            9,
            6
          ],
          [
            9,
            11
          ],
          [
            10,
            7
          ],
          [
            11,
            4
          ],
          [
            11,
            7
          ]
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-006",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":4,\"candidatos_descartados_en_recibos\":0}",
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
        6,
        8,
        13,
        11,
        12,
        5,
        2,
        9,
        3,
        4,
        10,
        7,
        1,
        0
      ],
      "layers": [
        [
          6
        ],
        [
          8
        ],
        [
          13
        ],
        [
          11,
          12
        ],
        [
          5
        ],
        [
          2,
          9
        ],
        [
          3
        ],
        [
          4,
          10
        ],
        [
          7
        ],
        [
          1
        ],
        [
          0
        ]
      ]
    },
    "operations": 7579,
    "elapsedNanos": 268276
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    6,
    8,
    13,
    11,
    12,
    5,
    2,
    9,
    3,
    4,
    10,
    7,
    1,
    0
  ],
  "layers": [
    [
      6
    ],
    [
      8
    ],
    [
      13
    ],
    [
      11,
      12
    ],
    [
      5
    ],
    [
      2,
      9
    ],
    [
      3
    ],
    [
      4,
      10
    ],
    [
      7
    ],
    [
      1
    ],
    [
      0
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
    "id": "holdout-dependencies-018",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 11
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
  "fullJournalSha256": "060314bb406d62afdc333789105f5424f4357bd6981c97103ffefe75235a9643",
  "familyState": {
    "schema": 1,
    "revision": 495,
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
        "nodes": 12,
        "edges": [
          [
            0,
            7
          ],
          [
            0,
            10
          ],
          [
            1,
            6
          ],
          [
            1,
            9
          ],
          [
            3,
            3
          ],
          [
            3,
            5
          ],
          [
            3,
            6
          ],
          [
            4,
            1
          ],
          [
            4,
            5
          ],
          [
            4,
            6
          ],
          [
            5,
            8
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
            7,
            0
          ],
          [
            7,
            3
          ],
          [
            8,
            0
          ],
          [
            8,
            9
          ],
          [
            9,
            1
          ],
          [
            9,
            2
          ],
          [
            9,
            6
          ],
          [
            9,
            11
          ],
          [
            10,
            7
          ],
          [
            11,
            4
          ],
          [
            11,
            7
          ]
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-006",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-018",
  "family": "dependencies",
  "description": "DAG aleatorio denso con etiquetas permutadas",
  "input": {
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
  }
}
```

### operations

```json
7579
```

### elapsedNanos

```json
614217
```

### adapted

```json
false
```

### reused

```json
true
```
