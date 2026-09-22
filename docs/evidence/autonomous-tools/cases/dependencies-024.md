# dependencies-024: Catorce nodos y ancho máximo

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-024-r1",
  "caseId": "dependencies-024",
  "round": 1,
  "family": "dependencies",
  "description": "Catorce nodos y ancho máximo",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
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
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13
      ],
      "layers": [
        [
          0
        ],
        [
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
          12
        ],
        [
          13
        ]
      ]
    },
    "operations": 5690,
    "elapsedNanos": 2724761
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
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13
    ],
    "layers": [
      [
        0
      ],
      [
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
        12
      ],
      [
        13
      ]
    ]
  },
  "independent": {
    "id": "dependencies-024-r1",
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
  "operations": 5690,
  "elapsedNanos": 4902154,
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
  "id": "dependencies-024-r2",
  "caseId": "dependencies-024",
  "round": 2,
  "family": "dependencies",
  "description": "Catorce nodos y ancho máximo",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
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
    "operations": 4696,
    "elapsedNanos": 246914
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
    "id": "dependencies-024-r2",
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
  "operations": 4696,
  "elapsedNanos": 646183,
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
  "id": "dependencies-024-r3",
  "caseId": "dependencies-024",
  "round": 3,
  "family": "dependencies",
  "description": "Catorce nodos y ancho máximo",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
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
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13
      ],
      "layers": [
        [
          0
        ],
        [
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
          12
        ],
        [
          13
        ]
      ]
    },
    "operations": 5664,
    "elapsedNanos": 1621806
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
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13
    ],
    "layers": [
      [
        0
      ],
      [
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
        12
      ],
      [
        13
      ]
    ]
  },
  "independent": {
    "id": "dependencies-024-r3",
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
  "operations": 5664,
  "elapsedNanos": 2953022,
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
  "id": "dependencies-024-r4",
  "caseId": "dependencies-024",
  "round": 4,
  "family": "dependencies",
  "description": "Catorce nodos y ancho máximo",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
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
        13,
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
        12,
        0
      ],
      "layers": [
        [
          13
        ],
        [
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
          12
        ],
        [
          0
        ]
      ]
    },
    "operations": 6464,
    "elapsedNanos": 234647
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "order": [
      13,
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
      12,
      0
    ],
    "layers": [
      [
        13
      ],
      [
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
        12
      ],
      [
        0
      ]
    ]
  },
  "independent": {
    "id": "dependencies-024-r4",
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
  "operations": 6464,
  "elapsedNanos": 386561,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
