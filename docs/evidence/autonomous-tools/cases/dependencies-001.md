# dependencies-001: Un nodo sin dependencias

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

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
      "strategy": "INPUT_ORDER"
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
    "elapsedNanos": 316016
  }
]
```

### Resultado y comprobación independiente

```json
{
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
  "independent": {
    "id": "dependencies-001-r1",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 1
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 29,
  "elapsedNanos": 811547,
  "version": 1,
  "adapted": false,
  "reused": false,
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
    "elapsedNanos": 639514
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
  "independent": {
    "id": "dependencies-001-r2",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 2
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 6829,
  "elapsedNanos": 975069,
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
    "elapsedNanos": 1819047
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
      1
    ],
    "layers": [
      [
        0,
        1
      ]
    ]
  },
  "independent": {
    "id": "dependencies-001-r3",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 1
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 6087,
  "elapsedNanos": 3369106,
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
    "elapsedNanos": 574048
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
  "independent": {
    "id": "dependencies-001-r4",
    "passed": true,
    "certificate": {
      "oracle": "Eliminación independiente de fuentes y validación de todas las aristas",
      "acyclic": true,
      "layer_count": 2
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 6826,
  "elapsedNanos": 730739,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
