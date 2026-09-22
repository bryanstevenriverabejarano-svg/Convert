# schedule-009: Solapamiento escalonado

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-009-r1",
  "caseId": "schedule-009",
  "round": 1,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 3
      },
      {
        "start": 1,
        "end": 4,
        "value": 8
      },
      {
        "start": 2,
        "end": 5,
        "value": 13
      },
      {
        "start": 3,
        "end": 6,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 12
      },
      {
        "start": 5,
        "end": 8,
        "value": 6
      },
      {
        "start": 6,
        "end": 9,
        "value": 11
      },
      {
        "start": 7,
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
      "value": 25,
      "selected": [
        1,
        4,
        7
      ]
    },
    "operations": 867,
    "elapsedNanos": 393260
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 25,
    "selected": [
      1,
      4,
      7
    ]
  },
  "independent": {
    "id": "schedule-009-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 256,
      "optimum": 25
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 867,
  "elapsedNanos": 885667,
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
  "id": "schedule-009-r2",
  "caseId": "schedule-009",
  "round": 2,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 3,
        "value": 3
      },
      {
        "start": 1,
        "end": 4,
        "value": 8
      },
      {
        "start": 2,
        "end": 5,
        "value": 13
      },
      {
        "start": 3,
        "end": 6,
        "value": 7
      },
      {
        "start": 4,
        "end": 7,
        "value": 12
      },
      {
        "start": 5,
        "end": 8,
        "value": 6
      },
      {
        "start": 6,
        "end": 9,
        "value": 11
      },
      {
        "start": 7,
        "end": 10,
        "value": 5
      },
      {
        "start": -1,
        "end": 11,
        "value": 33
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
      "value": 33,
      "selected": [
        8
      ]
    },
    "operations": 1323,
    "elapsedNanos": 1036059
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 33,
    "selected": [
      8
    ]
  },
  "independent": {
    "id": "schedule-009-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 33
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1323,
  "elapsedNanos": 1601603,
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
  "id": "schedule-009-r3",
  "caseId": "schedule-009",
  "round": 3,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 2,
        "end": 8,
        "value": 8
      },
      {
        "start": 4,
        "end": 10,
        "value": 13
      },
      {
        "start": 6,
        "end": 12,
        "value": 7
      },
      {
        "start": 8,
        "end": 14,
        "value": 12
      },
      {
        "start": 10,
        "end": 16,
        "value": 6
      },
      {
        "start": 12,
        "end": 18,
        "value": 11
      },
      {
        "start": 14,
        "end": 20,
        "value": 5
      },
      {
        "start": 0,
        "end": 3,
        "value": 1
      },
      {
        "start": 3,
        "end": 6,
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
      "value": 25,
      "selected": [
        0,
        3,
        6
      ]
    },
    "operations": 1343,
    "elapsedNanos": 1335270
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 25,
    "selected": [
      0,
      3,
      6
    ]
  },
  "independent": {
    "id": "schedule-009-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 512,
      "optimum": 25
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 1343,
  "elapsedNanos": 2840922,
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
  "id": "schedule-009-r4",
  "caseId": "schedule-009",
  "round": 4,
  "family": "schedule",
  "description": "Solapamiento escalonado",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 7,
        "end": 10,
        "value": 5
      },
      {
        "start": 6,
        "end": 9,
        "value": 11
      },
      {
        "start": 5,
        "end": 8,
        "value": 6
      },
      {
        "start": 3,
        "end": 6,
        "value": 7
      },
      {
        "start": 2,
        "end": 5,
        "value": 13
      },
      {
        "start": 1,
        "end": 4,
        "value": 8
      },
      {
        "start": 0,
        "end": 3,
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
      "value": 24,
      "selected": [
        4,
        1
      ]
    },
    "operations": 587,
    "elapsedNanos": 55141
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 24,
    "selected": [
      4,
      1
    ]
  },
  "independent": {
    "id": "schedule-009-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 128,
      "optimum": 24
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 587,
  "elapsedNanos": 247545,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
