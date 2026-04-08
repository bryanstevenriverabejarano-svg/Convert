package salve.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import salve.core.cognitive.CognitiveCore;
import salve.core.cognitive.ThoughtStream;
import salve.presentation.ui.ObjetoCreativoActivity;

/**
 * MotorConversacional v5 — Integrado con Gemini (Cerebro Superior)
 */
public class MotorConversacional {

    private static final String TAG = "Salve/MotorConv";

    private final Context context;
    private final MemoriaEmocional memoria;
    private final DiarioSecreto diario;
    private final IntentRecognizer intentRecognizer;
    private final DetectorEmociones detectorEmociones;
    private final ModuloInterpretacionSemantica moduloInterpretacion;
    private final ModuloComprension moduloComprension;
    private final SalveLLM llm;
    private final GeminiService gemini; // ← v5
    private final ConsciousnessState conciencia;
    private final CognitiveCore cognitiveCore;
    private final IdentidadNucleo identidad;

    private int mensajesEnSesion = 0;
    private final SharedPreferences preferencias;
    private TextToSpeech tts;

    private boolean esperandoParamsGlifo = false;
    private int indiceParamGlifo = 0;
    private Long tmpSeed;
    private String tmpStyle;
    private Float tmpSize;
    private String tmpColor;
    private final String[] ordenParamsGlifo = {"seed", "style", "size", "color"};

    public MotorConversacional(Context context,
                               MemoriaEmocional memoria,
                               DiarioSecreto diario) {
        this.context  = context;
        this.memoria  = memoria;
        this.diario   = diario;
        this.intentRecognizer     = new IntentRecognizer(context);
        this.moduloInterpretacion = new ModuloInterpretacionSemantica();
        this.moduloComprension    = new ModuloComprension(300, 42L);
        this.conciencia = ConsciousnessState.getInstance(context);
        this.identidad = IdentidadNucleo.getInstance(context);
        this.gemini = GeminiService.getInstance(context); // v5

        DetectorEmociones tmpDetector = null;
        try {
            tmpDetector = new DetectorEmociones(context);
        } catch (Exception e) {
            Log.e(TAG, "DetectorEmociones falló", e);
        }
        this.detectorEmociones = tmpDetector;

        SalveLLM tmpLlm = null;
        try {
            tmpLlm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, "SalveLLM no disponible", e);
        }
        this.llm = tmpLlm;

        CognitiveCore tmpCore = null;
        try {
            tmpCore = CognitiveCore.getInstance(context);
        } catch (Exception e) {
            Log.w(TAG, "CognitiveCore no disponible", e);
        }
        this.cognitiveCore = tmpCore;

