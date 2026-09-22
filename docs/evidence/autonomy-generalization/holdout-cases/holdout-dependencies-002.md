# holdout-dependencies-002

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-002

### identifier

```json
"holdout-dependencies-002"
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
  "fullJournalSha256": "db7f06cb589a026e901ad503463156511f67c45ef52a9d52753b355589cc7db8",
  "familyState": {
    "schema": 1,
    "revision": 455,
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
      },
      {
        "nodes": 4,
        "edges": [
          [
            1,
            1
          ],
          [
            2,
            0
          ]
        ]
      },
      {
        "nodes": 12,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            9
          ],
          [
            0,
            11
          ],
          [
            1,
            10
          ],
          [
            2,
            4
          ],
          [
            3,
            5
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
            9
          ],
          [
            6,
            7
          ],
          [
            6,
            10
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
      },
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
        2,
        8,
        1,
        4,
        7,
        5,
        0,
        6,
        3
      ],
      "layers": [
        [
          2
        ],
        [
          8
        ],
        [
          1,
          4
        ],
        [
          7
        ],
        [
          5
        ],
        [
          0,
          6
        ],
        [
          3
        ]
      ]
    },
    "operations": 5719,
    "elapsedNanos": 165914
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    2,
    8,
    1,
    4,
    7,
    5,
    0,
    6,
    3
  ],
  "layers": [
    [
      2
    ],
    [
      8
    ],
    [
      1,
      4
    ],
    [
      7
    ],
    [
      5
    ],
    [
      0,
      6
    ],
    [
      3
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
    "id": "holdout-dependencies-002",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 7
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
  "fullJournalSha256": "f1ffa1042e389063eaae273ca3391c30fe5c21a57f2936be42597b262ddedc25",
  "familyState": {
    "schema": 1,
    "revision": 456,
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
            1,
            1
          ],
          [
            2,
            0
          ]
        ]
      },
      {
        "nodes": 12,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            9
          ],
          [
            0,
            11
          ],
          [
            1,
            10
          ],
          [
            2,
            4
          ],
          [
            3,
            5
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
            9
          ],
          [
            6,
            7
          ],
          [
            6,
            10
          ],
          [
            8,
            11
          ]
        ]
      },
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
      }
    ],
    "receipts": [
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
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-002",
  "family": "dependencies",
  "description": "DAG aleatorio denso con etiquetas permutadas",
  "input": {
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
  }
}
```

### operations

```json
5719
```

### elapsedNanos

```json
676629
```

### adapted

```json
false
```

### reused

```json
true
```
