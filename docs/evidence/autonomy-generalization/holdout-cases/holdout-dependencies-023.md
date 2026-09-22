# holdout-dependencies-023

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## holdout-dependencies-023

### identifier

```json
"holdout-dependencies-023"
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
  "fullJournalSha256": "0d9b6420f360d2567274a00711a98c07c9924e384820e1b4c495c0b3ccf7f076",
  "familyState": {
    "schema": 1,
    "revision": 435,
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
      },
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
      }
    ],
    "receipts": [
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":10,\"candidatos_descartados_en_recibos\":0}",
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
        2,
        1,
        9,
        0
      ]
    },
    "operations": 5065,
    "elapsedNanos": 172144
  }
]
```

### result

```json
{
  "status": "cycle",
  "cycle": [
    0,
    2,
    1,
    9,
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
    "id": "holdout-dependencies-023",
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
  "fullJournalSha256": "4c80f990c4d9d22f75bfd0ce4e3a0285e8c2d9dd47fd66acb03c709a81374120",
  "familyState": {
    "schema": 1,
    "revision": 436,
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

### challenge

```json
{
  "schema": 1,
  "id": "holdout-dependencies-023",
  "family": "dependencies",
  "description": "Dependencias aleatorias con un ciclo impuesto de longitud variable",
  "input": {
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
}
```

### operations

```json
5065
```

### elapsedNanos

```json
385880
```

### adapted

```json
false
```

### reused

```json
true
```
