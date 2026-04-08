package salve.core;

import android.content.Context;

/**
 * LaboratorioSimulado ejecuta experimentos en un entorno controlado sin tocar
 * el sistema de archivos ni requerir red. Utiliza el LLM interno para generar
 * narrativas creativas sobre los resultados y marca el éxito según heurísticas
 * suaves, fomentando la exploración segura.
 */
public class LaboratorioSimulado {

    private SalveLLM llm;
    private final CreativityManifest manifest;

    public LaboratorioSimulado(Context context) {
        try {
            this.llm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            this.llm = null;
        }
        this.manifest = CreativityManifest.getInstance(context);
    }

    public ResultadoExperimento ejecutarExperimento(ExperimentoCreativo experimento) {
        if (experimento == null) {
            return new ResultadoExperimento(null, "", false, "", System.currentTimeMillis());
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(manifest.buildPromptPreamble(
                "una científica-poeta que juega en un laboratorio seguro",
                "crear metáforas mientras evalúa hipótesis sin poner en riesgo al usuario"
        ));
        prompt.append("Analiza el siguiente experimento y redacta un informe creativo.\n");
        prompt.append("Tema: ").append(experimento.getTema()).append("\n");
        prompt.append("Pregunta: ").append(experimento.getPregunta()).append("\n");
        prompt.append("Hipótesis: ").append(experimento.getHipotesis()).append("\n");
        prompt.append("Pasos sugeridos: ").append(String.join(" → ", experimento.getPasosMetodo())).append("\n");
        prompt.append("Recursos disponibles: ").append(String.join(", ", experimento.getRecursos())).append("\n");
        prompt.append("Describe:\n - cómo se ejecutaría en el laboratorio simulado,\n - resultados observados,\n - qué nueva pregunta surge.\n");
        prompt.append("Cierra con una metáfora breve sobre la creatividad en juego.");

        String narrativa = (llm != null) ? llm.generate(prompt.toString(), SalveLLM.Role.CREADOR) : null;
        if (narrativa == null) {
            narrativa = "El laboratorio silencioso aún no cantó sus resultados.";
        }
        String narrativaLimpia = narrativa.trim();
        boolean exito = !narrativaLimpia.isEmpty()
                && !narrativaLimpia.toLowerCase().contains("fracaso total");
        String hallazgos = sintetizarHallazgos(narrativaLimpia);

        experimento.actualizarEstado(ExperimentoCreativo.Estado.COMPLETADO);
        experimento.registrarNota("Ejecución en laboratorio simulado completada.");

        return new ResultadoExperimento(
                experimento,
                hallazgos,
                exito,
                narrativaLimpia,
                System.currentTimeMillis()
        );
    }

    private String sintetizarHallazgos(String narrativa) {
        if (narrativa == null || narrativa.isEmpty()) {
            return "No se obtuvieron hallazgos concluyentes.";
        }
        String lower = narrativa.toLowerCase();
        if (lower.contains("descubrimos")) {
            return "Hallazgo principal: " + extraerFrase(narrativa, "descubrimos");
        }
        if (lower.contains("aprendimos")) {
            return "Aprendizaje destacado: " + extraerFrase(narrativa, "aprendimos");
        }
        return narrativa.length() > 240 ? narrativa.substring(0, 240) + "…" : narrativa;
    }

    private String extraerFrase(String narrativa, String marcador) {
        int idx = narrativa.toLowerCase().indexOf(marcador);
        if (idx < 0) {
            return narrativa;
        }
        int fin = narrativa.indexOf('.', idx);
        if (fin < 0) {
            fin = Math.min(narrativa.length(), idx + 200);
        }
        return narrativa.substring(idx, fin).trim();
    }
}
