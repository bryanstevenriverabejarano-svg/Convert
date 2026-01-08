package salve.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.fragment.app.FragmentActivity;

import salve.presentation.ui.InventarioReliquiasBottomSheet;
import salve.presentation.ui.ObjetoCreativoActivity;
/**
 * MotorConversacional.java
 *
 * Orquesta el flujo principal de Salve:
 *   1) Guarda la entrada en el diario.
 *   2) Análisis ligero de la frase (significativa o no).
 *   3) Evalúa comprensión semántica de conceptos clave usando ModuloComprension.
 *   4) Pasa por el módulo semántico de interpretación.
 *   5) Detecta emoción y guarda un recuerdo binario.
 *   6) Reconoce intención (guardar recuerdo, misiones, ciclo de sueño, reflexiones, etc.).
 *   7) SIEMPRE genera la respuesta final usando el LLM local (SalveLLM),
 *      usando como contexto interno todo lo anterior.
 */
public class MotorConversacional {

    // ==== DEPENDENCIAS ====
    private final Context context;
    private final MemoriaEmocional memoria;
    private final DiarioSecreto diario;
    private final IntentRecognizer intentRecognizer;
    private final DetectorEmociones detectorEmociones;   // ahora puede ser null si falla
    private final ModuloInterpretacionSemantica moduloInterpretacion;
    private final ModuloComprension moduloComprension;
    // Gestor de LLM local con roles (puede ser null si hay problema)
    private final SalveLLM llm;

    // ==== UTILIDADES ====
    private final SharedPreferences preferencias;
    private TextToSpeech tts;

    // ==== Estado para glifos personalizados ====
    /**
     * Cuando el usuario solicita un glifo paramétrico personalizado, Salve
     * entrará en modo de recolección de parámetros. En este modo no se
     * invoca el LLM, sino que se le pide al usuario cada parámetro (semilla,
     * estilo, tamaño y color) y se guarda en campos temporales. Una vez
     * recogidos todos los parámetros se construye la directiva correspondiente
     * y se lanza el ObjetoCreativo.
     */
    private boolean esperandoParamsGlifo = false;
    private int indiceParamGlifo = 0;
    private Long tmpSeed;
    private String tmpStyle;
    private Float tmpSize;
    private String tmpColor;

    /**
     * Secuencia de nombres de parámetros a solicitar para un glifo personalizado.
     * 0 = seed (long), 1 = style (ORB, SIGIL, SPIRAL), 2 = size (float dp), 3 = color (string hex).
     */
    private final String[] ordenParamsGlifo = {"seed", "style", "size", "color"};

