# schedule-023: Límite exacto permite encadenar tres tareas

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-023-r1",
  "caseId": "schedule-023",
  "round": 1,
  "family": "schedule",
  "description": "Límite exacto permite encadenar tres tareas",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 4,
        "value": 6
      },
      {
        "start": 4,
        "end": 7,
        "value": 5
      },
      {
        "start": 7,
        "end": 10,
        "value": 8
      },
      {
        "start": 3,
        "end": 8,
        "value": 14
      },
      {
        "start": 0,
        "end": 10,
        "value": 18
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
      "value": 19,
      "selected": [
        0,
        1,
        2
      ]
    },
    "operations": 614,
    "elapsedNanos": 325040
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 19,
    "selected": [
      0,
      1,
      2
    ]
  },
  "independent": {
    "id": "schedule-023-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 32,
      "optimum": 19
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 614,
  "elapsedNanos": 812920,
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
  "id": "schedule-023-r2",
  "caseId": "schedule-023",
  "round": 2,
  "family": "schedule",
  "description": "Límite exacto permite encadenar tres tareas",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 4,
        "value": 6
      },
      {
        "start": 4,
        "end": 7,
        "value": 5
      },
      {
        "start": 7,
        "end": 10,
        "value": 8
      },
      {
        "start": 3,
        "end": 8,
        "value": 14
      },
      {
        "start": 0,
        "end": 10,
        "value": 18
      },
      {
        "start": -1,
        "end": 11,
        "value": 26
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
        5
      ]
    },
    "operations": 873,
    "elapsedNanos": 194857
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
      5
    ]
  },
  "independent": {
    "id": "schedule-023-r2",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
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
  "operations": 873,
  "elapsedNanos": 554378,
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
  "id": "schedule-023-r3",
  "caseId": "schedule-023",
  "round": 3,
  "family": "schedule",
  "description": "Límite exacto permite encadenar tres tareas",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 8,
        "end": 14,
        "value": 5
      },
      {
        "start": 14,
        "end": 20,
        "value": 8
      },
      {
        "start": 6,
        "end": 16,
        "value": 14
      },
      {
        "start": 0,
        "end": 20,
        "value": 18
      },
      {
        "start": 0,
        "end": 4,
        "value": 3
      },
      {
        "start": 4,
        "end": 8,
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
      "value": 20,
      "selected": [
        4,
        5,
        0,
        1
      ]
    },
    "operations": 887,
    "elapsedNanos": 1228184
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 20,
    "selected": [
      4,
      5,
      0,
      1
    ]
  },
  "independent": {
    "id": "schedule-023-r3",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 64,
      "optimum": 20
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 887,
  "elapsedNanos": 2730713,
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
  "id": "schedule-023-r4",
  "caseId": "schedule-023",
  "round": 4,
  "family": "schedule",
  "description": "Límite exacto permite encadenar tres tareas",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 10,
        "value": 18
      },
      {
        "start": 3,
        "end": 8,
        "value": 14
      },
      {
        "start": 4,
        "end": 7,
        "value": 5
      },
      {
        "start": 0,
        "end": 4,
        "value": 6
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
        0
      ]
    },
    "operations": 432,
    "elapsedNanos": 51777
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
      0
    ]
  },
  "independent": {
    "id": "schedule-023-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 16,
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
  "operations": 432,
  "elapsedNanos": 193326,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
