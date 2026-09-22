# dependencies-025: Orden válido mezclado con índices

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-025-r1",
  "caseId": "dependencies-025",
  "round": 1,
  "family": "dependencies",
  "description": "Orden válido mezclado con índices",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
}
```

### Programa generado

```json
{
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
}
```

### Intentos y feedback observable

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
        4,
        5,
        6,
        2,
        3,
        7,
        8
      ],
      "layers": [
        [
          0,
          1,
          4
        ],
        [
          5,
          6
        ],
        [
          2,
          3
        ],
        [
          7
        ],
        [
          8
        ]
      ]
    },
    "operations": 6160,
    "elapsedNanos": 2524185
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      0,
      1,
      4,
      5,
      6,
      2,
      3,
      7,
      8
    ],
    "layers": [
      [
        0,
        1,
        4
      ],
      [
        5,
        6
      ],
      [
        2,
        3
      ],
      [
        7
      ],
      [
        8
      ]
    ]
  },
  "independent": {
    "id": "dependencies-025-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 5
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 6160,
  "elapsedNanos": 4499430,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 2

Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-025-r2",
  "caseId": "dependencies-025",
  "round": 2,
  "family": "dependencies",
  "description": "Orden válido mezclado con índices",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
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
}
```

### Programa generado

```json
{
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
}
```

### Intentos y feedback observable

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
        5,
        0
      ]
    },
    "operations": 5180,
    "elapsedNanos": 298190
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "cycle",
    "cycle": [
      0,
      5,
      0
    ]
  },
  "independent": {
    "id": "dependencies-025-r2",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y testigo de ciclo",
      "acyclic": false
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 5180,
  "elapsedNanos": 689938,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 3

Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-025-r3",
  "caseId": "dependencies-025",
  "round": 3,
  "family": "dependencies",
  "description": "Orden válido mezclado con índices",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
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
}
```

### Programa generado

```json
{
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
}
```

### Intentos y feedback observable

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
        4,
        6,
        3,
        5,
        2,
        7,
        8
      ],
      "layers": [
        [
          0,
          1,
          4,
          6
        ],
        [
          3,
          5
        ],
        [
          2
        ],
        [
          7
        ],
        [
          8
        ]
      ]
    },
    "operations": 6144,
    "elapsedNanos": 1777195
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      0,
      1,
      4,
      6,
      3,
      5,
      2,
      7,
      8
    ],
    "layers": [
      [
        0,
        1,
        4,
        6
      ],
      [
        3,
        5
      ],
      [
        2
      ],
      [
        7
      ],
      [
        8
      ]
    ]
  },
  "independent": {
    "id": "dependencies-025-r3",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 5
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 6144,
  "elapsedNanos": 3275729,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 4

Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-025-r4",
  "caseId": "dependencies-025",
  "round": 4,
  "family": "dependencies",
  "description": "Orden válido mezclado con índices",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
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
}
```

### Programa generado

```json
{
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
}
```

### Intentos y feedback observable

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
        4,
        9,
        0,
        6,
        3,
        5,
        2,
        7,
        8
      ],
      "layers": [
        [
          1,
          4,
          9
        ],
        [
          0,
          6
        ],
        [
          3,
          5
        ],
        [
          2
        ],
        [
          7
        ],
        [
          8
        ]
      ]
    },
    "operations": 7048,
    "elapsedNanos": 149131
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      1,
      4,
      9,
      0,
      6,
      3,
      5,
      2,
      7,
      8
    ],
    "layers": [
      [
        1,
        4,
        9
      ],
      [
        0,
        6
      ],
      [
        3,
        5
      ],
      [
        2
      ],
      [
        7
      ],
      [
        8
      ]
    ]
  },
  "independent": {
    "id": "dependencies-025-r4",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 6
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 7048,
  "elapsedNanos": 299693,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
