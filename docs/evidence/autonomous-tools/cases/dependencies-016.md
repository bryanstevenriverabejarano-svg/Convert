# dependencies-016: Autodependencia

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "dependencies-016-r1",
  "caseId": "dependencies-016",
  "round": 1,
  "family": "dependencies",
  "description": "Autodependencia",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "nodes": 4,
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
        2
      ],
      [
        2,
        3
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
        2,
        2
      ]
    },
    "operations": 1763,
    "elapsedNanos": 934279
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "cycle",
    "cycle": [
      2,
      2
    ]
  },
  "independent": {
    "id": "dependencies-016-r1",
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
  "operations": 1763,
  "elapsedNanos": 2238393,
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
  "id": "dependencies-016-r2",
  "caseId": "dependencies-016",
  "round": 2,
  "family": "dependencies",
  "description": "Autodependencia",
  "mutation": "Nueva dependencia inversa: puede aparecer un ciclo que debe justificarse con un testigo.",
  "input": {
    "nodes": 4,
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
        2
      ],
      [
        2,
        3
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
    "operations": 1562,
    "elapsedNanos": 185153
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
    "id": "dependencies-016-r2",
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
  "operations": 1562,
  "elapsedNanos": 573056,
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
  "id": "dependencies-016-r3",
  "caseId": "dependencies-016",
  "round": 3,
  "family": "dependencies",
  "description": "Autodependencia",
  "mutation": "Se retira una dependencia o se agrega una tarea independiente; reevaluar el orden.",
  "input": {
    "nodes": 4,
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
        3
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
        ]
      ]
    },
    "operations": 1828,
    "elapsedNanos": 978415
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
      3
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
      ]
    ]
  },
  "independent": {
    "id": "dependencies-016-r3",
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
  "operations": 1828,
  "elapsedNanos": 2421066,
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
  "id": "dependencies-016-r4",
  "caseId": "dependencies-016",
  "round": 4,
  "family": "dependencies",
  "description": "Autodependencia",
  "mutation": "Aparece un prerrequisito nuevo o se invierte el flujo de dependencias.",
  "input": {
    "nodes": 5,
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
        2
      ],
      [
        2,
        3
      ],
      [
        4,
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
        2,
        2
      ]
    },
    "operations": 2373,
    "elapsedNanos": 83904
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "cycle",
    "cycle": [
      2,
      2
    ]
  },
  "independent": {
    "id": "dependencies-016-r4",
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
  "operations": 2373,
  "elapsedNanos": 224321,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
