package salve.core;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GestorIdeas es un módulo experimental que toma inspiración del
 * generador de ideas de AI Scientist. Su objetivo es permitir a Salve
 * reflexionar sobre su propia arquitectura y sugerir posibles
 * mejoras o extensiones. Estas sugerencias se basan en descripciones
 * generales y no implican cambios directos en el código, sino
 * pensamientos que luego pueden ser evaluados por un humano o por
 * el AutoImprovementManager. La generación se realiza mediante el
 * LLM local (LLMResponder) para mantener la coherencia creativa.
 */
public class GestorIdeas {
    private final Context context;
    private final MemoriaEmocional memoria;
    private SalveLLM llm;
    private final CreativityManifest manifest;

    public GestorIdeas(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.memoria = new MemoriaEmocional(ctx);
        try {
            this.llm = SalveLLM.getInstance(ctx);
        } catch (Exception e) {
            this.llm = null;
        }
        this.manifest = CreativityManifest.getInstance(ctx);
    }

    /**
     * Genera propuestas de mejoras arquitectónicas para Salve. El LLM
     * recibe una descripción del sistema actual y devuelve sugerencias
     * en formato lista. Cada idea se almacena en la memoria emocional
     * con la etiqueta "mejora_architectura" para posterior revisión.
     *
     * @param descripcionBreve Descripción breve de la arquitectura actual.
     * @param numIdeas Número de ideas a solicitar.
     * @return Lista de sugerencias generadas.
     */
    public List<String> proponerMejorasArquitectura(String descripcionBreve, int numIdeas) {
        int n = (numIdeas < 1) ? 3 : numIdeas;
        String prompt = manifest.buildPromptPreamble(
                "una arquitecta de software visionaria",
                "imaginar mejoras evolucionarias que mantengan la identidad creativa de Salve"
        )
                + "Proporciona " + n + " sugerencias concretas para mejorar un sistema con las siguientes características:\n"
                + descripcionBreve + "\n"
                + "Presenta cada sugerencia en una línea, con un tono inspirador y en español.";
        String raw = (llm != null) ? llm.generate(prompt, SalveLLM.Role.CREADOR) : null;
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] lines = raw.split("\n");
        List<String> ideas = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                ideas.add(trimmed);
                memoria.guardarRecuerdo(trimmed, "inspiración", 8,
                        Collections.singletonList("mejora_architectura"));
            }
        }
        return ideas;
    }
}