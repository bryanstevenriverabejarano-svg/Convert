# schedule-024: Doble peine de intervalos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-024-r1",
  "caseId": "schedule-024",
  "round": 1,
  "family": "schedule",
  "description": "Doble peine de intervalos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 4
      },
      {
        "start": 2,
        "end": 4,
        "value": 4
      },
      {
        "start": 4,
        "end": 6,
        "value": 4
      },
      {
        "start": 6,
        "end": 8,
        "value": 4
      },
      {
        "start": 8,
        "end": 10,
        "value": 4
      },
      {
        "start": 10,
        "end": 12,
        "value": 4
      },
      {
        "start": 1,
        "end": 4,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 7
      },
      {
        "start": 7,
        "end": 10,
        "value": 7
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
      "value": 26,
      "selected": [
        0,
        1,
        7,
        8,
        5
      ]
    },
    "operations": 1176,
    "elapsedNanos": 415974
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 26,
    "selected": [
      0,
      1,
      7,
      8,
      5
    ]
  },
  "independent": {
    "id": "schedule-024-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 26
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1176,
  "elapsedNanos": 981188,
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
  "id": "schedule-024-r2",
  "caseId": "schedule-024",
  "round": 2,
  "family": "schedule",
  "description": "Doble peine de intervalos",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 4
      },
      {
        "start": 2,
        "end": 4,
        "value": 4
      },
      {
        "start": 4,
        "end": 6,
        "value": 4
      },
      {
        "start": 6,
        "end": 8,
        "value": 4
      },
      {
        "start": 8,
        "end": 10,
        "value": 4
      },
      {
        "start": 10,
        "end": 12,
        "value": 4
      },
      {
        "start": 1,
        "end": 4,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 7
      },
      {
        "start": 7,
        "end": 10,
        "value": 7
      },
      {
        "start": -1,
        "end": 13,
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
      "value": 26,
      "selected": [
        0,
        1,
        7,
        8,
        5
      ]
    },
    "operations": 1896,
    "elapsedNanos": 261035
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 26,
    "selected": [
      0,
      1,
      7,
      8,
      5
    ]
  },
  "independent": {
    "id": "schedule-024-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 1024,
      "optimum": 26
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1896,
  "elapsedNanos": 632363,
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
  "id": "schedule-024-r3",
  "caseId": "schedule-024",
  "round": 3,
  "family": "schedule",
  "description": "Doble peine de intervalos",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 4,
        "end": 8,
        "value": 4
      },
      {
        "start": 8,
        "end": 12,
        "value": 4
      },
      {
        "start": 12,
        "end": 16,
        "value": 4
      },
      {
        "start": 16,
        "end": 20,
        "value": 4
      },
      {
        "start": 20,
        "end": 24,
        "value": 4
      },
      {
        "start": 2,
        "end": 8,
        "value": 7
      },
      {
        "start": 8,
        "end": 14,
        "value": 7
      },
      {
        "start": 14,
        "end": 20,
        "value": 7
      },
      {
        "start": 0,
        "end": 2,
        "value": 2
      },
      {
        "start": 2,
        "end": 4,
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
      "value": 27,
      "selected": [
        8,
        5,
        6,
        7,
        4
      ]
    },
    "operations": 1906,
    "elapsedNanos": 1319078
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
      8,
      5,
      6,
      7,
      4
    ]
  },
  "independent": {
    "id": "schedule-024-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 1024,
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
  "operations": 1906,
  "elapsedNanos": 2903719,
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
  "id": "schedule-024-r4",
  "caseId": "schedule-024",
  "round": 4,
  "family": "schedule",
  "description": "Doble peine de intervalos",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 7,
        "end": 10,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 7
      },
      {
        "start": 1,
        "end": 4,
        "value": 7
      },
      {
        "start": 10,
        "end": 12,
        "value": 4
      },
      {
        "start": 6,
        "end": 8,
        "value": 4
      },
      {
        "start": 4,
        "end": 6,
        "value": 4
      },
      {
        "start": 2,
        "end": 4,
        "value": 4
      },
      {
        "start": 0,
        "end": 2,
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
      "value": 26,
      "selected": [
        7,
        6,
        1,
        0,
        3
      ]
    },
    "operations": 758,
    "elapsedNanos": 54471
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 26,
    "selected": [
      7,
      6,
      1,
      0,
      3
    ]
  },
  "independent": {
    "id": "schedule-024-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 26
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
  "elapsedNanos": 257791,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
