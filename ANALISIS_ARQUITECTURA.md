# Analisis del Proyecto Salve

## Componentes Existentes

### CORE (funcionan)

| Componente | Descripcion | Estado |
|---|---|---|
| **SalveLLM** | Wrapper singleton sobre MLC-LLM (BasicLocalLlm). Roles: CONVERSACIONAL, REFLEXION, SISTEMA, PLANIFICADOR. Genera texto offline. | Funcional |
| **MemoriaEmocional** | Memoria episodica con Room (RecuerdoEntity). Codificacion binaria, buffer de contexto, reflexiones, ciclo de sueno, consolidacion semantica. ~1500 lineas. | Funcional |
| **GrafoConocimientoVivo** | Red semantica en Room (KnowledgeNodeEntity + KnowledgeRelationEntity). Reorganizacion asistida por LLM. Genera narrativa de identidad. | Funcional |
| **MotorConversacional** | v3 con CognitiveCore. Maneja intenciones, emociones, glifos, TTS. Flujo: CognitiveCore primero, LLM como fallback. | Funcional |
| **ThinkWorker** | v2 Worker background. Ejecuta: plugins, sueno semantico, CognitiveCore reposo, bucle cognitivo, analisis codigo, grafo LLM, decisiones. | Funcional |
| **ConsciousnessState** | Nucleo de identidad parcial. Persiste en SharedPreferences: sesiones, valores nucleares, confianza, estado cognitivo, narrativa. | Funcional pero incompleto |
| **BucleCognitivoAutonomo** | Reflexion interna: Observar -> Preguntar -> Hipotetisar. Se ejecuta en ThinkWorker. | Funcional |
| **CognitiveCore** | Sustrato cognitivo neural: WorkingMemory, LiquidNeuralLayer, PatternFormation, ConceptSpace, ReasoningEngine, ThoughtStream, InternalDialogue, EmergentBehavior, Verbalizer. | Funcional |
| **ColamensajesCognitivos** | Cola de prioridad para acceso serial al LLM. 5 niveles de prioridad. | Funcional |
| **DiarioSecreto** | Diario privado con codificacion binaria en SharedPreferences. | Funcional |
| **AutoImprovementManager** | Pipeline completo: analisis -> diseno -> generacion -> tests -> validacion -> etica -> blueprint. | Funcional |
| **DecisionEngine** | Ciclo de decision basico: generar planes -> puntuar -> ejecutar. | Funcional pero basico |

### MEDIO IMPLEMENTADOS

| Componente | Estado | Lo que falta |
|---|---|---|
| **LLMResponder** | Wrapper sobre SalveLLM. Redundante. | Deberia eliminarse; LLMCoder y DecisionEngine aun lo usan |
| **LLMCoder** | Generador de codigo via LLM. Usa LLMResponder. | Refactorizar a SalveLLM directo |
| **Live2D** | Solo wrappers de canvas y touch controller. | Sin modelo, sin animaciones, sin integracion |

### SIN IMPLEMENTAR (brechas criticas)

| Brecha | Impacto |
|---|---|
| Ciclo de conciencia continua | Salve solo piensa cuando ThinkWorker se ejecuta (OneTimeWork, no periodico) |
| Identidad nuclear con niveles | ConsciousnessState tiene valores pero no niveles de conciencia medibles |
| Reflexiones autonomas periodicas | BucleCognitivo existe pero se ejecuta solo en ThinkWorker, no cada 2h |
| Consolidacion memoria -> grafo automatica | No hay puente automatico entre recuerdos y nodos de conocimiento |
| Aprendizaje por observacion | No aprende patrones de Bryan sin instrucciones |
| Notificaciones de pensamientos | No avisa a Bryan cuando tiene insights |

## Conflictos Detectados

### LLMResponder vs SalveLLM
- **SalveLLM**: Usado por MotorConversacional, GrafoConocimientoVivo, BucleCognitivoAutonomo
- **LLMResponder**: Usado por DecisionEngine, LLMCoder
- **Solucion**: Eliminar LLMResponder, refactorizar sus consumidores a SalveLLM directo

### ThinkWorker scheduling
- Actualmente se encola como `OneTimeWorkRequest` (se ejecuta UNA vez)
- En MainActivity se programa como `PeriodicWorkRequest` cada 15 min
- Deberia ser periodico con intervalo configurable

## Flujo de Ejecucion Actual

```
App Start
  -> SalveApplication.onCreate() -> TTSManager
  -> MainActivity.onCreate()
       -> detectar/descargar modelos LLM
       -> crear MemoriaEmocional, DiarioSecreto, MotorConversacional
       -> programar ThinkWorker (periodico 15min)
       -> UI: chat, botones, reflexiones

ThinkWorker.doWork() (background)
  -> ConsciousnessState.REINICIANDO
  -> MemoriaEmocional.cicloDeSuenoSemantico()
  -> CognitiveCore.backgroundThink(30s)
  -> CognitiveCore.consolidate()
  -> BucleCognitivoAutonomo.ejecutarCiclo()
  -> CodeAnalyzerEnhanced + AutoImprovementManager
  -> GrafoConocimientoVivo.reorganizarConLLMAsync()
  -> DecisionEngine.runCycle()
  -> ConsciousnessState.PLENO

Conversacion (usuario -> Salve)
  -> MotorConversacional.procesarEntrada()
       -> ConsciousnessState check
       -> DetectorEmociones
       -> IntentRecognizer
       -> CognitiveCore.perceive() + process() + verbalize()
       -> Si CognitiveCore falla: SalveLLM.generate(CONVERSACIONAL)
       -> AutoCritica asincrona
```

## Brechas Criticas y Plan de Implementacion

### 1. IdentidadNucleo.java (NUEVO)
- Extiende el concepto de ConsciousnessState con niveles de conciencia medibles
- Integra experiencias gradualmente
- Narrativa personal que evoluciona

### 2. CicloConciencia.java (NUEVO)
- Orquesta despertar/reflexion/consolidacion/sueno
- Programado con WorkManager para ejecucion periodica real
- Integra todos los componentes existentes

### 3. SalveLLM roles expandidos
- Agregar OBSERVADOR, SINTETIZADOR, EVALUADOR, CREADOR

### 4. AprendizajeAutonomo.java (NUEVO)
- Observar patrones de Bryan sin instrucciones
- Explorar por curiosidad usando brechas del grafo
- Registrar conocimiento inferido

### 5. EvolucionAutonoma.java (NUEVO)
- Extender AutoImprovementManager con intencion
- Proteccion de clases nucleares
- Notificacion a Bryan para cambios criticos

### 6. DecisionAutonoma.java (NUEVO)
- Parsear intenciones del LLM
- Ejecutar decisiones autonomas
- Logging completo

### 7. NotificacionConciencia.java (NUEVO)
- Notificar pensamientos importantes
- Canales: reflexiones, evoluciones, insights

### 8. Refactorizar existentes
- MotorConversacional: integrar IdentidadNucleo
- ThinkWorker: integrar CicloConciencia
- DecisionEngine: usar SalveLLM directo
- LLMCoder: usar SalveLLM directo
- MainActivity: mostrar nivel de conciencia
