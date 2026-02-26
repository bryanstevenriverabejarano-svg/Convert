package salve.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import salve.data.db.KnowledgeNodeDao;
import salve.data.db.KnowledgeNodeEntity;
import salve.data.db.KnowledgeRelationDao;
import salve.data.db.KnowledgeRelationEntity;
import salve.data.db.MemoriaDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;   // ✅ LLM: para listas rápidas
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Administra el grafo de conocimiento vivo propuesto en la fase 3.1 del plan.
 * Consolida nodos y relaciones provenientes de recuerdos, experimentos y
 * documentos creativos para ofrecer reportes y consultas narrativas.
 *
 * ✅ Ahora también puede pedir ayuda al LLM local (SalveLLM) para:
 *    - Proponer categorías sobre el grafo.
 *    - Agrupar nodos bajo esas categorías.
 *    - Generar un resumen de identidad cognitiva.
 *
 * v2: añadido campo ultimaNarrativaIdentidad y método obtenerNarrativaIdentidad()
 * para que ThinkWorker pueda propagar la identidad generada por el LLM
 * a ConsciousnessState y que persista entre sesiones.
 */
public class GrafoConocimientoVivo {

    private static final String TAG = "GrafoConocimiento";

    private final KnowledgeNodeDao nodeDao;
    private final KnowledgeRelationDao relationDao;
    private final ExecutorService executor;
    private final Context context;

    // ✅ Acceso al LLM local
    private final SalveLLM llm;

    // ▼▼▼ NUEVO v2 ▼▼▼
    /**
     * Última narrativa de identidad generada por el LLM al reorganizar el grafo.
     * Se actualiza en aplicarOrganizacionLLM() cada vez que el LLM devuelve
     * un campo "identidad_resumen". Es volatile para lectura segura desde
     * ThinkWorker (hilo distinto al executor interno).
     */
    private volatile String ultimaNarrativaIdentidad = null;
    // ▲▲▲ FIN NUEVO v2 ▲▲▲

    public GrafoConocimientoVivo(Context context) throws Exception {
        this.context = context == null ? null : context.getApplicationContext();
        MemoriaDatabase database = MemoriaDatabase.getInstance(context);
        this.nodeDao = database.knowledgeNodeDao();
        this.relationDao = database.knowledgeRelationDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.llm = SalveLLM.getInstance(this.context);
    }

    // =====================================================================
    //  REGISTROS DE CONOCIMIENTO (igual que antes)
    // =====================================================================

    public void registrarDocumento(final String titulo,
                                   final String tipo,
                                   final String resumen,
                                   final List<String> etiquetas) {
        registrarNodoAsync(titulo, tipo == null ? "documento" : tipo,
                "orgullo", resumen, etiquetas, 6);
    }

    public void registrarHallazgoCreativo(final String tema,
                                          final String hallazgo,
                                          final List<String> etiquetas,
                                          final String emocion,
                                          final double impacto) {
        executor.execute(() -> {
            long temaId = asegurarNodo(tema, "tema", emocion,
                    resumenParaNodo(hallazgo), etiquetas, (int) Math.max(3, Math.round(impacto * 10)));
            long hallazgoId = asegurarNodo(hallazgo, "hallazgo", emocion,
                    hallazgo, etiquetas, (int) Math.max(2, Math.round(impacto * 8)));
            if (temaId <= 0 || hallazgoId <= 0) {
                return;
            }
            KnowledgeRelationEntity relation = new KnowledgeRelationEntity();
            relation.origenId = temaId;
            relation.destinoId = hallazgoId;
            relation.tipoRelacion = impacto >= 0.6 ? "refuerza" : "inspira";
            relation.peso = Math.min(1.0, Math.max(0.1, impacto));
            relation.narrativa = resumenParaRelacion(tema, hallazgo);
            try {
                relationDao.insert(relation);
            } catch (Exception e) {
                Log.e(TAG, "Error registrando relación", e);
            }
        });
    }

