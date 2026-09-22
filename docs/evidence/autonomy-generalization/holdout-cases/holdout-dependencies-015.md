# holdout-dependencies-015

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-015

### identifier

```json
"holdout-dependencies-015"
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
  "fullJournalSha256": "b8dc66cd7e9876fdcf0975d2a71defc2ef8584b31cb36960271072e4a5827a11",
  "familyState": {
    "schema": 1,
    "revision": 536,
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
      "status": "cycle",
      "cycle": [
        0,
        6,
        4,
        2,
        0
      ]
    },
    "operations": 5679,
    "elapsedNanos": 174728
  }
]
```

### result

```json
{
  "status": "cycle",
  "cycle": [
    0,
    6,
    4,
    2,
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
    "id": "holdout-dependencies-015",
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
  "fullJournalSha256": "a9d07a552bb89439dbe48bd6c7d77e4bc01a055dd5b48ce1408e563971d1722b",
  "familyState": {
    "schema": 1,
    "revision": 537,
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-015",
  "family": "dependencies",
  "description": "Dependencias aleatorias con un ciclo impuesto de longitud variable",
  "input": {
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
}
```

### operations

```json
5679
```

### elapsedNanos

```json
402644
```

### adapted

```json
false
```

### reused

```json
true
```