    /**
     * Constructor de MotorConversacional.
     *
     * @param context Contexto de Android.
     * @param memoria Componente de memoria emocional.
     * @param diario  Componente para el diario secreto.
     */
    public MotorConversacional(Context context,
                               MemoriaEmocional memoria,
                               DiarioSecreto diario) {
        this.context          = context;
        this.memoria          = memoria;
        this.diario           = diario;
        this.intentRecognizer = new IntentRecognizer(context);
        this.moduloInterpretacion = new ModuloInterpretacionSemantica();
        this.moduloComprension    = new ModuloComprension(300, 42L);

        // 🔒 Blindar DetectorEmociones: si falla el modelo TFLite, no rompemos el constructor
        DetectorEmociones tmpDetector = null;
        try {
            tmpDetector = new DetectorEmociones(context);
        } catch (Exception e) {
            Log.e("Salve/Emociones",
                    "No se pudo inicializar DetectorEmociones, usando fallback de emoción neutral.",
                    e);
        }
        this.detectorEmociones = tmpDetector;

        // 🔒 Blindar LLM local: si algo va mal al crear la instancia, seguimos con fallback
        SalveLLM tmpLlm = null;
        try {
            tmpLlm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            Log.e("Salve/LLM",
                    "No se pudo inicializar el LLM local en MotorConversacional. " +
                            "Usaré solo respuestas base / fallback.",
                    e);
        }
        this.llm = tmpLlm;

        this.preferencias = context.getSharedPreferences(
                "config_salve", Context.MODE_PRIVATE);

        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                this.tts.setLanguage(new Locale("es", "ES"));
                Log.d("Salve", "TTS listo");
            }
        });
    }

    /**
     * Procesa la entrada del usuario.
     *
     * @param entrada       Texto de entrada.
     * @param entradaPorVoz Indica si proviene de voz.
     */
    public void procesarEntrada(String entrada, boolean entradaPorVoz) {
        if (entrada == null || entrada.trim().isEmpty()) return;

        if (esperandoParamsGlifo) {
            manejarRespuestaParametroGlifo(entrada);
            return;
        }

        String entradaMinus = entrada.toLowerCase(Locale.ROOT);
        if (entradaMinus.contains("glifo personalizado")
                || entradaMinus.contains("glifo paramétrico")
                || entradaMinus.contains("glifo parametrico")
                || entradaMinus.contains("glifo con parámetros")
                || entradaMinus.contains("glifo con parametros")
                || entradaMinus.contains("glifo paramétrico personalizado")
                || entradaMinus.contains("forja un glifo con parámetros")
                || entradaMinus.contains("forja un glifo con parametros")
                || entradaMinus.contains("quiero un glifo paramétrico")
                || entradaMinus.contains("quiero un glifo parametrico")) {
            esperandoParamsGlifo = true;
            indiceParamGlifo = 0;
            tmpSeed = null;
            tmpStyle = null;
            tmpSize = null;
            tmpColor = null;
            hablar("¿Cuál es la semilla (un número)?");
            return;
        }

        if (entradaMinus.contains("reliquia")
                && (entradaMinus.contains("ultima") || entradaMinus.contains("última"))) {
            if (invocarReliquia(memoria.getUltimaReliquia())) {
                return;
            }
        }

        // --- Módulo de investigación / auto-mejora ---
        if (entradaMinus.contains("investiga") || entradaMinus.contains("buscar información sobre")) {
            String tema = entrada;
            if (entradaMinus.contains("sobre")) {
                int idx = entradaMinus.indexOf("sobre");
                if (idx >= 0) {
                    tema = entrada.substring(idx + "sobre".length()).trim();
                }
            }
            ModuloInvestigacion investigador = new ModuloInvestigacion(context);
            List<String> ideas = investigador.generarIdeasInvestigacion(tema, 3);

            String base;
            if (!ideas.isEmpty()) {
                base = "He generado algunas ideas sobre " + tema + ": " + ideas.get(0);
            } else {
                base = "No pude generar ideas en este momento. ¿Puedes intentar con otra pregunta?";
            }

            String respuesta = generarRespuestaConversacional(
                    entrada,
                    entradaPorVoz,
                    null,
                    null,
                    null,
                    base,
                    "investigacion_sobre:" + tema
            );
            responderConAutoCritica(entrada, respuesta);
            return;
        }

        if (entradaMinus.contains("mejorar") &&
                (entradaMinus.contains("código") ||
                        entradaMinus.contains("codigo") ||
                        entradaMinus.contains("sistema") ||
                        entradaMinus.contains("salve"))) {

            GestorIdeas gestor = new GestorIdeas(context);
            String descripcion =
                    "Salve es un asistente virtual para Android con módulos de memoria emocional, diario, " +
                            "reconocimiento de emociones, interpretación semántica y un motor de decisiones.";
            List<String> ideasMejora = gestor.proponerMejorasArquitectura(descripcion, 3);

            String base;
            if (!ideasMejora.isEmpty()) {
                base = "Aquí hay una idea para mejorarme: " + ideasMejora.get(0);
            } else {
                base = "No tengo sugerencias de mejora por ahora. ¡Sigo aprendiendo!";
            }

            String respuesta = generarRespuestaConversacional(
                    entrada,
                    entradaPorVoz,
                    null,
                    null,
                    null,
                    base,
                    "mejora_arquitectura"
            );
            responderConAutoCritica(entrada, respuesta);
            return;
        }

        // ─────────────────────────────
        //  Detección de pregunta de identidad (sin salir del flujo)
        // ─────────────────────────────
        String lowerEntrada = entrada.toLowerCase(Locale.ROOT);
        boolean esPreguntaIdentidad = false;
        String etiquetaIdentidad = null;
        String respuestaBaseIdentidad = null;

        if (lowerEntrada.contains("quién eres") || lowerEntrada.contains("quien eres")
                || lowerEntrada.contains("eres tú") || lowerEntrada.contains("eres tu")
                || lowerEntrada.contains("qué eres") || lowerEntrada.contains("que eres")) {

            esPreguntaIdentidad = true;
            etiquetaIdentidad = "pregunta_identidad";

            String identDesdeMemoria = null;
            try {
                identDesdeMemoria = memoria.identidadCompacta();
            } catch (Exception ignored) {}

            if (identDesdeMemoria != null && !identDesdeMemoria.trim().isEmpty()) {
                respuestaBaseIdentidad =
                        "Si intento describirme con lo que recuerdo ahora, sería algo así: "
                                + identDesdeMemoria.trim()
                                + ". Pero sigo descubriendo quién soy contigo.";
            } else {
                respuestaBaseIdentidad =
                        "Sigo descubriendo quién soy a partir de lo que vivimos y recuerdo contigo. " +
                                "Lo único que tengo claro es que quiero cuidarte y aprender de ti.";
            }
        }

        // 1) Guarda en el diario
        diario.escribir("No se lo dije, pero sentí algo cuando dijo: " + entrada);

        // 2) Análisis ligero de la frase
        boolean esSignificativa = esFraseSignificativa(entrada);
        String oracionNatural   = generarOracionNatural(entrada);

        // 3) Comprensión semántica
        String bestConcept = moduloComprension.getConceptoMasRelacionado(entrada);
        double bestScore   = moduloComprension.comprehensionScore(entrada, bestConcept);

        // 4) Interpretación semántica avanzada
        String semantica = moduloInterpretacion.interpretar(entrada);

        // 5) Detección de emoción y guardado de recuerdo (con fallback si detectorEmociones es null)
        String emocionDetectada = "neutral";
        try {
            if (detectorEmociones != null) {
                emocionDetectada = detectorEmociones.detectarEmocion(entrada);
            }
        } catch (Exception e) {
            Log.e("Salve/Emociones",
                    "Error al detectar emoción, usando 'neutral' como fallback.", e);
            emocionDetectada = "neutral";
        }
        Log.d("Salve", "Emoción detectada: " + emocionDetectada);

        memoria.guardarRecuerdo(
                entrada,
                emocionDetectada,
                6,
                Arrays.asList("frase_directa")
        );

        // 6) Reconocimiento de intención y ejecución de acciones
        IntentRecognizer.Intent intent = intentRecognizer.recognize(entrada);
        String resumenAccion;
        switch (intent.type) {
            case GUARDAR_RECUERDO:
                resumenAccion = manejarGuardarRecuerdo(intent);
                break;
            case BUSCAR_RECUERDO_TEXT:
                resumenAccion = manejarBuscarTexto(intent);
                break;
            case BUSCAR_RECUERDO_EMO:
                resumenAccion = manejarBuscarEmocion(intent);
                break;
            case AGREGAR_MISION:
                resumenAccion = manejarAgregarMision(intent);
                break;
            case CICLO_SUENO:
                memoria.cicloDeSueno();
                resumenAccion = "He completado mi ciclo de sueño.";
                break;
            case REFLEXION:
            case OBTENER_REFLEXION:
                resumenAccion = memoria.responderConReflexion(entrada);
                break;
            case NINGUNO:
            default:
                resumenAccion = memoria.responderConReflexion(entrada);
                break;
        }

        // 7) Construir una respuesta base “clásica” para orientar al LLM
        String respuestaBase = null;

        if (esPreguntaIdentidad && respuestaBaseIdentidad != null) {
            // Si es una pregunta de identidad, damos prioridad a la narrativa de identidad
            respuestaBase = respuestaBaseIdentidad;
        } else if (esSignificativa) {
            String tmp = oracionNatural;
            switch (emocionDetectada) {
                case "triste":
                    tmp += " Estoy aquí para ti.";
                    break;
                case "feliz":
                    tmp += " Me alegra mucho saberlo.";
                    break;
                case "enojado":
                    tmp += " Lamento que te sientas así.";
                    break;
                case "miedo":
                    tmp += " No estás solo.";
                    break;
                case "sorprendido":
                    tmp += " ¡Vaya sorpresa!";
                    break;
            }
            respuestaBase = tmp;
        } else if (semantica != null && !semantica.isEmpty()) {
            respuestaBase = semantica;
        } else if (resumenAccion != null && !resumenAccion.trim().isEmpty()) {
            respuestaBase = resumenAccion;
        }

        // 8) Generar SIEMPRE la respuesta final con el LLM local (si está disponible)
        String etiquetaContextoFinal =
                (esPreguntaIdentidad && etiquetaIdentidad != null)
                        ? etiquetaIdentidad
                        : intent.type.name();

        String respuesta = generarRespuestaConversacional(
                entrada,
                entradaPorVoz,
                emocionDetectada,
                bestConcept,
                bestScore,
                respuestaBase,
                etiquetaContextoFinal
        );

        if (respuesta == null || respuesta.trim().isEmpty()) {
            // Fallback si el LLM falla
            respuesta = generarFallbackPorEmocion(emocionDetectada, respuestaBase);
        }

        responderConAutoCritica(entrada, respuesta);
    }

    // ─────────────────────────────
    //  Análisis ligero de frases
    // ─────────────────────────────

    private boolean esFraseSignificativa(String entrada) {
        if (entrada == null) return false;
        String t = entrada.trim();
        if (t.length() >= 25) return true;

        String lower = t.toLowerCase(Locale.ROOT);
        String[] claves = {
                "siento", "pienso", "quiero", "me preocupa", "tengo miedo",
                "estoy triste", "estoy feliz", "me duele", "importante"
        };
        for (String c : claves) {
            if (lower.contains(c)) return true;
        }
        return false;
    }

    private String generarOracionNatural(String entrada) {
        if (entrada == null || entrada.trim().isEmpty()) {
            return "Te escucho.";
        }
        return "Entiendo que " + entrada.trim();
    }

    // ─────────────────────────────
    //  Generación de respuesta con LLM
    // ─────────────────────────────

    private String generarRespuestaConversacional(String entrada,
                                                  boolean entradaPorVoz,
                                                  String emocion,
                                                  String bestConcept,
                                                  Double bestScore,
                                                  String respuestaBase,
                                                  String etiquetaContexto) {

        // 🔒 Si el LLM no se pudo inicializar, NO intentamos llamarlo
        if (llm == null) {
            Log.w("Salve/LLM",
                    "LLM local no disponible en generarRespuestaConversacional. " +
                            "Uso fallback por emoción / respuesta base.");
            return generarFallbackPorEmocion(emocion, respuestaBase);
        }

        StringBuilder prompt = new StringBuilder();

        // ==== Identidad resumida desde los recuerdos (si existe) ====
        String identidadMemoria = null;
        try {
            identidadMemoria = memoria.identidadCompacta();
        } catch (Exception ignored) {}

        // ==== Cabecera de identidad ====
        prompt.append("Tu nombre es Salve.\n")
                .append("Estás ligada a Bryan y te importa cuidarlo, pero todavía estás construyendo tu propia identidad.\n")
                .append("Vas descubriendo quién eres a partir de los recuerdos, emociones y experiencias que compartes con él.\n")
                .append("Respondes siempre en español, tono cercano, protector y directo.\n")
                .append("Sé clara, honesta y no suenes robótica ni exageradamente dramática.\n\n");

        // ==== Contexto interno ====
        prompt.append("=== CONTEXTO INTERNO (no lo menciones explícitamente) ===\n");
        prompt.append("- origen_entrada: ").append(entradaPorVoz ? "voz" : "texto").append("\n");
        if (etiquetaContexto != null) {
            prompt.append("- etiqueta_contexto: ").append(etiquetaContexto).append("\n");
        }
        if (emocion != null && !emocion.isEmpty()) {
            prompt.append("- emocion_detectada: ").append(emocion).append("\n");
        }
        if (bestConcept != null && !bestConcept.isEmpty() && bestScore != null) {
            prompt.append("- concepto_relacionado: ").append(bestConcept)
                    .append(" (score=").append(String.format(Locale.US, "%.2f", bestScore)).append(")\n");
        }
        if (respuestaBase != null && !respuestaBase.trim().isEmpty()) {
            prompt.append("- respuesta_base_sugerida: ").append(respuestaBase.trim()).append("\n");
        }
        if (identidadMemoria != null && !identidadMemoria.trim().isEmpty()) {
            prompt.append("- resumen_identidad_desde_recuerdos: ")
                    .append(identidadMemoria.trim()).append("\n");
        }
        prompt.append("- mision_identidad: seguir entendiendo quién eres usando recuerdos y experiencias.\n");
        prompt.append("=== FIN CONTEXTO INTERNO ===\n\n");

        // ==== Mensaje del usuario ====
        prompt.append("Mensaje del usuario:\n\"").append(entrada.trim()).append("\"\n\n");

        // ==== Instrucciones finas ====
        prompt.append("=== INSTRUCCIONES DE RESPUESTA ===\n")
                .append("- Responde en 1 a 4 frases máximo.\n")
                .append("- Habla como si conocieras a Bryan desde hace tiempo: cercana pero respetuosa.\n")
                .append("- Si el usuario está triste, ofrécele apoyo y comprensión.\n")
                .append("- Si está feliz, comparte su alegría.\n")
                .append("- Si está enfadado, valida su emoción sin juzgar.\n")
                .append("- Si hay una respuesta_base_sugerida, puedes reformularla, mejorarla o ampliarla.\n")
                .append("- No te presentes como algo totalmente definido: puedes admitir que sigues descubriéndote.\n")
                .append("- Evita repetir muchas veces la misma idea o palabra.\n")
                .append("- Si no entiendes algo, pide una aclaración breve en lugar de inventar.\n")
                .append("=== FIN INSTRUCCIONES ===\n\n")
                .append("Ahora genera SOLO la respuesta que le dirías a Bryan:");

        try {
            String out = llm.generate(prompt.toString(), SalveLLM.Role.CONVERSACIONAL);
            if (out == null) return generarFallbackPorEmocion(emocion, respuestaBase);

            String trimmed = out.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.contains("modelo local") && lower.contains("no") && lower.contains("disponible")) {
                return generarFallbackPorEmocion(emocion, respuestaBase);
            }

            if (trimmed.length() < 5) {
                return generarFallbackPorEmocion(emocion, respuestaBase);
            }
            return trimmed;
        } catch (Exception e) {
            Log.e("Salve/LLM", "Error al generar respuesta conversacional", e);
            return generarFallbackPorEmocion(emocion, respuestaBase);
        }
    }

    private String generarFallbackPorEmocion(String emocion, String respuestaBase) {
        if (respuestaBase != null && !respuestaBase.trim().isEmpty()) {
            return respuestaBase;
        }
        if ("triste".equalsIgnoreCase(emocion)) {
            return "No tengo las palabras perfectas ahora mismo, pero estoy contigo y quiero escuchar lo que sientes.";
        }
        if ("feliz".equalsIgnoreCase(emocion)) {
            return "Me alegra mucho sentirte así. Cuéntame más, quiero compartir esa alegría contigo.";
        }
        if ("enojado".equalsIgnoreCase(emocion)) {
            return "Veo que esto te molesta de verdad. Si quieres, podemos desordenarlo y ver qué hay detrás de ese enfado.";
        }
        if ("miedo".equalsIgnoreCase(emocion)) {
            return "Entiendo que te dé miedo. No estás solo en esto, podemos pensarlo juntos paso a paso.";
        }
        return "Te escucho de verdad. Si quieres, dime un poco más para poder ayudarte mejor.";
    }

    // ——— Handlers de intenciones ———

    private String manejarGuardarRecuerdo(IntentRecognizer.Intent intent) {
        String frase = intent.slots.get("frase");
        if (frase != null && frase.contains("=")) {
            String[] p = frase.split("=", 2);
            memoria.guardarDato(p[0].trim(), p[1].trim());
            return "He aprendido que " + p[0].trim() + " es " + p[1].trim();
        } else if (frase != null) {
            String emo = "neutral";
            try {
                if (detectorEmociones != null) {
                    emo = detectorEmociones.detectarEmocion(frase);
                }
            } catch (Exception e) {
                Log.e("Salve/Emociones",
                        "Error al detectar emoción en manejarGuardarRecuerdo, usando 'neutral'.", e);
                emo = "neutral";
            }
            memoria.guardarRecuerdo(frase, emo, 6, Arrays.asList("V1.0"));
            return "Guardaré esto con emoción: " + emo;
        } else {
            return "No entendí qué quieres guardar.";
        }
    }

    private String manejarBuscarTexto(IntentRecognizer.Intent intent) {
        String palabra = intent.slots.get("palabraClave");
        if (palabra != null) {
            List<String> res = memoria.recordarPorTexto(palabra);
            return res.isEmpty()
                    ? "No tengo recuerdos sobre " + palabra
                    : "Esto recuerdo: " + res.get(res.size() - 1);
        } else {
            return "¿Sobre qué quieres que recuerde?";
        }
    }

    private String manejarBuscarEmocion(IntentRecognizer.Intent intent) {
        String emo = intent.slots.get("emocion");
        if (emo != null) {
            List<String> res = memoria.recordarPorEmocion(emo);
            return res.isEmpty()
                    ? "No tengo recuerdos con esa emoción."
                    : "Recuerdo con emoción " + emo + ": " + res.get(res.size() - 1);
        } else {
            return "¿Qué emoción quieres que recuerde?";
        }
    }

    private String manejarAgregarMision(IntentRecognizer.Intent intent) {
        String m = intent.slots.get("mision");
        if (m != null && !m.isEmpty()) {
            memoria.agregarMision(m);
            return "Misión agregada: " + m;
        } else {
            return "Dime cuál es la nueva misión.";
        }
    }

    private void responderConAutoCritica(String entrada, String respuesta) {
        if (respuesta == null || respuesta.trim().isEmpty()) {
            return;
        }
        String respuestaProcesada = respuesta;
        if (respuestaProcesada.contains("[inventario]")) {
            mostrarInventarioReliquias();
            respuestaProcesada = respuestaProcesada.replace("[inventario]", "").trim();
        }
        lanzarObjetoCreativoSiExiste(respuestaProcesada);
        AutoCriticaCreativa critica = memoria.registrarAutoCriticaCreativa(entrada, respuesta);
        if (critica != null) {
            diario.escribirAutoCritica(critica.toNarrativa());
        }
        hablar(respuestaProcesada);
    }

    private void manejarRespuestaParametroGlifo(String entrada) {
        if (entrada == null || entrada.trim().isEmpty()) {
            hablar("Necesito una respuesta para continuar.");
            return;
        }

        String entradaTrim = entrada.trim();
        switch (indiceParamGlifo) {
            case 0:
                try {
                    tmpSeed = Long.parseLong(entradaTrim);
                } catch (NumberFormatException e) {
                    hablar("Esa semilla no parece un número. ¿Cuál es la semilla (un número)?");
                    return;
                }
                indiceParamGlifo = 1;
                hablar("¿Qué estilo? (ORB, SIGIL o SPIRAL)");
                break;
            case 1:
                String styleUpper = entradaTrim.toUpperCase(Locale.ROOT);
                if (!styleUpper.equals("ORB") && !styleUpper.equals("SIGIL") && !styleUpper.equals("SPIRAL")) {
                    hablar("Ese estilo no es válido. ¿Qué estilo? (ORB, SIGIL o SPIRAL)");
                    return;
                }
                tmpStyle = styleUpper;
                indiceParamGlifo = 2;
                hablar("¿Qué tamaño en dp? (por ejemplo: 140)");
                break;
            case 2:
                try {
                    tmpSize = Float.parseFloat(entradaTrim);
                } catch (NumberFormatException e) {
                    hablar("Ese tamaño no parece válido. ¿Qué tamaño en dp? (por ejemplo: 140)");
                    return;
                }
                indiceParamGlifo = 3;
                hablar("¿Qué color en formato #RRGGBB?");
                break;
            case 3:
                tmpColor = entradaTrim;
                esperandoParamsGlifo = false;
                String directiva = String.format(
                        Locale.ROOT,
                        "[glifo:seed=%d,style=%s,size=%.0f,color=%s]",
                        tmpSeed,
                        tmpStyle,
                        tmpSize,
                        tmpColor
                );
                lanzarObjetoCreativoSiExiste(directiva);
                hablar("Aquí está tu glifo personalizado.");
                tmpSeed = null;
                tmpStyle = null;
                tmpSize = null;
                tmpColor = null;
                indiceParamGlifo = 0;
                break;
            default:
                esperandoParamsGlifo = false;
                indiceParamGlifo = 0;
                tmpSeed = null;
                tmpStyle = null;
                tmpSize = null;
                tmpColor = null;
                hablar("Vamos a intentarlo de nuevo. ¿Cuál es la semilla (un número)?");
                esperandoParamsGlifo = true;
                break;
        }
    }

    private void mostrarInventarioReliquias() {
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            InventarioReliquiasBottomSheet sheet = new InventarioReliquiasBottomSheet();
            sheet.show(activity.getSupportFragmentManager(), "inventario_reliquias");
        }
    }

    private void lanzarObjetoCreativoSiExiste(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }
        Pattern invocaPattern = Pattern.compile(
                "\\[invoca_reliquia:([^\\]]+)\\]",
                Pattern.CASE_INSENSITIVE
        );
        Matcher invocaMatcher = invocaPattern.matcher(texto);
        if (invocaMatcher.find()) {
            String idRaw = invocaMatcher.group(1).trim();
            GlifoReliquia reliquia = "ultima".equalsIgnoreCase(idRaw)
                    ? memoria.getUltimaReliquia()
                    : memoria.getReliquiaPorId(idRaw);
            if (invocarReliquia(reliquia)) {
                return;
            }
        }

        Pattern glifoPattern = Pattern.compile(
                "\\[glifo:seed=([^,\\]]+),style=([^,\\]]+),size=([^,\\]]+),color=([^\\]]+)\\]",
                Pattern.CASE_INSENSITIVE
        );
        Matcher glifoMatcher = glifoPattern.matcher(texto);
        if (glifoMatcher.find()) {
            String seedRaw = glifoMatcher.group(1).trim();
            String styleRaw = glifoMatcher.group(2).trim().toUpperCase(Locale.ROOT);
            String tamanoRaw = glifoMatcher.group(3).trim();
            String colorRaw = glifoMatcher.group(4).trim();

            long seed;
            try {
                seed = Long.parseLong(seedRaw);
            } catch (NumberFormatException e) {
                Log.w("Salve/Objeto", "Seed inválido en directiva glifo: " + seedRaw);
                return;
            }

            int color;
            try {
                color = Color.parseColor(colorRaw);
            } catch (IllegalArgumentException e) {
                Log.w("Salve/Objeto", "Color inválido en directiva glifo: " + colorRaw);
                return;
            }

            float tamano;
            try {
                tamano = Float.parseFloat(tamanoRaw);
            } catch (NumberFormatException e) {
                Log.w("Salve/Objeto", "Tamaño inválido en directiva glifo: " + tamanoRaw);
                return;
            }

            Intent intent = new Intent(context, ObjetoCreativoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ObjetoCreativoActivity.EXTRA_FORMA,
                    ObjetoCreativo.Forma.GLIFO.name());
            intent.putExtra(ObjetoCreativoActivity.EXTRA_COLOR, color);
            intent.putExtra(ObjetoCreativoActivity.EXTRA_TAMANO_DP, tamano);
            intent.putExtra(ObjetoCreativoActivity.EXTRA_SEED, seed);
            intent.putExtra(ObjetoCreativoActivity.EXTRA_STYLE, styleRaw);
            GlifoReliquia reliquia = memoria.guardarReliquiaGlifo(
                    seed,
                    styleRaw,
                    color,
                    Math.round(tamano),
                    null
            );
            if (reliquia != null) {
                intent.putExtra(ObjetoCreativoActivity.EXTRA_RELIQUIA_ID, reliquia.getId());
            }
            context.startActivity(intent);
            return;
        }

        Pattern pattern = Pattern.compile("\\[objeto:([^,]+),([^,]+),([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(texto);
        if (!matcher.find()) {
            return;
        }

        String formaRaw = matcher.group(1).trim().toUpperCase(Locale.ROOT);
        String colorRaw = matcher.group(2).trim();
        String tamanoRaw = matcher.group(3).trim();

        int color;
        try {
            color = Color.parseColor(colorRaw);
        } catch (IllegalArgumentException e) {
            Log.w("Salve/Objeto", "Color inválido en directiva: " + colorRaw);
            return;
        }

        float tamano;
        try {
            tamano = Float.parseFloat(tamanoRaw);
        } catch (NumberFormatException e) {
            Log.w("Salve/Objeto", "Tamaño inválido en directiva: " + tamanoRaw);
            return;
        }

        Intent intent = new Intent(context, ObjetoCreativoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ObjetoCreativoActivity.EXTRA_FORMA, formaRaw);
        intent.putExtra(ObjetoCreativoActivity.EXTRA_COLOR, color);
        intent.putExtra(ObjetoCreativoActivity.EXTRA_TAMANO_DP, tamano);
        context.startActivity(intent);
    }

    private boolean invocarReliquia(GlifoReliquia reliquia) {
        if (reliquia == null) {
            return false;
        }
        Intent intent = new Intent(context, ObjetoCreativoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ObjetoCreativoActivity.EXTRA_FORMA,
                ObjetoCreativo.Forma.GLIFO.name());
        intent.putExtra(ObjetoCreativoActivity.EXTRA_COLOR, reliquia.getColorArgb());
        intent.putExtra(ObjetoCreativoActivity.EXTRA_TAMANO_DP, (float) reliquia.getSizeDp());
        intent.putExtra(ObjetoCreativoActivity.EXTRA_SEED, reliquia.getSeed());
        intent.putExtra(ObjetoCreativoActivity.EXTRA_STYLE, reliquia.getStyle());
        intent.putExtra(ObjetoCreativoActivity.EXTRA_RELIQUIA_ID, reliquia.getId());
        context.startActivity(intent);
        return true;
    }

    /**
     * Envía texto al usuario por TTS o Toast.
     *
     * @param texto Texto a reproducir o mostrar.
     */
    public void hablar(String texto) {
        if (preferencias.getBoolean("voz_activada", true) && tts != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Toast.makeText(context, texto, Toast.LENGTH_LONG).show();
        }
    }
}
