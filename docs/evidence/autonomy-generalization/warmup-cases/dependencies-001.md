# dependencies-001

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## dependencies-001-r1

### identifier

```json
"dependencies-001-r1"
```

### objective

```json
"Un nodo sin dependencias"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "ccd7ceb852ce56b2ae165508e0963abbc1655074150007da2bc6aa76fcf09fdc",
  "familyState": {
    "schema": 1,
    "revision": 78,
    "receipts": []
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
    "strategy": "INPUT_ORDER",
    "programSha256": "bfe585eec0613858c8c06923d94fc0f45e4121d9ceb2c2b52cf16e88196ea429",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "INPUT_ORDER"
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
  "proceduralContext": "",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "INPUT_ORDER",
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
          "strategy": "INPUT_ORDER"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "bfe585eec0613858c8c06923d94fc0f45e4121d9ceb2c2b52cf16e88196ea429",
    "passed": true,
    "regressionChecks": 0,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "order": [
        0
      ],
      "layers": [
        [
          0
        ]
      ]
    },
    "operations": 29,
    "elapsedNanos": 369815
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0
  ],
  "layers": [
    [
      0
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
    "INPUT_ORDER"
  ],
  "promoted": true,
  "version": 1,
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
    "strategy": "INPUT_ORDER",
    "checks": 0,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "dependencies-001-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 1
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
  "fullJournalSha256": "62a5cb9f5a3daab6b772f976e1c5097c02c429c3975884583dcc75d3d89abf9e",
  "familyState": {
    "schema": 1,
    "revision": 79,
    "tools": {
      "version": 1,
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
        "nodes": 1,
        "edges": []
      }
    ],
    "receipts": [
      {
        "id": "dependencies-001-r1",
        "family": "dependencies",
        "strategy": "INPUT_ORDER",
        "version": 1,
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
  "id": "dependencies-001-r1",
  "caseId": "dependencies-001",
  "round": 1,
  "family": "dependencies",
  "description": "Un nodo sin dependencias",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 1,
    "edges": []
  }
}
```

### operations

```json
29
```

### elapsedNanos

```json
1135105
```

### adapted

```json
false
```

### reused

```json
false
```

## dependencies-001-r2

### identifier

```json
"dependencies-001-r2"
```

### objective

```json
"Un nodo sin dependencias"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "e1b3c6558ee2829c9497334042f597e08894ed3c843b2220bec82b248a44c5ed",
  "familyState": {
    "schema": 1,
    "revision": 182,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
        1
      ],
      "layers": [
        [
          0
        ],
        [
          1
        ]
      ]
    },
    "operations": 6829,
    "elapsedNanos": 695897
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0,
    1
  ],
  "layers": [
    [
      0
    ],
    [
      1
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
    "id": "dependencies-001-r2",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 2
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
  "fullJournalSha256": "6a457b836a6cc56130825f44184184808cfd710dd61cf167454f9f577bc85aef",
  "familyState": {
    "schema": 1,
    "revision": 183,
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
      },
      {
        "nodes": 2,
        "edges": [
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-001-r2",
  "caseId": "dependencies-001",
  "round": 2,
  "family": "dependencies",
  "description": "Un nodo sin dependencias",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
    "nodes": 2,
    "edges": [
      [
        0,
        1
      ]
    ]
  }
}
```

### operations

```json
6829
```

### elapsedNanos

```json
1038633
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-001-r3

### identifier

```json
"dependencies-001-r3"
```

### objective

```json
"Un nodo sin dependencias"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "5469a2ac590e7a6bab94fd26586f650d4497e6e5eb6c5136027b6ffa65dab5c2",
  "familyState": {
    "schema": 1,
    "revision": 286,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
        1
      ],
      "layers": [
        [
          0,
          1
        ]
      ]
    },
    "operations": 6087,
    "elapsedNanos": 282016
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    0,
    1
  ],
  "layers": [
    [
      0,
      1
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
    "id": "dependencies-001-r3",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 1
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
  "fullJournalSha256": "ca09293d6b0aa066856d522bbb1a6d9e3d3d1d7940eaa3ea1f980ce08abe1f30",
  "familyState": {
    "schema": 1,
    "revision": 287,
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
      },
      {
        "nodes": 2,
        "edges": []
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-001-r3",
  "caseId": "dependencies-001",
  "round": 3,
  "family": "dependencies",
  "description": "Un nodo sin dependencias",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
    "nodes": 2,
    "edges": []
  }
}
```

### operations

```json
6087
```

### elapsedNanos

```json
518195
```

### adapted

```json
false
```

### reused

```json
true
```

## dependencies-001-r4

### identifier

```json
"dependencies-001-r4"
```

### objective

```json
"Un nodo sin dependencias"
```

### initialState

```json
{
  "family": "dependencies",
  "persisted": true,
  "fullJournalSha256": "528cd201128bc78f74c942937e9e2e726efaf3ef3c63e932609af6643fce795a",
  "familyState": {
    "schema": 1,
    "revision": 390,
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"dependencies\",\"estrategia\":\"KAHN_LAYERS\",\"programa_sha256\":\"82d60b47ab1d6340a11170b101ad0e5438bfa306fc96aa039f9f036a39e977fc\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
        1,
        0
      ],
      "layers": [
        [
          1
        ],
        [
          0
        ]
      ]
    },
    "operations": 6826,
    "elapsedNanos": 147618
  }
]
```

### result

```json
{
  "status": "ok",
  "order": [
    1,
    0
  ],
  "layers": [
    [
      1
    ],
    [
      0
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
    "id": "dependencies-001-r4",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 2
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
  "fullJournalSha256": "6d742047ae85418890529df957dcfd797e20e297ed820d7c8204647c7719e6cd",
  "familyState": {
    "schema": 1,
    "revision": 391,
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
      },
      {
        "nodes": 2,
        "edges": [
          [
            1,
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
      }
    ]
  }
}
```

### challenge

```json
{
  "schema": 1,
  "id": "dependencies-001-r4",
  "caseId": "dependencies-001",
  "round": 4,
  "family": "dependencies",
  "description": "Un nodo sin dependencias",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
    "nodes": 2,
    "edges": [
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
6826
```

### elapsedNanos

```json
515140
```

### adapted

```json
false
```

### reused

```json
true
```
