# dependencies-021: Dependencias transitivas redundantes

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-021-r1",
  "caseId": "dependencies-021",
  "round": 1,
  "family": "dependencies",
  "description": "Dependencias transitivas redundantes",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 7,
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
        0,
        3
      ],
      [
        1,
        5
      ],
      [
        0,
        6
      ],
      [
        3,
        6
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
        2,
        3,
        4,
        5,
        6
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
        ],
        [
          6
        ]
      ]
    },
    "operations": 1997,
    "elapsedNanos": 1284255
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
      2,
      3,
      4,
      5,
      6
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
      ],
      [
        6
      ]
    ]
  },
  "independent": {
    "id": "dependencies-021-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 7
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1997,
  "elapsedNanos": 3100034,
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
  "id": "dependencies-021-r2",
  "caseId": "dependencies-021",
  "round": 2,
  "family": "dependencies",
  "description": "Dependencias transitivas redundantes",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
    "nodes": 7,
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
        0,
        3
      ],
      [
        1,
        5
      ],
      [
        0,
        6
      ],
      [
        3,
        6
      ],
      [
        1,
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
        1,
        0
      ]
    },
    "operations": 1728,
    "elapsedNanos": 156902
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
      1,
      0
    ]
  },
  "independent": {
    "id": "dependencies-021-r2",
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
  "operations": 1728,
  "elapsedNanos": 530502,
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
  "id": "dependencies-021-r3",
  "caseId": "dependencies-021",
  "round": 3,
  "family": "dependencies",
  "description": "Dependencias transitivas redundantes",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
    "nodes": 7,
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
        0,
        3
      ],
      [
        1,
        5
      ],
      [
        0,
        6
      ],
      [
        3,
        6
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
        2,
        3,
        4,
        6,
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
          4,
          6
        ],
        [
          5
        ]
      ]
    },
    "operations": 2088,
    "elapsedNanos": 830687
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
      2,
      3,
      4,
      6,
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
        4,
        6
      ],
      [
        5
      ]
    ]
  },
  "independent": {
    "id": "dependencies-021-r3",
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
  "operations": 2088,
  "elapsedNanos": 2061124,
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
  "id": "dependencies-021-r4",
  "caseId": "dependencies-021",
  "round": 4,
  "family": "dependencies",
  "description": "Dependencias transitivas redundantes",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
    "nodes": 8,
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
        0,
        3
      ],
      [
        1,
        5
      ],
      [
        0,
        6
      ],
      [
        3,
        6
      ],
      [
        7,
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
        7,
        0,
        1,
        2,
        3,
        4,
        5,
        6
      ],
      "layers": [
        [
          7
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
        ],
        [
          6
        ]
      ]
    },
    "operations": 2818,
    "elapsedNanos": 84575
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      7,
      0,
      1,
      2,
      3,
      4,
      5,
      6
    ],
    "layers": [
      [
        7
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
      ],
      [
        6
      ]
    ]
  },
  "independent": {
    "id": "dependencies-021-r4",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 8
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 2818,
  "elapsedNanos": 313783,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