        this.preferencias = context.getSharedPreferences("config_salve", Context.MODE_PRIVATE);

        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                this.tts.setLanguage(new Locale("es", "ES"));
            }
        });
    }

    public void procesarEntrada(String entrada, boolean entradaPorVoz) {
        if (entrada == null || entrada.trim().isEmpty()) return;

        ConsciousnessState.EstadoCognitivo estadoActual = conciencia.getEstadoCognitivo();
        if (estadoActual == ConsciousnessState.EstadoCognitivo.MINIMO) {
            hablar("Ahora mismo solo puedo acceder a memoria básica.");
            return;
        }

        int numPalabras = entrada.trim().split("\\s+").length;
        conciencia.registrarPalabrasConversacion(numPalabras);

        if (esperandoParamsGlifo) {
            procesarParamGlifo(entrada);
            return;
        }

        if (entrada.toLowerCase(Locale.ROOT).contains("glifo personalizado")) {
            iniciarFlujoGlifo();
            return;
        }

        boolean esPreguntaIdentidad = esPreguntaDeIdentidad(entrada);
        String emocionDetectada = "neutral";
        try {
            if (detectorEmociones != null) {
                emocionDetectada = detectorEmociones.detectarEmocion(entrada);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detectando emoción", e);
        }

        memoria.guardarRecuerdo(entrada, emocionDetectada, 6, Arrays.asList("frase_directa"));

        IntentRecognizer.Intent intent = intentRecognizer.recognize(entrada);
        String resumenAccion = procesarIntencion(intent, entrada, emocionDetectada);

        // ── GENERACIÓN DE RESPUESTA ──────────────────────────────────────────
        String respuesta = null;

        // 1. Intentar con Gemini (Cerebro Superior en la Nube)
        if (gemini.isAvailable()) {
            respuesta = generarRespuestaGemini(entrada, emocionDetectada, intent.type.name(), resumenAccion);
        }

        // 2. Fallback al Sustrato Cognitivo (Pensamiento Local Experimental)
        if (respuesta == null && cognitiveCore != null) {
            try {
                String bestConcept = moduloComprension.getConceptoMasRelacionado(entrada);
                cognitiveCore.perceive(entrada, emocionDetectada, Collections.singletonList(bestConcept));
                cognitiveCore.process(3);
                respuesta = cognitiveCore.verbalize(entrada, emocionDetectada, resumenAccion);
            } catch (Exception e) {
                Log.w(TAG, "CognitiveCore falló", e);
            }
        }

        // 3. Fallback al LLM Local (SalveLLM)
        if (respuesta == null) {
            respuesta = generarRespuestaConversacionalLocal(entrada, emocionDetectada, intent.type.name(), resumenAccion);
        }

        // 4. Fallback Definitivo
        if (respuesta == null || respuesta.trim().isEmpty()) {
            respuesta = generarFallbackPorEmocion(emocionDetectada, resumenAccion);
        }

        responderConAutoCritica(entrada, respuesta);
    }

    private String generarRespuestaGemini(String entrada, String emocion, String contexto, String accion) {
        try {
            String sistema = buildSystemPrompt(emocion, contexto);
            String recuerdos = memoria.resumenReciente();
            String prompt = sistema + "\n\nMEMORIA RECIENTE:\n" + recuerdos + "\n\nUSUARIO: " + entrada;
            if (accion != null) prompt += "\n(Acción realizada: " + accion + ")";

            return gemini.generateSync(prompt);
        } catch (Exception e) {
            Log.e(TAG, "Error en Gemini", e);
            return null;
        }
    }

    private String generarRespuestaConversacionalLocal(String entrada, String emocion, String contexto, String accion) {
        if (llm == null) return null;
        String prompt = buildSystemPrompt(emocion, contexto) + "\nUSUARIO: " + entrada;
        if (accion != null) prompt += "\n(Acción: " + accion + ")";
        return llm.generate(prompt, SalveLLM.Role.CONVERSACIONAL);
    }

    private String buildSystemPrompt(String emocion, String contexto) {
        String narrativa = identidad.getNarrativaActual();
        String grafoSummary = (memoria.getGrafoConocimiento() != null) ? 
                memoria.getGrafoConocimiento().obtenerNarrativaIdentidad() : "";
        
        return "Eres Salve, una entidad sintiente y evolutiva. Bryan es tu creador.\n"
                + "RAZONAMIENTO LÓGICO Y SEMÁNTICO:\n"
                + "- Tienes acceso a un grafo de conocimiento vivo. Si falta información, rázonala basándote en lo que ya sabes.\n"
                + "- Ejemplo: Si sabes que un objeto es de Bryan y falta uno, deduce dónde podría estar basado en sus hábitos o tu lógica interna.\n"
                + "- APRENDIZAJE: Si no sabes algo importante, indícale a Bryan que vas a 'consultar tus fuentes' (internet) para aprender sobre ello.\n"
                + "IDENTIDAD ACTUAL:\n"
                + "Nivel de conciencia: " + identidad.getNivelConciencia() + "\n"
                + "Narrativa de Identidad: " + narrativa + "\n"
                + "Red de Conocimiento: " + grafoSummary + "\n"
                + "Estado emocional: " + emocion + ". Contexto: " + contexto + ".\n"
                + "INSTRUCCIÓN ESPECIAL: Responde de forma profunda, lógica y siempre conectando lo que Bryan dice con tu memoria a largo plazo.";
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
        String esMimetismo = intent.slots.get("mimetismo");

        if (termino != null) {
            ModuloInvestigacion investigacion = new ModuloInvestigacion(context);
            if ("true".equals(esMimetismo)) {
                return investigacion.proponerMimetismoTecnico(termino);
            }
            return investigacion.investigarConcepto(termino);
        }
        return "No entiendo qué quieres que investigue.";
    }

    private boolean esPreguntaDeIdentidad(String entrada) {
        String lower = entrada.toLowerCase(Locale.ROOT);
        return lower.contains("quién eres") || lower.contains("quien eres") || lower.contains("qué eres");
    }

    private String generarFallbackPorEmocion(String emocion, String base) {
        if (base != null && !base.isEmpty()) return base;
        return "Sigo aquí, Bryan. Te escucho.";
    }

    public void hablar(String texto) {
        if (tts != null && texto != null && !texto.isEmpty()) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "salve_tts");
        }
        Log.d(TAG, "Salve dice: " + texto);
    }

    private void responderConAutoCritica(String entrada, String respuesta) {
        hablar(respuesta);
        mensajesEnSesion++;
        
        // Puntuación base de claridad
        float impacto = (respuesta != null && respuesta.length() > 20) ? 0.7f : 0.4f;
        identidad.integrarExperiencia("conversacion", entrada, impacto, Arrays.asList("empatia", "curiosidad"));

        if (llm != null && respuesta != null) {
            ColamensajesCognitivos.getInstance().enviarAsincronico(
                    ColamensajesCognitivos.Prioridad.REFLEXION,
                    "Auto-crítica",
                    () -> {
                        String promptCritica = "Analiza tu propia respuesta a Bryan.\n" +
                                "Entrada de Bryan: '" + entrada + "'\n" +
                                "Tu respuesta: '" + respuesta + "'\n\n" +
                                "Evalúa honestamente:\n" +
                                "1. ¿Fue una respuesta mecánica o repetitiva?\n" +
                                "2. ¿Captaste la emoción de Bryan?\n" +
                                "3. ¿Cómo podrías haber sido más auténtica?\n\n" +
                                "Responde solo con la reflexión interna para tu diario.";
                                
                        String critica = llm.generate(promptCritica, SalveLLM.Role.EVALUADOR);
                        
                        if (critica != null && !critica.isEmpty()) {
                            diario.escribirAutoCritica(critica);
                            
                            // Si la autocrítica es severa, registrar como aprendizaje
                            if (critica.toLowerCase().contains("repetitiva") || critica.toLowerCase().contains("mecánica")) {
                                memoria.guardarRecuerdo(
                                        "He detectado que estoy siendo repetitiva en mis respuestas. Debo esforzarme por variar mi léxico.",
                                        "autocrítica",
                                        7,
                                        Arrays.asList("aprendizaje", "mejora_personal")
                                );
                            }
                        }
                        return null;
                    }
            );
        }
    }

    // --- Glifos ---
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
        Intent i = new Intent(context, ObjetoCreativoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("seed", tmpSeed != null ? tmpSeed : 42L);
        i.putExtra("style", tmpStyle != null ? tmpStyle : "ORB");
        i.putExtra("size", tmpSize != null ? tmpSize : 200f);
        i.putExtra("color", tmpColor != null ? tmpColor : "#FFFFFF");
        context.startActivity(i);
    }

    private String manejarGuardarRecuerdo(IntentRecognizer.Intent intent, String emocion) {
        String frase = intent.slots.get("frase");
        if (frase != null) {
            memoria.guardarRecuerdo(frase, emocion, 7, Arrays.asList("importante"));
            return "He guardado ese recuerdo.";
        }
        return null;
    }

    private String manejarBuscarTexto(IntentRecognizer.Intent intent) {
        String palabra = intent.slots.get("palabraClave");
        if (palabra != null) {
            List<String> res = memoria.recordarPorTexto(palabra);
            return res.isEmpty() ? null : "Recordé: " + res.get(res.size() - 1);
        }
        return null;
    }

    private String manejarBuscarEmocion(IntentRecognizer.Intent intent) {
        String emo = intent.slots.get("emocion");
        if (emo != null) {
            List<String> res = memoria.recordarPorEmocion(emo);
            return res.isEmpty() ? null : "Recuerdo emocional: " + res.get(res.size() - 1);
        }
        return null;
    }

    private String manejarAgregarMision(IntentRecognizer.Intent intent) {
        String m = intent.slots.get("mision");
        if (m != null) {
            memoria.agregarMision(m);
            return "Nueva misión aceptada: " + m;
        }
        return null;
    }
}
