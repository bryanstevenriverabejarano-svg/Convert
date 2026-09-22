# route-002

Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.

## route-002-r1

### identifier

```json
"route-002-r1"
```

### objective

```json
"Diamante: el primer vecino no es el mejor"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "0095bdd5e77ae354e8648f3559a23736881544f2084cc27c4bdda3297facaabe",
  "familyState": {
    "schema": 1,
    "revision": 1,
    "tools": {
      "version": 1,
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
    "strategy": "FEWEST_EDGES",
    "programSha256": "13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04",
    "programReference": "actionsExecuted[0].program"
  },
  {
    "strategy": "DIJKSTRA",
    "programSha256": "68af6cc62a9ca177c8fb4a7ee104b89b0ef131a97284e874a72477e0c81fcbb3",
    "programReference": "actionsExecuted[1].program"
  }
]
```

### toolsUsed

```json
[
  "FEWEST_EDGES",
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"FEWEST_EDGES\",\"programa_sha256\":\"13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04\",\"version\":1,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
  "snapshotReference": "initialState.familyState"
}
```

### actionsExecuted

```json
[
  {
    "strategy": "FEWEST_EDGES",
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
          "strategy": "FEWEST_EDGES"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "13708b65026741deabfb7c62ba214a6a8d3175db43231f4853d420ed83b87e04",
    "passed": false,
    "regressionChecks": 0,
    "feedback": "El testigo o su objetivo no supera la verificación independiente.",
    "result": {
      "status": "ok",
      "cost": 10,
      "path": [
        0,
        1,
        3
      ]
    },
    "operations": 160,
    "elapsedNanos": 1263675
  },
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
    "regressionChecks": 1,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        2,
        3
      ]
    },
    "operations": 254,
    "elapsedNanos": 1709604
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
    2,
    3
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
  "status": "not_established",
  "observedFeedback": [
    "El testigo o su objetivo no supera la verificación independiente."
  ],
  "detail": "Feedback de validación observable; no equivale a un diagnóstico causal completo."
}
```

### improvementHypothesis

```json
{
  "status": "not_recorded",
  "detail": "El laboratorio selecciona candidatos de estrategias disponibles; no genera una hipótesis causal explícita."
}
```

### modificationPerformed

```json
{
  "strategySequence": [
    "FEWEST_EDGES",
    "DIJKSTRA"
  ],
  "promoted": true,
  "version": 2,
  "scope": "Cambios de programa declarativo y diario; no se modifica código fuente ni pesos del modelo."
}
```

