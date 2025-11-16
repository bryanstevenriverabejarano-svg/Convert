package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import salve.core.AnalizarFrase.AnalizarFrase;
import salve.core.GestorIdeas;
import salve.core.ModuloInvestigacion;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * MotorConversacional.java
 *
 * Orquesta el flujo principal de Salve:
 *   1) Guarda la entrada en el diario.
 *   2) Analiza la frase con AnalizarFrase.
 *   2.5) Evalúa comprensión semántica de conceptos clave usando ModuloComprension.
 *        Si supera el umbral, genera respuesta con LLM local.
 *   3) Pasa por el módulo semántico de interpretación.
 *   4) Detecta emoción y guarda un recuerdo binario.
 *   5) Si la frase es significativa, responde con oraciones naturales.
 *   6) En caso contrario, entra al switch de intenciones.
 */
public class MotorConversacional {

    // ==== DEPENDENCIAS ====
    private final Context context;
    private final MemoriaEmocional memoria;
    private final DiarioSecreto diario;
    private final IntentRecognizer intentRecognizer;
    private final DetectorEmociones detectorEmociones;
    private final ModuloInterpretacionSemantica moduloInterpretacion;
    private final AnalizarFrase analizadorFrase;
    private final ModuloComprension moduloComprension;
    private final LLMResponder llm;                    // ← Importa o crea esta clase

    // ==== UTILIDADES ====
    private final SharedPreferences preferencias;
    private TextToSpeech tts;

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
        this.context             = context;
        this.memoria             = memoria;
        this.diario              = diario;
        this.intentRecognizer    = new IntentRecognizer();
        this.detectorEmociones   = new DetectorEmociones(context);
        this.moduloInterpretacion= new ModuloInterpretacionSemantica();
        this.analizadorFrase     = new AnalizarFrase();
        this.moduloComprension   = new ModuloComprension(300, 42L);
        this.llm                 = LLMResponder.getInstance(context);

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
        // Manejo experimental de peticiones de investigación o auto‑mejora
        String entradaMinus = entrada.toLowerCase();
        // Si el usuario pide investigar un tema (palabras clave "investiga" o "buscar información sobre")
        if (entradaMinus.contains("investiga") || entradaMinus.contains("buscar información sobre")) {
            // Extraer tema sencillo: todo después de la palabra clave
            String tema = entrada;
            if (entradaMinus.contains("sobre")) {
                int idx = entradaMinus.indexOf("sobre");
                if (idx >= 0) {
                    tema = entrada.substring(idx + "sobre".length()).trim();
                }
            }
            ModuloInvestigacion investigador = new ModuloInvestigacion(context);
            List<String> ideas = investigador.generarIdeasInvestigacion(tema, 3);
            if (!ideas.isEmpty()) {
                responderConAutoCritica(entrada, "He generado algunas ideas sobre " + tema + ": " + ideas.get(0));
            } else {
                responderConAutoCritica(entrada, "No pude generar ideas en este momento. ¿Puedes intentar con otra pregunta?");
            }
            return;
        }
        // Si el usuario sugiere que Salve mejore su código o arquitectura
        if (entradaMinus.contains("mejorar") && (entradaMinus.contains("código") || entradaMinus.contains("sistema") || entradaMinus.contains("salve"))) {
            GestorIdeas gestor = new GestorIdeas(context);
            // Descripción breve del sistema actual utilizada como base
            String descripcion = "Salve es un asistente virtual para Android con módulos de memoria emocional, diario, reconocimiento de emociones, interpretación semántica y un motor de decisiones.";
            List<String> ideasMejora = gestor.proponerMejorasArquitectura(descripcion, 3);
            if (!ideasMejora.isEmpty()) {
                responderConAutoCritica(entrada, "Aquí hay una idea para mejorarme: " + ideasMejora.get(0));
            } else {
                responderConAutoCritica(entrada, "No tengo sugerencias de mejora por ahora. ¡Sigo aprendiendo!");
            }
            return;
        }
        // 2.4) Identidad: si el usuario pregunta quién es Salve, responder con identidad dinámica
        String lowerEntrada = entrada.toLowerCase();
        if (lowerEntrada.contains("quién eres") || lowerEntrada.contains("eres tú")) {
            String ident = memoria.identidadCompacta();
            responderConAutoCritica(entrada, ident);
            return;
        }
        // 1) Guarda en el diario
        diario.escribir("No se lo dije, pero sentí algo cuando dijo: " + entrada);

