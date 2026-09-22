# holdout-dependencies-003

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-003

### identifier

```json
"holdout-dependencies-003"
```

### objective

```json
"Dependencias aleatorias con un ciclo impuesto de longitud variable"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "a6fbd6974626863a56fb05b0a867a2b302c0340332c8cf6bc76b044be12b29e1",
  "familyState": {
    "schema": 1,
    "revision": 509,
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
      },
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
      }
    ],
    "receipts": [
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
      },
      {
        "id": "holdout-dependencies-030",
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
      "status": "cycle",
      "cycle": [
        0,
        1,
        2,
        4,
        0
      ]
    },
    "operations": 5597,
    "elapsedNanos": 174787
  }
]
```

### result

```json
{
  "status": "cycle",
  "cycle": [
    0,
    1,
    2,
    4,
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
    "id": "holdout-dependencies-003",
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
  "fullJournalSha256": "b0fae5fa8bf304833b565e41d19ef4a690cb4c034f84b5ac3594e9d7e61d8fbe",
  "familyState": {
    "schema": 1,
    "revision": 510,
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
      },
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
      }
    ],
    "receipts": [
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-003",
  "family": "dependencies",
  "description": "Dependencias aleatorias con un ciclo impuesto de longitud variable",
  "input": {
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
  }
}
```

### operations

```json
5597
```

### elapsedNanos

```json
400681
```

### adapted

```json
false
```

### reused

```json
true
```
