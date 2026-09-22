# route-003

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-003-r1

### identifier

```json
"route-003-r1"
```

### objective

```json
"Cadena con acceso directo caro"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "a5a0f3add792e1f1843eba63fb1a8f641eb94ee587fac97da0688492c9762f1a",
  "familyState": {
    "schema": 1,
    "revision": 2,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "FEWEST_EDGES"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 2,
        "edges": [
          [
            0,
            1,
            7
          ]
        ],
        "source": 0,
        "target": 1
      },
      {
        "nodes": 4,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            3,
            9
          ],
          [
            0,
            2,
            4
          ],
          [
            2,
            3,
            2
          ]
        ],
        "source": 0,
        "target": 3
      }
    ],
    "receipts": [
      {
        "id": "route-001-r1",
        "family": "route",
        "strategy": "FEWEST_EDGES",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 2
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
    "strategy": "DIJKSTRA",
    "programSha256": "68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "DIJKSTRA"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"DIJKSTRA\",\"programa_sha256\":\"68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3\",\"version\":2,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":1}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "DIJKSTRA",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "DIJKSTRA"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3",
    "passed": true,
    "regressionChecks": 2,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 5,
      "path": [
        0,
        1,
        2,
        3,
        4,
        5
      ]
    },
    "operations": 705,
    "elapsedNanos": 3186953
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 5,
  "path": [
    0,
    1,
    2,
    3,
    4,
    5
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
    "DIJKSTRA"
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
    "strategy": "DIJKSTRA",
    "checks": 2,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-003-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 5,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "95dc7aa34278d7b20c4060d5afc519d78375756066bbdf3c917fb3a5979f1eb8",
  "familyState": {
    "schema": 1,
    "revision": 3,
    "tools": {
      "version": 2,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "FEWEST_EDGES"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 2,
        "edges": [
          [
            0,
            1,
            7
          ]
        ],
        "source": 0,
        "target": 1
      },
      {
        "nodes": 4,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            3,
            9
          ],
          [
            0,
            2,
            4
          ],
          [
            2,
            3,
            2
          ]
        ],
        "source": 0,
        "target": 3
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            2,
            3,
            1
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            1
          ],
          [
            0,
            5,
            12
          ]
        ],
        "source": 0,
        "target": 5
      }
    ],
    "receipts": [
      {
        "id": "route-001-r1",
        "family": "route",
        "strategy": "FEWEST_EDGES",
        "version": 1,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 2
      },
      {
        "id": "route-003-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
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
  "id": "route-003-r1",
  "caseId": "route-003",
  "round": 1,
  "family": "route",
  "description": "Cadena con acceso directo caro",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        1
      ],
      [
        2,
        3,
        1
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
        1
      ],
      [
        0,
        5,
        12
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
705
```

### elapsedNanos

```json
4672346
```

### adapted

```json
false
```

### reused

```json
true
```

## route-003-r2

### identifier

```json
"route-003-r2"
```

### objective

```json
"Cadena con acceso directo caro"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "c5ed9b5d9e51b31ef8a896a643a9e044cbbf4a2d68009b6ce9af33f549021c57",
  "familyState": {
    "schema": 1,
    "revision": 106,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
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
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
          ],
          [
            3,
            4,
            3
          ],
          [
            4,
            5,
            3
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            3
          ],
          [
            8,
            9,
            3
          ],
          [
            9,
            10,
            3
          ],
          [
            10,
            11,
            3
          ],
          [
            11,
            12,
            3
          ],
          [
            12,
            13,
            3
          ],
          [
            1,
            11,
            1
          ],
          [
            0,
            13,
            40
          ],
          [
            8,
            3,
            2
          ]
        ],
        "source": 0,
        "target": 13
      },
      {
        "nodes": 2,
        "edges": [],
        "source": 0,
        "target": 1
      },
      {
        "nodes": 4,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            3,
            9
          ],
          [
            2,
            3,
            2
          ]
        ],
        "source": 0,
        "target": 3
      }
    ],
    "receipts": [
      {
        "id": "route-001-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 12,
      "path": [
        0,
        5
      ]
    },
    "operations": 4094,
    "elapsedNanos": 366621
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 12,
  "path": [
    0,
    5
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-003-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 12,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "a141ff3cdd293c16a6d2e0fa30f98e01d88ff06e07cc6cd6a4e8453df7a515b6",
  "familyState": {
    "schema": 1,
    "revision": 107,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 2,
        "edges": [],
        "source": 0,
        "target": 1
      },
      {
        "nodes": 4,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            3,
            9
          ],
          [
            2,
            3,
            2
          ]
        ],
        "source": 0,
        "target": 3
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            2,
            3,
            1
          ],
          [
            4,
            5,
            1
          ],
          [
            0,
            5,
            12
          ]
        ],
        "source": 0,
        "target": 5
      }
    ],
    "receipts": [
      {
        "id": "route-001-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-003-r2",
  "caseId": "route-003",
  "round": 2,
  "family": "route",
  "description": "Cadena con acceso directo caro",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        1
      ],
      [
        2,
        3,
        1
      ],
      [
        4,
        5,
        1
      ],
      [
        0,
        5,
        12
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
4094
```

