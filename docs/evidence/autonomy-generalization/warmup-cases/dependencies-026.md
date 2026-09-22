# dependencies-026

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## dependencies-026-r1

### identifier

```json
"dependencies-026-r1"
```

### objective

```json
"Ciclo largo con cuerdas"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "483db1b026cd2ad8fe79e540ce2c9bc5ca3558219be42d6948256a3b7fd6eaf1",
  "familyState": {
    "schema": 1,
    "revision": 103,
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
            2
          ],
          [
            1,
            3
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
            3,
            5
          ],
          [
            4,
            6
          ],
          [
            5,
            6
          ]
        ]
      },
      {
        "nodes": 14,
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
            4
          ],
          [
            0,
            5
          ],
          [
            0,
            6
          ],
          [
            0,
            7
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
            0,
            11
          ],
          [
            0,
            12
          ],
          [
            1,
            13
          ],
          [
            2,
            13
          ],
          [
            3,
            13
          ],
          [
            4,
            13
          ],
          [
            5,
            13
          ],
          [
            6,
            13
          ],
          [
            7,
            13
          ],
          [
            8,
            13
          ],
          [
            9,
            13
          ],
          [
            10,
            13
          ],
          [
            11,
            13
          ],
          [
            12,
            13
          ]
        ]
      },
      {
        "nodes": 9,
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
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-008-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 2
      },
      {
        "id": "dependencies-009-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-015-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-017-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-024-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":18,\"candidatos_descartados_en_recibos\":1}",
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
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        0
      ]
    },
    "operations": 7392,
    "elapsedNanos": 756206
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
    3,
    4,
    5,
    6,
    7,
    8,
    9,
    10,
    11,
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
    "id": "dependencies-026-r1",
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
  "fullJournalSha256": "024e965b1b51e49357acfd76edbd4ebbd51589825304fab2b538e9704fcc7535",
  "familyState": {
    "schema": 1,
    "revision": 104,
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
            4
          ],
          [
            0,
            5
          ],
          [
            0,
            6
          ],
          [
            0,
            7
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
            0,
            11
          ],
          [
            0,
            12
          ],
          [
            1,
            13
          ],
          [
            2,
            13
          ],
          [
            3,
            13
          ],
          [
            4,
            13
          ],
          [
            5,
            13
          ],
          [
            6,
            13
          ],
          [
            7,
            13
          ],
          [
            8,
            13
          ],
          [
            9,
            13
          ],
          [
            10,
            13
          ],
          [
            11,
            13
          ],
          [
            12,
            13
          ]
        ]
      },
      {
        "nodes": 9,
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
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-008-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 2
      },
      {
        "id": "dependencies-009-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-015-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-017-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-024-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r1",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-026-r1",
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
  "id": "dependencies-026-r1",
  "caseId": "dependencies-026",
  "round": 1,
  "family": "dependencies",
  "description": "Ciclo largo con cuerdas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 12,
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
      ]
    ]
  }
}
```

### operations

```json
7392
```

### elapsedNanos

