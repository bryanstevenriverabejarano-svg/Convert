# holdout-dependencies-017

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-017

### identifier

```json
"holdout-dependencies-017"
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
  "fullJournalSha256": "1579e7af5922862accdc5704db4b1f191b865cb0ed8bce1fc6d5007ec8b53082",
  "familyState": {
    "schema": 1,
    "revision": 519,
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
            4
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
            5
          ],
          [
            3,
            1
          ],
          [
            5,
            1
          ],
          [
            5,
            4
          ],
          [
            6,
            3
          ]
        ]
      },
      {
        "nodes": 5,
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
            2
          ],
          [
            1,
            4
          ],
          [
            2,
            4
          ],
          [
            3,
            0
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
          ]
        ]
      },
      {
        "nodes": 9,
        "edges": [
          [
            3,
            0
          ],
          [
            4,
            1
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
            8
          ],
          [
            6,
            8
          ],
          [
            7,
            4
          ],
          [
            7,
            7
          ],
          [
            7,
            8
          ],
          [
            8,
            1
          ],
          [
            8,
            4
          ]
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-dependencies-030",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
        0,
        2,
        3,
        5,
        4,
        6,
        1,
        7,
        8
      ],
      "layers": [
        [
          0,
          2,
          3,
          5
        ],
        [
          4,
          6
        ],
        [
          1,
          7,
          8
        ]
      ]
    },
    "operations": 2998,
    "elapsedNanos": 111455
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
    5,
    4,
    6,
    1,
    7,
    8
  ],
  "layers": [
    [
      0,
      2,
      3,
      5
    ],
    [
      4,
      6
    ],
    [
      1,
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
    "id": "holdout-dependencies-017",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 3
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
  "fullJournalSha256": "3d7ee7775c8aea6305ccc89b84928149ee07c4017bac040906db34d32ec35643",
  "familyState": {
    "schema": 1,
    "revision": 520,
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
        "nodes": 5,
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
            2
          ],
          [
            1,
            4
          ],
          [
            2,
            4
          ],
          [
            3,
            0
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
          ]
        ]
      },
      {
        "nodes": 9,
        "edges": [
          [
            3,
            0
          ],
          [
            4,
            1
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
            8
          ],
          [
            6,
            8
          ],
          [
            7,
            4
          ],
          [
            7,
            7
          ],
          [
            7,
            8
          ],
          [
            8,
            1
          ],
          [
            8,
            4
          ]
        ]
      },
      {
        "nodes": 9,
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
            2,
            8
          ],
          [
            5,
            1
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
          ]
        ]
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-dependencies-030",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-017",
  "family": "dependencies",
  "description": "DAG aleatorio disperso",
  "input": {
    "nodes": 9,
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
        2,
        8
      ],
      [
        5,
        1
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
      ]
    ]
  }
}
```

### operations

```json
2998
```

### elapsedNanos

```json
324279
```

### adapted

```json
false
```

### reused

```json
true
```
