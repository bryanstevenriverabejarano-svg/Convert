# schedule-016: Tiempos negativos válidos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-016-r1",
  "caseId": "schedule-016",
  "round": 1,
  "family": "schedule",
  "description": "Tiempos negativos válidos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": -8,
        "end": -5,
        "value": 4
      },
      {
        "start": -6,
        "end": -2,
        "value": 9
      },
      {
        "start": -5,
        "end": -3,
        "value": 5
      },
      {
        "start": -3,
        "end": 0,
        "value": 6
      },
      {
        "start": 0,
        "end": 2,
        "value": 2
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "schedule",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 17,
      "selected": [
        0,
        2,
        3,
        4
      ]
    },
    "operations": 554,
    "elapsedNanos": 333993
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 17,
    "selected": [
      0,
      2,
      3,
      4
    ]
  },
  "independent": {
    "id": "schedule-016-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 17
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 554,
  "elapsedNanos": 879929,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 2

Aparece un encargo largo que compite con todo el calendario.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-016-r2",
  "caseId": "schedule-016",
  "round": 2,
  "family": "schedule",
  "description": "Tiempos negativos válidos",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": -8,
        "end": -5,
        "value": 4
      },
      {
        "start": -6,
        "end": -2,
        "value": 9
      },
      {
        "start": -5,
        "end": -3,
        "value": 5
      },
      {
        "start": -3,
        "end": 0,
        "value": 6
      },
      {
        "start": 0,
        "end": 2,
        "value": 2
      },
      {
        "start": -9,
        "end": 3,
        "value": 14
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "schedule",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 17,
      "selected": [
        0,
        2,
        3,
        4
      ]
    },
    "operations": 782,
    "elapsedNanos": 167487
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 17,
    "selected": [
      0,
      2,
      3,
      4
    ]
  },
  "independent": {
    "id": "schedule-016-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 17
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 782,
  "elapsedNanos": 501289,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 3

Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-016-r3",
  "caseId": "schedule-016",
  "round": 3,
  "family": "schedule",
  "description": "Tiempos negativos válidos",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": -12,
        "end": -4,
        "value": 9
      },
      {
        "start": -10,
        "end": -6,
        "value": 5
      },
      {
        "start": -6,
        "end": 0,
        "value": 6
      },
      {
        "start": 0,
        "end": 4,
        "value": 2
      },
      {
        "start": -16,
        "end": -13,
        "value": 2
      },
      {
        "start": -13,
        "end": -10,
        "value": 3
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "schedule",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 18,
      "selected": [
        4,
        5,
        1,
        2,
        3
      ]
    },
    "operations": 795,
    "elapsedNanos": 1145472
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 18,
    "selected": [
      4,
      5,
      1,
      2,
      3
    ]
  },
  "independent": {
    "id": "schedule-016-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 18
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 795,
  "elapsedNanos": 2604327,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```

## Ronda 4

Se cancela un encargo y llega el calendario en orden inverso.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-016-r4",
  "caseId": "schedule-016",
  "round": 4,
  "family": "schedule",
  "description": "Tiempos negativos válidos",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 2
      },
      {
        "start": -3,
        "end": 0,
        "value": 6
      },
      {
        "start": -6,
        "end": -2,
        "value": 9
      },
      {
        "start": -8,
        "end": -5,
        "value": 4
      }
    ]
  }
}
```

### Programa generado

```json
{
  "schema": 1,
  "language": "salve-tools/1",
  "family": "schedule",
  "steps": [
    {
      "op": "validate_input"
    },
    {
      "op": "solve",
      "strategy": "WEIGHTED_DP"
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
    "strategy": "WEIGHTED_DP",
    "program": {
      "schema": 1,
      "language": "salve-tools/1",
      "family": "schedule",
      "steps": [
        {
          "op": "validate_input"
        },
        {
          "op": "solve",
          "strategy": "WEIGHTED_DP"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "6f87abc2cec0b268b365f7bca87b752a1826fa2d35fcc789840ac889298ea076",
    "passed": true,
    "regressionChecks": 3,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 12,
      "selected": [
        3,
        1,
        0
      ]
    },
    "operations": 389,
    "elapsedNanos": 130152
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 12,
    "selected": [
      3,
      1,
      0
    ]
  },
  "independent": {
    "id": "schedule-016-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 12
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 389,
  "elapsedNanos": 283298,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
