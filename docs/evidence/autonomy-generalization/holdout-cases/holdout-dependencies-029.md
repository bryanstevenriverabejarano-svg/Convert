# holdout-dependencies-029

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-029

### identifier

```json
"holdout-dependencies-029"
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
  "fullJournalSha256": "6083407e33877548c466fa5a57813959b987f5e1d2647604c668e119f6d2a3ea",
  "familyState": {
    "schema": 1,
    "revision": 430,
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
        "nodes": 13,
        "edges": [
          [
            0,
            1
          ],
          [
            1,
            2
          ],
          [
            2,
            3
          ],
          [
            3,
            4
          ],
          [
            4,
            5
          ],
          [
            5,
            6
          ],
          [
            6,
            7
          ],
          [
            7,
            8
          ],
          [
            8,
            9
          ],
          [
            9,
            10
          ],
          [
            10,
            11
          ],
          [
            11,
            0
          ],
          [
            0,
            4
          ],
          [
            4,
            8
          ],
          [
            2,
            7
          ],
          [
            12,
            0
          ]
        ]
      },
      {
        "nodes": 9,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            4
          ],
          [
            1,
            3
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
            3,
            0
          ],
          [
            3,
            2
          ],
          [
            3,
            5
          ],
          [
            4,
            0
          ],
          [
            4,
            6
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
            6,
            0
          ],
          [
            6,
            5
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
            8,
            7
          ]
        ]
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            0
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
            5
          ],
          [
            2,
            6
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
            7
          ],
          [
            5,
            1
          ],
          [
            5,
            11
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
            11
          ],
          [
            8,
            12
          ],
          [
            9,
            12
          ],
          [
            10,
            6
          ],
          [
            10,
            8
          ],
          [
            10,
            9
          ],
          [
            10,
            11
          ],
          [
            11,
            3
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-017-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":12,\"candidatos_descartados_en_recibos\":0}",
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
        0,
        5,
        1,
        2,
        3
      ],
      "layers": [
        [
          4
        ],
        [
          0
        ],
        [
          5
        ],
        [
          1,
          2,
          3
        ]
      ]
    },
    "operations": 6555,
    "elapsedNanos": 217741
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    4,
    0,
    5,
    1,
    2,
    3
  ],
  "layers": [
    [
      4
    ],
    [
      0
    ],
    [
      5
    ],
    [
      1,
      2,
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
    "id": "holdout-dependencies-029",
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
  "fullJournalSha256": "88c105e0a5723850dfd2643ac21f42fe3512d6e510a7ae7bb4dea3f5098bb34b",
  "familyState": {
    "schema": 1,
    "revision": 431,
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
            1
          ],
          [
            0,
            4
          ],
          [
            1,
            3
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
            3,
            0
          ],
          [
            3,
            2
          ],
          [
            3,
            5
          ],
          [
            4,
            0
          ],
          [
            4,
            6
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
            6,
            0
          ],
          [
            6,
            5
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
            8,
            7
          ]
        ]
      },
      {
        "nodes": 13,
        "edges": [
          [
            0,
            0
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
            5
          ],
          [
            2,
            6
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
            7
          ],
          [
            5,
            1
          ],
          [
            5,
            11
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
            11
          ],
          [
            8,
            12
          ],
          [
            9,
            12
          ],
          [
            10,
            6
          ],
          [
            10,
            8
          ],
          [
            10,
            9
          ],
          [
            10,
            11
          ],
          [
            11,
            3
          ]
        ]
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1
          ],
          [
            0,
            5
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
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-018-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-029",
  "family": "dependencies",
  "description": "DAG aleatorio disperso",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1
      ],
      [
        0,
        5
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
      ]
    ]
  }
}
```

### operations

```json
6555
```

### elapsedNanos

```json
416825
```

### adapted

```json
false
```

### reused

```json
true
```
