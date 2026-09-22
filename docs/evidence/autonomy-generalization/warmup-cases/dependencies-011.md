# dependencies-011

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## dependencies-011-r1

### identifier

```json
"dependencies-011-r1"
```

### objective

```json
"DAG completo"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "0c00504ef39152b17434ff2c067962d6afb0095ee9f1d619e5cd7ff793b3d580",
  "familyState": {
    "schema": 1,
    "revision": 88,
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
            0
          ],
          [
            2,
            1
          ],
          [
            3,
            2
          ],
          [
            4,
            3
          ],
          [
            5,
            4
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
            0,
            2
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
            2,
            5
          ],
          [
            2,
            6
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
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ]
        ]
      },
      {
        "nodes": 10,
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
            1
          ],
          [
            4,
            1
          ],
          [
            5,
            2
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-004-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-005-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-006-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":3,\"candidatos_descartados_en_recibos\":1}",
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
        1,
        2,
        3,
        4,
        5
      ],
      "layers": [
        [
          0
        ],
        [
          1
        ],
        [
          2
        ],
        [
          3
        ],
        [
          4
        ],
        [
          5
        ]
      ]
    },
    "operations": 5275,
    "elapsedNanos": 633295
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0,
    1,
    2,
    3,
    4,
    5
  ],
  "layers": [
    [
      0
    ],
    [
      1
    ],
    [
      2
    ],
    [
      3
    ],
    [
      4
    ],
    [
      5
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
    "id": "dependencies-011-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 6
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
  "fullJournalSha256": "d151e100ec0d974615adf22f3dedc22dde37bfd1138d4043b85fa789562a1f0e",
  "familyState": {
    "schema": 1,
    "revision": 89,
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
            0,
            2
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
            2,
            5
          ],
          [
            2,
            6
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
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ]
        ]
      },
      {
        "nodes": 10,
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
            1
          ],
          [
            4,
            1
          ],
          [
            5,
            2
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
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
            1,
            2
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
            1,
            5
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
            4
          ],
          [
            3,
            5
          ],
          [
            4,
            5
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-005-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-006-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-007-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-011-r1",
  "caseId": "dependencies-011",
  "round": 1,
  "family": "dependencies",
  "description": "DAG completo",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 6,
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
        1,
        2
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
        1,
        5
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
        4
      ],
      [
        3,
        5
      ],
      [
        4,
        5
      ]
    ]
  }
}
```

### operations

```json
5275
```

### elapsedNanos

