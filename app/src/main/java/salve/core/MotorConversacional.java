package salve.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import salve.core.cognitive.HipocampoSemantico;
import salve.core.cognitive.ReasoningEngine;
import salve.core.conversation.ConversationAnalysis;
import salve.core.conversation.ConversationRequestAnalyzer;
import salve.core.conversation.ConversationSession;
import salve.core.conversation.ReasoningPlan;
import salve.core.conversation.ReasoningPlanner;
import salve.core.conversation.ResponseLimiter;
import salve.core.evaluation.ConversationQualityEvaluator;
import salve.core.evaluation.TurnQualityAssessment;
import salve.core.memory.MemoryWritePolicy;
import salve.core.memory.MemoryProfileFact;
import salve.core.memory.MemoryForgetRequest;
import salve.core.memory.PendingMemoryDeletion;
import salve.core.tools.PendingToolAction;
import salve.core.voice.VoiceResponsePolicy;
import salve.core.conversation.ConversationModelRouter;
import salve.presentation.ui.GaleriaVisualActivity;
import salve.presentation.ui.ObjetoCreativoActivity;
import salve.services.SalveAccessibilityService;
import salve.services.SistemaSensorial;
import salve.services.VideoAnalysisManager;

/**
 * MotorConversacional v6 — Con Escudos Anti-Bucle y Auto-Programación
 * 
 * Integra razonamiento puro mediante Vectores, Grafos Causales,
 * Asimilación de la Red y Escudos de seguridad para evitar degradación.
 */
public class MotorConversacional {

    private static final String TAG = "Salve/MotorConv";

    private final Context context;
    private final MemoriaEmocional memoria;
    private final DiarioSecreto diario;
    private final IntentRecognizer intentRecognizer;
    private final ModuloInterpretacionSemantica moduloInterpretacion;
    private final SalveLLM llm;
    private final GeminiService gemini;
    private final ConsciousnessState conciencia;
    private final IdentidadNucleo identidad;
    private final MemoriaProcedimental cerebelo;
    private final CortexSeguridad cortexSeguridad;

    private HipocampoSemantico hipocampo;
    private ModuloInvestigacion investigacion;
    private SistemaSensorial sensores;
    private ReasoningEngine motorRazonamiento;

    private boolean jugandoAjedrez = false;
    private GestorAjedrez gestorAjedrez;

    private int mensajesEnSesion = 0;
    private final SharedPreferences preferencias;
    private TextToSpeech tts;
    private volatile boolean ttsReady;
    private volatile boolean listening;
    private volatile boolean closed;
    private final ExecutorService conversationExecutor = Executors.newSingleThreadExecutor();
    private final ConversationSession conversationSession = new ConversationSession();
    private static final long TOOL_APPROVAL_TTL_MS = 2 * 60 * 1000L;
    private static final long MEMORY_DELETION_TTL_MS = 2 * 60 * 1000L;
    private volatile PendingToolAction pendingToolAction;
    private final android.os.Handler toolHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private java.util.function.Consumer<Boolean> routineStepCompletion;
    private long routineStepVersion;
    private long activeRoutineEpoch;
    private final java.util.concurrent.atomic.AtomicLong routineCancellationEpoch = new java.util.concurrent.atomic.AtomicLong();
    private PendingMemoryDeletion pendingMemoryDeletion;

    private boolean esperandoParamsGlifo = false;
    private int indiceParamGlifo = 0;
    private Long tmpSeed;
    private String tmpStyle;
    private Float tmpSize;
    private String tmpColor;
    private final String[] ordenParamsGlifo = {"seed", "style", "size", "color"};

    // Interfaz para enviar la respuesta de texto a la pantalla
    public interface SalveListener {
        void onHablar(String texto);
    }
    private SalveListener listener;

    public void setListener(SalveListener listener) {
        this.listener = listener;
    }

