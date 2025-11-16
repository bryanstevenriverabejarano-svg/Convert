package salve.core;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObservatorioTendencias centraliza fuentes de conocimiento offline y permite
 * generar resúmenes creativos sobre categorías específicas. Está pensado para
 * funcionar incluso sin conexión a Internet, apoyándose en datasets curados
 * manualmente.
 */
public class ObservatorioTendencias {

    public static class Fuente {
        private final String titulo;
        private final String descripcion;
        private final List<String> etiquetas;

        public Fuente(String titulo, String descripcion, List<String> etiquetas) {
            this.titulo = titulo == null ? "" : titulo.trim();
            this.descripcion = descripcion == null ? "" : descripcion.trim();
            this.etiquetas = etiquetas == null ? new ArrayList<>() : new ArrayList<>(etiquetas);
        }

        public String getTitulo() {
            return titulo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public List<String> getEtiquetas() {
            return etiquetas;
        }
    }

    private final Map<String, List<Fuente>> fuentesPorCategoria;
    private final LLMResponder llm;
    private final CreativityManifest manifest;

    public ObservatorioTendencias(Context context) {
        this.fuentesPorCategoria = new HashMap<>();
        this.llm = LLMResponder.getInstance(context);
        this.manifest = CreativityManifest.getInstance(context);
    }

    public void registrarFuente(String categoria, Fuente fuente) {
        if (categoria == null || categoria.trim().isEmpty() || fuente == null) {
            return;
        }
        String key = categoria.trim().toLowerCase();
        List<Fuente> lista = fuentesPorCategoria.computeIfAbsent(key, k -> new ArrayList<>());
        lista.add(fuente);
    }

    public String generarInformeCreativo(String categoria, int maxFuentes) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return "No se especificó una categoría para analizar.";
        }
        String key = categoria.trim().toLowerCase();
        List<Fuente> fuentes = fuentesPorCategoria.get(key);
        if (fuentes == null || fuentes.isEmpty()) {
            return "Aún no hay fuentes registradas para la categoría " + categoria + ".";
        }
        int limite = Math.max(1, maxFuentes);
        StringBuilder contexto = new StringBuilder();
        int contador = 0;
        for (Fuente fuente : fuentes) {
            if (contador >= limite) {
                break;
            }
            contexto.append("Título: ").append(fuente.getTitulo()).append('\n');
            contexto.append("Descripción: ").append(fuente.getDescripcion()).append('\n');
            contexto.append("Etiquetas: ").append(String.join(", ", fuente.getEtiquetas())).append("\n\n");
            contador++;
        }

        String prompt = manifest.buildPromptPreamble(
                "una analista cultural futurista",
                "extraer patrones con calidez narrativa"
        ) + "Analiza las siguientes fuentes offline y elabora un boletín creativo en español.\n"
                + contexto
                + "Responde en formato:\n"
                + "1. Macro tendencia detectada\n"
                + "2. Micro hallazgos\n"
                + "3. Recomendaciones creativas para Salve\n"
                + "4. Pregunta inspiradora\n";

        String informe = llm.generate(prompt);
        if (informe == null || informe.trim().isEmpty()) {
            return "El observatorio no pudo generar un informe en este momento.";
        }
        return informe.trim();
    }
}
