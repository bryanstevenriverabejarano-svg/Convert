# holdout-dependencies-008

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-008

### identifier

```json
"holdout-dependencies-008"
```

### objective

```json
"Autodependencia insertada en una red aleatoria"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "3abfd01783c685dd9a3b4aae2b8c6c7ab35b8d34ba84a2a8744f510a93f31b85",
  "familyState": {
    "schema": 1,
    "revision": 428,
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
            5
          ],
          [
            5,
            2
          ],
          [
            2,
            7
          ],
          [
            1,
            6
          ],
          [
            6,
            3
          ],
          [
            3,
            8
          ],
          [
            7,
            8
          ],
          [
            9,
            0
          ]
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "dependencies-015-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":13,\"candidatos_descartados_en_recibos\":0}",
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
      "status": "cycle",
      "cycle": [
        0,
        0
      ]
    },
    "operations": 7593,
    "elapsedNanos": 183371
  }
]
```

### result

```json
{
  "status": "cycle",
  "cycle": [
    0,
    0
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
    "id": "holdout-dependencies-008",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y testigo de ciclo",
      "acyclic": false
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
  "fullJournalSha256": "c96a41a77d2e63eb6628cf46897e60d053f3ac4fd367dd7a3695c229ebf9177d",
  "familyState": {
    "schema": 1,
    "revision": 429,
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
        "id": "dependencies-016-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-008",
  "family": "dependencies",
  "description": "Autodependencia insertada en una red aleatoria",
  "input": {
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
}
```

### operations

```json
7593
```

### elapsedNanos

```json
429705
```

### adapted

```json
false
```

### reused

```json
true
```
