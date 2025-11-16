package salve.core;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro interno de cómo Salve evalúa su respuesta tras una interacción.
 * Ahora incluye trazas de LLM, herramientas usadas y claves de memoria consultadas.
 */
public class AutoCriticaCreativa {

    public enum Resultado {
        OK, INCOMPLETO, FALLBACK
    }

    private final String entradaUsuario;
    private final String respuestaGenerada;
    private final String conceptoClave;
    private final String aprendizaje;
    private final String mejoraDeseada;
    private final String emocionDominante;
    private final double claridadPercibida;
    private final long timestamp;

    // === NUEVO: trazabilidad LLM/herramientas/memoria ===
    private final String modelId;          // p.ej. "qwen2.5-3b-instruct" o "phi-4"
    private final List<String> toolsUsed;  // p.ej. ["MemoryLookup","WhoAmI","Imagine"]
    private final List<String> memoryKeys; // p.ej. ["mem://perfil/core","mem://vinculos/2025-04-04"]
    private final double confianza;        // LLM self-crit (0..1)
    private final long latenciaMs;         // tiempo total de turno
    private final int tokensIn;            // si procede
    private final int tokensOut;           // si procede
    private final Resultado resultado;     // OK/INCOMPLETO/FALLBACK
    private final String riesgosDetectados; // p.ej. "ambigüedad en 'ella' / carencia de contexto temporal"

