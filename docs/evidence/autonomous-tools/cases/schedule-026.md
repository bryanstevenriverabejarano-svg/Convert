# schedule-026: Catorce trabajos y subproblemas repetidos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-026-r1",
  "caseId": "schedule-026",
  "round": 1,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 1,
        "value": 3
      },
      {
        "start": 0,
        "end": 2,
        "value": 8
      },
      {
        "start": 2,
        "end": 3,
        "value": 13
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 4,
        "end": 5,
        "value": 10
      },
      {
        "start": 4,
        "end": 6,
        "value": 15
      },
      {
        "start": 6,
        "end": 7,
        "value": 7
      },
      {
        "start": 6,
        "end": 8,
        "value": 12
      },
      {
        "start": 8,
        "end": 9,
        "value": 4
      },
      {
        "start": 8,
        "end": 10,
        "value": 9
      },
      {
        "start": 10,
        "end": 11,
        "value": 14
      },
      {
        "start": 10,
        "end": 12,
        "value": 6
      },
      {
        "start": 12,
        "end": 13,
        "value": 11
      },
      {
        "start": 12,
        "end": 14,
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
      "value": 82,
      "selected": [
        1,
        2,
        5,
        7,
        9,
        10,
        12
      ]
    },
    "operations": 22282,
    "elapsedNanos": 2524436
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 82,
    "selected": [
      1,
      2,
      5,
      7,
      9,
      10,
      12
    ]
  },
  "independent": {
    "id": "schedule-026-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 82
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 22282,
  "elapsedNanos": 3088478,
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
  "id": "schedule-026-r2",
  "caseId": "schedule-026",
  "round": 2,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 2,
        "value": 8
      },
      {
        "start": 2,
        "end": 3,
        "value": 13
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 4,
        "end": 5,
        "value": 10
      },
      {
        "start": 4,
        "end": 6,
        "value": 15
      },
      {
        "start": 6,
        "end": 7,
        "value": 7
      },
      {
        "start": 6,
        "end": 8,
        "value": 12
      },
      {
        "start": 8,
        "end": 9,
        "value": 4
      },
      {
        "start": 8,
        "end": 10,
        "value": 9
      },
      {
        "start": 10,
        "end": 11,
        "value": 14
      },
      {
        "start": 10,
        "end": 12,
        "value": 6
      },
      {
        "start": 12,
        "end": 13,
        "value": 11
      },
      {
        "start": 12,
        "end": 14,
        "value": 3
      },
      {
        "start": -1,
        "end": 15,
        "value": 61
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
      "value": 82,
      "selected": [
        0,
        1,
        4,
        6,
        8,
        9,
        11
      ]
    },
    "operations": 27045,
    "elapsedNanos": 738911
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 82,
    "selected": [
      0,
      1,
      4,
      6,
      8,
      9,
      11
    ]
  },
  "independent": {
    "id": "schedule-026-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 82
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 27045,
  "elapsedNanos": 1116437,
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
  "id": "schedule-026-r3",
  "caseId": "schedule-026",
  "round": 3,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 4,
        "value": 8
      },
      {
        "start": 4,
        "end": 6,
        "value": 13
      },
      {
        "start": 4,
        "end": 8,
        "value": 5
      },
      {
        "start": 8,
        "end": 10,
        "value": 10
      },
      {
        "start": 8,
        "end": 12,
        "value": 15
      },
      {
        "start": 12,
        "end": 14,
        "value": 7
      },
      {
        "start": 12,
        "end": 16,
        "value": 12
      },
      {
        "start": 16,
        "end": 18,
        "value": 4
      },
      {
        "start": 16,
        "end": 20,
        "value": 9
      },
      {
        "start": 20,
        "end": 22,
        "value": 14
      },
      {
        "start": 20,
        "end": 24,
        "value": 6
      },
      {
        "start": 24,
        "end": 26,
        "value": 11
      },
      {
        "start": 0,
        "end": 1,
        "value": 1
      },
      {
        "start": 1,
        "end": 2,
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
      "value": 82,
      "selected": [
        0,
        1,
        4,
        6,
        8,
        9,
        11
      ]
    },
    "operations": 27066,
    "elapsedNanos": 2978380
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 82,
    "selected": [
      0,
      1,
      4,
      6,
      8,
      9,
      11
    ]
  },
  "independent": {
    "id": "schedule-026-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16384,
      "optimum": 82
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 27066,
  "elapsedNanos": 4635677,
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
  "id": "schedule-026-r4",
  "caseId": "schedule-026",
  "round": 4,
  "family": "schedule",
  "description": "Catorce trabajos y subproblemas repetidos",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 12,
        "end": 14,
        "value": 3
      },
      {
        "start": 12,
        "end": 13,
        "value": 11
      },
      {
        "start": 10,
        "end": 12,
        "value": 6
      },
      {
        "start": 10,
        "end": 11,
        "value": 14
      },
      {
        "start": 8,
        "end": 10,
        "value": 9
      },
      {
        "start": 8,
        "end": 9,
        "value": 4
      },
      {
        "start": 6,
        "end": 7,
        "value": 7
      },
      {
        "start": 4,
        "end": 6,
        "value": 15
      },
      {
        "start": 4,
        "end": 5,
        "value": 10
      },
      {
        "start": 2,
        "end": 4,
        "value": 5
      },
      {
        "start": 2,
        "end": 3,
        "value": 13
      },
      {
        "start": 0,
        "end": 2,
        "value": 8
      },
      {
        "start": 0,
        "end": 1,
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
      "value": 77,
      "selected": [
        11,
        10,
        7,
        6,
        4,
        3,
        1
      ]
    },
    "operations": 11608,
    "elapsedNanos": 133296
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 77,
    "selected": [
      11,
      10,
      7,
      6,
      4,
      3,
      1
    ]
  },
  "independent": {
    "id": "schedule-026-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 77
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 11608,
  "elapsedNanos": 280214,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