```json
1170087
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-026-r2

### identifier

```json
"dependencies-026-r2"
```

### objective

```json
"Ciclo largo con cuerdas"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "35cb685a8e42a5f53c0691b970e36d4819f236ce79a775b083a2d8d3f8bc6a2c",
  "familyState": {
    "schema": 1,
    "revision": 207,
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
            2
          ],
          [
            1,
            3
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
            3,
            5
          ],
          [
            4,
            6
          ],
          [
            5,
            6
          ],
          [
            1,
            0
          ]
        ]
      },
      {
        "nodes": 14,
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
            4
          ],
          [
            0,
            5
          ],
          [
            0,
            6
          ],
          [
            0,
            7
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
            0,
            11
          ],
          [
            0,
            12
          ],
          [
            1,
            13
          ],
          [
            2,
            13
          ],
          [
            3,
            13
          ],
          [
            4,
            13
          ],
          [
            5,
            13
          ],
          [
            6,
            13
          ],
          [
            7,
            13
          ],
          [
            8,
            13
          ],
          [
            9,
            13
          ],
          [
            10,
            13
          ],
          [
            11,
            13
          ],
          [
            12,
            13
          ],
          [
            1,
            0
          ]
        ]
      },
      {
        "nodes": 9,
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
            5,
            0
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-005-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-006-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-008-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-009-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-015-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-017-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-024-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
        0
      ]
    },
    "operations": 6504,
    "elapsedNanos": 281476
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
    "id": "dependencies-026-r2",
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
  "fullJournalSha256": "d98249f773320e5bc1dd84efd2d9692e1182be0e3000e6c5fbdb6357888ef0ce",
  "familyState": {
    "schema": 1,
    "revision": 208,
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
            4
          ],
          [
            0,
            5
          ],
          [
            0,
            6
          ],
          [
            0,
            7
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
            0,
            11
          ],
          [
            0,
            12
          ],
          [
            1,
            13
          ],
          [
            2,
            13
          ],
          [
            3,
            13
          ],
          [
            4,
            13
          ],
          [
            5,
            13
          ],
          [
            6,
            13
          ],
          [
            7,
            13
          ],
          [
            8,
            13
          ],
          [
            9,
            13
          ],
          [
            10,
            13
          ],
          [
            11,
            13
          ],
          [
            12,
            13
          ],
          [
            1,
            0
          ]
        ]
      },
      {
        "nodes": 9,
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
            5,
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
            1,
            0
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-006-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-008-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-009-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-015-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-017-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-024-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-026-r2",
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
  "id": "dependencies-026-r2",
  "caseId": "dependencies-026",
  "round": 2,
  "family": "dependencies",
  "description": "Ciclo largo con cuerdas",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
    "nodes": 12,
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
        1,
        0
      ]
    ]
  }
}
```

### operations

```json
6504
```

### elapsedNanos

```json
629128
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-026-r3

### identifier

```json
"dependencies-026-r3"
```

### objective

```json
"Ciclo largo con cuerdas"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "3b16f29f5161612a189b3dd1ea35c6a765aba93b6ff157281f6124ad15e66aa7",
  "familyState": {
    "schema": 1,
    "revision": 311,
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
            2
          ],
          [
            1,
            3
          ],
          [
            2,
            3
          ],
          [
            3,
            5
          ],
          [
            4,
            6
          ],
          [
            5,
            6
          ]
        ]
      },
      {
        "nodes": 14,
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
            4
          ],
          [
            0,
            5
          ],
          [
            0,
            6
          ],
          [
            0,
            7
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
            0,
            11
          ],
          [
            0,
            12
          ],
          [
            2,
            13
          ],
          [
            3,
            13
          ],
          [
            4,
            13
          ],
          [
            5,
            13
          ],
          [
            6,
            13
          ],
          [
            7,
            13
          ],
          [
            8,
            13
          ],
          [
            9,
            13
          ],
          [
            10,
            13
          ],
          [
            11,
            13
          ],
          [
            12,
            13
          ]
        ]
      },
      {
        "nodes": 9,
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
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-005-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-006-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-008-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-009-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-015-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-017-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-024-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
        3,
        4,
        8,
        9,
        10,
        11,
        0
      ]
    },
    "operations": 7385,
    "elapsedNanos": 348174
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
    3,
    4,
    8,
    9,
    10,
    11,
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
    "id": "dependencies-026-r3",
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
  "fullJournalSha256": "d3060a5baedb8194e35df4359c48197929ca55284ea49cd650bad4aaf409240c",
  "familyState": {
    "schema": 1,
    "revision": 312,
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
            4
          ],
          [
            0,
            5
          ],
          [
            0,
            6
          ],
          [
            0,
            7
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
            0,
            11
          ],
          [
            0,
            12
          ],
          [
            2,
            13
          ],
          [
            3,
            13
          ],
          [
            4,
            13
          ],
          [
            5,
            13
          ],
          [
            6,
            13
          ],
          [
            7,
            13
          ],
          [
            8,
            13
          ],
          [
            9,
            13
          ],
          [
            10,
            13
          ],
          [
            11,
            13
          ],
          [
            12,
            13
          ]
        ]
      },
      {
        "nodes": 9,
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
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-006-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-008-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-009-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-015-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-016-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-017-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-018-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-019-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-020-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-021-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-022-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-023-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-024-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-025-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-026-r3",
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
  "id": "dependencies-026-r3",
  "caseId": "dependencies-026",
  "round": 3,
  "family": "dependencies",
  "description": "Ciclo largo con cuerdas",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
    "nodes": 12,
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
      ]
    ]
  }
}
```

