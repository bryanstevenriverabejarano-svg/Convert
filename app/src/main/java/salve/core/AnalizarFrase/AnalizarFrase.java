package salve.core.AnalizarFrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * AnalizarFrase – Intérprete conversacional minimalista sin dependencias externas.
 *
 * - NO requiere LLMClient, MemoryClient ni Tool de otros paquetes.
 * - Usa java.util.function.* para recibir funciones/lambdas:
 *     • llmInstruct:   BiFunction<systemPrompt, userPrompt, LLMResult>
 *     • llmCreative:   BiFunction<systemPrompt, userPrompt, LLMResult>
 *     • memoryFetch:   Function<query, Memories>
 *     • tools:         Map<String, Function<Map<String,String>, String>>
 *
 * Así puedes adaptar tus clases existentes con una sola lambda.
 */
public class AnalizarFrase {

    /* ------------------ Tipos internos simples ------------------ */
    public static class LLMResult {
        public final String modelId;
        public final String text;
        public final int tokensIn;
        public final int tokensOut;
        public final double confidence;
        public LLMResult(String modelId, String text, int tokensIn, int tokensOut, double confidence) {
            this.modelId = modelId == null ? "" : modelId;
            this.text = text == null ? "" : text;
            this.tokensIn = Math.max(0, tokensIn);
            this.tokensOut = Math.max(0, tokensOut);
            this.confidence = Math.max(0, Math.min(1, confidence));
        }
    }

    public static class Memories {
        public final List<String> keys;
        public final List<String> contents;
        public Memories(List<String> keys, List<String> contents) {
            this.keys = (keys == null) ? Collections.emptyList() : new ArrayList<>(keys);
            this.contents = (contents == null) ? Collections.emptyList() : new ArrayList<>(contents);
        }
    }

    /** Resultado estructurado de interpretación. */
    public static class AnalisisIntencional {
        public String intencion = "desconocida";
        public String tono = "";
        public String sentimiento = "";
        public String sujeto = "";
        public String accion = "";
        public String objetivo = "";
        public List<String> recuerdosAsociados = new ArrayList<>();
        public List<String> herramientasSugeridas = new ArrayList<>();
        public double confianza = 0.0;
        public String modeloUsado = "";
        public String respuestaBaseLLM = "";

        @Override public String toString() {
            return "Intencion=" + intencion + " | Tono=" + tono +
                    " | Sujeto=" + sujeto + " | Accion=" + accion +
                    " | Objetivo=" + objetivo + " | Tools=" + herramientasSugeridas +
                    " | Confianza=" + confianza + " | Modelo=" + modeloUsado;
        }
    }

    /* ------------------ Campos (funcionales) ------------------ */
    private final BiFunction<String, String, LLMResult> llmInstruct;  // p.ej. Qwen2.5-3B-Instruct
    private final BiFunction<String, String, LLMResult> llmCreative;  // p.ej. Phi-4 (opcional)
    private final Function<String, Memories> memoryFetch;             // traer recuerdos (Namecheap)
    private final Map<String, Function<Map<String,String>, String>> tools; // herramientas

    public AnalizarFrase(BiFunction<String,String,LLMResult> llmInstruct,
                         BiFunction<String,String,LLMResult> llmCreative,
                         Function<String,Memories> memoryFetch,
                         Map<String, Function<Map<String,String>, String>> tools) {
        this.llmInstruct = llmInstruct;
        this.llmCreative = llmCreative;
        this.memoryFetch = memoryFetch != null ? memoryFetch : q -> new Memories(null, null);
        this.tools = (tools == null) ? new HashMap<>() : new HashMap<>(tools);
    }

