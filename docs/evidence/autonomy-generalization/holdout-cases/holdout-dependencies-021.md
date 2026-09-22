# holdout-dependencies-021

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-021

### identifier

```json
"holdout-dependencies-021"
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
  "fullJournalSha256": "40e918e934b503e6285d9229bae44006fba70e52fc78fb56b2c0ff387f5e71d3",
  "familyState": {
    "schema": 1,
    "revision": 538,
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
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            6
          ],
          [
            1,
            2
          ],
          [
            2,
            0
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
            5,
            0
          ],
          [
            6,
            4
          ]
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-dependencies-015",
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
        4,
        7,
        8,
        9,
        11,
        12,
        1,
        10,
        0,
        5,
        3,
        2,
        6
      ],
      "layers": [
        [
          4,
          7,
          8,
          9,
          11,
          12
        ],
        [
          1,
          10
        ],
        [
          0,
          5
        ],
        [
          3
        ],
        [
          2,
          6
        ]
      ]
    },
    "operations": 8075,
    "elapsedNanos": 276468
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    4,
    7,
    8,
    9,
    11,
    12,
    1,
    10,
    0,
    5,
    3,
    2,
    6
  ],
  "layers": [
    [
      4,
      7,
      8,
      9,
      11,
      12
    ],
    [
      1,
      10
    ],
    [
      0,
      5
    ],
    [
      3
    ],
    [
      2,
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
    "id": "holdout-dependencies-021",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 5
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
  "fullJournalSha256": "b54497f155c5478fbb4e1b784f758f1a4e8146212ec6f72bb44989d39397f60d",
  "familyState": {
    "schema": 1,
    "revision": 539,
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
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            6
          ],
          [
            1,
            2
          ],
          [
            2,
            0
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
            5,
            0
          ],
          [
            6,
            4
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
            5
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
            4,
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
            7,
            1
          ],
          [
            8,
            6
          ],
          [
            8,
            10
          ]
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-dependencies-015",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "holdout-dependencies-021",
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
  "id": "holdout-dependencies-021",
  "family": "dependencies",
  "description": "DAG aleatorio disperso",
  "input": {
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
        5
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
        4,
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
        7,
        1
      ],
      [
        8,
        6
      ],
      [
        8,
        10
      ]
    ]
  }
}
```

### operations

```json
8075
```

### elapsedNanos

```json
505737
```

### adapted

```json
false
```

### reused

```json
true
```