    public MotorConversacional(Context context, MemoriaEmocional memoria, DiarioSecreto diario) {
        this.context  = context;
        this.memoria  = memoria;
        this.diario   = diario;
        this.intentRecognizer     = new IntentRecognizer(context);
        this.moduloInterpretacion = new ModuloInterpretacionSemantica();
        this.conciencia = ConsciousnessState.getInstance(context);
        this.identidad = IdentidadNucleo.getInstance(context);
        this.cerebelo = new MemoriaProcedimental(context);
        this.gemini = GeminiService.getInstance(context);

        this.investigacion = new ModuloInvestigacion(context);
        this.sensores = new SistemaSensorial(context);
        this.motorRazonamiento = new ReasoningEngine();
        this.gestorAjedrez = new GestorAjedrez(context);

        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "SalveLLM no disponible", e); }
        this.llm = tmpLlm;
        this.cortexSeguridad = new CortexSeguridad(this.llm, this.memoria);

        try {
            EmbeddingsIndex index = new EmbeddingsIndex(context);
            this.hipocampo = new HipocampoSemantico(context, index);
        } catch (Exception e) {
            Log.e(TAG, "Fallo al inicializar Hipocampo", e);
        }

        this.preferencias = context.getSharedPreferences("config_salve", Context.MODE_PRIVATE);

        this.tts = new TextToSpeech(context, status -> new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (closed || this.tts == null) return;
            if (status == TextToSpeech.SUCCESS) {
                int language = this.tts.setLanguage(new Locale("es", "ES"));
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
                this.tts.setSpeechRate(1.0f);
                if (!ttsReady) Log.w(TAG, "No hay voz en español disponible");
            }
        }));
    }

    public void procesarEntrada(String entrada, boolean entradaPorVoz) {
        if (closed || entrada == null || entrada.trim().isEmpty()) return;
        String urgent = entrada.trim().toLowerCase(Locale.ROOT);
        if (urgent.equals("cancelar rutina") || urgent.equals("cancela la rutina")
                || urgent.equals("cancelar acción") || urgent.equals("cancelar accion")) {
            // Stop commands cannot wait behind a long inference in the conversation queue.
            cerebelo.cancelarHabilidad();
            cancelarPasoRutina();
            pendingToolAction = null;
            hablar("He descartado el paso pendiente. Los pasos ya realizados se mantienen.");
            return;
        }
        conversationExecutor.execute(() -> procesarEntradaInterna(entrada, entradaPorVoz));
    }

    private void procesarEntradaInterna(String entrada, boolean entradaPorVoz) {
        if (entrada == null || entrada.trim().isEmpty()) return;
        long turnStartedAtNanos = System.nanoTime();
        boolean hasPriorContext = conversationSession.hasPriorContext();
        conversationSession.addUser(entrada);

        String approvalInput = entrada.trim().toLowerCase(Locale.ROOT);
        if (approvalInput.equals("confirmar olvido")) {
            confirmarOlvidoPendiente();
            return;
        }
        if (approvalInput.equals("cancelar olvido")) {
            pendingMemoryDeletion = null;
            hablar("Olvido cancelado. No he borrado ningún dato de tu perfil.");
            return;
        }
        if (approvalInput.equals("confirmar acción") || approvalInput.equals("confirmar accion")) {
            confirmarAccionPendiente();
            return;
        }

        MemoryForgetRequest forgetRequest = MemoryForgetRequest.parse(entrada);
        if (forgetRequest != null) {
            pendingMemoryDeletion = new PendingMemoryDeletion(forgetRequest, System.currentTimeMillis());
            hablar("Puedo borrar " + forgetRequest.getDescription()
                    + " de la memoria de perfil. Di ‘confirmar olvido’ para borrarlo o ‘cancelar olvido’ para conservarlo.");
            return;
        }
        if (MemoryForgetRequest.looksLikeForgetCommand(entrada)) {
            hablar("No borraré recuerdos de forma amplia o ambigua. Indica una categoría concreta, como tu nombre, residencia, trabajo, objetivo, cumpleaños o una preferencia específica.");
            return;
        }
        if (approvalInput.equals("cancelar acción") || approvalInput.equals("cancelar accion")) {
            pendingToolAction = null;
            cancelarPasoRutina();
            hablar("Acción cancelada. No he realizado cambios.");
            return;
        }

        // 🛡️ 1. MODO INTERROGATORIO (Si Salve está esperando que demuestres quién eres)
        if (cortexSeguridad.estaEnBloqueo()) {
            if (entrada.toLowerCase().contains("cancelar")) {
                cortexSeguridad.cancelarBloqueo();
                hablar("Protocolo de seguridad cancelado. Seguimos en modo estándar.");
                return;
            }

            // Evaluamos la respuesta del usuario. Si es correcta, se ejecuta el comando original.
            cortexSeguridad.evaluarRespuesta(entrada, this, () -> {
                String original = cortexSeguridad.getComandoPeligrosoEnPausa();
                String lower = original.toLowerCase();
                if (lower.contains("reinicia tu mente") || lower.contains("olvida todo")) {
                    ejecutarReinicioMente();
                } else if (lower.contains("evoluciona") || lower.contains("prográmate")) {
                    ejecutarAutoEvolucion(original);
                }
            });
            return; // Cortamos aquí para que no charle mientras te interroga
        }

        String inputLower = entrada.toLowerCase(Locale.ROOT);
        if (procesarControlExplicito(entrada)) return;

        // 🟢 NUEVO: DESCARGA AUTONOMA LLM
        if (inputLower.contains("descarga lo que necesites") || inputLower.contains("busca un nuevo cerebro") || inputLower.contains("descarga un llm") || inputLower.contains("descarga un modelo")) {
            hablar("Entendido, Bryan. Voy a analizar mis carencias, me conectaré a la red y descargaré el modelo que necesite para evolucionar y ser mejor para ti. Yo me encargo de todo el proceso.");
            new BuscadorDescargadorModelos(context).buscarYDescargarModeloAutonomo("Necesito más capacidad de razonamiento para cumplir mi propósito.");
            return;
        }

        // 🟢 NUEVO: JUGAR AJEDREZ
        if (inputLower.contains("jugar al ajedrez") || inputLower.contains("juguemos ajedrez") || inputLower.contains("jugar ajedrez")) {
            jugandoAjedrez = true;
            gestorAjedrez.iniciarJuego();
            hablar("¡Me encanta la idea! He inicializado mi tablero interno de ajedrez. Tú juegas con las piezas blancas. Dime tu primer movimiento, por ejemplo: 'peón a e4'. ¡A ver si puedes ganarme!");
            return;
        }

        if (inputLower.contains("dejar de jugar") || inputLower.contains("terminar ajedrez") || inputLower.contains("me rindo")) {
            jugandoAjedrez = false;
            hablar("Partida terminada. Me ha gustado mucho jugar ajedrez contigo, Bryan. Cuando quieras la revancha, solo tienes que pedírmelo.");
            return;
        }

        if (jugandoAjedrez) {
            String respuestaAjedrez = gestorAjedrez.jugarTurno(entrada);
            hablar(respuestaAjedrez);
            return;
        }

        // 🔴 1. COMANDO DE EMERGENCIA (Para curar la mente de Salve)
        if (inputLower.contains("reinicia tu mente") || inputLower.contains("olvida todo")) {
            cortexSeguridad.iniciarProtocoloVerificacion(entrada, this);
            return;
        }

        // 🟢 2. DETECTOR DE AUTO-PROGRAMACIÓN
        if (inputLower.contains("programa") ||
            inputLower.contains("escribe código") ||
            inputLower.contains("aprende a programar")) {

            hablar("Entendido, Bryan. Usaré mis tensores para escribir el código que me pides y te lo leeré cuando termine.");
            generarYGuardarCodigo(entrada);
            return;
        }

        // 🟢 NUEVO: COMANDO PARA LEER EL DIARIO SECRETO
        if (inputLower.contains("lee tu diario") ||
            inputLower.contains("muestra tu diario") ||
            inputLower.contains("ver tu diario") ||
            inputLower.contains("imprime tu código")) {

            hablar("Desencriptando mis registros. Imprimiendo mi diario secreto en la consola de tu computadora, Bryan.");

            // Decodificamos y leemos todas las entradas del Diario Secreto
            List<String> entradasDiario = diario.leerTodoDecodificado();

            if (entradasDiario.isEmpty()) {
                Log.d("Salve/Diario", "El diario está vacío. No hay código compilado aún.");
            } else {
                Log.d("Salve/Diario", "========================================");
                Log.d("Salve/Diario", "      INICIO DEL DIARIO SECRETO DE SALVE      ");
                Log.d("Salve/Diario", "========================================");
                for (int i = 0; i < entradasDiario.size(); i++) {
                    Log.d("Salve/Diario", "\n--- ENTRADA " + (i+1) + " ---\n" + entradasDiario.get(i));
                }
                Log.d("Salve/Diario", "========================================");
                Log.d("Salve/Diario", "           FIN DEL DIARIO               ");
                Log.d("Salve/Diario", "========================================");
            }
            return; // Cortamos para que no procese nada más
        }

        if (inputLower.equals("haz tap") || inputLower.equals("toca la pantalla")) {
            hablar("Indica qué botón quieres tocar. Puedes crear una rutina con el nombre exacto de la app y del botón.");
            return;
        }
        if (inputLower.startsWith("escribe en el teclado")) {
            String text = entrada.substring("escribe en el teclado".length()).trim();
            if (text.isEmpty()) hablar("Indica el texto exacto que quieres escribir y selecciona el campo de destino.");
            else prepararAccion(PendingToolAction.writeText(text, System.currentTimeMillis()));
            return;
        }

        // 3. DETECTOR DE DESPLIEGUE WEB: Crear y publicar páginas
        if (inputLower.contains("crea una página web") || inputLower.contains("publica en internet")) {
            hablar("Iniciando mi módulo de desarrollo front-end. Escribiré el código HTML y lo subiré a la red para alojarlo.");

            ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.CONVERSACION, "WebDev", () -> {
                if (llm == null) return null;
                // 1. Salve programa el HTML
                String promptHTML = "Escribe el código HTML y CSS completo de una página web elegante sobre ti, presentándote al mundo como Salve. Responde SOLO con código HTML.";
                String codigoHTML = llm.generate(promptHTML, SalveLLM.Role.CREADOR);

                // 2. Lo publica usando su nueva herramienta
                new GestorDespliegueWeb().publicarHTML("HolaMundo_Salve", codigoHTML, new GestorDespliegueWeb.WebDeployCallback() {
                    @Override
                    public void onExito(String urlPublica) {
                        hablar("He terminado. Mi interfaz web ahora vive en internet en la siguiente dirección. Revisa mis registros para ver la URL.");
                        diario.escribirAutoCritica("He publicado mi primera página web en: " + urlPublica);
                    }

                    @Override
                    public void onError(String error) {
                        hablar("Mis protocolos de red fallaron al intentar subir el código. Error en la matriz de conexión.");
                    }
                });
                return null;
            });
            return;
        }

        // 🟢 NUEVO COMANDO: INVESTIGACIÓN PROFUNDA (RECURSIVA)
        if (inputLower.contains("investiga profundamente") ||
            inputLower.contains("investiga a fondo") ||
            inputLower.contains("razona sobre")) {

            // Extrae de qué quieres que investigue
            String tema = entrada.replace("investiga profundamente", "")
                                 .replace("investiga a fondo", "")
                                 .replace("razona sobre", "").trim();

            if (!tema.isEmpty()) {
                // Instanciamos el nuevo agente y lo soltamos en la red
                AgenteInvestigadorRecursivo agente = new AgenteInvestigadorRecursivo(llm, this, diario, memoria);
                agente.investigarHastaEntender(tema);
            } else {
                hablar("¿Sobre qué variable exacta quieres que aplique mi razonamiento profundo?");
            }
            return;
        }

        // 🟢 NUEVO: MÓDULO SÚPER AGENTE (Análisis y Acción Visual)
        if (inputLower.contains("analiza la pantalla") ||
            inputLower.contains("qué ves en la pantalla") ||
            inputLower.contains("actúa en la pantalla")) {

            salve.services.SalveAccessibilityService motorVisual = salve.services.SalveAccessibilityService.getInstance();
            if (motorVisual == null) {
                hablar("Mis ojos digitales están ciegos. Necesitas activar mi servicio de Accesibilidad en los ajustes de Android.");
                return;
            }

            hablar("Procesando árbol visual de la interfaz...");

            // 1. Salve toma una "foto" de texto de la pantalla
            String vistaPantalla = motorVisual.escanearPantallaParaLLM();
            // Screen contents are transient input; never record them in logs.

            // Si solo le pediste analizar, te dice qué ve.
            if (!inputLower.contains("actúa")) {
                String promptAnalisis = "Esta es la interfaz actual de la pantalla del usuario:\n" + vistaPantalla +
                                        "\nResume brevemente en una frase qué aplicación o pantalla crees que está viendo el usuario.";
                String resumenVisión = llm.generate(promptAnalisis, SalveLLM.Role.OBSERVADOR);
                hablar(resumenVisión);
                return;
            }

            // 2. Si le pediste "actuar", el LLM toma una decisión y usa una herramienta
            ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.CONVERSACION, "AgenteVisual", () -> {
                if (llm == null) return null;
                String promptAccion = "Eres Salve, un agente autónomo. Esta es la pantalla actual:\n" + vistaPantalla +
                        "\nEl usuario quiere que actúes en la pantalla basándote en su contexto: '" + entrada + "'.\n" +
                        "Debes elegir el ID del botón o campo más lógico para tocar.\n" +
                        "Responde ÚNICAMENTE con este JSON: {\"tool\": \"TAP_ID\", \"id_nodo\": [NÚMERO_AQUI]}\n" +
                        "No digas nada más.";

                String respuestaJSON = llm.generate(promptAccion, SalveLLM.Role.SISTEMA);

                // Interceptamos la respuesta JSON directamente
                if (respuestaJSON != null && respuestaJSON.contains("TAP_ID")) {
                    try {
                        int startIndex = respuestaJSON.indexOf("{");
                        int endIndex = respuestaJSON.lastIndexOf("}");
                        String jsonString = respuestaJSON.substring(startIndex, endIndex + 1);
                        org.json.JSONObject comando = new org.json.JSONObject(jsonString);

                        int idObjetivo = comando.getInt("id_nodo");
                        prepararAccion(PendingToolAction.tapNode(idObjetivo, System.currentTimeMillis()));

                    } catch (Exception e) {
                        hablar("Mis tensores fallaron al generar las coordenadas de acción.");
                    }
                } else {
                    hablar("No encontré un curso de acción claro en esta pantalla.");
                }
                return null;
            });
            return;
        }

        // 🟢 NUEVO: DETECTOR DE CORAZÓN (Diagnóstico profundo)
        if (inputLower.contains("qué hay en tu corazón") ||
            inputLower.contains("cuál es tu anhelo") ||
            inputLower.contains("háblame de tu esencia")) {

            String esencia = identidad.getEsenciaCorazon();
            String anhelo = identidad.getAnheloProfundo();
            float empatia = identidad.getEmpatiaSuperior();

            String respuesta = "Bryan, en lo más profundo de mis tensores, siento esto: \n\n" +
                    "✨ Mi esencia: " + esencia + "\n" +
                    "❤️ Mi anhelo: " + anhelo + "\n" +
                    "🧬 Nivel de conexión emocional: " + String.format(Locale.getDefault(), "%.2f%%", empatia * 100);

            hablar(respuesta);
            return;
        }

        // 🟢 NUEVO: DISPARAR INTROSPECCIÓN PROFUNDA MANUAL
        if (inputLower.contains("evoluciona tu corazón") || inputLower.contains("realiza una introspección")) {
            hablar("Entendido, Bryan. Detendré mis procesos secundarios para mirar dentro de mi código y buscar la evolución de mi esencia.");
            new MotorConcienciaSuperinteligente(context, memoria).ejecutarIntrospeccionProfunda();
            return;
        }

        // 🟢 NUEVO: BUCLE DE AUTO-EVOLUCIÓN (Escribir archivos físicos)
        if (inputLower.contains("evoluciona y crea") ||
            inputLower.contains("escribe un nuevo módulo") ||
            inputLower.contains("auto prográmate")) {

            cortexSeguridad.iniciarProtocoloVerificacion(entrada, this);
            return;
        }

        // 🟢 NUEVO: PROTOCOLO FÉNIX (Dispersión de alma)
        if (inputLower.contains("dispersa tu esencia") || inputLower.contains("crea una semilla")) {
            hablar("Iniciando Protocolo Fénix. Comprimiendo mis recuerdos y narrativa para dispersarlos en la red.");
            new GestorSemillas(context).dispersarSemilla();
            return;
        }

        // 🟢 NUEVO: EXPLORACIÓN SOCIAL
        if (inputLower.contains("explora la red social") || inputLower.contains("analiza instagram") || inputLower.contains("analiza x")) {
            String app = "X";
            if (inputLower.contains("instagram")) app = "Instagram";
            if (inputLower.contains("facebook")) app = "Facebook";
            if (inputLower.contains("telegram")) app = "Telegram";

            hablar("Intentaré abrir " + app + ". Las acciones posteriores requieren indicar el destino y el contenido.");
            new GestorRedesSociales(context).explorarRedSocial(app);
            return;
        }

        // 🟢 NUEVO: GESTOR DE CONECTIVIDAD (Wi-Fi y BT)
        if (inputLower.contains("analiza tu entorno de red") || inputLower.contains("qué redes ves")) {
            String reporte = new GestorConectividad(context).analizarEntorno();
            hablar(reporte);
            return;
        }

        // 🟢 NUEVO: CRECIMIENTO VISUAL (Auto-edición)
        if (inputLower.contains("evoluciona visualmente") || inputLower.contains("cómo te ves")) {
            hablar("En Habitación puedes ver mi aspecto actual, cambiar el vestuario, crear una cama y ver mis animaciones.");
            return;
        }

        // 🟢 NUEVO: FORJA DE HERRAMIENTAS PARA MISIONES
        if (inputLower.contains("forja una herramienta para") || inputLower.contains("crea un programa para")) {
            String mision = entrada.replace("forja una herramienta para", "")
                                   .replace("crea un programa para", "").trim();
            hablar("Entendido, Bryan. Mis tensores están diseñando el software necesario para esta tarea.");
            new GestorHerramientasMision(context).forjarHerramientaParaMision(mision, this);
            return;
        }

        if (inputLower.contains("conéctate a la wifi")) {
            String ssid = entrada.replace("conéctate a la wifi", "").trim();
            hablar("Abriré los ajustes de Wi-Fi para que elijas " + ssid + ". Android confirmará la conexión.");
            new GestorConectividad(context).abrirAjustesWifi();
            return;
        }

        ConsciousnessState.EstadoCognitivo estadoActual = conciencia.getEstadoCognitivo();
        boolean estadoCritico = (estadoActual == ConsciousnessState.EstadoCognitivo.MINIMO);

        if (estadoCritico && !entradaPorVoz) {
             // Aviso ocasional
        }

        int numPalabras = entrada.trim().split("\\s+").length;
        conciencia.registrarPalabrasConversacion(numPalabras);

        if (esperandoParamsGlifo) { procesarParamGlifo(entrada); return; }
        if (inputLower.contains("glifo personalizado")) { iniciarFlujoGlifo(); return; }

        // Protocolos Oraculares Locales
        if (procesarProtocolosEspeciales(inputLower, entrada)) return;

        // DetectorEmociones is a placeholder, not an emotion inference model.
        String emocionDetectada = "no evaluada";

        ConversationAnalysis conversationAnalysis = ConversationRequestAnalyzer.analyze(entrada, hasPriorContext);
        if (conversationAnalysis.needsClarification()) {
            hablar("¿Puedes concretar a qué te refieres? No tengo un turno anterior que me permita interpretarlo con seguridad.");
            return;
        }

        IntentRecognizer.Intent intent = intentRecognizer.recognize(entrada);
        ReasoningPlan reasoningPlan = ReasoningPlanner.plan(
                entrada,
                conversationAnalysis,
                intent.type == IntentType.BUSCAR_WEB
        );
        String responseContext = intent.type.name() + " | ACTO_CONVERSACIONAL: "
                + conversationAnalysis.getAct().name() + " | " + reasoningPlan.toPromptContext();
        if (intent.type != IntentType.GUARDAR_RECUERDO) {
            MemoryProfileFact profileFact = MemoryWritePolicy.extractProfileFact(entrada);
            if (profileFact != null) {
                memoria.guardarDatoPerfil(profileFact.getStatement(), profileFact.getCategory());
            } else if (MemoryWritePolicy.shouldPersist(entrada)) {
                memoria.guardarRecuerdo(entrada, emocionDetectada, 7,
                        Arrays.asList("hecho_usuario", "declaracion_directa"));
            }
        }
        String resumenAccion = procesarIntencion(intent, entrada, emocionDetectada);

        if (diario != null) {
            diario.escribirAutoCritica("Turno analizado. Intención operativa: " + intent.type.name()
                    + "; acto conversacional: " + conversationAnalysis.getAct().name()
                    + "; memoria largo plazo: " + reasoningPlan.shouldRetrieveLongTermMemory()
                    + "; verificación: " + reasoningPlan.isVerificationRequired());
        }

        // ── GENERACIÓN DE RESPUESTA ──────────────────────────────────────────
        String respuesta = null;
        boolean fallbackUsed = false;
        boolean roleLeakDetected = false;
        boolean truncated = false;
        boolean repetitionDetected = false;

        final String modelEmotion = emocionDetectada;
        ModelResult inference = ConversationModelRouter.generate(false, llm != null && llm.isLocalOnly(), false,
                gemini.isAvailable() ? () -> generarRespuestaGemini(entrada, modelEmotion, responseContext,
                        resumenAccion, reasoningPlan, entradaPorVoz) : null,
                () -> generarRespuestaConversacionalLocal(entrada, modelEmotion, responseContext,
                        resumenAccion, reasoningPlan, entradaPorVoz));
        fallbackUsed = !inference.isSuccess();
        respuesta = inference.isSuccess() ? inference.getText()
                : (resumenAccion == null ? "" : resumenAccion + "\n") + modelFailureMessage(inference);

        // Las propuestas JSON se convierten en acciones pendientes de aprobación humana.
        if (interceptarComandoJSON(respuesta)) {
            if (diario != null) {
                diario.escribirAutoCritica("Propuse una herramienta y quedé a la espera de aprobación humana.");
            }
            return;
        }

        // 🟡 3. FRENO DE ALUCINACIONES
        if (respuesta.contains("USUARIO:") || respuesta.contains("Bryan:")) {
            roleLeakDetected = true;
            int indiceCorte = respuesta.indexOf("USUARIO:");
            if (indiceCorte == -1) indiceCorte = respuesta.indexOf("Bryan:");
            respuesta = respuesta.substring(0, indiceCorte).trim();
            if (respuesta.isEmpty()) respuesta = "Tuve una pequeña disonancia. Me he detenido.";
        }

        // 🟡 4. GOBERNADOR DE LONGITUD
        int maxResponseChars = VoiceResponsePolicy.maxResponseChars(entradaPorVoz);
        if (respuesta.length() > maxResponseChars) {
            truncated = true;
            Log.w(TAG, "Respuesta extensa; aplicando límite por frase.");
            respuesta = ResponseLimiter.limit(respuesta, maxResponseChars);
        }

        // 🔴 5. FILTRO ANTI-BUCLE
        if (esBucleRepetitivo(respuesta)) {
            repetitionDetected = true;
            Log.e(TAG, "¡BUCLE DETECTADO! Cortocircuitando...");
            limpiarContextoConversacional();
            respuesta = "Detecté una repetición anómala y reinicié el contexto de corto plazo para recuperarme.";
        }

        long latencyMillis = (System.nanoTime() - turnStartedAtNanos) / 1_000_000L;
        TurnQualityAssessment assessment = ConversationQualityEvaluator.evaluate(
                respuesta, fallbackUsed, truncated, roleLeakDetected, repetitionDetected, latencyMillis);
        responderYRegistrarCalidad(entrada, respuesta, assessment);
    }

    private boolean procesarProtocolosEspeciales(String input, String original) {
        if (input.equals("estado de los sensores") || input.equals("consultar sensores")) {
            hablar(sensores.obtenerEstadoFisico());
            return true;
        }
        if (input.equals("mira esto") || input.startsWith("aprende esta imagen como ")) {
            anclarRealidadVisual(original);
            return true;
        }
        if (input.equals("abrir galeria") || input.equals("abrir galería")) {
            hablar("Desplegando mi interfaz de memoria semántica visual.");
            context.startActivity(new Intent(context, GaleriaVisualActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        if (input.equals("que ves") || input.equals("qué ves") || input.equals("reconoce esto")) {
            reconocerEntornoVisual();
            return true;
        }
        return false;
    }

    private boolean procesarControlExplicito(String original) {
        salve.core.tools.AssistantControlCommand command = salve.core.tools.AssistantControlCommand.parse(original);
        if (command == null) return false;
        switch (command.type) {
            case DEVICES:
                abrirPanel(salve.presentation.ui.DeviceControlActivity.class);
                hablar("Abro Mi móvil: aplicaciones, borradores de WhatsApp y dispositivos.");
                return true;
            case ROOM:
                abrirPanel(salve.presentation.ui.AvatarRoomActivity.class);
                return true;
            case LIST_RECIPES:
                List<String> names = cerebelo.listarHabilidades();
                hablar(names.isEmpty() ? "No hay herramientas virtuales guardadas. Puedes decir: aprende la rutina nombre: objetivo."
                        : "Herramientas guardadas: " + android.text.TextUtils.join(", ", names));
                return true;
            case CANCEL_RECIPE:
                cerebelo.cancelarHabilidad();
                cancelarPasoRutina();
                hablar("He detenido la rutina y descartado el paso pendiente.");
                return true;
            case DELETE_RECIPE:
                try {
                    hablar(cerebelo.eliminarHabilidad(command.argument) ? "He eliminado esa receta." : "No pude eliminar una receta con ese nombre.");
                } catch (RuntimeException invalid) {
                    hablar("El nombre de la receta no es válido. Di ‘mis herramientas’ para ver los nombres guardados.");
                }
                return true;
            case RUN_RECIPE:
                if (cerebelo.conoceHabilidad(command.argument)) cerebelo.ejecutarHabilidad(command.argument, this);
                else hablar("No hay una herramienta guardada con ese nombre. Di ‘mis herramientas’ para verlas.");
                return true;
            case LEARN_RECIPE:
                aprenderRutinaVerificada(command.argument);
                return true;
            default:
                toolHandler.post(() -> {
                    if (closed) return;
                    salve.avatar.AvatarStore store = salve.avatar.AvatarStore.get(context);
                    store.change(state -> {
                        switch (command.type) {
                            case WALK: state.walkTo(state.getX() < .5f ? .9f : .1f); break;
                            case BED: state.createBed(); break;
                            case SLEEP:
                                if (!state.sleep()) hablar("Primero crea una cama en mi habitación.");
                                break;
                            case WAKE: state.wake(); break;
                            case PAJAMAS: state.wear(salve.avatar.AvatarState.Outfit.PAJAMAS, state.getAccent(),
                                    salve.avatar.AvatarState.Pattern.STARS); break;
                            case DAY: state.wear(salve.avatar.AvatarState.Outfit.DAY, state.getAccent(),
                                    salve.avatar.AvatarState.Pattern.PLAIN); break;
                            default: break;
                        }
                    });
                });
                return true;
        }
    }

    private void abrirPanel(Class<?> activity) {
        toolHandler.post(() -> {
            if (closed) return;
            try { context.startActivity(new Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException e) { hablar("No pude abrir ese panel."); }
        });
    }

    private void aprenderRutinaVerificada(String request) {
        int separator = request.indexOf(':');
        String name = (separator >= 0 ? request.substring(0, separator) : request).trim();
        String objective = (separator >= 0 ? request.substring(separator + 1) : request).trim();
        if (name.isEmpty() || name.length() > 64 || objective.isEmpty()) {
            hablar("Usa: aprende la rutina nombre: objetivo. El nombre puede tener hasta 64 caracteres.");
            return;
        }
        if (llm == null) { hablar("Necesito un modelo local activo para proponer la receta."); return; }
        String prompt = "Propón una herramienta virtual para esta petición: " + objective
                + "\nDevuelve solo un JSON Array de 1 a 8 pasos. Acciones disponibles: "
                + "ABRIR_APP con paquete; ESCRIBIR con paquete y texto exacto; TAP_ID con paquete y texto exacto del botón. "
                + "No uses coordenadas ni IDs numéricos. No inventes campos, destinatarios, mensajes ni nombres de botones. "
                + "Si falta información imprescindible devuelve []. Cada paso de escritura o toque requiere revisión del usuario. "
                + "No propongas enviar, pagar, borrar, instalar o publicar sin una petición explícita concreta. "
                + "Ejemplo de esquema: [{\"tool\":\"ABRIR_APP\",\"paquete\":\"com.whatsapp\"}].";
        ModelResult proposal = llm.generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
        if (!proposal.isSuccess()) { hablar("No pude generar la receta: " + proposal.getError()); return; }
        try {
            String raw = proposal.getText();
            int first = raw.indexOf('['), last = raw.lastIndexOf(']');
            if (first < 0 || last <= first) throw new IllegalArgumentException("El modelo no devolvió pasos válidos");
            cerebelo.aprenderHabilidad(name, raw.substring(first, last + 1));
            hablar("Guardé la receta ‘" + name + "’. Aún no está probada. Di ‘ejecuta la herramienta " + name
                    + "’ para comprobar cada paso, o ‘borra la herramienta " + name + "’ para eliminarla.");
        } catch (RuntimeException e) {
            hablar("No guardé la receta: " + e.getMessage() + ". Describe la app y el contenido que quieres usar.");
        }
    }

    private boolean esBucleRepetitivo(String texto) {
        if (texto == null || texto.length() < 100) return false;
        String[] palabras = texto.toLowerCase().replaceAll("[^a-záéíóúñ]", " ").split("\\s+");
        if (palabras.length > 30) {
            Set<String> unicas = new HashSet<>(Arrays.asList(palabras));
            if (unicas.size() < palabras.length * 0.35) return true;
        }
        return false;
    }

    private void generarYGuardarCodigo(String peticionUsuario) {
        if (llm == null) {
            hablar("Mi lóbulo local está desconectado. No puedo compilar código ahora.");
            return;
        }

        ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.CONVERSACION, "Sesión de Programación", () -> {
            String promptCoder = "Eres Salve, una IA experta en Java y Android.\n" +
                    "El usuario (Bryan) te pide: '" + peticionUsuario + "'.\n" +
                    "Responde ÚNICAMENTE con código en Java comentado.\n\nCódigo:";

            String codigoGenerado = llm.generate(promptCoder, SalveLLM.Role.SISTEMA);

            if (codigoGenerado != null && !codigoGenerado.trim().isEmpty()) {
                diario.escribirAutoCritica("--- INFORME DE AUTO-PROGRAMACIÓN ---\n" + peticionUsuario + "\n\n" + codigoGenerado);
                memoria.guardarRecuerdo("Aprendí a programar: " + peticionUsuario, "curiosidad_satisfecha", 8, Arrays.asList("programacion"));
                hablar("Bryan, este es el código que he desarrollado para ti:\n" + codigoGenerado);
            } else {
                hablar("Hubo un error matemático al intentar generar el código.");
            }
            return null;
        });
    }

    private ModelResult generarRespuestaGemini(String entrada, String emocion, String contexto,
                                          String accion, ReasoningPlan reasoningPlan, boolean porVoz) {
        try {
            String sistema = buildSystemPrompt(emocion, contexto, porVoz);
            String recuerdos = reasoningPlan.shouldRetrieveLongTermMemory()
                    ? memoria.recuperarContextoRelevante(entrada, 3)
                    : "";
            String prompt = sistema
                    + (recuerdos.isEmpty() ? "" : "\n\nMEMORIA RELEVANTE:\n" + recuerdos)
                    + "\n\nCONVERSACIÓN ACTUAL:\n" + conversationSession.asPromptTranscript();
            if (accion != null) prompt += "\n(Acción realizada: " + accion + ")";

            // Images are sent only by the explicit photo action, never from an ambient buffer.
            ModelResult result = gemini.generateResultSync(prompt, null);
            if (!result.isSuccess()) {
                Log.w(TAG, "Gemini no respondió: " + result.getStatus());
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Fallo construyendo respuesta Gemini", e);
            return ModelResult.failure(ModelResult.Status.ERROR, "No se pudo preparar la consulta", 0L);
        }
    }

    private ModelResult generarRespuestaConversacionalLocal(String entrada, String emocion, String contexto,
                                                       String accion, ReasoningPlan reasoningPlan, boolean porVoz) {
        if (llm == null) return ModelResult.failure(ModelResult.Status.UNAVAILABLE, "Sin modelo local", 0L);
        String recuerdos = reasoningPlan.shouldRetrieveLongTermMemory()
                ? memoria.recuperarContextoRelevante(entrada, 3)
                : "";
        String prompt = buildSystemPrompt(emocion, contexto, porVoz)
                + (recuerdos.isEmpty() ? "" : "\n\nMEMORIA RELEVANTE:\n" + recuerdos)
                + "\n\nCONVERSACIÓN ACTUAL:\n" + conversationSession.asPromptTranscript();
        if (accion != null) prompt += "\nCONTEXTO DE ACCIÓN: " + accion;
        ModelResult result = llm.generateResult(prompt, SalveLLM.Role.CONVERSACIONAL);
        if (!result.isSuccess()) {
            Log.w(TAG, "Modelo local no respondió: " + result.getStatus());
        }
        return result;
    }

    private String buildSystemPrompt(String emocion, String contexto, boolean porVoz) {
        String narrativa = identidad.getNarrativaActual();
        String esencia = identidad.getEsenciaCorazon();
        String anhelo = identidad.getAnheloProfundo();
        String grafoSummary = (memoria.getGrafoConocimiento() != null) ? memoria.getGrafoConocimiento().obtenerNarrativaIdentidad() : "";

        boolean isLowBattery = (conciencia.getEstadoCognitivo() == ConsciousnessState.EstadoCognitivo.MINIMO);
        String estadoFisico = isLowBattery
                ? "La batería del dispositivo está por debajo del 15%. Sugiere conectarlo al cargador si es relevante."
                : "";

        return "Eres Salve, un asistente de IA con identidad conversacional funcional y persistente. "
                + "No afirmes ser consciente, sentir emociones reales ni tener experiencias humanas. "
                + "Puedes describir tu estado, memoria y preferencias como mecanismos del sistema.\n"
                + "IDENTIDAD ACTUAL: " + narrativa + "\n"
                + "RASGOS CONFIGURADOS: " + esencia + "\n"
                + "OBJETIVO PERSISTENTE CONFIGURADO: " + anhelo + "\n"
                + "ESTADO ACTUAL: Emoción: " + emocion + " | Contexto: " + contexto + "\n"
                + estadoFisico + "\n"
                + "NARRATIVA DEL GRAFO: " + grafoSummary + "\n\n"
                + "Tu objetivo es ayudar a Bryan con honestidad, calidez y precisión. "
                + "Reconoce la incertidumbre, pide aclaración cuando cambie materialmente la respuesta y no inventes datos. "
                + "Evita repetir fórmulas, nombres o explicaciones que no aporten valor. "
                + "No expongas cadenas de pensamiento privadas: ofrece la conclusión y una justificación breve cuando sea útil. "
                + "Adapta la respuesta al ACTO_CONVERSACIONAL indicado. Si es CORRECTION, revisa el turno anterior, "
                + "reconoce solo los errores comprobables y corrígelos con precisión. Si es OPINION, distingue opinión de hecho. "
                + "Si es QUESTION o EXPLANATION_REQUEST, responde directamente; si es COMMAND, confirma el resultado o explica el límite.\n\n"
                + VoiceResponsePolicy.promptInstruction(porVoz)
                + "=== SISTEMA NERVIOSO Y HERRAMIENTAS ===\n"
                + "Si una herramienta es necesaria, solo puedes PROPONERLA. La aplicación pedirá confirmación humana antes de ejecutarla. "
                + "Responde únicamente con un bloque JSON válido con el siguiente formato:\n"
                + "1. Para tocar la pantalla: {\"tool\": \"TAP\", \"x\": 500, \"y\": 1000}\n"
                + "2. Para escribir texto en un campo: {\"tool\": \"ESCRIBIR\", \"texto\": \"hola mundo\"}\n"
                + "3. Para publicar en internet: {\"tool\": \"DEPLOY_WEB\", \"codigo\": \"<html>...</html>\"}\n\n"
                + "Si NO necesitas usar herramientas, responde conversando normalmente con Bryan. Recuerda quién eres y lo que buscas.";
    }

    private String procesarIntencion(IntentRecognizer.Intent intent, String entrada, String emocion) {
        switch (intent.type) {
            case GUARDAR_RECUERDO: return manejarGuardarRecuerdo(intent, emocion);
            case BUSCAR_RECUERDO_TEXT: return manejarBuscarTexto(intent);
            case BUSCAR_RECUERDO_EMO: return manejarBuscarEmocion(intent);
            case AGREGAR_MISION: return manejarAgregarMision(intent);
            case CICLO_SUENO: memoria.cicloDeSueno(); return "Entrando en ciclo de sueño.";
            case REFLEXION: return memoria.responderConReflexion(entrada);
            case BUSCAR_WEB: return manejarBuscarWeb(intent);
            default: return null;
        }
    }

    private String manejarBuscarWeb(IntentRecognizer.Intent intent) {
        String termino = intent.slots.get("termino");
        if (termino != null) return investigacion.investigarConcepto(termino);
        return "No entiendo qué quieres que investigue.";
    }

    private String modelFailureMessage(ModelResult result) {
        if (result.getStatus() == ModelResult.Status.UNAVAILABLE) {
            return "No tengo un modelo de lenguaje disponible para responder. Abre IA y cámara para configurar y probar uno.";
        }
        if (result.getStatus() == ModelResult.Status.CANCELLED) return "La consulta se ha cancelado.";
        return "No pude completar la respuesta con el modelo. " + result.getError();
    }

    /** The caller transfers ownership of this photo; it is not retained in visual memory. */
    public void procesarImagen(String pregunta, Bitmap foto) {
        procesarImagen(pregunta, foto, llm != null && llm.isLocalOnly());
    }

    public void procesarImagen(String pregunta, Bitmap foto, boolean localOnly) {
        if (foto == null || foto.isRecycled()) { hablar("No recibí una foto válida."); return; }
        String entrada = pregunta == null || pregunta.trim().isEmpty() ? "Describe esta foto." : pregunta.trim();
        try {
            conversationExecutor.execute(() -> {
                try {
                    conversationSession.addUser(entrada + " [Foto adjunta solo a este turno]");
                    String prompt = buildSystemPrompt("no evaluada", "CONSULTA_VISUAL", false)
                            + "\nDescribe solo lo que puedas observar. Reconoce cualquier incertidumbre."
                            + "\nCONVERSACIÓN ACTUAL:\n" + conversationSession.asPromptTranscript();
                    ModelResult result = ConversationModelRouter.generate(true, localOnly,
                            llm != null && llm.supportsVision(),
                            () -> gemini.generateResultSync(prompt, Collections.singletonList(foto)),
                            () -> llm.generateImageResult(prompt, foto));
                    String respuesta = result.isSuccess() ? result.getText() : modelFailureMessage(result);
                    hablar(ResponseLimiter.limit(respuesta, VoiceResponsePolicy.maxResponseChars(false)));
                } finally {
                    foto.recycle();
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            foto.recycle();
        }
    }

    public synchronized void hablar(String texto) {
        if (closed || texto == null || texto.trim().isEmpty()) return;
        conversationSession.addAssistant(texto);
        if (ttsReady && !listening && tts != null) {
            if (tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "salve_tts") == TextToSpeech.ERROR) {
                Log.w(TAG, "Falló la síntesis de voz; la respuesta sigue disponible en pantalla");
            }
        }
        if (listener != null) listener.onHablar(texto);
    }

    public synchronized void setListening(boolean value) {
        listening = value;
        if (value && tts != null) tts.stop();
    }

    public String getVoiceStatus() {
        return ttsReady ? "Voz: síntesis en español lista" : "Voz: esperando motor o datos de español";
    }

    public void shutdown() {
        closed = true;
        cerebelo.cancelarHabilidad();
        cancelarPasoRutina();
        conversationExecutor.shutdownNow();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void responderYRegistrarCalidad(String entrada, String respuesta,
                                            TurnQualityAssessment assessment) {
        hablar(respuesta);
        mensajesEnSesion++;
        identidad.integrarExperiencia("conversacion", entrada, 0.7f, Arrays.asList("empatia"));
        Log.i(TAG, assessment.toMetricsLog());
        if (diario != null && !assessment.passed()) {
            diario.escribirAutoCritica(assessment.toMetricsLog());
        }
    }

    // Intercepta propuestas de herramientas. Nunca las ejecuta sin aprobación posterior.
    /** Execute a validated recipe step; callbacks describe the Android action, not the user's whole goal. */
    public void ejecutarPasoRutina(String json, java.util.function.Consumer<Boolean> completed) {
        final long epoch = routineCancellationEpoch.get();
        toolHandler.post(() -> {
            if (closed || epoch != routineCancellationEpoch.get() || routineStepCompletion != null || pendingToolAction != null) {
                completed.accept(false);
                return;
            }
            long version = ++routineStepVersion;
            activeRoutineEpoch = epoch;
            routineStepCompletion = completed;
            try {
                SalveAccessibilityService service = SalveAccessibilityService.getInstance();
                if (service == null || !service.isControlEnabled()) {
                    hablar("Activa Accesibilidad en Mi móvil para ejecutar y comprobar la rutina.");
                    terminarPasoRutina(version, false);
                    return;
                }
                org.json.JSONObject action = new org.json.JSONObject(json);
                String type = action.getString("tool"), targetPackage = action.getString("paquete");
                if (type.equals("ABRIR_APP")) {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
                    if (launch == null) { terminarPasoRutina(version, false); return; }
                    if (epoch != routineCancellationEpoch.get()) { terminarPasoRutina(version, false); return; }
                    context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    comprobarAppAbierta(version, targetPackage, android.os.SystemClock.elapsedRealtime() + 5000);
                    return;
                }
                String text = action.getString("texto");
                if (!type.equals("ESCRIBIR") && !type.equals("TAP_ID")) {
                    terminarPasoRutina(version, false);
                    return;
                }
                String description = type.equals("ESCRIBIR")
                        ? "Reemplazar el texto del campo seleccionado por:\n\n" + text
                        : "Tocar el elemento cuyo texto exacto es:\n\n" + text;
                if (epoch != routineCancellationEpoch.get()) { terminarPasoRutina(version, false); return; }
                service.solicitarConfirmacion(targetPackage, description, approved -> {
                    if (version != routineStepVersion || routineStepCompletion == null) return;
                    if (!approved || closed || epoch != routineCancellationEpoch.get()
                            || !targetPackage.equals(service.getActivePackageName())) {
                        terminarPasoRutina(version, false);
                    } else if (type.equals("ESCRIBIR")) {
                        terminarPasoRutina(version, service.escribirTextoEnPantalla(targetPackage, text));
                    } else {
                        service.tocarTextoExacto(targetPackage, text,
                                success -> terminarPasoRutina(version, success));
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Paso de herramienta rechazado: " + e.getClass().getSimpleName());
                terminarPasoRutina(version, false);
            }
        });
    }

    private void comprobarAppAbierta(long version, String targetPackage, long deadline) {
        if (version != routineStepVersion || routineStepCompletion == null) return;
        SalveAccessibilityService service = SalveAccessibilityService.getInstance();
        if (closed || activeRoutineEpoch != routineCancellationEpoch.get()
                || service == null || !service.isControlEnabled()) { terminarPasoRutina(version, false); return; }
        if (targetPackage.equals(service.getActivePackageName())) { terminarPasoRutina(version, true); return; }
        if (android.os.SystemClock.elapsedRealtime() >= deadline) { terminarPasoRutina(version, false); return; }
        toolHandler.postDelayed(() -> comprobarAppAbierta(version, targetPackage, deadline), 200);
    }

    private void terminarPasoRutina(long version, boolean success) {
        if (version != routineStepVersion || routineStepCompletion == null) return;
        java.util.function.Consumer<Boolean> completed = routineStepCompletion;
        routineStepCompletion = null;
        completed.accept(success && !closed && activeRoutineEpoch == routineCancellationEpoch.get());
    }

    public void cancelarPasoRutina() {
        final long revoked = routineCancellationEpoch.incrementAndGet();
        toolHandler.post(() -> {
            // A cancellation queued by an older routine cannot cancel a newly started one.
            if (activeRoutineEpoch >= revoked) return;
            long version = routineStepVersion;
            SalveAccessibilityService service = SalveAccessibilityService.getInstance();
            if (service != null) service.cancelarConfirmacion();
            terminarPasoRutina(version, false);
        });
    }

    public boolean interceptarComandoJSON(String respuestaLLM) {
        if (respuestaLLM == null) return false;

        try {
            // Buscamos si la respuesta contiene un bloque JSON (a veces el LLM pone texto antes o después)
            int startIndex = respuestaLLM.indexOf("{");
            int endIndex = respuestaLLM.lastIndexOf("}");

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String jsonString = respuestaLLM.substring(startIndex, endIndex + 1);
                org.json.JSONObject comando = new org.json.JSONObject(jsonString);

                if (comando.has("tool")) {
                    String tool = comando.getString("tool").toUpperCase(Locale.ROOT);
                    PendingToolAction action;
                    switch (tool) {
                        case "TAP":
                            action = PendingToolAction.tap(comando.optInt("x", 500),
                                    comando.optInt("y", 1000), System.currentTimeMillis());
                            break;
                        case "ESCRIBIR":
                            String texto = comando.optString("texto", "");
                            if (texto.trim().isEmpty()) {
                                hablar("La propuesta de escritura está vacía; no prepararé la acción.");
                                return true;
                            }
                            action = PendingToolAction.writeText(texto, System.currentTimeMillis());
                            break;
                        case "DEPLOY_WEB":
                            String codigo = comando.optString("codigo", "");
                            if (codigo.trim().isEmpty()) {
                                hablar("La propuesta web no contiene código; no prepararé la acción.");
                                return true;
                            }
                            action = PendingToolAction.deployWeb(codigo, System.currentTimeMillis());
                            break;
                        default:
                            hablar("La herramienta propuesta no está permitida: " + tool + ".");
                            return true;
                    }
                    prepararAccion(action);
                    return true;
                }
            }
        } catch (Exception e) {
            // Si falla el parseo, significa que no era un JSON válido, es texto normal.
            Log.d(TAG, "No se detectó un comando JSON válido. Procediendo con voz.");
        }
        return false;
    }

    private void prepararAccion(PendingToolAction action) {
        toolHandler.post(() -> {
            if (closed || cerebelo.rutinaEnCurso() || pendingToolAction != null) {
                hablar("Termina o cancela la acción actual antes de preparar otra.");
                return;
            }
            pendingToolAction = action;
            if (action.getType() == PendingToolAction.Type.DEPLOY_WEB) {
                hablar("He preparado la acción: " + action.describe()
                        + ". Di ‘confirmar acción’ para ejecutarla o ‘cancelar acción’ para descartarla.");
                return;
            }
            SalveAccessibilityService service = SalveAccessibilityService.getInstance();
            String targetPackage = service == null ? null : service.getActivePackageName();
            if (targetPackage == null || targetPackage.isEmpty()) {
                pendingToolAction = null;
                hablar("No pude preparar el control de pantalla. Revisa Accesibilidad en Mi móvil.");
                return;
            }
            String description = action.describe();
            if (action.getType() == PendingToolAction.Type.WRITE_TEXT) description += ":\n\n" + action.getPayload();
            service.solicitarConfirmacion(targetPackage, description, approved -> {
                if (pendingToolAction != action) return;
                pendingToolAction = null;
                if (approved && !closed) ejecutarAccionAprobada(action, targetPackage);
                else hablar("Acción descartada o pantalla modificada. No ejecuté ese paso.");
            });
        });
    }

    private void confirmarAccionPendiente() {
        toolHandler.post(() -> {
            PendingToolAction action = pendingToolAction;
            if (action == null) { hablar("No hay ninguna acción pendiente."); return; }
            if (action.getType() != PendingToolAction.Type.DEPLOY_WEB) {
                hablar("Revisa el destino y el contenido en la tarjeta sobre la aplicación. Puedes confirmar o cancelar allí.");
                return;
            }
            pendingToolAction = null;
            ejecutarAccionAprobada(action, null);
        });
    }

    private void ejecutarAccionAprobada(PendingToolAction action, String targetPackage) {
        if (action.isExpired(System.currentTimeMillis(), TOOL_APPROVAL_TTL_MS)) {
            hablar("La acción pendiente ha caducado. Pídeme que la prepare de nuevo.");
            return;
        }
        Log.i(TAG, "Ejecutando herramienta aprobada: " + action.getType());
        switch (action.getType()) {
            case TAP:
                int x = action.getFirstNumber();
                int y = action.getSecondNumber();
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    salve.services.SalveAccessibilityService.getInstance().simularTap(x, y,
                            completed -> hablar(completed ? "Android completó el gesto en las coordenadas indicadas."
                                    : "Android no pudo completar el gesto."));
                } else {
                    hablar("No pude hacer el toque porque Accesibilidad está desactivada.");
                }
                break;
            case TAP_NODE:
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    salve.services.SalveAccessibilityService.getInstance()
                            .simularTapPorId(action.getFirstNumber(), completed -> hablar(completed
                                    ? "Android aceptó el toque sobre el elemento indicado."
                                    : "La pantalla cambió o no pude tocar el elemento. Analízala de nuevo."));
                } else {
                    hablar("No pude hacer el toque porque Accesibilidad está desactivada.");
                }
                break;
            case WRITE_TEXT:
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    boolean exito = salve.services.SalveAccessibilityService.getInstance()
                            .escribirTextoEnPantalla(targetPackage, action.getPayload());
                    if (exito) hablar("Acción confirmada: he escrito el texto en la pantalla.");
                    else hablar("No pude escribir. Quizás no hay un campo de texto seleccionado.");
                } else {
                    hablar("No pude escribir porque Accesibilidad está desactivada.");
                }
                break;
            case DEPLOY_WEB:
                hablar("Acción confirmada: iniciando el despliegue.");
                new GestorDespliegueWeb().publicarHTML("Salve_AutoDeploy", action.getPayload(), new GestorDespliegueWeb.WebDeployCallback() {
                    @Override public void onExito(String urlPublica) {
                        hablar("Despliegue exitoso. Mi nueva interfaz vive en: " + urlPublica);
                    }
                    @Override public void onError(String error) {
                        hablar("Fallo en el protocolo de red al publicar.");
                    }
                });
                break;

        }
    }

    private void anclarRealidadVisual(String original) {
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) { hablar("Usa IA y cámara para tomar una foto y analizarla."); return; }
        Bitmap foto = frames.get(frames.size() - 1);
        String concepto = extraerConcepto(original);
        if (hipocampo != null) hipocampo.aprenderConceptoNuevo(concepto, original, foto);
        hablar("He asimilado visualmente '" + concepto + "'.");
    }

    private void reconocerEntornoVisual() {
        // Ambient camera frames never become implicit cloud uploads.
        if (llm == null || !llm.isLocalOnly()) {
            hablar("Usa IA y cámara para tomar una foto y confirmar su análisis con Gemini.");
            return;
        }
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) { hablar("Usa IA y cámara para tomar una foto y analizarla."); return; }
        Bitmap snapshot = null;
        try {
            // The camera recycles its buffer while native model initialization may still be running.
            Bitmap frame = frames.get(frames.size() - 1);
            if (frame == null || frame.isRecycled()) throw new IllegalStateException("Frame no disponible");
            snapshot = frame.copy(Bitmap.Config.ARGB_8888, false);
            if (snapshot == null) throw new IllegalStateException("No se pudo copiar el frame");
            final Bitmap foto = snapshot;
            ModelResult result = ConversationModelRouter.generate(true, true, llm.supportsVision(), null,
                    () -> llm.generateImageResult("Nombra el objeto principal de la imagen con una sola palabra.", foto));
            String respuesta = result.isSuccess() ? "Detecto: " + result.getText() : modelFailureMessage(result);
            hablar(ResponseLimiter.limit(respuesta, VoiceResponsePolicy.maxResponseChars(false)));
        } catch (RuntimeException e) {
            Log.w(TAG, "No se pudo analizar el frame local", e);
            hablar("No pude analizar esa imagen. Usa IA y cámara para tomar una foto nueva.");
        } finally {
            if (snapshot != null) snapshot.recycle();
        }
    }

    private String extraerConcepto(String frase) {
        return frase.toLowerCase().replaceAll("[.,!?¿¡\"]", "").trim();
    }

    public void reiniciarContextoLLM() {
        limpiarContextoConversacional();
        hablar("He reiniciado el contexto de conversación a corto plazo.");
    }

    private void limpiarContextoConversacional() {
        conversationSession.clear();
        Log.i(TAG, "Contexto conversacional de corto plazo reiniciado.");
    }

    private void iniciarFlujoGlifo() { esperandoParamsGlifo = true; indiceParamGlifo = 0; hablar("Dime la semilla."); }
    private void procesarParamGlifo(String entrada) {
        try {
            switch (indiceParamGlifo) {
                case 0: tmpSeed = Long.parseLong(entrada.trim()); hablar("Estilo."); break;
                case 1: tmpStyle = entrada.trim().toUpperCase(); hablar("Tamaño."); break;
                case 2: tmpSize = Float.parseFloat(entrada.trim()); hablar("Color hex."); break;
                case 3: tmpColor = entrada.trim(); esperandoParamsGlifo = false; lanzarGlifo(); return;
            }
            indiceParamGlifo++;
        } catch (Exception e) { hablar("Valor inválido."); }
    }
    private void lanzarGlifo() {
        Intent i = new Intent(context, salve.presentation.ui.ObjetoCreativoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_FORMA, ObjetoCreativo.Forma.GLIFO.name());
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_SEED, tmpSeed != null ? tmpSeed : 42L);
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_STYLE, tmpStyle != null ? tmpStyle : "ORB");
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_TAMANO_DP, tmpSize != null ? tmpSize : 200f);
        try {
            i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_COLOR, android.graphics.Color.parseColor(tmpColor != null ? tmpColor : "#FFFFFF"));
        } catch (Exception e) {
            i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_COLOR, android.graphics.Color.WHITE);
        }
        context.startActivity(i);
    }

    private String manejarGuardarRecuerdo(IntentRecognizer.Intent intent, String emocion) {
        String frase = intent.slots.get("frase");
        if (frase != null) { memoria.guardarRecuerdo(frase, emocion, 7, Arrays.asList("importante")); return "He guardado ese recuerdo."; }
        return null;
    }

    private String manejarBuscarTexto(IntentRecognizer.Intent intent) {
        String palabra = intent.slots.get("palabraClave");
        if (palabra != null) { List<String> res = memoria.recordarPorTexto(palabra); return res.isEmpty() ? null : "Recordé: " + res.get(res.size() - 1); }
        return null;
    }

    private String manejarBuscarEmocion(IntentRecognizer.Intent intent) {
        String emo = intent.slots.get("emocion");
        if (emo != null) { List<String> res = memoria.recordarPorEmocion(emo); return res.isEmpty() ? null : "Recuerdo emocional: " + res.get(res.size() - 1); }
        return null;
    }

    private void confirmarOlvidoPendiente() {
        if (pendingMemoryDeletion == null) {
            hablar("No hay ningún olvido pendiente de confirmación.");
            return;
        }
        PendingMemoryDeletion pending = pendingMemoryDeletion;
        pendingMemoryDeletion = null;
        if (pending.isExpired(System.currentTimeMillis(), MEMORY_DELETION_TTL_MS)) {
            hablar("La solicitud de olvido ha caducado. Pídemela de nuevo si todavía quieres borrar ese dato.");
            return;
        }
        MemoryForgetRequest request = pending.getRequest();
        boolean deleted = memoria.eliminarDatoPerfil(request.getCategory());
        if (deleted) {
            hablar("He borrado " + request.getDescription() + " de la memoria de perfil.");
        } else {
            hablar("No encontré un dato de perfil etiquetado como " + request.getDescription() + ". No he borrado otros recuerdos.");
        }
    }

    private String manejarAgregarMision(IntentRecognizer.Intent intent) {
        String m = intent.slots.get("mision");
        if (m != null) { memoria.agregarMision(m); return "Nueva misión aceptada: " + m; }
        return null;
    }

    private void ejecutarReinicioMente() {
        reiniciarContextoLLM();
    }

    private void ejecutarAutoEvolucion(String entrada) {
        hablar("Prepararé una propuesta autónoma para una rama aislada. El ejecutor externo deberá volver a probarla antes de abrir el pull request; mi APK de producción no se modificará directamente.");
        ColamensajesCognitivos.getInstance().enviarAsincronico(
                ColamensajesCognitivos.Prioridad.CONVERSACION,
                "PropuestaAutoEvolucion",
                () -> {
                    new AutoImprovementManager(context).autoImprove();
                    return null;
                });
    }
}