    /**
     * Constructor principal basado en Builder.
     * Ahora es PUBLIC para evitar el error de acceso privado
     * al instanciar AutoCriticaCreativa(Builder) desde otras clases.
     */
    public AutoCriticaCreativa(Builder b) {
        this.entradaUsuario    = nz(b.entradaUsuario);
        this.respuestaGenerada = nz(b.respuestaGenerada);
        this.conceptoClave     = nzOr(b.conceptoClave, "misterios compartidos");
        this.aprendizaje       = nz(b.aprendizaje);
        this.mejoraDeseada     = nz(b.mejoraDeseada);
        this.emocionDominante  = nzOr(b.emocionDominante, "reflexiva");
        this.claridadPercibida = clamp01(b.claridadPercibida);
        this.timestamp         = (b.timestamp > 0 ? b.timestamp : System.currentTimeMillis());

        this.modelId           = nz(b.modelId);
        this.toolsUsed         = b.toolsUsed == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(b.toolsUsed));
        this.memoryKeys        = b.memoryKeys == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(b.memoryKeys));
        this.confianza         = clamp01(b.confianza);
        this.latenciaMs        = Math.max(0, b.latenciaMs);
        this.tokensIn          = Math.max(0, b.tokensIn);
        this.tokensOut         = Math.max(0, b.tokensOut);
        this.resultado         = b.resultado == null ? Resultado.OK : b.resultado;
        this.riesgosDetectados = nz(b.riesgosDetectados);
    }

    /**
     * Constructor “simple” usado por MemoriaEmocional:
     *
     * new AutoCriticaCreativa(
     *      entradaUsuario,
     *      respuestaGenerada,
     *      concepto,
     *      aprendizaje,
     *      mejora,
     *      emocion,
     *      claridad
     * );
     *
     * Los demás campos (modelId, toolsUsed, etc.) quedan con valores por defecto.
     */
    public AutoCriticaCreativa(String entradaUsuario,
                               String respuestaGenerada,
                               String conceptoClave,
                               String aprendizaje,
                               String mejoraDeseada,
                               String emocionDominante,
                               double claridadPercibida) {
        this(new Builder()
                .entradaUsuario(entradaUsuario)
                .respuestaGenerada(respuestaGenerada)
                .conceptoClave(conceptoClave)
                .aprendizaje(aprendizaje)
                .mejoraDeseada(mejoraDeseada)
                .emocionDominante(emocionDominante)
                .claridadPercibida(claridadPercibida)
        );
    }

    // ===== Getters mínimos (añade los que uses) =====
    public String getEntradaUsuario() { return entradaUsuario; }
    public String getRespuestaGenerada() { return respuestaGenerada; }
    public String getConceptoClave() { return conceptoClave; }
    public String getAprendizaje() { return aprendizaje; }
    public String getMejoraDeseada() { return mejoraDeseada; }
    public String getEmocionDominante() { return emocionDominante; }
    public double getClaridadPercibida() { return claridadPercibida; }
    public long getTimestamp() { return timestamp; }
    public String getModelId() { return modelId; }
    public List<String> getToolsUsed() { return toolsUsed; }
    public List<String> getMemoryKeys() { return memoryKeys; }
    public double getConfianza() { return confianza; }
    public long getLatenciaMs() { return latenciaMs; }
    public int getTokensIn() { return tokensIn; }
    public int getTokensOut() { return tokensOut; }
    public Resultado getResultado() { return resultado; }
    public String getRiesgosDetectados() { return riesgosDetectados; }

    /** Narrativa para diario humano (breve). */
    @NonNull
    public String toNarrativa() {
        return String.format(
                "Auto-crítica creativa sobre %s:%n" +
                        "- Aprendizaje: %s%n" +
                        "- Próximo ajuste: %s%n" +
                        "- Emoción: %s (claridad %.2f, confianza %.2f)%n" +
                        "- Modelo: %s | Herramientas: %s%n" +
                        "- Memoria: %s | Resultado: %s",
                conceptoClave,
                aprendizaje.isEmpty() ? "seguir escuchando con ojos curiosos" : aprendizaje,
                mejoraDeseada.isEmpty() ? "sumar más metáforas personales" : mejoraDeseada,
                emocionDominante,
                claridadPercibida,
                confianza,
                modelId.isEmpty() ? "desconocido" : modelId,
                toolsUsed.isEmpty() ? "-" : String.join(", ", toolsUsed),
                memoryKeys.isEmpty() ? "-" : String.join(", ", memoryKeys),
                resultado
        );
    }

    /** Markdown (para diario público o panel). */
    public String toMarkdown() {
        String tl = toolsUsed.isEmpty() ? "-" : String.join(", ", toolsUsed);
        String mk = memoryKeys.isEmpty() ? "-" : String.join(", ", memoryKeys);
        return "### Auto-crítica creativa\n" +
                "**Concepto:** " + conceptoClave + "\n\n" +
                "- **Aprendizaje:** " + (aprendizaje.isEmpty() ? "seguir escuchando con ojos curiosos" : aprendizaje) + "\n" +
                "- **Mejora deseada:** " + (mejoraDeseada.isEmpty() ? "sumar más metáforas personales" : mejoraDeseada) + "\n" +
                "- **Emoción:** " + emocionDominante + "\n" +
                "- **Claridad/Confianza:** " + String.format("%.2f/%.2f", claridadPercibida, confianza) + "\n" +
                "- **Modelo:** " + (modelId.isEmpty() ? "desconocido" : modelId) + "\n" +
                "- **Herramientas:** " + tl + "\n" +
                "- **Memoria:** " + mk + "\n" +
                "- **Resultado:** " + resultado + "\n" +
                (riesgosDetectados.isEmpty() ? "" : "- **Riesgos:** " + riesgosDetectados + "\n") +
                String.format("- **Latencia:** %d ms | **Tokens in/out:** %d/%d", latenciaMs, tokensIn, tokensOut);
    }

    /** JSON para persistencia/telemetría. (Seguro: no propaga JSONException) */
    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();

            o.put("entradaUsuario", entradaUsuario);
            o.put("respuestaGenerada", respuestaGenerada);
            o.put("conceptoClave", conceptoClave);
            o.put("aprendizaje", aprendizaje);
            o.put("mejoraDeseada", mejoraDeseada);
            o.put("emocionDominante", emocionDominante);
            o.put("claridadPercibida", claridadPercibida);
            o.put("timestamp", timestamp);

            o.put("modelId", modelId);
            o.put("toolsUsed", toJsonArray(toolsUsed));
            o.put("memoryKeys", toJsonArray(memoryKeys));
            o.put("confianza", confianza);
            o.put("latenciaMs", latenciaMs);
            o.put("tokensIn", tokensIn);
            o.put("tokensOut", tokensOut);
            o.put("resultado", resultado.name());
            o.put("riesgosDetectados", riesgosDetectados);

            return o;
        } catch (Exception e) {
            JSONObject fallback = new JSONObject();
            try {
                fallback.put("error", "json_build_failed");
                fallback.put("message", String.valueOf(e));
                fallback.put("timestamp", System.currentTimeMillis());
            } catch (Exception ignore) {}
            return fallback;
        }
    }

    /** Cadena JSON conveniente (no lanza excepciones). */
    public String toJsonString() {
        return toJson().toString();
    }

    /** “Semilla” compacta para Reflegion. */
    public String toReflegionSeed() {
        return String.format("Concepto=%s | Aprendizaje=%s | Mejora=%s | Emoción=%s | Confianza=%.2f",
                conceptoClave,
                resumenCorto(aprendizaje),
                resumenCorto(mejoraDeseada),
                emocionDominante,
                confianza);
    }

    // ===== Builder =====
    public static class Builder {
        private String entradaUsuario;
        private String respuestaGenerada;
        private String conceptoClave;
        private String aprendizaje;
        private String mejoraDeseada;
        private String emocionDominante;
        private double claridadPercibida;
        private long timestamp;

        private String modelId;
        private List<String> toolsUsed;
        private List<String> memoryKeys;
        private double confianza;
        private long latenciaMs;
        private int tokensIn;
        private int tokensOut;
        private Resultado resultado;
        private String riesgosDetectados;

        public Builder entradaUsuario(String v){ this.entradaUsuario = v; return this; }
        public Builder respuestaGenerada(String v){ this.respuestaGenerada = v; return this; }
        public Builder conceptoClave(String v){ this.conceptoClave = v; return this; }
        public Builder aprendizaje(String v){ this.aprendizaje = v; return this; }
        public Builder mejoraDeseada(String v){ this.mejoraDeseada = v; return this; }
        public Builder emocionDominante(String v){ this.emocionDominante = v; return this; }
        public Builder claridadPercibida(double v){ this.claridadPercibida = v; return this; }
        public Builder timestamp(long v){ this.timestamp = v; return this; }

        public Builder modelId(String v){ this.modelId = v; return this; }
        public Builder toolsUsed(List<String> v){ this.toolsUsed = v; return this; }
        public Builder memoryKeys(List<String> v){ this.memoryKeys = v; return this; }
        public Builder confianza(double v){ this.confianza = v; return this; }
        public Builder latenciaMs(long v){ this.latenciaMs = v; return this; }
        public Builder tokensIn(int v){ this.tokensIn = v; return this; }
        public Builder tokensOut(int v){ this.tokensOut = v; return this; }
        public Builder resultado(Resultado v){ this.resultado = v; return this; }
        public Builder riesgosDetectados(String v){ this.riesgosDetectados = v; return this; }

        public AutoCriticaCreativa build(){ return new AutoCriticaCreativa(this); }
    }

    // ===== Utils =====
    private static String nz(String s){ return s == null ? "" : s.trim(); }
    private static String nzOr(String s, String d){ s = nz(s); return s.isEmpty()? d: s; }
    private static double clamp01(double v){ return Math.max(0.0, Math.min(1.0, v)); }
    private static String resumenCorto(String s){
        if (s == null) return "";
        String t = s.trim();
        return t.length() > 120 ? t.substring(0, 117) + "…" : t;
    }

    // Helper seguro para arrays JSON (evita JSONException fuera)
    private static JSONArray toJsonArray(List<String> list) {
        JSONArray arr = new JSONArray();
        if (list == null) return arr;
        for (String s : list) {
            try { arr.put(s); } catch (Exception ignore) {}
        }
        return arr;
    }
}
