# route-006

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-006-r1

### identifier

```json
"route-006-r1"
```

### objective

```json
"Anillo con ciclo positivo y dos salidas"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "2e97a66ffb7793abe312c3ad72bdaba603525a070dbfa7fe344f437c21d5691b",
  "familyState": {
    "schema": 1,
    "revision": 5,
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
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            0,
            4,
            4
          ],
          [
            3,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            3,
            1
          ],
          [
            3,
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
            4,
            1
          ],
          [
            0,
            4,
            9
          ]
        ],
        "source": 0,
        "target": 4
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
      },
      {
        "id": "route-004-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r1",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"DIJKSTRA\",\"programa_sha256\":\"68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3\",\"version\":2,\"recibos_verificados_conservados\":4,\"candidatos_descartados_en_recibos\":1}",
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
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 8,
      "path": [
        0,
        1,
        2,
        5
      ]
    },
    "operations": 1642,
    "elapsedNanos": 1969086
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 8,
  "path": [
    0,
    1,
    2,
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
    "checks": 3,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-006-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 8,
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
  "fullJournalSha256": "17a9742f16d3002c3ce1ac11c270fc12accd4e424f54a0653780c34d5b53c8fd",
  "familyState": {
    "schema": 1,
    "revision": 6,
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
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            0,
            4,
            4
          ],
          [
            3,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            3,
            1
          ],
          [
            3,
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
            4,
            1
          ],
          [
            0,
            4,
            9
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            2
          ],
          [
            2,
            3,
            2
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            0,
            2
          ],
          [
            2,
            5,
            4
          ],
          [
            4,
            5,
            1
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
      },
      {
        "id": "route-004-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r1",
        "family": "route",
        "strategy": "DIJKSTRA",
        "version": 2,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r1",
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
  "id": "route-006-r1",
  "caseId": "route-006",
  "round": 1,
  "family": "route",
  "description": "Anillo con ciclo positivo y dos salidas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        1,
        2,
        2
      ],
      [
        2,
        3,
        2
      ],
      [
        3,
        4,
        2
      ],
      [
        4,
        0,
        2
      ],
      [
        2,
        5,
        4
      ],
      [
        4,
        5,
        1
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
1642
```

### elapsedNanos

```json
2666896
```

### adapted

```json
false
```

### reused

```json
true
```

## route-006-r2

### identifier

```json
"route-006-r2"
```

### objective

```json
"Anillo con ciclo positivo y dos salidas"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "a254d9f81d474aeb0c33f430ada9562028e71cdda1ad9adead9f0b4c5a3c25d8",
  "familyState": {
    "schema": 1,
    "revision": 109,
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
            0,
            2,
            2
          ],
          [
            0,
            4,
            4
          ],
          [
            3,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            3,
            1
          ],
          [
            3,
            1,
            1
          ],
          [
            2,
            4,
            1
          ],
          [
            0,
            4,
            9
          ]
        ],
        "source": 0,
        "target": 4
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
      },
      {
        "id": "route-004-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r2",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 8,
      "path": [
        0,
        1,
        2,
        5
      ]
    },
    "operations": 1443,
    "elapsedNanos": 263869
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 8,
  "path": [
    0,
    1,
    2,
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
    "id": "route-006-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 8,
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
  "fullJournalSha256": "3f1065c583c8c82e731ccaa0926fd894f831d0d90071359cd08eaf6bb5dd59b4",
  "familyState": {
    "schema": 1,
    "revision": 110,
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
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            4,
            4
          ],
          [
            3,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            3,
            1
          ],
          [
            3,
            1,
            1
          ],
          [
            2,
            4,
            1
          ],
          [
            0,
            4,
            9
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            2
          ],
          [
            2,
            3,
            2
          ],
          [
            4,
            0,
            2
          ],
          [
            2,
            5,
            4
          ],
          [
            4,
            5,
            1
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
      },
      {
        "id": "route-004-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r2",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r2",
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
  "id": "route-006-r2",
  "caseId": "route-006",
  "round": 2,
  "family": "route",
  "description": "Anillo con ciclo positivo y dos salidas",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        1,
        2,
        2
      ],
      [
        2,
        3,
        2
      ],
      [
        4,
        0,
        2
      ],
      [
        2,
        5,
        4
      ],
      [
        4,
        5,
        1
      ]
    ],
    "source": 0,
    "target": 5
  }
}
```

### operations

```json
1443
```

### elapsedNanos

```json
653385
```

### adapted

```json
false
```

### reused

```json
true
```

## route-006-r3

### identifier

```json
"route-006-r3"
```

### objective