### operations

```json
7385
```

### elapsedNanos

```json
792149
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-026-r4

### identifier

```json
"dependencies-026-r4"
```

### objective

```json
"Ciclo largo con cuerdas"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "8958fd797eeb59594aa698f189ef72320c6eb48921b63ebc15984b3e67ace96f",
  "familyState": {
    "schema": 1,
    "revision": 415,
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
        "nodes": 8,
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
            1,
            3
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
            3,
            5
          ],
          [
            4,
            6
          ],
          [
            5,
            6
          ],
          [
            7,
            0
          ]
        ]
      },
      {
        "nodes": 14,
        "edges": [
          [
            1,
            0
          ],
          [
            2,
            0
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
            5,
            0
          ],
          [
            6,
            0
          ],
          [
            7,
            0
          ],
          [
            8,
            0
          ],
          [
            9,
            0
          ],
          [
            10,
            0
          ],
          [
            11,
            0
          ],
          [
            12,
            0
          ],
          [
            13,
            1
          ],
          [
            13,
            2
          ],
          [
            13,
            3
          ],
          [
            13,
            4
          ],
          [
            13,
            5
          ],
          [
            13,
            6
          ],
          [
            13,
            7
          ],
          [
            13,
            8
          ],
          [
            13,
            9
          ],
          [
            13,
            10
          ],
          [
            13,
            11
          ],
          [
            13,
            12
          ]
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "dependencies-005-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-006-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-008-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-009-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":21,\"candidatos_descartados_en_recibos\":0}",
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
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        0
      ]
    },
    "operations": 8514,
    "elapsedNanos": 326672
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
    3,
    4,
    5,
    6,
    7,
    8,
    9,
    10,
    11,
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
    "id": "dependencies-026-r4",
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
  "fullJournalSha256": "860eb49cd85027211a63940f8e4e835d149597c4e9eecfeec7d4ae91a590edb2",
  "familyState": {
    "schema": 1,
    "revision": 416,
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
            1,
            0
          ],
          [
            2,
            0
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
            5,
            0
          ],
          [
            6,
            0
          ],
          [
            7,
            0
          ],
          [
            8,
            0
          ],
          [
            9,
            0
          ],
          [
            10,
            0
          ],
          [
            11,
            0
          ],
          [
            12,
            0
          ],
          [
            13,
            1
          ],
          [
            13,
            2
          ],
          [
            13,
            3
          ],
          [
            13,
            4
          ],
          [
            13,
            5
          ],
          [
            13,
            6
          ],
          [
            13,
            7
          ],
          [
            13,
            8
          ],
          [
            13,
            9
          ],
          [
            13,
            10
          ],
          [
            13,
            11
          ],
          [
            13,
            12
          ]
        ]
      },
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
      }
    ],
    "receipts": [
      {
        "id": "dependencies-006-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-008-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-009-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-010-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-011-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-012-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-013-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-014-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-026-r4",
  "caseId": "dependencies-026",
  "round": 4,
  "family": "dependencies",
  "description": "Ciclo largo con cuerdas",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
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
  }
}
```

### operations

```json
8514
```

### elapsedNanos

```json
722206
```

### adapted

```json
false
```

### reused

```json
true
```