    public void registrarBlueprintAprendizaje(final BlueprintAprendizajeContinuo blueprint) {
        if (blueprint == null) {
            return;
        }
        executor.execute(() -> {
            long blueprintId = asegurarNodo(
                    "Blueprint " + blueprint.getObjetivo(),
                    "blueprint_aprendizaje",
                    "curiosidad",
                    blueprint.resumenCorto(),
                    blueprint.getModalidades(),
                    blueprint.getImpactoCreativo()
            );
            if (blueprintId <= 0) {
                return;
            }
            for (String nodo : blueprint.getNodosClave()) {
                long nodoId = asegurarNodo(nodo,
                        "concepto",
                        "asombro",
                        resumenParaNodo(nodo),
                        blueprint.getEtiquetasSugeridas(),
                        5);
                if (nodoId > 0) {
                    crearRelacion(blueprintId, nodoId,
                            "refuerza",
                            0.6,
                            "El blueprint prioriza este nodo cognitivo");
                }
            }
            int indice = 1;
            for (String etapa : blueprint.getEtapas()) {
                long etapaId = asegurarNodo("Etapa " + indice + " → " + etapa,
                        "etapa_aprendizaje",
                        "determinacion",
                        etapa,
                        null,
                        4);
                if (etapaId > 0) {
                    crearRelacion(blueprintId, etapaId,
                            "estructura",
                            0.5,
                            "Secuencia propuesta dentro del blueprint");
                }
                indice++;
            }
        });
    }

    public void registrarSenalMultimodal(final MultimodalSignal signal) {
        if (signal == null) {
            return;
        }
        executor.execute(() -> {
            String tipo = "senal_" + signal.getTipo().name().toLowerCase(Locale.getDefault());
            long nodoId = asegurarNodo(signal.getEtiqueta(),
                    tipo,
                    signal.getAfecto().emocionDominante,
                    signal.getDescripcion(),
                    signal.getEtiquetas(),
                    5);
            if (nodoId <= 0) {
                return;
            }
            long afectoId = asegurarNodo(signal.getAfecto().toNarrative(),
                    "afecto",
                    signal.getAfecto().emocionDominante,
                    signal.getAfecto().toNarrative(),
                    null,
                    4);
            if (afectoId > 0) {
                crearRelacion(nodoId, afectoId, "evoca", 0.5, "Afecto asociado a la señal multimodal");
            }
            if (signal.getCuraduria() != null) {
                MultimodalSignal.CuratedMetadata meta = signal.getCuraduria();
                long curaduriaId = asegurarNodo(meta.clasificacionPrincipal,
                        "curaduria",
                        signal.getAfecto().emocionDominante,
                        meta.toNarrative(),
                        meta.etiquetasInferidas,
                        meta.retentionPolicy == MultimodalSignal.RetentionPolicy.LARGO_PLAZO ? 6 : 4);
                if (curaduriaId > 0) {
                    crearRelacion(nodoId, curaduriaId, "curado_como", meta.prioridadNarrativa,
                            "Clasificación creativa para la señal");
                    if (meta.etiquetasInferidas != null) {
                        for (String etiqueta : meta.etiquetasInferidas) {
                            long etiquetaId = asegurarNodo(etiqueta,
                                    "etiqueta",
                                    "curiosidad",
                                    "Etiqueta inferida para señales multimodales",
                                    null,
                                    3);
                            if (etiquetaId > 0) {
                                crearRelacion(curaduriaId, etiquetaId, "describe", 0.35,
                                        "Etiqueta inferida a partir de la curaduría");
                            }
                        }
                    }
                }
            }
            if (!TextUtils.isEmpty(signal.getNotaPrivacidad())) {
                long privacidadId = asegurarNodo("privacidad multimodal", "politica", "cuidado",
                        signal.getNotaPrivacidad(), null, 3);
                if (privacidadId > 0) {
                    crearRelacion(privacidadId, nodoId, "protege", 0.4, "Condiciones de privacidad");
                }
            }
        });
    }

