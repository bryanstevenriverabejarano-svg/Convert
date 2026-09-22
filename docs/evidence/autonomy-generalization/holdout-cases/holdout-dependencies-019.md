# holdout-dependencies-019

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-019

### identifier

```json
"holdout-dependencies-019"
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
  "fullJournalSha256": "dca14fd0bbb9b6f7f5b9716ee1fb36c80bf6f6d2fd1aa76537d76c29da5c7251",
  "familyState": {
    "schema": 1,
    "revision": 528,
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
            0,
            3
          ],
          [
            1,
            0
          ],
          [
            3,
            1
          ]
        ]
      },
      {
        "nodes": 6,
        "edges": [
          [
            1,
            5
          ],
          [
            2,
            4
          ],
          [
            3,
            1
          ],
          [
            3,
            4
          ],
          [
            5,
            3
          ],
          [
            5,
            4
          ]
        ]
      },
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
      }
    ],
    "receipts": [
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
        3,
        5,
        6,
        0
      ]
    },
    "operations": 3046,
    "elapsedNanos": 89422
  }
]
```

### result

```json
{
  "status": "cycle",
  "cycle": [
    0,
    3,
    5,
    6,
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
    "id": "holdout-dependencies-019",
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
  "fullJournalSha256": "d1547375e0b5244362a5d46e3e999508b494428b6e1301b429273753924a25c9",
  "familyState": {
    "schema": 1,
    "revision": 529,
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
            1,
            5
          ],
          [
            2,
            4
          ],
          [
            3,
            1
          ],
          [
            3,
            4
          ],
          [
            5,
            3
          ],
          [
            5,
            4
          ]
        ]
      },
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
      }
    ],
    "receipts": [
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-019",
  "family": "dependencies",
  "description": "Dependencias aleatorias con un ciclo impuesto de longitud variable",
  "input": {
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
  }
}
```

### operations

```json
3046
```

### elapsedNanos

```json
308886
```

### adapted

```json
false
```

### reused

```json
true
```