        // 2) Análisis ligero de la frase
        AnalizarFrase.Analysis a;
        a = analizadorFrase.analizar(entrada);

        // 2.5) Comprensión semántica
        String bestConcept = moduloComprension.getConceptoMasRelacionado(entrada);
        double bestScore   = moduloComprension.comprehensionScore(entrada, bestConcept);
        if (bestScore >= 0.7) {
            String prompt = String.format(
                    "El usuario habla de \"%s\": \"%s\".%n" +
                            "Responde de forma empática y breve en español:",
                    bestConcept, entrada
            );
            String respuesta = llm.generate(prompt);
            if (respuesta == null || respuesta.length() < 10) {
                respuesta = "Cuéntame más sobre eso, por favor.";
            }
            responderConAutoCritica(entrada, respuesta);
            return;
        }

        // 3) Interpretación semántica avanzada
        String semantica = moduloInterpretacion.interpretar(entrada);
        if (semantica != null && !semantica.isEmpty()) {
            responderConAutoCritica(entrada, semantica);
            return;
        }

        // 4) Detección de emoción y guardado de recuerdo
        String emocionDetectada = detectorEmociones.detectarEmocion(entrada);
        Log.d("Salve", "Emoción detectada: " + emocionDetectada);
        memoria.guardarRecuerdo(
                entrada,
                emocionDetectada,
                6,
                Arrays.asList("frase_directa")
        );

        // 5) Responder si es significativo
        if (a.esSignificativo()) {
            String respuesta = a.aOracionNatural();
            switch (emocionDetectada) {
                case "triste":      respuesta += " Estoy aquí para ti.";      break;
                case "feliz":       respuesta += " Me alegra mucho saberlo.";  break;
                case "enojado":     respuesta += " Lamento que te sientas así."; break;
                case "miedo":       respuesta += " No estás solo.";             break;
                case "sorprendido": respuesta += " ¡Vaya sorpresa!";           break;
            }
            responderConAutoCritica(entrada, respuesta);
            return;
        }

        // 6) Flujo normal de intenciones
        IntentRecognizer.Intent intent = intentRecognizer.recognize(entrada);
        String respuestaFinal = null;
        switch (intent.type) {
            case GUARDAR_RECUERDO:
                respuestaFinal = manejarGuardarRecuerdo(intent);
                break;
            case BUSCAR_RECUERDO_TEXT:
                respuestaFinal = manejarBuscarTexto(intent);
                break;
            case BUSCAR_RECUERDO_EMO:
                respuestaFinal = manejarBuscarEmocion(intent);
                break;
            case AGREGAR_MISION:
                respuestaFinal = manejarAgregarMision(intent);
                break;
            case CICLO_SUENO:
                memoria.cicloDeSueno();
                respuestaFinal = "He completado mi ciclo de sueño.";
                break;
            case REFLEXION:
            case OBTENER_REFLEXION:
                respuestaFinal = memoria.responderConReflexion(entrada);
                break;
            case NINGUNO:
            default:
                respuestaFinal = memoria.responderConReflexion(entrada);
        }
        if (respuestaFinal != null) {
            responderConAutoCritica(entrada, respuestaFinal);
        }
    }

    // ——— Handlers de intenciones ———

    /**
     * Atiende la intención GUARDAR_RECUERDO.
     */
    private String manejarGuardarRecuerdo(IntentRecognizer.Intent intent) {
        String frase = intent.slots.get("frase");
        if (frase != null && frase.contains("=")) {
            String[] p = frase.split("=", 2);
            memoria.guardarDato(p[0].trim(), p[1].trim());
            return "He aprendido que " + p[0].trim() + " es " + p[1].trim();
        } else if (frase != null) {
            String emo = detectorEmociones.detectarEmocion(frase);
            memoria.guardarRecuerdo(frase, emo, 6, Arrays.asList("V1.0"));
            return "Guardaré esto con emoción: " + emo;
        } else {
            return "No entendí qué quieres guardar.";
        }
    }

    /**
     * Atiende la intención BUSCAR_RECUERDO_TEXT.
     */
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

    /**
     * Atiende la intención BUSCAR_RECUERDO_EMO.
     */
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

    /**
     * Atiende la intención AGREGAR_MISION.
     */
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
        AutoCriticaCreativa critica = memoria.registrarAutoCriticaCreativa(entrada, respuesta);
        if (critica != null) {
            diario.escribirAutoCritica(critica.toNarrativa());
        }
        hablar(respuesta);
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