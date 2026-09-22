# holdout-dependencies-006

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-006

### identifier

```json
"holdout-dependencies-006"
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
  "fullJournalSha256": "22f5f955461cb578d2187e6e6de069d38a687fccc470a8d60205651b5886c008",
  "familyState": {
    "schema": 1,
    "revision": 472,
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
        "nodes": 9,
        "edges": [
          [
            1,
            5
          ],
          [
            1,
            6
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
            2,
            6
          ],
          [
            2,
            7
          ],
          [
            2,
            8
          ],
          [
            4,
            0
          ],
          [
            4,
            3
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
            4,
            7
          ],
          [
            5,
            0
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
            6,
            3
          ],
          [
            7,
            3
          ],
          [
            7,
            5
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
            7
          ]
        ]
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            5
          ],
          [
            0,
            7
          ],
          [
            1,
            0
          ],
          [
            1,
            2
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
            1,
            6
          ],
          [
            1,
            7
          ],
          [
            2,
            7
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
            5
          ],
          [
            3,
            6
          ],
          [
            4,
            2
          ],
          [
            4,
            7
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
            3
          ],
          [
            8,
            4
          ],
          [
            8,
            7
          ]
        ]
      },
      {
        "nodes": 8,
        "edges": [
          [
            0,
            1
          ],
          [
            2,
            0
          ],
          [
            3,
            1
          ],
          [
            3,
            2
          ],
          [
            6,
            6
          ],
          [
            7,
            0
          ],
          [
            7,
            4
          ],
          [
            7,
            6
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-016",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-013",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-002",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-014",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-032",
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
        4,
        3,
        5,
        2,
        6,
        11,
        9,
        0,
        10,
        1,
        12,
        7,
        8
      ],
      "layers": [
        [
          4
        ],
        [
          3,
          5
        ],
        [
          2
        ],
        [
          6,
          11
        ],
        [
          9
        ],
        [
          0
        ],
        [
          10
        ],
        [
          1,
          12
        ],
        [
          7,
          8
        ]
      ]
    },
    "operations": 6369,
    "elapsedNanos": 231071
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    4,
    3,
    5,
    2,
    6,
    11,
    9,
    0,
    10,
    1,
    12,
    7,
    8
  ],
  "layers": [
    [
      4
    ],
    [
      3,
      5
    ],
    [
      2
    ],
    [
      6,
      11
    ],
    [
      9
    ],
    [
      0
    ],
    [
      10
    ],
    [
      1,
      12
    ],
    [
      7,
      8
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
    "id": "holdout-dependencies-006",
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
  "fullJournalSha256": "2998bff22f21a8b0a47b876081eaf8e2291ce0e66aface6cfd0e4676b36790ee",
  "familyState": {
    "schema": 1,
    "revision": 473,
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
        "nodes": 9,
        "edges": [
          [
            0,
            5
          ],
          [
            0,
            7
          ],
          [
            1,
            0
          ],
          [
            1,
            2
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
            1,
            6
          ],
          [
            1,
            7
          ],
          [
            2,
            7
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
            5
          ],
          [
            3,
            6
          ],
          [
            4,
            2
          ],
          [
            4,
            7
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
            3
          ],
          [
            8,
            4
          ],
          [
            8,
            7
          ]
        ]
      },
      {
        "nodes": 8,
        "edges": [
          [
            0,
            1
          ],
          [
            2,
            0
          ],
          [
            3,
            1
          ],
          [
            3,
            2
          ],
          [
            6,
            6
          ],
          [
            7,
            0
          ],
          [
            7,
            4
          ],
          [
            7,
            6
          ]
        ]
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            7
          ],
          [
            0,
            8
          ],
          [
            0,
            10
          ],
          [
            0,
            12
          ],
          [
            1,
            7
          ],
          [
            1,
            8
          ],
          [
            2,
            6
          ],
          [
            2,
            7
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
            2,
            10
          ],
          [
            2,
            11
          ],
          [
            2,
            12
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
            2
          ],
          [
            3,
            6
          ],
          [
            3,
            7
          ],
          [
            3,
            10
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
            0
          ],
          [
            4,
            1
          ],
          [
            4,
            3
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
            4,
            8
          ],
          [
            4,
            9
          ],
          [
            4,
            11
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
            5,
            8
          ],
          [
            5,
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
            12
          ],
          [
            9,
            0
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
            9,
            12
          ],
          [
            10,
            1
          ],
          [
            10,
            7
          ],
          [
            10,
            8
          ],
          [
            10,
            12
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
            8
          ],
          [
            11,
            10
          ],
          [
            11,
            12
          ],
          [
            12,
            7
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-016",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-013",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-002",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-014",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-032",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-006",
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
  "id": "holdout-dependencies-006",
  "family": "dependencies",
  "description": "DAG aleatorio denso con etiquetas permutadas",
  "input": {
    "nodes": 13,
    "edges": [
      [
        0,
        7
      ],
      [
        0,
        8
      ],
      [
        0,
        10
      ],
      [
        0,
        12
      ],
      [
        1,
        7
      ],
      [
        1,
        8
      ],
      [
        2,
        6
      ],
      [
        2,
        7
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
        2,
        10
      ],
      [
        2,
        11
      ],
      [
        2,
        12
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
        2
      ],
      [
        3,
        6
      ],
      [
        3,
        7
      ],
      [
        3,
        10
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
        0
      ],
      [
        4,
        1
      ],
      [
        4,
        3
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
        4,
        8
      ],
      [
        4,
        9
      ],
      [
        4,
        11
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
        5,
        8
      ],
      [
        5,
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
        12
      ],
      [
        9,
        0
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
        9,
        12
      ],
      [
        10,
        1
      ],
      [
        10,
        7
      ],
      [
        10,
        8
      ],
      [
        10,
        12
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
        8
      ],
      [
        11,
        10
      ],
      [
        11,
        12
      ],
      [
        12,
        7
      ]
    ]
  }
}
```

### operations

```json
6369
```

### elapsedNanos

```json
499147
```

### adapted

```json
false
```

### reused

```json
true
```
