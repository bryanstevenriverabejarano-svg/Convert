# holdout-dependencies-025

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-025

### identifier

```json
"holdout-dependencies-025"
```

### objective

```json
"DAG aleatorio disperso"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "0314791aa7230abf02df6309f61fd5fbd90aabfc48999dc25fa149167815baa0",
  "familyState": {
    "schema": 1,
    "revision": 532,
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
            1,
            1
          ],
          [
            1,
            5
          ],
          [
            1,
            7
          ],
          [
            1,
            11
          ],
          [
            2,
            4
          ],
          [
            4,
            5
          ],
          [
            5,
            2
          ],
          [
            5,
            8
          ],
          [
            6,
            3
          ],
          [
            6,
            7
          ],
          [
            7,
            2
          ],
          [
            8,
            3
          ],
          [
            10,
            7
          ],
          [
            10,
            9
          ],
          [
            11,
            0
          ],
          [
            11,
            3
          ],
          [
            11,
            9
          ]
        ]
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            3
          ],
          [
            2,
            6
          ],
          [
            3,
            5
          ],
          [
            5,
            4
          ],
          [
            5,
            6
          ],
          [
            6,
            0
          ]
        ]
      },
      {
        "nodes": 9,
        "edges": [
          [
            2,
            6
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
            2
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
            2
          ],
          [
            7,
            6
          ],
          [
            8,
            5
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-003",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-024",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-017",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-031",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-007",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-028",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-019",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-005",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":8,\"candidatos_descartados_en_recibos\":0}",
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
        2,
        3,
        9,
        11,
        13,
        4,
        7,
        10,
        1,
        6,
        8,
        5,
        12
      ],
      "layers": [
        [
          0,
          2,
          3,
          9,
          11,
          13
        ],
        [
          4,
          7,
          10
        ],
        [
          1,
          6,
          8
        ],
        [
          5,
          12
        ]
      ]
    },
    "operations": 7215,
    "elapsedNanos": 142490
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0,
    2,
    3,
    9,
    11,
    13,
    4,
    7,
    10,
    1,
    6,
    8,
    5,
    12
  ],
  "layers": [
    [
      0,
      2,
      3,
      9,
      11,
      13
    ],
    [
      4,
      7,
      10
    ],
    [
      1,
      6,
      8
    ],
    [
      5,
      12
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
    "id": "holdout-dependencies-025",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 4
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
  "fullJournalSha256": "c46a970c9c254f2e2567b8d0b54e9728d6dcc33c2ab0c4c998d57296233c3737",
  "familyState": {
    "schema": 1,
    "revision": 533,
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
        "nodes": 7,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            3
          ],
          [
            2,
            6
          ],
          [
            3,
            5
          ],
          [
            5,
            4
          ],
          [
            5,
            6
          ],
          [
            6,
            0
          ]
        ]
      },
      {
        "nodes": 9,
        "edges": [
          [
            2,
            6
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
            2
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
            2
          ],
          [
            7,
            6
          ],
          [
            8,
            5
          ]
        ]
      },
      {
        "nodes": 14,
        "edges": [
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
            12
          ],
          [
            1,
            5
          ],
          [
            1,
            12
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
            3,
            10
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
            6,
            5
          ],
          [
            7,
            8
          ],
          [
            10,
            6
          ],
          [
            10,
            12
          ],
          [
            13,
            5
          ],
          [
            13,
            7
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "holdout-dependencies-003",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-024",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-017",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-031",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-007",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-028",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-019",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-005",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-025",
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
  "id": "holdout-dependencies-025",
  "family": "dependencies",
  "description": "DAG aleatorio disperso",
  "input": {
    "nodes": 14,
    "edges": [
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
        12
      ],
      [
        1,
        5
      ],
      [
        1,
        12
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
        3,
        10
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
        6,
        5
      ],
      [
        7,
        8
      ],
      [
        10,
        6
      ],
      [
        10,
        12
      ],
      [
        13,
        5
      ],
      [
        13,
        7
      ]
    ]
  }
}
```

### operations

```json
7215
```

### elapsedNanos

```json
358960
```

### adapted

```json
false
```

### reused

```json
true
```
