# schedule-019: Alternancia entre ventanas cortas y largas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-019-r1",
  "caseId": "schedule-019",
  "round": 1,
  "family": "schedule",
  "description": "Alternancia entre ventanas cortas y largas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 5
      },
      {
        "start": 1,
        "end": 5,
        "value": 8
      },
      {
        "start": 2,
        "end": 4,
        "value": 6
      },
      {
        "start": 4,
        "end": 6,
        "value": 5
      },
      {
        "start": 5,
        "end": 9,
        "value": 10
      },
      {
        "start": 6,
        "end": 8,
        "value": 6
      },
      {
        "start": 8,
        "end": 10,
        "value": 5
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
      "value": 27,
      "selected": [
        0,
        2,
        3,
        5,
        6
      ]
    },
    "operations": 758,
    "elapsedNanos": 345640
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 27,
    "selected": [
      0,
      2,
      3,
      5,
      6
    ]
  },
  "independent": {
    "id": "schedule-019-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 27
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 758,
  "elapsedNanos": 865217,
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
  "id": "schedule-019-r2",
  "caseId": "schedule-019",
  "round": 2,
  "family": "schedule",
  "description": "Alternancia entre ventanas cortas y largas",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 5
      },
      {
        "start": 1,
        "end": 5,
        "value": 8
      },
      {
        "start": 2,
        "end": 4,
        "value": 6
      },
      {
        "start": 4,
        "end": 6,
        "value": 5
      },
      {
        "start": 5,
        "end": 9,
        "value": 10
      },
      {
        "start": 6,
        "end": 8,
        "value": 6
      },
      {
        "start": 8,
        "end": 10,
        "value": 5
      },
      {
        "start": -1,
        "end": 11,
        "value": 23
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
      "value": 27,
      "selected": [
        0,
        2,
        3,
        5,
        6
      ]
    },
    "operations": 1109,
    "elapsedNanos": 177512
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 27,
    "selected": [
      0,
      2,
      3,
      5,
      6
    ]
  },
  "independent": {
    "id": "schedule-019-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 27
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1109,
  "elapsedNanos": 511845,
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
  "id": "schedule-019-r3",
  "caseId": "schedule-019",
  "round": 3,
  "family": "schedule",
  "description": "Alternancia entre ventanas cortas y largas",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 2,
        "end": 10,
        "value": 8
      },
      {
        "start": 4,
        "end": 8,
        "value": 6
      },
      {
        "start": 8,
        "end": 12,
        "value": 5
      },
      {
        "start": 10,
        "end": 18,
        "value": 10
      },
      {
        "start": 12,
        "end": 16,
        "value": 6
      },
      {
        "start": 16,
        "end": 20,
        "value": 5
      },
      {
        "start": 0,
        "end": 2,
        "value": 2
      },
      {
        "start": 2,
        "end": 4,
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
      "value": 28,
      "selected": [
        6,
        7,
        1,
        2,
        4,
        5
      ]
    },
    "operations": 1114,
    "elapsedNanos": 1230197
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 28,
    "selected": [
      6,
      7,
      1,
      2,
      4,
      5
    ]
  },
  "independent": {
    "id": "schedule-019-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 28
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1114,
  "elapsedNanos": 2813896,
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
  "id": "schedule-019-r4",
  "caseId": "schedule-019",
  "round": 4,
  "family": "schedule",
  "description": "Alternancia entre ventanas cortas y largas",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 8,
        "end": 10,
        "value": 5
      },
      {
        "start": 6,
        "end": 8,
        "value": 6
      },
      {
        "start": 5,
        "end": 9,
        "value": 10
      },
      {
        "start": 2,
        "end": 4,
        "value": 6
      },
      {
        "start": 1,
        "end": 5,
        "value": 8
      },
      {
        "start": 0,
        "end": 2,
        "value": 5
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
      "value": 22,
      "selected": [
        5,
        3,
        1,
        0
      ]
    },
    "operations": 529,
    "elapsedNanos": 50855
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 22,
    "selected": [
      5,
      3,
      1,
      0
    ]
  },
  "independent": {
    "id": "schedule-019-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 22
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 529,
  "elapsedNanos": 227266,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