```json
"Anillo con ciclo positivo y dos salidas"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "d1249e81de1c0ee4212b7ea48cd06baa5fc755c7be9dcd52c427a933f28a30c4",
  "familyState": {
    "schema": 1,
    "revision": 213,
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
            0,
            2,
            -3
          ],
          [
            0,
            3,
            -13
          ],
          [
            0,
            4,
            -6
          ],
          [
            3,
            5,
            13
          ],
          [
            0,
            5,
            0
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            3,
            -15
          ],
          [
            3,
            1,
            6
          ],
          [
            1,
            2,
            7
          ],
          [
            2,
            4,
            -4
          ],
          [
            0,
            4,
            -1
          ],
          [
            0,
            1,
            -7
          ]
        ],
        "source": 0,
        "target": 4
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
      },
      {
        "id": "route-004-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r3",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 4,
      "path": [
        0,
        2,
        5
      ]
    },
    "operations": 1697,
    "elapsedNanos": 1515046
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 4,
  "path": [
    0,
    2,
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
    "id": "route-006-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 4,
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
  "fullJournalSha256": "a7fc1d264393391f1a2a78242e48ca38e9e9284fad606385c97c1d20047e4cbe",
  "familyState": {
    "schema": 1,
    "revision": 214,
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
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -10
          ],
          [
            0,
            2,
            -3
          ],
          [
            0,
            3,
            -13
          ],
          [
            0,
            4,
            -6
          ],
          [
            3,
            5,
            13
          ],
          [
            0,
            5,
            0
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 5,
        "edges": [
          [
            0,
            3,
            -15
          ],
          [
            3,
            1,
            6
          ],
          [
            1,
            2,
            7
          ],
          [
            2,
            4,
            -4
          ],
          [
            0,
            4,
            -1
          ],
          [
            0,
            1,
            -7
          ]
        ],
        "source": 0,
        "target": 4
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            1,
            -9
          ],
          [
            1,
            2,
            8
          ],
          [
            2,
            3,
            -9
          ],
          [
            3,
            4,
            8
          ],
          [
            4,
            0,
            12
          ],
          [
            2,
            5,
            5
          ],
          [
            4,
            5,
            7
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
      },
      {
        "id": "route-004-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r3",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r3",
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
  "id": "route-006-r3",
  "caseId": "route-006",
  "round": 3,
  "family": "route",
  "description": "Anillo con ciclo positivo y dos salidas",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
    "nodes": 6,
    "edges": [
      [
        0,
        1,
        -9
      ],
      [
        1,
        2,
        8
      ],
      [
        2,
        3,
        -9
      ],
      [
        3,
        4,
        8
      ],
      [
        4,
        0,
        12
      ],
      [
        2,
        5,
        5
      ],
      [
        4,
        5,
        7
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
1697
```

### elapsedNanos

```json
2064386
```

### adapted

```json
false
```

### reused

```json
true
```

## route-006-r4

### identifier

```json
"route-006-r4"
```

### objective

```json
"Anillo con ciclo positivo y dos salidas"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "cc9c363bfb84ee9f9f9cb9f6dbe2106c7fe0c099d596bed2b1dd8d1e4d6ce01c",
  "familyState": {
    "schema": 1,
    "revision": 317,
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
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            0,
            4,
            4
          ],
          [
            3,
            5,
            1
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            3,
            1
          ],
          [
            3,
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
            4,
            1
          ],
          [
            0,
            4,
            9
          ],
          [
            4,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
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
      },
      {
        "id": "route-004-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r4",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":5,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 9,
      "path": [
        0,
        1,
        2,
        5,
        6
      ]
    },
    "operations": 2197,
    "elapsedNanos": 168128
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 9,
  "path": [
    0,
    1,
    2,
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
    "id": "route-006-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 9,
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
  "fullJournalSha256": "a4a85a2445f30608ecfe07952dcdfd01e0ebe224fb978e819849d7328f15ba2a",
  "familyState": {
    "schema": 1,
    "revision": 318,
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
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            1
          ],
          [
            0,
            2,
            2
          ],
          [
            0,
            3,
            3
          ],
          [
            0,
            4,
            4
          ],
          [
            3,
            5,
            1
          ],
          [
            5,
            6,
            1
          ]
        ],
        "source": 0,
        "target": 6
      },
      {
        "nodes": 6,
        "edges": [
          [
            0,
            3,
            1
          ],
          [
            3,
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
            4,
            1
          ],
          [
            0,
            4,
            9
          ],
          [
            4,
            5,
            1
          ]
        ],
        "source": 0,
        "target": 5
      },
      {
        "nodes": 7,
        "edges": [
          [
            0,
            1,
            2
          ],
          [
            1,
            2,
            2
          ],
          [
            2,
            3,
            2
          ],
          [
            3,
            4,
            2
          ],
          [
            4,
            0,
            2
          ],
          [
            2,
            5,
            4
          ],
          [
            4,
            5,
            1
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
      },
      {
        "id": "route-004-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-005-r4",
        "family": "route",
        "strategy": "BELLMAN_FORD",
        "version": 3,
        "verified": true,
        "attempts": 1
      },
      {
        "id": "route-006-r4",
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
  "id": "route-006-r4",
  "caseId": "route-006",
  "round": 4,
  "family": "route",
  "description": "Anillo con ciclo positivo y dos salidas",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
    "nodes": 7,
    "edges": [
      [
        0,
        1,
        2
      ],
      [
        1,
        2,
        2
      ],
      [
        2,
        3,
        2
      ],
      [
        3,
        4,
        2
      ],
      [
        4,
        0,
        2
      ],
      [
        2,
        5,
        4
      ],
      [
        4,
        5,
        1
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
2197
```

### elapsedNanos

```json
619775
```

### adapted

```json
false
```

### reused

```json
true
```
