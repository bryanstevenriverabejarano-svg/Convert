# dependencies-010: Árbol de agregación

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-010-r1",
  "caseId": "dependencies-010",
  "round": 1,
  "family": "dependencies",
  "description": "Árbol de agregación",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
        5,
        6,
        7,
        8,
        9,
        2,
        3,
        4,
        1,
        0
      ],
      "layers": [
        [
          5,
          6,
          7,
          8,
          9
        ],
        [
          2,
          3,
          4
        ],
        [
          1
        ],
        [
          0
        ]
      ]
    },
    "operations": 5170,
    "elapsedNanos": 580557
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      5,
      6,
      7,
      8,
      9,
      2,
      3,
      4,
      1,
      0
    ],
    "layers": [
      [
        5,
        6,
        7,
        8,
        9
      ],
      [
        2,
        3,
        4
      ],
      [
        1
      ],
      [
        0
      ]
    ]
  },
  "independent": {
    "id": "dependencies-010-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 4
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 5170,
  "elapsedNanos": 1834878,
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
  "id": "dependencies-010-r2",
  "caseId": "dependencies-010",
  "round": 2,
  "family": "dependencies",
  "description": "Árbol de agregación",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
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
    "operations": 4524,
    "elapsedNanos": 228497
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
    "id": "dependencies-010-r2",
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
  "operations": 4524,
  "elapsedNanos": 577823,
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
  "id": "dependencies-010-r3",
  "caseId": "dependencies-010",
  "round": 3,
  "family": "dependencies",
  "description": "Árbol de agregación",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
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
        5,
        6,
        7,
        8,
        9,
        2,
        3,
        4,
        1,
        0
      ],
      "layers": [
        [
          5,
          6,
          7,
          8,
          9
        ],
        [
          2,
          3,
          4
        ],
        [
          1
        ],
        [
          0
        ]
      ]
    },
    "operations": 5303,
    "elapsedNanos": 959627
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      5,
      6,
      7,
      8,
      9,
      2,
      3,
      4,
      1,
      0
    ],
    "layers": [
      [
        5,
        6,
        7,
        8,
        9
      ],
      [
        2,
        3,
        4
      ],
      [
        1
      ],
      [
        0
      ]
    ]
  },
  "independent": {
    "id": "dependencies-010-r3",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 4
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 5303,
  "elapsedNanos": 2023739,
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
  "id": "dependencies-010-r4",
  "caseId": "dependencies-010",
  "round": 4,
  "family": "dependencies",
  "description": "Árbol de agregación",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
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
        5,
        6,
        7,
        8,
        9,
        10,
        2,
        3,
        4,
        1,
        0
      ],
      "layers": [
        [
          5,
          6,
          7,
          8,
          9,
          10
        ],
        [
          2,
          3,
          4
        ],
        [
          1
        ],
        [
          0
        ]
      ]
    },
    "operations": 6602,
    "elapsedNanos": 115871
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      5,
      6,
      7,
      8,
      9,
      10,
      2,
      3,
      4,
      1,
      0
    ],
    "layers": [
      [
        5,
        6,
        7,
        8,
        9,
        10
      ],
      [
        2,
        3,
        4
      ],
      [
        1
      ],
      [
        0
      ]
    ]
  },
  "independent": {
    "id": "dependencies-010-r4",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 4
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 6602,
  "elapsedNanos": 264451,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