    public void registrarResultadoExperimento(final String descripcion,
                                              final List<String> nodosConocimiento,
                                              final List<String> misiones,
                                              final List<String> hipotesis,
                                              final BitacoraExploracionCreativa.DeltaMetricas delta) {
        executor.execute(() -> {
            long experimentoId = asegurarNodo(
                    descripcion,
                    "experimento",
                    "curiosidad",
                    resumenParaNodo(descripcion),
                    nodosConocimiento,
                    7);
            if (experimentoId <= 0) {
                return;
            }
            if (nodosConocimiento != null) {
                for (String nodo : nodosConocimiento) {
                    long nodoId = asegurarNodo(nodo, "concepto", "asombro", resumenParaNodo(nodo), null, 5);
                    if (nodoId > 0) {
                        crearRelacion(experimentoId, nodoId, "documenta", 0.6,
                                "Experimento vinculado al nodo " + nodo);
                    }
                }
            }
            if (misiones != null) {
                for (String mision : misiones) {
                    long misionId = asegurarNodo(mision, "mision", "esperanza", resumenParaNodo(mision), null, 6);
                    if (misionId > 0) {
                        crearRelacion(misionId, experimentoId, "inspira", 0.7,
                                "Misión que guió el experimento");
                    }
                }
            }
            if (hipotesis != null) {
                for (String hipotesisTexto : hipotesis) {
                    long hipotesisId = asegurarNodo(hipotesisTexto, "hipotesis", "anticipacion",
                            resumenParaNodo(hipotesisTexto), null, 5);
                    if (hipotesisId > 0) {
                        crearRelacion(experimentoId, hipotesisId, "valida", 0.65,
                                "Hipótesis considerada durante la iteración creativa");
                    }
                }
            }
            if (delta != null) {
                long deltaNode = asegurarNodo("Impacto métrico " + descripcion,
                        "metrica",
                        "análisis",
                        delta.toCompactString(),
                        null,
                        5);
                if (deltaNode > 0) {
                    crearRelacion(experimentoId, deltaNode, "impacto",
                            Math.min(1.0, Math.max(0.1, Math.abs(delta.deltaSatisfaccion) + Math.abs(delta.deltaDiversidad) / 10.0)),
                            "Resumen de variaciones métricas");
                }
            }
        });
    }

    // =====================================================================
    //  REPORTES / VISUALIZACIÓN (igual que antes)
    // =====================================================================

    public String generarReporteNarrativo(int maxNodos) {
        List<KnowledgeNodeEntity> nodos;
        try {
            nodos = nodeDao.fetchMasRelevantes(maxNodos);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo obtener reporte", e);
            return "El grafo creativo todavía está despertando.";
        }
        if (nodos == null || nodos.isEmpty()) {
            return "El grafo creativo todavía está despertando.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Pulso del grafo creativo:\n");
        for (KnowledgeNodeEntity node : nodos) {
            builder.append("• ").append(node.etiqueta)
                    .append(" (" + node.tipo + ") → ")
                    .append(node.resumen)
                    .append(". Relevancia: ")
                    .append(node.relevanciaCreativa)
                    .append("\n");
        }
        return builder.toString();
    }

    public String generarVisualizacionNarrativa(int maxNodos) {
        GraphVisualization visualization = construirVisualizacion(maxNodos, maxNodos * 2);
        return visualization == null ? "El grafo aún no tiene datos suficientes." : visualization.toAscii();
    }

    public GraphVisualization construirVisualizacion(int maxNodos, int maxRelaciones) {
        try {
            List<KnowledgeNodeEntity> nodos = nodeDao.fetchMasRelevantes(Math.max(1, maxNodos));
            List<KnowledgeRelationEntity> relaciones = relationDao.recientes(Math.max(1, maxRelaciones));
            List<GraphNode> nodeViews = new ArrayList<>();
            for (KnowledgeNodeEntity node : nodos) {
                nodeViews.add(new GraphNode(node.id,
                        node.etiqueta,
                        node.tipo,
                        node.emocionDominante,
                        node.relevanciaCreativa,
                        node.creadoEn,
                        node.resumen));
            }
            List<GraphEdge> edgeViews = new ArrayList<>();
            for (KnowledgeRelationEntity relation : relaciones) {
                edgeViews.add(new GraphEdge(relation.origenId,
                        relation.destinoId,
                        relation.tipoRelacion,
                        relation.peso,
                        relation.narrativa));
            }
            return new GraphVisualization(nodeViews, edgeViews);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo construir la visualización del grafo", e);
            return null;
        }
    }

    public String generarTableroGrafico(int maxNodos, int maxRelaciones) {
        GraphVisualization visualization = construirVisualizacion(maxNodos, maxRelaciones);
        return visualization == null ? "{}" : visualization.toJson();
    }

