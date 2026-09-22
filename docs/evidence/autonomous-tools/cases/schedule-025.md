# schedule-025: Doce trabajos con diferentes grados de conflicto

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-025-r1",
  "caseId": "schedule-025",
  "round": 1,
  "family": "schedule",
  "description": "Doce trabajos con diferentes grados de conflicto",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 1,
        "value": 2
      },
      {
        "start": 1,
        "end": 3,
        "value": 9
      },
      {
        "start": 2,
        "end": 5,
        "value": 16
      },
      {
        "start": 3,
        "end": 7,
        "value": 6
      },
      {
        "start": 4,
        "end": 5,
        "value": 13
      },
      {
        "start": 5,
        "end": 7,
        "value": 3
      },
      {
        "start": 6,
        "end": 9,
        "value": 10
      },
      {
        "start": 7,
        "end": 11,
        "value": 17
      },
      {
        "start": 8,
        "end": 9,
        "value": 7
      },
      {
        "start": 9,
        "end": 11,
        "value": 14
      },
      {
        "start": 10,
        "end": 13,
        "value": 4
      },
      {
        "start": 11,
        "end": 15,
        "value": 11
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
      "value": 59,
      "selected": [
        0,
        1,
        4,
        6,
        9,
        11
      ]
    },
    "operations": 5562,
    "elapsedNanos": 971303
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 59,
    "selected": [
      0,
      1,
      4,
      6,
      9,
      11
    ]
  },
  "independent": {
    "id": "schedule-025-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4096,
      "optimum": 59
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 5562,
  "elapsedNanos": 1592369,
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
  "id": "schedule-025-r2",
  "caseId": "schedule-025",
  "round": 2,
  "family": "schedule",
  "description": "Doce trabajos con diferentes grados de conflicto",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 1,
        "value": 2
      },
      {
        "start": 1,
        "end": 3,
        "value": 9
      },
      {
        "start": 2,
        "end": 5,
        "value": 16
      },
      {
        "start": 3,
        "end": 7,
        "value": 6
      },
      {
        "start": 4,
        "end": 5,
        "value": 13
      },
      {
        "start": 5,
        "end": 7,
        "value": 3
      },
      {
        "start": 6,
        "end": 9,
        "value": 10
      },
      {
        "start": 7,
        "end": 11,
        "value": 17
      },
      {
        "start": 8,
        "end": 9,
        "value": 7
      },
      {
        "start": 9,
        "end": 11,
        "value": 14
      },
      {
        "start": 10,
        "end": 13,
        "value": 4
      },
      {
        "start": 11,
        "end": 15,
        "value": 11
      },
      {
        "start": -1,
        "end": 16,
        "value": 57
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
      "value": 59,
      "selected": [
        0,
        1,
        4,
        6,
        9,
        11
      ]
    },
    "operations": 10387,
    "elapsedNanos": 417076
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 59,
    "selected": [
      0,
      1,
      4,
      6,
      9,
      11
    ]
  },
  "independent": {
    "id": "schedule-025-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 59
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 10387,
  "elapsedNanos": 785068,
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
  "id": "schedule-025-r3",
  "caseId": "schedule-025",
  "round": 3,
  "family": "schedule",
  "description": "Doce trabajos con diferentes grados de conflicto",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 2,
        "end": 6,
        "value": 9
      },
      {
        "start": 4,
        "end": 10,
        "value": 16
      },
      {
        "start": 6,
        "end": 14,
        "value": 6
      },
      {
        "start": 8,
        "end": 10,
        "value": 13
      },
      {
        "start": 10,
        "end": 14,
        "value": 3
      },
      {
        "start": 12,
        "end": 18,
        "value": 10
      },
      {
        "start": 14,
        "end": 22,
        "value": 17
      },
      {
        "start": 16,
        "end": 18,
        "value": 7
      },
      {
        "start": 18,
        "end": 22,
        "value": 14
      },
      {
        "start": 20,
        "end": 26,
        "value": 4
      },
      {
        "start": 22,
        "end": 30,
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
      "value": 60,
      "selected": [
        11,
        12,
        0,
        3,
        5,
        8,
        10
      ]
    },
    "operations": 10402,
    "elapsedNanos": 2047995
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 60,
    "selected": [
      11,
      12,
      0,
      3,
      5,
      8,
      10
    ]
  },
  "independent": {
    "id": "schedule-025-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8192,
      "optimum": 60
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 10402,
  "elapsedNanos": 3674978,
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
  "id": "schedule-025-r4",
  "caseId": "schedule-025",
  "round": 4,
  "family": "schedule",
  "description": "Doce trabajos con diferentes grados de conflicto",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 11,
        "end": 15,
        "value": 11
      },
      {
        "start": 10,
        "end": 13,
        "value": 4
      },
      {
        "start": 9,
        "end": 11,
        "value": 14
      },
      {
        "start": 8,
        "end": 9,
        "value": 7
      },
      {
        "start": 7,
        "end": 11,
        "value": 17
      },
      {
        "start": 5,
        "end": 7,
        "value": 3
      },
      {
        "start": 4,
        "end": 5,
        "value": 13
      },
      {
        "start": 3,
        "end": 7,
        "value": 6
      },
      {
        "start": 2,
        "end": 5,
        "value": 16
      },
      {
        "start": 1,
        "end": 3,
        "value": 9
      },
      {
        "start": 0,
        "end": 1,
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
      "value": 59,
      "selected": [
        10,
        9,
        6,
        5,
        3,
        2,
        0
      ]
    },
    "operations": 3088,
    "elapsedNanos": 86458
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 59,
    "selected": [
      10,
      9,
      6,
      5,
      3,
      2,
      0
    ]
  },
  "independent": {
    "id": "schedule-025-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 2048,
      "optimum": 59
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 3088,
  "elapsedNanos": 236279,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
