# holdout-dependencies-009

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-009

### identifier

```json
"holdout-dependencies-009"
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
  "fullJournalSha256": "707455652d46a5210fd5af1b7e4486a2828a498265510e0b214bd3e66a3c8312",
  "familyState": {
    "schema": 1,
    "revision": 437,
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
            0
          ],
          [
            3,
            0
          ],
          [
            5,
            0
          ],
          [
            6,
            0
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
            0
          ],
          [
            7,
            4
          ],
          [
            8,
            4
          ]
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "dependencies-024-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-026-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":9,\"candidatos_descartados_en_recibos\":0}",
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
        2,
        6,
        8,
        1,
        3,
        7,
        11,
        0,
        4,
        5,
        9,
        10
      ],
      "layers": [
        [
          2,
          6,
          8
        ],
        [
          1,
          3,
          7,
          11
        ],
        [
          0,
          4,
          5
        ],
        [
          9,
          10
        ]
      ]
    },
    "operations": 6983,
    "elapsedNanos": 195869
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    2,
    6,
    8,
    1,
    3,
    7,
    11,
    0,
    4,
    5,
    9,
    10
  ],
  "layers": [
    [
      2,
      6,
      8
    ],
    [
      1,
      3,
      7,
      11
    ],
    [
      0,
      4,
      5
    ],
    [
      9,
      10
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
    "id": "holdout-dependencies-009",
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
  "fullJournalSha256": "a8e45e3891ed8d3271a66facd19f5694cd1c3fd99a769ded70d761e2abe97aaa",
  "familyState": {
    "schema": 1,
    "revision": 438,
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
        "id": "dependencies-025-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-026-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-009",
  "family": "dependencies",
  "description": "DAG aleatorio disperso",
  "input": {
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
}
```

### operations

```json
6983
```

### elapsedNanos

```json
398127
```

### adapted

```json
false
```

### reused

```json
true
```
