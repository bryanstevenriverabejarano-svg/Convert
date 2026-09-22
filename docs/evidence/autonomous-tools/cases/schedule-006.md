# schedule-006: Ventanas idénticas con diferentes recompensas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-006-r1",
  "caseId": "schedule-006",
  "round": 1,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 2,
        "end": 6,
        "value": 3
      },
      {
        "start": 2,
        "end": 6,
        "value": 12
      },
      {
        "start": 2,
        "end": 6,
        "value": 8
      },
      {
        "start": 6,
        "end": 8,
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
      "value": 14,
      "selected": [
        1,
        3
      ]
    },
    "operations": 426,
    "elapsedNanos": 294014
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 14,
    "selected": [
      1,
      3
    ]
  },
  "independent": {
    "id": "schedule-006-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
      "optimum": 14
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 426,
  "elapsedNanos": 831978,
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
  "id": "schedule-006-r2",
  "caseId": "schedule-006",
  "round": 2,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 2,
        "end": 6,
        "value": 3
      },
      {
        "start": 2,
        "end": 6,
        "value": 12
      },
      {
        "start": 2,
        "end": 6,
        "value": 8
      },
      {
        "start": 6,
        "end": 8,
        "value": 2
      },
      {
        "start": 1,
        "end": 9,
        "value": 13
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
      "value": 14,
      "selected": [
        1,
        3
      ]
    },
    "operations": 606,
    "elapsedNanos": 210591
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 14,
    "selected": [
      1,
      3
    ]
  },
  "independent": {
    "id": "schedule-006-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 14
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 606,
  "elapsedNanos": 570141,
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
  "id": "schedule-006-r3",
  "caseId": "schedule-006",
  "round": 3,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 4,
        "end": 12,
        "value": 12
      },
      {
        "start": 4,
        "end": 12,
        "value": 8
      },
      {
        "start": 12,
        "end": 16,
        "value": 2
      },
      {
        "start": 4,
        "end": 8,
        "value": 1
      },
      {
        "start": 8,
        "end": 12,
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
      "value": 14,
      "selected": [
        0,
        2
      ]
    },
    "operations": 610,
    "elapsedNanos": 1187913
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 14,
    "selected": [
      0,
      2
    ]
  },
  "independent": {
    "id": "schedule-006-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 14
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 610,
  "elapsedNanos": 2833632,
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
  "id": "schedule-006-r4",
  "caseId": "schedule-006",
  "round": 4,
  "family": "schedule",
  "description": "Ventanas idénticas con diferentes recompensas",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 6,
        "end": 8,
        "value": 2
      },
      {
        "start": 2,
        "end": 6,
        "value": 12
      },
      {
        "start": 2,
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
      "value": 14,
      "selected": [
        1,
        0
      ]
    },
    "operations": 296,
    "elapsedNanos": 67690
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 14,
    "selected": [
      1,
      0
    ]
  },
  "independent": {
    "id": "schedule-006-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
      "optimum": 14
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 296,
  "elapsedNanos": 274205,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