### elapsedNanos

```json
786882
```

### adapted

```json
false
```

### reused

```json
true
```

## route-003-r3

### identifier

```json
"route-003-r3"
```

### objective

```json
"Cadena con acceso directo caro"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "e3152f41e192a2bd0ad4c55495cf63d1ca994069ad25f72489d7d9260812fc8a",
  "familyState": {
    "schema": 1,
    "revision": 210,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
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
            1,
            3
          ],
          [
            1,
            2,
            3
          ],
          [
            2,
            3,
            3
          ],
          [
            3,
            4,
            3
          ],
          [
            4,
            5,
            3
          ],
          [
            5,
            6,
            3
          ],
          [
            6,
            7,
            3
          ],
          [
            7,
            8,
            3
          ],
          [
            9,
            10,
            3
          ],
          [
            10,
            11,
            3
          ],
          [
            11,
            12,
            3
          ],
          [
            12,
            13,
            3
          ],
          [
            1,
            11,
            1
          ],
          [
            0,
            13,
            40
          ],
          [
            8,
            3,
            2
          ]
        ],
        "source": 0,
        "target": 13
      },
      {
        "nodes": 2,
        "edges": [
          [
            0,
            1,
            -4
          ],
          [
            1,
            0,
            15
          ]
        ],
        "source": 0,
        "target": 1
      },
      {
        "nodes": 4,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            1,
            3,
            4
          ],
          [
            0,
            2,
            -1
          ],
          [
            2,
            3,
            -9
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 3
      }
    ],
    "receipts": [
      {
        "id": "route-001-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 1,
      "path": [
        0,
        1,
        2,
        3,
        4,
        5
      ]
    },
    "operations": 4194,
    "elapsedNanos": 382064
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 1,
  "path": [
    0,
    1,
    2,
    3,
    4,
    5
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-003-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 1,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "d92b644f2daec53d2ef867ce05f8ecd40a0701440996b225b655667c825228f3",
  "familyState": {
    "schema": 1,
    "revision": 211,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 2,
        "edges": [
          [
            0,
            1,
            -4
          ],
          [
            1,
            0,
            15
          ]
        ],
        "source": 0,
        "target": 1
      },
      {
        "nodes": 4,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            1,
            3,
            4
          ],
          [
            0,
            2,
            -1
          ],
          [
            2,
            3,
            -9
          ],
          [
            0,
            3,
            -12
          ]
        ],
        "source": 0,
        "target": 3
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            1,
            2,
            7
          ],
          [
            2,
            3,
            -10
          ],
          [
            3,
            4,
            7
          ],
          [
            4,
            5,
            7
          ],
          [
            0,
            5,
            8
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 5
      }
    ],
    "receipts": [
      {
        "id": "route-001-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-003-r3",
  "caseId": "route-003",
  "round": 3,
  "family": "route",
  "description": "Cadena con acceso directo caro",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        -10
      ],
      [
        1,
        2,
        7
      ],
      [
        2,
        3,
        -10
      ],
      [
        3,
        4,
        7
      ],
      [
        4,
        5,
        7
      ],
      [
        0,
        5,
        8
      ],
      [
        0,
        2,
        -1
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
4194
```