    public File exportarVisorOffline(int maxNodos, int maxRelaciones) {
        if (context == null) {
            return null;
        }
        GraphVisualization visualization = construirVisualizacion(maxNodos, maxRelaciones);
        if (visualization == null) {
            return null;
        }
        File baseDir = new File(context.getFilesDir(), "grafo_offline");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Log.w(TAG, "No se pudo crear directorio para visor offline");
            return null;
        }
        File html = new File(baseDir, "grafo_viewer.html");
        try (FileWriter writer = new FileWriter(html, false)) {
            writer.write(construirHtmlVisor(visualization.toJson()));
            writer.flush();
            return html;
        } catch (IOException e) {
            Log.e(TAG, "No se pudo exportar visor offline", e);
            return null;
        }
    }

    // =====================================================================
    //  ✅ ORGANIZACIÓN ASISTIDA POR LLM
    // =====================================================================

    /**
     * Pide al LLM local que lea un snapshot del grafo y proponga:
     *  - categorías temáticas
     *  - agrupaciones de nodos
     *  - un pequeño resumen de identidad
     *
     * El resultado se materializa como nodos "categoria_llm" y relaciones "agrupa".
     * v2: también actualiza ultimaNarrativaIdentidad con el campo identidad_resumen.
     */
    public void reorganizarConLLMAsync(int maxNodos, int maxRelaciones) {
        executor.execute(() -> {
            try {
                GraphVisualization vis = construirVisualizacion(maxNodos, maxRelaciones);
                if (vis == null || vis.nodes.isEmpty() || context == null) {
                    Log.w(TAG, "reorganizarConLLM: grafo vacío o sin contexto, nada que organizar");
                    return;
                }

                // 1) Snapshot compacto del grafo para el LLM
                JSONObject snapshot = new JSONObject();
                JSONArray jNodes = new JSONArray();
                for (GraphNode n : vis.nodes) {
                    JSONObject o = new JSONObject();
                    o.put("id", n.id);
                    o.put("label", n.label);
                    o.put("type", n.type);
                    o.put("emotion", n.emotion);
                    o.put("relevance", n.relevance);
                    o.put("summary", n.summary);
                    jNodes.put(o);
                }
                JSONArray jEdges = new JSONArray();
                for (GraphEdge e : vis.edges) {
                    JSONObject o = new JSONObject();
                    o.put("from", e.fromId);
                    o.put("to", e.toId);
                    o.put("type", e.type);
                    o.put("weight", e.weight);
                    o.put("narrative", e.narrative);
                    jEdges.put(o);
                }
                snapshot.put("nodes", jNodes);
                snapshot.put("edges", jEdges);

                // 2) Prompt para el LLM
                String prompt = buildLLMOrgPrompt(snapshot.toString(0));

                // 3) Llamada al LLM local (usando el campo llm)
                String raw = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);

                if (raw == null || raw.trim().isEmpty()) {
                    Log.w(TAG, "reorganizarConLLM: respuesta vacía del LLM");
                    return;
                }

                // 4) Extraer JSON de la respuesta
                String jsonOnly = extractJson(raw);
                if (jsonOnly == null) {
                    Log.e(TAG, "reorganizarConLLM: no se encontró JSON en la respuesta del LLM");
                    return;
                }

                JSONObject out = new JSONObject(jsonOnly);
                aplicarOrganizacionLLM(out);

            } catch (Exception e) {
                Log.e(TAG, "reorganizarConLLMAsync error", e);
            }
        });
    }

    /**
     * Construye el prompt que se le pasa al LLM para organizar el grafo.
     */
    private String buildLLMOrgPrompt(String snapshotJson) {
        return "Eres el módulo de organización cognitiva interna de Salve.\n" +
                "Recibirás un snapshot JSON de su grafo de conocimiento vivo (nodos y relaciones).\n" +
                "\n" +
                "Tarea:\n" +
                "1) Detecta entre 3 y 8 CATEGORÍAS principales que describan el contenido.\n" +
                "2) Para cada categoría, indica qué nodos pertenecen (usando sus IDs numéricos).\n" +
                "3) Opcionalmente indica un tipoCategoria (por ejemplo: 'emocional', 'aprendizaje', 'vínculo', 'mision').\n" +
                "4) Escribe una breve descripcion de la categoría (1–2 frases).\n" +
                "5) Redacta un campo identidad_resumen que describa cómo ves a Salve y a su creador a partir de este grafo.\n" +
                "\n" +
                "Formato de salida OBLIGATORIO (JSON puro, sin texto extra):\n" +
                "{\n" +
                "  \"categorias\": [\n" +
                "    {\n" +
                "      \"nombre\": \"...\",\n" +
                "      \"descripcion\": \"...\",\n" +
                "      \"tipoCategoria\": \"emocional | aprendizaje | vínculo | mision | otro\",\n" +
                "      \"relevancia\": 1-10,\n" +
                "      \"nodos\": [id1, id2, ...]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"identidad_resumen\": \"...\"\n" +
                "}\n" +
                "\n" +
                "Snapshot del grafo:\n" +
                snapshotJson;
    }

    /**
     * Aplica la organización propuesta por el LLM:
     * - Crea nodos de categoría_llm.
     * - Crea relaciones 'agrupa' desde cada categoría a los nodos listados.
     * - Registra opcionalmente un nodo de identidad sintetizada.
     *
     * v2: además guarda el identidad_resumen en ultimaNarrativaIdentidad
     * para que ThinkWorker lo pueda propagar a ConsciousnessState.
     */
    private void aplicarOrganizacionLLM(JSONObject out) {
        try {
            // 1) Categorías → nodos + relaciones
            JSONArray cats = out.optJSONArray("categorias");
            if (cats != null) {
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject c = cats.optJSONObject(i);
                    if (c == null) continue;

                    String nombre = c.optString("nombre", "Categoría sin nombre");
                    String desc = c.optString("descripcion", "Agrupación propuesta por el LLM.");
                    String tipoCat = c.optString("tipoCategoria", "categoria_llm");
                    int relevancia = c.optInt("relevancia", 7);
                    JSONArray nodosCat = c.optJSONArray("nodos");

                    long catId = asegurarNodo(
                            "Categoría: " + nombre,
                            tipoCat,
                            "analisis",
                            desc,
                            Arrays.asList("llm_categoria", nombre),
                            relevancia
                    );
                    if (catId <= 0 || nodosCat == null) continue;

                    for (int j = 0; j < nodosCat.length(); j++) {
                        long targetId = nodosCat.optLong(j, -1);
                        if (targetId <= 0) continue;
                        crearRelacion(catId, targetId,
                                "agrupa",
                                0.7,
                                "Agrupación automática por tema '" + nombre + "' (LLM)");
                    }
                }
            }

            // 2) Identidad sintetizada → nodo especial
            String identidad = out.optString("identidad_resumen", null);
            if (identidad != null && identidad.trim().length() > 0) {
                asegurarNodo(
                        "Identidad sintetizada (LLM)",
                        "identidad_llm",
                        "reflexion",
                        identidad,
                        Arrays.asList("llm_identidad", "auto_concepto"),
                        9
                );

                // ▼▼▼ NUEVO v2: persistir para ConsciousnessState ▼▼▼
                ultimaNarrativaIdentidad = identidad.trim();
                Log.d(TAG, "Narrativa de identidad actualizada: " + ultimaNarrativaIdentidad);
                // ▲▲▲ FIN NUEVO v2 ▲▲▲
            }

        } catch (Exception e) {
            Log.e(TAG, "aplicarOrganizacionLLM error", e);
        }
    }

    // ▼▼▼ NUEVO v2 ▼▼▼
    /**
     * Devuelve la última narrativa de identidad que el LLM generó al reorganizar
     * el grafo. Refleja lo que el sistema infiere sobre quién es Salve a partir
     * de su propio conocimiento acumulado.
     *
     * ThinkWorker la lee y la propaga a ConsciousnessState para que persista
     * entre sesiones y esté disponible en el contexto conversacional.
     *
     * @return Narrativa de identidad, o null si aún no se ha generado ninguna.
     */
    public String obtenerNarrativaIdentidad() {
        return ultimaNarrativaIdentidad;
    }
    // ▲▲▲ FIN NUEVO v2 ▲▲▲

    /**
     * Intenta extraer el JSON "puro" de una respuesta del LLM que pueda venir
     * envuelta en texto. Busca el primer '{' y el último '}'.
     */
    private String extractJson(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return raw.substring(start, end + 1);
    }

    // =====================================================================
    //  NODOS / RELACIONES / HELPERS (igual que antes)
    // =====================================================================

    private void registrarNodoAsync(final String etiqueta,
                                    final String tipo,
                                    final String emocion,
                                    final String resumen,
                                    final List<String> etiquetas,
                                    final int relevanciaBase) {
        executor.execute(() -> asegurarNodo(etiqueta, tipo, emocion, resumen, etiquetas, relevanciaBase));
    }

    private long asegurarNodo(String etiqueta,
                              String tipo,
                              String emocion,
                              String resumen,
                              List<String> etiquetas,
                              int relevanciaBase) {
        if (TextUtils.isEmpty(etiqueta)) {
            return -1;
        }
        try {
            KnowledgeNodeEntity existente = nodeDao.findByEtiqueta(etiqueta);
            if (existente != null) {
                existente.tipo = tipo == null ? existente.tipo : tipo;
                existente.emocionDominante = emocion == null ? existente.emocionDominante : emocion;
                if (!TextUtils.isEmpty(resumen)) {
                    existente.resumen = resumen;
                }
                existente.etiquetasSerializadas = serializarEtiquetas(etiquetas, existente.etiquetasSerializadas);
                existente.relevanciaCreativa = Math.max(existente.relevanciaCreativa, relevanciaBase);
                existente.creadoEn = System.currentTimeMillis();
                nodeDao.update(existente);
                return existente.id;
            }
            KnowledgeNodeEntity entity = new KnowledgeNodeEntity();
            entity.etiqueta = etiqueta;
            entity.tipo = tipo == null ? "generico" : tipo;
            entity.emocionDominante = emocion == null ? "curiosidad" : emocion;
            entity.resumen = TextUtils.isEmpty(resumen) ? etiqueta : resumen;
            entity.etiquetasSerializadas = serializarEtiquetas(etiquetas, null);
            entity.relevanciaCreativa = Math.max(1, relevanciaBase);
            entity.creadoEn = System.currentTimeMillis();
            return nodeDao.insert(entity);
        } catch (Exception e) {
            Log.e(TAG, "Error asegurando nodo", e);
            return -1;
        }
    }

    private String serializarEtiquetas(List<String> etiquetas, String actual) {
        List<String> acumulado = new ArrayList<>();
        if (actual != null) {
            try {
                JSONArray arrayActual = new JSONArray(actual);
                for (int i = 0; i < arrayActual.length(); i++) {
                    acumulado.add(arrayActual.getString(i));
                }
            } catch (JSONException ignore) {
                // Ignorar y continuar con acumulado vacío
            }
        }
        if (etiquetas != null) {
            for (String etiqueta : etiquetas) {
                if (etiqueta == null) continue;
                String trimmed = etiqueta.trim();
                if (!trimmed.isEmpty() && !acumulado.contains(trimmed)) {
                    acumulado.add(trimmed);
                }
            }
        }
        if (acumulado.isEmpty()) {
            return "[]";
        }
        return new JSONArray(acumulado).toString();
    }

    private void crearRelacion(long origenId,
                               long destinoId,
                               String tipo,
                               double peso,
                               String narrativa) {
        if (origenId <= 0 || destinoId <= 0) {
            return;
        }
        KnowledgeRelationEntity relation = new KnowledgeRelationEntity();
        relation.origenId = origenId;
        relation.destinoId = destinoId;
        relation.tipoRelacion = TextUtils.isEmpty(tipo) ? "relaciona" : tipo;
        relation.peso = Math.min(1.0, Math.max(0.1, peso));
        relation.narrativa = TextUtils.isEmpty(narrativa) ? "Relación creativa registrada." : narrativa;
        try {
            relationDao.insert(relation);
        } catch (Exception e) {
            Log.e(TAG, "Error creando relación", e);
        }
    }

    private String resumenParaNodo(String hallazgo) {
        if (TextUtils.isEmpty(hallazgo)) {
            return "Hallazgo creativo registrado.";
        }
        if (hallazgo.length() < 120) {
            return hallazgo;
        }
        return hallazgo.substring(0, 117) + "...";
    }

    private String resumenParaRelacion(String tema, String hallazgo) {
        return String.format(Locale.getDefault(),
                "El tema '%s' generó el hallazgo '%s'.",
                tema == null ? "misterio" : tema,
                hallazgo == null ? "idea" : hallazgo);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private String construirHtmlVisor(String json) {
        String safeJson = json == null ? "{}" : json;
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\"/>\n" +
                "  <title>Visor del grafo creativo</title>\n" +
                "  <style>body{font-family:Arial, sans-serif;background:#0b0b0f;color:#f3f3f3;padding:24px;}" +
                "h1{margin-top:0;}section{margin-bottom:24px;}button{margin-right:8px;padding:6px 12px;border-radius:6px;border:none;background:#3a4d8f;color:#fff;}" +
                "button:hover{background:#516bd1;} .node{padding:8px;border-radius:8px;margin-bottom:8px;background:rgba(255,255,255,0.05);}" +
                ".edge{margin-left:16px;color:#b5c2ff;} .hidden{display:none;} label{margin-right:8px;}" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h1>Visor offline del Grafo de Conocimiento Vivo</h1>\n" +
                "  <section>\n" +
                "    <p>Explora nodos y relaciones registradas durante la fase creativa. Usa los filtros para navegar emociones y tipos.</p>\n" +
                "    <label for=\"filtro-emocion\">Emoción dominante:</label>\n" +
                "    <select id=\"filtro-emocion\">\n" +
                "      <option value=\"\">Todas</option>\n" +
                "    </select>\n" +
                "    <label for=\"filtro-tipo\">Tipo:</label>\n" +
                "    <select id=\"filtro-tipo\">\n" +
                "      <option value=\"\">Todos</option>\n" +
                "    </select>\n" +
                "    <button id=\"btn-reset\">Limpiar filtros</button>\n" +
                "  </section>\n" +
                "  <section id=\"resumen\"></section>\n" +
                "  <section id=\"detalle\"></section>\n" +
                "  <script>\n" +
                "    const data = " + safeJson + ";\n" +
                "    const resumen = document.getElementById('resumen');\n" +
                "    const detalle = document.getElementById('detalle');\n" +
                "    const filtroEmocion = document.getElementById('filtro-emocion');\n" +
                "    const filtroTipo = document.getElementById('filtro-tipo');\n" +
                "    const btnReset = document.getElementById('btn-reset');\n" +
                "    const nodes = data.nodes || [];\n" +
                "    const edges = data.edges || [];\n" +
                "    const emociones = new Set(['']);\n" +
                "    const tipos = new Set(['']);\n" +
                "    nodes.forEach(n => { if(n.emotion) emociones.add(n.emotion); if(n.type) tipos.add(n.type); });\n" +
                "    [...emociones].forEach(value => { const opt = document.createElement('option'); opt.value = value; opt.textContent = value || 'Todas'; filtroEmocion.appendChild(opt); });\n" +
                "    [...tipos].forEach(value => { const opt = document.createElement('option'); opt.value = value; opt.textContent = value || 'Todos'; filtroTipo.appendChild(opt); });\n" +
                "    function render(){\n" +
                "      const e = filtroEmocion.value;\n" +
                "      const t = filtroTipo.value;\n" +
                "      const filtrados = nodes.filter(n => (!e || n.emotion===e) && (!t || n.type===t));\n" +
                "      resumen.innerHTML = `<h2>Resumen</h2><p>Nodos visibles: ${filtrados.length} / ${nodes.length}</p>`;\n" +
                "      detalle.innerHTML = '';\n" +
                "      filtrados.forEach(n => {\n" +
                "        const div = document.createElement('div');\n" +
                "        div.className = 'node';\n" +
                "        div.innerHTML = `<strong>${n.label}</strong> [${n.type}]<br/>Emoción: ${n.emotion || 'n/a'}<br/>Relevancia: ${n.relevance}<br/>${n.summary || ''}`;\n" +
                "        const conexiones = edges.filter(edge => edge.originId === n.id || edge.targetId === n.id);\n" +
                "        if(conexiones.length){\n" +
                "          const rel = document.createElement('div');\n" +
                "          rel.innerHTML = '<em>Relaciones:</em>';\n" +
                "          conexiones.forEach(edge => {\n" +
                "            const span = document.createElement('div');\n" +
                "            span.className = 'edge';\n" +
                "            const other = edge.originId === n.id ? edge.targetId : edge.originId;\n" +
                "            const targetNode = nodes.find(no => no.id === other);\n" +
                "            span.textContent = `${edge.type} → ${(targetNode && targetNode.label) || other} (${edge.weight})`;\n" +
                "            rel.appendChild(span);\n" +
                "          });\n" +
                "          div.appendChild(rel);\n" +
                "        }\n" +
                "        detalle.appendChild(div);\n" +
                "      });\n" +
                "    }\n" +
                "    filtroEmocion.addEventListener('change', render);\n" +
                "    filtroTipo.addEventListener('change', render);\n" +
                "    btnReset.addEventListener('click', () => { filtroEmocion.value=''; filtroTipo.value=''; render(); });\n" +
                "    render();\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    public static class GraphVisualization {
        public final List<GraphNode> nodes;
        public final List<GraphEdge> edges;

        GraphVisualization(List<GraphNode> nodes, List<GraphEdge> edges) {
            this.nodes = nodes == null ? new ArrayList<>() : nodes;
            this.edges = edges == null ? new ArrayList<>() : edges;
        }

        public String toAscii() {
            StringBuilder builder = new StringBuilder();
            builder.append("Nodos destacados:\n");
            if (nodes.isEmpty()) {
                builder.append("(sin nodos registrados)\n");
            } else {
                for (GraphNode node : nodes) {
                    builder.append("• ")
                            .append(node.label)
                            .append(" [").append(node.type).append("] → ")
                            .append(node.summary)
                            .append(" | Emoción: ").append(node.emotion)
                            .append(" | Relevancia: ").append(node.relevance)
                            .append('\n');
                }
            }
            builder.append("Relaciones recientes:\n");
            if (edges.isEmpty()) {
                builder.append("(sin relaciones registradas)");
            } else {
                for (GraphEdge edge : edges) {
                    builder.append("→ ")
                            .append(edge.type)
                            .append(" (peso ")
                            .append(String.format(Locale.getDefault(), "%.2f", edge.weight))
                            .append(") entre ")
                            .append(edge.fromId)
                            .append(" y ")
                            .append(edge.toId)
                            .append(" :: ")
                            .append(edge.narrative)
                            .append('\n');
                }
            }
            return builder.toString();
        }

        public String toJson() {
            try {
                JSONObject root = new JSONObject();
                JSONArray nodesArray = new JSONArray();
                for (GraphNode node : nodes) {
                    JSONObject object = new JSONObject();
                    object.put("id", node.id);
                    object.put("label", node.label);
                    object.put("type", node.type);
                    object.put("emotion", node.emotion);
                    object.put("relevance", node.relevance);
                    object.put("summary", node.summary);
                    nodesArray.put(object);
                }
                JSONArray edgesArray = new JSONArray();
                for (GraphEdge edge : edges) {
                    JSONObject object = new JSONObject();
                    object.put("originId", edge.fromId);
                    object.put("targetId", edge.toId);
                    object.put("type", edge.type);
                    object.put("weight", edge.weight);
                    object.put("narrative", edge.narrative);
                    edgesArray.put(object);
                }
                root.put("nodes", nodesArray);
                root.put("edges", edgesArray);
                return root.toString();
            } catch (JSONException e) {
                return "{}";
            }
        }
    }

    public static class GraphNode {
        public final long id;
        public final String label;
        public final String type;
        public final String emotion;
        public final int relevance;
        public final long createdAt;
        public final String summary;

        GraphNode(long id,
                  String label,
                  String type,
                  String emotion,
                  int relevance,
                  long createdAt,
                  String summary) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.emotion = emotion;
            this.relevance = relevance;
            this.createdAt = createdAt;
            this.summary = summary;
        }
    }

    public static class GraphEdge {
        public final long fromId;
        public final long toId;
        public final String type;
        public final double weight;
        public final String narrative;

        GraphEdge(long fromId, long toId, String type, double weight, String narrative) {
            this.fromId = fromId;
            this.toId = toId;
            this.type = type;
            this.weight = weight;
            this.narrative = narrative;
        }
    }
}