    /* ------------------ API principal ------------------ */
    /** Interpreta una frase con LLM + Memoria y sugiere herramientas. */
    public AnalisisIntencional interpretar(String frase) {
        long t0 = System.currentTimeMillis();

        // 1) Memoria relevante
        Memories mem = memoryFetch.apply(frase == null ? "" : frase);

        // 2) Prompt para el LLM instructivo
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analiza la frase en español y responde SOLO con líneas 'clave: valor' ")
                .append("para: intencion, sujeto, accion, objetivo, tono, sentimiento.\n\n")
                .append("Frase: ").append(frase == null ? "" : frase).append("\n\n")
                .append("Recuerdos relevantes:\n");
        for (String s : mem.contents) prompt.append("- ").append(s).append("\n");

        // 3) LLM instructivo (si no hay, caer a heurística simple)
        LLMResult r;
        if (llmInstruct != null) {
            r = llmInstruct.apply(
                    "Eres Salve, IA personal. Extrae intención y contexto de forma concisa.",
                    prompt.toString()
            );
        } else {
            r = heuristicaBasica(frase);
        }

        // 4) Parseo "clave: valor"
        AnalisisIntencional out = parseResultado(r.text);
        out.confianza = (r.confidence > 0 ? r.confidence : 0.75);
        out.modeloUsado = r.modelId == null ? "" : r.modelId;
        out.respuestaBaseLLM = r.text;
        out.recuerdosAsociados = mem.keys;

        // 5) Sugerir herramientas según intención/tono
        String intent = out.intencion == null ? "" : out.intencion.toLowerCase(Locale.ROOT);
        String tono = out.tono == null ? "" : out.tono.toLowerCase(Locale.ROOT);

        if (intent.contains("identidad")) toolsSuggest(out, "WhoAmI");
        if (intent.contains("buscar") || intent.contains("recordar")) toolsSuggest(out, "MemoryLookup");
        if (intent.contains("imaginar") || intent.contains("crear")) toolsSuggest(out, "Imagine");
        if (intent.contains("sentir") || tono.contains("emocional") || tono.contains("poet"))
            toolsSuggest(out, "Reflegion");

        // 6) Si hay componente creativo y existe llmCreative, indicamos cambio de modelo
        if (out.herramientasSugeridas.contains("Imagine") && llmCreative != null) {
            out.modeloUsado = out.modeloUsado.isEmpty() ? "phi-4" : out.modeloUsado;
        }

        System.out.println("[AnalizarFrase] " + out + " (" + (System.currentTimeMillis()-t0) + "ms)");
        return out;
    }

    /* ------------------ Helpers ------------------ */

    private void toolsSuggest(AnalisisIntencional out, String name){
        if (!out.herramientasSugeridas.contains(name)) out.herramientasSugeridas.add(name);
    }

    /** Parseo simple de líneas "clave: valor". Tolerante a ruido. */
    private AnalisisIntencional parseResultado(String text) {
        AnalisisIntencional a = new AnalisisIntencional();
        if (text == null) return a;
        try {
            for (String raw : text.split("\\r?\\n")) {
                String line = raw.trim();
                if (line.isEmpty() || !line.contains(":")) continue;
                String[] kv = line.split(":", 2);
                String key = kv[0].trim().toLowerCase(Locale.ROOT);
                String val = kv[1].trim();
                switch (key) {
                    case "intencion": a.intencion = val; break;
                    case "sujeto": a.sujeto = val; break;
                    case "accion": a.accion = val; break;
                    case "objetivo": a.objetivo = val; break;
                    case "tono": a.tono = val; break;
                    case "sentimiento": a.sentimiento = val; break;
                }
            }
        } catch (Exception ignore) {}
        return a;
    }

    /** Heurística de respaldo si no se pasó LLM instructivo. */
    private LLMResult heuristicaBasica(String frase) {
        String f = (frase == null ? "" : frase).toLowerCase(Locale.ROOT);
        String intent = "pregunta";
        if (f.contains("quien eres") || f.contains("quién eres")) intent = "identidad";
        if (f.contains("recuerda") || f.contains("recuerdas") || f.contains("buscar")) intent = "buscar";
        if (f.contains("imagina") || f.contains("poema") || f.contains("dibuja")) intent = "imaginar";
        String fake =
                "intencion: " + intent + "\n" +
                        "sujeto: usuario\n" +
                        "accion: consultar\n" +
                        "objetivo: obtener ayuda\n" +
                        "tono: neutral\n" +
                        "sentimiento: curioso";
        return new LLMResult("heuristica", fake, 0, 0, 0.4);
    }
}