### elapsedNanos

```json
1728041
```

### adapted

```json
false
```

### reused

```json
true
```

## route-003-r4

### identifier

```json
"route-003-r4"
```

### objective

```json
"Cadena con acceso directo caro"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "d4285af26aa01db31991f60a6005f62102e141c909e9f7d2fdf4fd837b101155",
  "familyState": {
    "schema": 1,
    "revision": 314,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
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
            1,
            -8
          ],
          [
            1,
            2,
            9
          ],
          [
            2,
            3,
            -8
          ],
          [
            3,
            4,
            9
          ],
          [
            4,
            5,
            9
          ],
          [
            5,
            6,
            -8
          ],
          [
            6,
            7,
            9
          ],
          [
            7,
            8,
            9
          ],
          [
            8,
            9,
            -8
          ],
          [
            9,
            10,
            9
          ],
          [
            10,
            11,
            9
          ],
          [
            11,
            12,
            -8
          ],
          [
            12,
            13,
            9
          ],
          [
            1,
            11,
            10
          ],
          [
            0,
            13,
            33
          ],
          [
            8,
            3,
            -11
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 13
      },
      {
        "nodes": 3,
        "edges": [
          [
            0,
            1,
            7
          ],
          [
            1,
            2,
            1
          ]
        ],
        "source": 0,
        "target": 2
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            3,
            9
          ],
          [
            0,
            2,
            4
          ],
          [
            2,
            3,
            2
          ],
          [
            3,
            4,
            1
          ]
        ],
        "source": 0,
        "target": 4
      }
    ],
    "receipts": [
      {
        "id": "route-001-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "programReference": "actionsExecuted[0].program"
  }
]
```

### toolsUsed

```json
[
  "BELLMAN_FORD"
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":2,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "BELLMAN_FORD",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "route",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "BELLMAN_FORD"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        1,
        2,
        3,
        4,
        5,
        6
      ]
    },
    "operations": 4853,
    "elapsedNanos": 228307
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 6,
  "path": [
    0,
    1,
    2,
    3,
    4,
    5,
    6
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
    "BELLMAN_FORD"
  ],
  "promoted": true,
  "version": 3,
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
    "strategy": "BELLMAN_FORD",
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-003-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 6,
      "reachable": true
    }
  },
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "73233ca75dc409e2da6fee05e45594e646e03842b44c082205410b73da74b209",
  "familyState": {
    "schema": 1,
    "revision": 315,
    "tools": {
      "version": 3,
      "program": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "BELLMAN_FORD"
          },
          {
            "op": "verify_exact"
          }
        ]
      },
      "previous": {
        "schema": 1,
        "language": "salve-tools/1",
        "family": "route",
        "steps": [
          {
            "op": "validate_input"
          },
          {
            "op": "solve",
            "strategy": "DIJKSTRA"
          },
          {
            "op": "verify_exact"
          }
        ]
      }
    },
    "regressions": [
      {
        "nodes": 3,
        "edges": [
          [
            0,
            1,
            7
          ],
          [
            1,
            2,
            1
          ]
        ],
        "source": 0,
        "target": 2
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            3,
            9
          ],
          [
            0,
            2,
            4
          ],
          [
            2,
            3,
            2
          ],
          [
            3,
            4,
            1
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            1,
            2,
            1
          ],
          [
            2,
            3,
            1
          ],
          [
            3,
            4,
            1
          ],
          [
            4,
            5,
            1
          ],
          [
            0,
            5,
            12
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      }
    ],
    "receipts": [
      {
        "id": "route-001-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-002-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-003-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
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
  "id": "route-003-r4",
  "caseId": "route-003",
  "round": 4,
  "family": "route",
  "description": "Cadena con acceso directo caro",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        1
      ],
      [
        1,
        2,
        1
      ],
      [
        2,
        3,
        1
      ],
      [
        3,
        4,
        1
      ],
      [
        4,
        5,
        1
      ],
      [
        0,
        5,
        12
      ],
      [
        5,
        6,
        1
      ]
    ],
    "source": 0,
    "target": 6
  }
}
```

### operations

```json
4853
```

### elapsedNanos

```json
571544
```

### adapted

```json
false
```

### reused

```json
true
```