```json
1107675
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-011-r2

### identifier

```json
"dependencies-011-r2"
```

### objective

```json
"DAG completo"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "17011447708ca80d3f56085f76158f297c83c386abff0e203dcdfccb60e957e5",
  "familyState": {
    "schema": 1,
    "revision": 192,
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
            0
          ],
          [
            2,
            1
          ],
          [
            3,
            2
          ],
          [
            4,
            3
          ],
          [
            5,
            4
          ],
          [
            0,
            1
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
            0,
            2
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
            2,
            5
          ],
          [
            2,
            6
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
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ],
          [
            1,
            0
          ]
        ]
      },
      {
        "nodes": 10,
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
            1
          ],
          [
            4,
            1
          ],
          [
            5,
            2
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
          ],
          [
            0,
            1
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-001-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-002-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-003-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-004-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
        1,
        0
      ]
    },
    "operations": 4486,
    "elapsedNanos": 319922
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
    "id": "dependencies-011-r2",
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
  "fullJournalSha256": "0817b66f100f4fb2a6e5f1bc530018961276c89a75dda8db93c8d144666c8a6a",
  "familyState": {
    "schema": 1,
    "revision": 193,
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
            0,
            2
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
            2,
            5
          ],
          [
            2,
            6
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
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ],
          [
            1,
            0
          ]
        ]
      },
      {
        "nodes": 10,
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
            1
          ],
          [
            4,
            1
          ],
          [
            5,
            2
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
          ],
          [
            0,
            1
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
            1,
            2
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
            1,
            5
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
            4
          ],
          [
            3,
            5
          ],
          [
            4,
            5
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
        "id": "dependencies-001-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-002-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-003-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-004-r2",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-011-r2",
  "caseId": "dependencies-011",
  "round": 2,
  "family": "dependencies",
  "description": "DAG completo",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
    "nodes": 6,
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
        1,
        2
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
        1,
        5
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
        4
      ],
      [
        3,
        5
      ],
      [
        4,
        5
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
4486
```

### elapsedNanos

```json
688176
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-011-r3

### identifier

```json
"dependencies-011-r3"
```

### objective

```json
"DAG completo"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "4dd29279e51b61d272984e995492726897c332be074c334056ae49ca34bec26a",
  "familyState": {
    "schema": 1,
    "revision": 296,
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
            0
          ],
          [
            2,
            1
          ],
          [
            4,
            3
          ],
          [
            5,
            4
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
            0,
            2
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
            2,
            5
          ],
          [
            2,
            6
          ],
          [
            3,
            8
          ],
          [
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ]
        ]
      },
      {
        "nodes": 10,
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
            1
          ],
          [
            4,
            1
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-001-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-002-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-003-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-004-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      "status": "ok",
      "order": [
        0,
        1,
        2,
        3,
        4,
        5
      ],
      "layers": [
        [
          0
        ],
        [
          1
        ],
        [
          2
        ],
        [
          3
        ],
        [
          4
        ],
        [
          5
        ]
      ]
    },
    "operations": 5232,
    "elapsedNanos": 122480
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0,
    1,
    2,
    3,
    4,
    5
  ],
  "layers": [
    [
      0
    ],
    [
      1
    ],
    [
      2
    ],
    [
      3
    ],
    [
      4
    ],
    [
      5
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
    "id": "dependencies-011-r3",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 6
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
  "fullJournalSha256": "a315f5ceff1404ac2dbf5ed5fae7c32555f357670852b5acf1bccb79f0fbc933",
  "familyState": {
    "schema": 1,
    "revision": 297,
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
            0,
            2
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
            2,
            5
          ],
          [
            2,
            6
          ],
          [
            3,
            8
          ],
          [
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ]
        ]
      },
      {
        "nodes": 10,
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
            1
          ],
          [
            4,
            1
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
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
            1,
            2
          ],
          [
            1,
            3
          ],
          [
            1,
            5
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
            4
          ],
          [
            3,
            5
          ],
          [
            4,
            5
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-001-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-002-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-003-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-004-r3",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-011-r3",
  "caseId": "dependencies-011",
  "round": 3,
  "family": "dependencies",
  "description": "DAG completo",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
    "nodes": 6,
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
        1,
        2
      ],
      [
        1,
        3
      ],
      [
        1,
        5
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
        4
      ],
      [
        3,
        5
      ],
      [
        4,
        5
      ]
    ]
  }
}
```

### operations

```json
5232
```

### elapsedNanos

```json
368344
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-011-r4

### identifier

```json
"dependencies-011-r4"
```

### objective

```json
"DAG completo"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "c8694ef10f3e7ffe18909438a8ec44c4add4dda573424133d0b5c28de5d927cc",
  "familyState": {
    "schema": 1,
    "revision": 400,
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
            1,
            0
          ],
          [
            2,
            1
          ],
          [
            3,
            2
          ],
          [
            4,
            3
          ],
          [
            5,
            4
          ],
          [
            6,
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
            1,
            3
          ],
          [
            1,
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
            7
          ],
          [
            3,
            8
          ],
          [
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ],
          [
            13,
            0
          ]
        ]
      },
      {
        "nodes": 11,
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
            1
          ],
          [
            4,
            1
          ],
          [
            5,
            2
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
          ],
          [
            10,
            0
          ]
        ]
      }
    ],
    "receipts": [
      {
        "id": "dependencies-001-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-002-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-003-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-004-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      "status": "ok",
      "order": [
        6,
        0,
        1,
        2,
        3,
        4,
        5
      ],
      "layers": [
        [
          6
        ],
        [
          0
        ],
        [
          1
        ],
        [
          2
        ],
        [
          3
        ],
        [
          4
        ],
        [
          5
        ]
      ]
    },
    "operations": 6712,
    "elapsedNanos": 315175
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    6,
    0,
    1,
    2,
    3,
    4,
    5
  ],
  "layers": [
    [
      6
    ],
    [
      0
    ],
    [
      1
    ],
    [
      2
    ],
    [
      3
    ],
    [
      4
    ],
    [
      5
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
    "id": "dependencies-011-r4",
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
  "fullJournalSha256": "47eb7aead951ad0822668feb7428f06ae1b4665b3ca4a60203e240c687cdd100",
  "familyState": {
    "schema": 1,
    "revision": 401,
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
            1,
            3
          ],
          [
            1,
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
            7
          ],
          [
            3,
            8
          ],
          [
            4,
            9
          ],
          [
            4,
            10
          ],
          [
            5,
            11
          ],
          [
            5,
            12
          ],
          [
            13,
            0
          ]
        ]
      },
      {
        "nodes": 11,
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
            1
          ],
          [
            4,
            1
          ],
          [
            5,
            2
          ],
          [
            6,
            2
          ],
          [
            7,
            3
          ],
          [
            8,
            3
          ],
          [
            9,
            4
          ],
          [
            10,
            0
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
            1,
            2
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
            1,
            5
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
            4
          ],
          [
            3,
            5
          ],
          [
            4,
            5
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
        "id": "dependencies-001-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-002-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-003-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "dependencies-004-r4",
        "family": "dependencies",
        "strategy": "KAHN_LAYERS",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-011-r4",
  "caseId": "dependencies-011",
  "round": 4,
  "family": "dependencies",
  "description": "DAG completo",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
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
        1,
        2
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
        1,
        5
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
        4
      ],
      [
        3,
        5
      ],
      [
        4,
        5
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
6712
```

### elapsedNanos

```json
530864
```

### adapted

```json
false
```

### reused

```json
true
```