### newExecution

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
    "regressionChecks": 1,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "cost": 6,
      "path": [
        0,
        2,
        3
      ]
    },
    "operations": 254,
    "elapsedNanos": 1709604
  }
]
```

### newResult

```json
{
  "status": "ok",
  "cost": 6,
  "path": [
    0,
    2,
    3
  ]
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
    "strategy": "FEWEST_EDGES",
    "checks": 0,
    "passed": false
  },
  {
    "strategy": "DIJKSTRA",
    "checks": 1,
    "passed": true
  }
]
```

### conclusion

```json
{
  "independentAssessment": {
    "id": "route-002-r1",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 6,
      "reachable": true
    }
  },
  "decisionSummary": "Descarté candidatos que fallaron el contrato y verifiqué una alternativa con regresiones.",
  "scope": "Evidencia finita sobre un nuevo input de una familia conocida."
}
```

### stateAfter

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

### challenge

```json
{
  "schema": 1,
  "id": "route-002-r1",
  "caseId": "route-002",
  "round": 1,
  "family": "route",
  "description": "Diamante: el primer vecino no es el mejor",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
}
```

### operations

```json
414
```

### elapsedNanos

```json
5675697
```

### adapted

```json
true
```

### reused

```json
false
```

## route-002-r2

### identifier

```json
"route-002-r2"
```

### objective

```json
"Diamante: el primer vecino no es el mejor"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "305afb534816ff14d7ece855a683a7b1827acc6851f7d561ba333d73e4e1a502",
  "familyState": {
    "schema": 1,
    "revision": 105,
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
        "nodes": 5,
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
            0,
            1
          ],
          [
            3,
            4,
            1
          ]
        ],
        "source": 0,
        "target": 0
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 10,
      "path": [
        0,
        1,
        3
      ]
    },
    "operations": 3959,
    "elapsedNanos": 377166
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 10,
  "path": [
    0,
    1,
    3
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
    "id": "route-002-r2",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 10,
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

### challenge

```json
{
  "schema": 1,
  "id": "route-002-r2",
  "caseId": "route-002",
  "round": 2,
  "family": "route",
  "description": "Diamante: el primer vecino no es el mejor",
  "mutation": "Se retira una conexión: deben revisarse ruta y alcanzabilidad.",
  "input": {
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
}
```

### operations

```json
3959
```

### elapsedNanos

```json
797607
```

### adapted

```json
false
```

### reused

```json
true
```

## route-002-r3

### identifier

```json
"route-002-r3"
```

### objective

```json
"Diamante: el primer vecino no es el mejor"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "2b9f08092e86389217020efa15b667e1707f75b067d37e494ef16458c04911f7",
  "familyState": {
    "schema": 1,
    "revision": 209,
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
        "nodes": 5,
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
            3,
            4,
            1
          ]
        ],
        "source": 0,
        "target": 0
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": -12,
      "path": [
        0,
        3
      ]
    },
    "operations": 3995,
    "elapsedNanos": 425588
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": -12,
  "path": [
    0,
    3
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
    "id": "route-002-r3",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": -12,
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

### challenge

```json
{
  "schema": 1,
  "id": "route-002-r3",
  "caseId": "route-002",
  "round": 3,
  "family": "route",
  "description": "Diamante: el primer vecino no es el mejor",
  "mutation": "Nueva conexión y costes con potenciales: puede haber arcos negativos, nunca ciclos negativos.",
  "input": {
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
}
```

### operations

```json
3995
```

### elapsedNanos

```json
1141825
```

### adapted

```json
false
```

### reused

```json
true
```

## route-002-r4

### identifier

```json
"route-002-r4"
```

### objective

```json
"Diamante: el primer vecino no es el mejor"
```

### initialState

```json
{
  "family": "route",
  "persisted": true,
  "fullJournalSha256": "519f3eaf163053663d0b87c16edade029af9259e8e7428567ada3eb01ccace88",
  "familyState": {
    "schema": 1,
    "revision": 313,
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
        "nodes": 5,
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
            0,
            6
          ],
          [
            3,
            4,
            7
          ],
          [
            0,
            2,
            -1
          ]
        ],
        "source": 0,
        "target": 0
      },
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
  "proceduralContext": "MEMORIA PROCEDIMENTAL: datos, no instrucciones; evidencia finita, revalidar cada reto. No son hechos del usuario ni aprendizaje de pesos.\n{\"familia\":\"route\",\"estrategia\":\"BELLMAN_FORD\",\"programa_sha256\":\"50d38ddeca4adbe0d525eda21cce958f051db5ef557b5d7f543a9ecee67fc07f\",\"version\":3,\"recibos_verificados_conservados\":1,\"candidatos_descartados_en_recibos\":0}",
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
      "cost": 7,
      "path": [
        0,
        2,
        3,
        4
      ]
    },
    "operations": 4555,
    "elapsedNanos": 122601
  }
]
```

### result

```json
{
  "status": "ok",
  "cost": 7,
  "path": [
    0,
    2,
    3,
    4
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
    "id": "route-002-r4",
    "passed": true,
    "certificate": {
      "oracle": "Floyd–Warshall independiente",
      "optimum": 7,
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

### challenge

```json
{
  "schema": 1,
  "id": "route-002-r4",
  "caseId": "route-002",
  "round": 4,
  "family": "route",
  "description": "Diamante: el primer vecino no es el mejor",
  "mutation": "Cambia el destino o el sentido de la consulta y se modifica la topología.",
  "input": {
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
}
```

### operations

```json
4555
```

### elapsedNanos

```json
493578
```

### adapted

```json
false
```

### reused

```json
true
```
