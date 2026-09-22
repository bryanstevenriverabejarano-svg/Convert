# dependencies-013: Tres capas y un trabajo aislado

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-013-r1",
  "caseId": "dependencies-013",
  "round": 1,
  "family": "dependencies",
  "description": "Tres capas y un trabajo aislado",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 10,
    "edges": [
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
        6
      ],
      [
        4,
        7
      ],
      [
        4,
        8
      ],
      [
        5,
        6
      ],
      [
        5,
        7
      ],
      [
        5,
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
        2,
        9,
        3,
        4,
        5,
        6,
        7,
        8
      ],
      "layers": [
        [
          0,
          1,
          2,
          9
        ],
        [
          3,
          4,
          5
        ],
        [
          6,
          7,
          8
        ]
      ]
    },
    "operations": 4257,
    "elapsedNanos": 2885157
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
      9,
      3,
      4,
      5,
      6,
      7,
      8
    ],
    "layers": [
      [
        0,
        1,
        2,
        9
      ],
      [
        3,
        4,
        5
      ],
      [
        6,
        7,
        8
      ]
    ]
  },
  "independent": {
    "id": "dependencies-013-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 3
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 4257,
  "elapsedNanos": 4228419,
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
  "id": "dependencies-013-r2",
  "caseId": "dependencies-013",
  "round": 2,
  "family": "dependencies",
  "description": "Tres capas y un trabajo aislado",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
    "nodes": 10,
    "edges": [
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
        6
      ],
      [
        4,
        7
      ],
      [
        4,
        8
      ],
      [
        5,
        6
      ],
      [
        5,
        7
      ],
      [
        5,
        8
      ],
      [
        3,
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
        3,
        0
      ]
    },
    "operations": 3639,
    "elapsedNanos": 271160
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
      3,
      0
    ]
  },
  "independent": {
    "id": "dependencies-013-r2",
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
  "operations": 3639,
  "elapsedNanos": 699823,
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
  "id": "dependencies-013-r3",
  "caseId": "dependencies-013",
  "round": 3,
  "family": "dependencies",
  "description": "Tres capas y un trabajo aislado",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
    "nodes": 10,
    "edges": [
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
        7
      ],
      [
        3,
        8
      ],
      [
        4,
        6
      ],
      [
        4,
        7
      ],
      [
        4,
        8
      ],
      [
        5,
        6
      ],
      [
        5,
        7
      ],
      [
        5,
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
        2,
        9,
        3,
        4,
        5,
        6,
        7,
        8
      ],
      "layers": [
        [
          0,
          1,
          2,
          9
        ],
        [
          3,
          4,
          5
        ],
        [
          6,
          7,
          8
        ]
      ]
    },
    "operations": 4241,
    "elapsedNanos": 2043939
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
      9,
      3,
      4,
      5,
      6,
      7,
      8
    ],
    "layers": [
      [
        0,
        1,
        2,
        9
      ],
      [
        3,
        4,
        5
      ],
      [
        6,
        7,
        8
      ]
    ]
  },
  "independent": {
    "id": "dependencies-013-r3",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 3
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 4241,
  "elapsedNanos": 3692584,
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
  "id": "dependencies-013-r4",
  "caseId": "dependencies-013",
  "round": 4,
  "family": "dependencies",
  "description": "Tres capas y un trabajo aislado",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
    "nodes": 11,
    "edges": [
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
        6
      ],
      [
        4,
        7
      ],
      [
        4,
        8
      ],
      [
        5,
        6
      ],
      [
        5,
        7
      ],
      [
        5,
        8
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
        1,
        2,
        9,
        10,
        0,
        3,
        4,
        5,
        6,
        7,
        8
      ],
      "layers": [
        [
          1,
          2,
          9,
          10
        ],
        [
          0
        ],
        [
          3,
          4,
          5
        ],
        [
          6,
          7,
          8
        ]
      ]
    },
    "operations": 5566,
    "elapsedNanos": 141569
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
      2,
      9,
      10,
      0,
      3,
      4,
      5,
      6,
      7,
      8
    ],
    "layers": [
      [
        1,
        2,
        9,
        10
      ],
      [
        0
      ],
      [
        3,
        4,
        5
      ],
      [
        6,
        7,
        8
      ]
    ]
  },
  "independent": {
    "id": "dependencies-013-r4",
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
  "operations": 5566,
  "elapsedNanos": 292682,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
