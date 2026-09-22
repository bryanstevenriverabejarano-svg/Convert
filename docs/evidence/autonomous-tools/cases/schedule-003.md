# schedule-003: Un trabajo largo pierde frente a dos cortos

Cuatro situaciones relacionadas, con cambios explícitos en recursos o estructura. Los programas y las decisiones registradas son observables; no se solicitan razonamientos internos privados.

## Ronda 1

Situación inicial; el programa no recibe respuestas esperadas.

### Desafío ejecutado

```json
{
  "schema": 1,
  "id": "schedule-003-r1",
  "caseId": "schedule-003",
  "round": 1,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Situación inicial; el programa no recibe respuestas esperadas.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 10,
        "value": 15
      },
      {
        "start": 0,
        "end": 5,
        "value": 9
      },
      {
        "start": 5,
        "end": 10,
        "value": 9
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
      "strategy": "EARLIEST_FINISH"
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
    "strategy": "EARLIEST_FINISH",
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
          "strategy": "EARLIEST_FINISH"
        },
        {
          "op": "verify_exact"
        }
      ]
    },
    "programSha256": "4dfc415d8c8ed79e60e86190087963c87221e8e020c11788eca2b75cf18391ea",
    "passed": true,
    "regressionChecks": 2,
    "feedback": "Testigo y objetivo comprobados por un verificador independiente.",
    "result": {
      "status": "ok",
      "value": 18,
      "selected": [
        1,
        2
      ]
    },
    "operations": 293,
    "elapsedNanos": 258712
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
      1,
      2
    ]
  },
  "independent": {
    "id": "schedule-003-r1",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 8,
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
  "operations": 293,
  "elapsedNanos": 863845,
  "version": 1,
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
  "id": "schedule-003-r2",
  "caseId": "schedule-003",
  "round": 2,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Aparece un encargo largo que compite con todo el calendario.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 10,
        "value": 15
      },
      {
        "start": 0,
        "end": 5,
        "value": 9
      },
      {
        "start": 5,
        "end": 10,
        "value": 9
      },
      {
        "start": -1,
        "end": 11,
        "value": 17
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
        1,
        2
      ]
    },
    "operations": 17329,
    "elapsedNanos": 605534
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
      1,
      2
    ]
  },
  "independent": {
    "id": "schedule-003-r2",
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
  "operations": 17329,
  "elapsedNanos": 968238,
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
  "id": "schedule-003-r3",
  "caseId": "schedule-003",
  "round": 3,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Un encargo se divide en dos etapas compatibles; cambia el grafo de conflictos.",
  "input": {
    "jobs": [
      {
        "start": 0,
        "end": 10,
        "value": 9
      },
      {
        "start": 10,
        "end": 20,
        "value": 9
      },
      {
        "start": 0,
        "end": 10,
        "value": 7
      },
      {
        "start": 10,
        "end": 20,
        "value": 9
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
        0,
        1
      ]
    },
    "operations": 17337,
    "elapsedNanos": 2013331
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
      0,
      1
    ]
  },
  "independent": {
    "id": "schedule-003-r3",
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
  "operations": 17337,
  "elapsedNanos": 3704446,
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
  "id": "schedule-003-r4",
  "caseId": "schedule-003",
  "round": 4,
  "family": "schedule",
  "description": "Un trabajo largo pierde frente a dos cortos",
  "mutation": "Se cancela un encargo y llega el calendario en orden inverso.",
  "input": {
    "jobs": [
      {
        "start": 5,
        "end": 10,
        "value": 9
      },
      {
        "start": 0,
        "end": 10,
        "value": 15
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
      "value": 15,
      "selected": [
        1
      ]
    },
    "operations": 17086,
    "elapsedNanos": 131425
  }
]
```

### Resultado y comprobación independiente

```json
{
  "result": {
    "status": "ok",
    "value": 15,
    "selected": [
      1
    ]
  },
  "independent": {
    "id": "schedule-003-r4",
    "passed": true,
    "certificate": {
      "oracle": "Enumeración exhaustiva independiente",
      "subsets": 4,
      "optimum": 15
    }
  }
}
```

### Medidas y resumen de decisión

Tiempos de la JVM anfitriona; no representan latencia en el teléfono ni inferencia del modelo.

```json
{
  "decisionSummary": "Verifiqué el programa con los datos actuales y las regresiones conservadas.",
  "operations": 17086,
  "elapsedNanos": 270650,
  "version": 2,
  "adapted": false,
  "reused": true,
  "promoted": true,
  "previousRejected": false,
  "status": "verified"
}
```
