package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * PanelMetricasCreatividad consolida las métricas definidas en los próximos
 * pasos del plan: satisfacción del usuario, diversidad creativa y estabilidad
 * del ciclo de auto-mejora. Utiliza SharedPreferences para persistir las
 * estadísticas y producir reportes ejecutivos.
 */
public class PanelMetricasCreatividad {

    private static final String PREFS = "panel_metricas_creatividad";
    private static final String KEY_INTERACCIONES = "total_interacciones";
    private static final String KEY_SUMA_SATISFACCION = "suma_satisfaccion";
    private static final String KEY_EXPERIMENTOS = "total_experimentos";
    private static final String KEY_EXPERIMENTOS_EXITO = "experimentos_exito";
    private static final String KEY_AUTO_MEJORA = "ciclos_auto_mejora";
    private static final String KEY_AUTO_MEJORA_ESTABLES = "ciclos_estables";
    private static final String KEY_VALIDACIONES = "validaciones_ejecutadas";
    private static final String KEY_VALIDACIONES_OK = "validaciones_exitosas";
    private static final String KEY_REVISIONES_ETICAS = "revisiones_eticas";
    private static final String KEY_REVISIONES_ETICAS_OK = "revisiones_eticas_aprobadas";
    private static final String KEY_ETIQUETAS = "etiquetas_creativas";
    private static final String KEY_SNAPSHOT = "snapshot_semanal";
    private static final String KEY_SNAPSHOT_TS = "snapshot_timestamp";
    private static final String KEY_COBERTURA_LINEAS = "cobertura_lineas";
    private static final String KEY_COBERTURA_RAMAS = "cobertura_ramas";
    private static final String KEY_COBERTURA_DELTA_LINEAS = "cobertura_delta_lineas";
    private static final String KEY_COBERTURA_DELTA_RAMAS = "cobertura_delta_ramas";
    private static final String KEY_ETICA_VOTOS_HUMANOS = "etica_votos_humanos";
    private static final String KEY_ETICA_VOTOS_HUMANOS_OK = "etica_votos_humanos_ok";
    private static final String KEY_GUARDRAILS_BLOQUEOS = "guardrails_bloqueos";
    private static final String KEY_GUARDRAILS_ALERTAS = "guardrails_alertas";
    private static final String KEY_GUARDRAILS_SUGERENCIAS = "guardrails_sugerencias";
    private static final String KEY_MULTIMODAL_TOTAL = "multimodal_total_etiquetas";
    private static final String KEY_MULTIMODAL_ACIERTOS = "multimodal_total_aciertos";
    private static final String KEY_MULTIMODAL_PRECISION = "multimodal_precision";
    private static final String KEY_PROTOCOLO_HISTORIAL = "etica_historial_protocolos";
    private static final String KEY_RADAR_HISTORIAL = "radar_historial_metricas";
    private static final String KEY_APRENDIZAJE_CICLOS = "aprendizaje_ciclos";
    private static final String KEY_APRENDIZAJE_AUTOGENERADOS = "aprendizaje_autogenerados";
    private static final String KEY_APRENDIZAJE_MULTIMODAL = "aprendizaje_multimodal";
    private static final String KEY_APRENDIZAJE_CONFIANZA = "aprendizaje_confianza";
    private static final String KEY_APRENDIZAJE_MUESTRAS = "aprendizaje_confianza_muestras";

    private final SharedPreferences prefs;

    public PanelMetricasCreatividad(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void registrarInteraccionCreativa(double satisfaccion) {
        int total = prefs.getInt(KEY_INTERACCIONES, 0);
        double suma = Double.longBitsToDouble(prefs.getLong(KEY_SUMA_SATISFACCION,
                Double.doubleToRawLongBits(0.0)));
        total += 1;
        suma += Math.max(0.0, Math.min(1.0, satisfaccion));
        prefs.edit()
                .putInt(KEY_INTERACCIONES, total)
                .putLong(KEY_SUMA_SATISFACCION, Double.doubleToRawLongBits(suma))
                .apply();
    }

    public void registrarExperimento(boolean exito, Iterable<String> etiquetas) {
        int total = prefs.getInt(KEY_EXPERIMENTOS, 0) + 1;
        int exitosos = prefs.getInt(KEY_EXPERIMENTOS_EXITO, 0) + (exito ? 1 : 0);
        prefs.edit()
                .putInt(KEY_EXPERIMENTOS, total)
                .putInt(KEY_EXPERIMENTOS_EXITO, exitosos)
                .apply();
        actualizarEtiquetas(etiquetas);
    }

    public void registrarCicloAutoMejora(boolean estable) {
        int ciclos = prefs.getInt(KEY_AUTO_MEJORA, 0) + 1;
        int estables = prefs.getInt(KEY_AUTO_MEJORA_ESTABLES, 0) + (estable ? 1 : 0);
        prefs.edit()
                .putInt(KEY_AUTO_MEJORA, ciclos)
                .putInt(KEY_AUTO_MEJORA_ESTABLES, estables)
                .apply();
    }

    public void registrarValidacionAutomatica(boolean ejecutada, boolean exitosa) {
        if (!ejecutada) {
            return;
        }
        int total = prefs.getInt(KEY_VALIDACIONES, 0) + 1;
        int exitosas = prefs.getInt(KEY_VALIDACIONES_OK, 0) + (exitosa ? 1 : 0);
        prefs.edit()
                .putInt(KEY_VALIDACIONES, total)
                .putInt(KEY_VALIDACIONES_OK, exitosas)
                .apply();
    }

    public void registrarRevisionEtica(boolean aprobada) {
        int total = prefs.getInt(KEY_REVISIONES_ETICAS, 0) + 1;
        int aprobadas = prefs.getInt(KEY_REVISIONES_ETICAS_OK, 0) + (aprobada ? 1 : 0);
        prefs.edit()
                .putInt(KEY_REVISIONES_ETICAS, total)
                .putInt(KEY_REVISIONES_ETICAS_OK, aprobadas)
                .apply();
    }

    public void registrarCicloAprendizaje(double confianza,
                                          boolean multimodal,
                                          boolean autoGenerado) {
        double confianzaNormalizada = Math.max(0.0, Math.min(1.0, confianza));
        int ciclos = prefs.getInt(KEY_APRENDIZAJE_CICLOS, 0) + 1;
        int autoGenerados = prefs.getInt(KEY_APRENDIZAJE_AUTOGENERADOS, 0) + (autoGenerado ? 1 : 0);
        int multimodales = prefs.getInt(KEY_APRENDIZAJE_MULTIMODAL, 0) + (multimodal ? 1 : 0);
        double sumaConfianza = Double.longBitsToDouble(prefs.getLong(KEY_APRENDIZAJE_CONFIANZA,
                Double.doubleToRawLongBits(0.0)));
        long muestras = prefs.getLong(KEY_APRENDIZAJE_MUESTRAS, 0) + 1;
        sumaConfianza += confianzaNormalizada;
        prefs.edit()
                .putInt(KEY_APRENDIZAJE_CICLOS, ciclos)
                .putInt(KEY_APRENDIZAJE_AUTOGENERADOS, autoGenerados)
                .putInt(KEY_APRENDIZAJE_MULTIMODAL, multimodales)
                .putLong(KEY_APRENDIZAJE_CONFIANZA, Double.doubleToRawLongBits(sumaConfianza))
                .putLong(KEY_APRENDIZAJE_MUESTRAS, muestras)
                .apply();
    }

    public CoverageSnapshot registrarCobertura(double lineCoverage,
                                               double branchCoverage,
                                               double deltaLine,
                                               double deltaBranch) {
        double previoLineas = Double.longBitsToDouble(prefs.getLong(KEY_COBERTURA_LINEAS,
                Double.doubleToRawLongBits(0.0)));
        double previoRamas = Double.longBitsToDouble(prefs.getLong(KEY_COBERTURA_RAMAS,
                Double.doubleToRawLongBits(0.0)));
        double deltaVsPrevLineas = lineCoverage - previoLineas;
        double deltaVsPrevRamas = branchCoverage - previoRamas;
        double deltaLineasRegistrada = Double.isNaN(deltaLine) ? deltaVsPrevLineas : deltaLine;
        double deltaRamasRegistrada = Double.isNaN(deltaBranch) ? deltaVsPrevRamas : deltaBranch;
        prefs.edit()
                .putLong(KEY_COBERTURA_LINEAS, Double.doubleToRawLongBits(normalizar(lineCoverage)))
                .putLong(KEY_COBERTURA_RAMAS, Double.doubleToRawLongBits(normalizar(branchCoverage)))
                .putLong(KEY_COBERTURA_DELTA_LINEAS, Double.doubleToRawLongBits(deltaLineasRegistrada))
                .putLong(KEY_COBERTURA_DELTA_RAMAS, Double.doubleToRawLongBits(deltaRamasRegistrada))
                .apply();
        return new CoverageSnapshot(normalizar(lineCoverage),
                normalizar(branchCoverage),
                deltaLineasRegistrada,
                deltaRamasRegistrada,
                deltaVsPrevLineas,
                deltaVsPrevRamas);
    }

    public void registrarVotoEticoHumano(boolean aprobado) {
        int total = prefs.getInt(KEY_ETICA_VOTOS_HUMANOS, 0) + 1;
        int aprobados = prefs.getInt(KEY_ETICA_VOTOS_HUMANOS_OK, 0) + (aprobado ? 1 : 0);
        prefs.edit()
                .putInt(KEY_ETICA_VOTOS_HUMANOS, total)
                .putInt(KEY_ETICA_VOTOS_HUMANOS_OK, aprobados)
                .apply();
    }

    public GuardrailDecision evaluarGuardrailsCobertura(AutoTestGenerator.GeneratedTestSuite suite,
                                                        CoverageSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        String target = suite == null ? "" : suite.getTargetClass();
        double lineCoverage = snapshot.lineCoverage;
        double branchCoverage = snapshot.branchCoverage;
        double deltaLine = snapshot.deltaLine;
        double deltaBranch = snapshot.deltaBranch;
        boolean coverageCritical = lineCoverage < 0.50 || branchCoverage < 0.40;
        boolean deltaCritical = deltaLine < -0.08 || deltaBranch < -0.08;
        boolean alert = lineCoverage < 0.65 || branchCoverage < 0.55
                || deltaLine < -0.03 || deltaBranch < -0.03;
        boolean block = coverageCritical || deltaCritical;

        List<String> sugerencias = new ArrayList<>();
        String objetivo = TextUtils.isEmpty(target) ? "las clases afectadas" : target;
        if (lineCoverage < 0.60) {
            sugerencias.add("Agregar casos felices y de error para " + objetivo
                    + ", cubriendo ramificaciones no ejercitadas.");
        }
        if (branchCoverage < 0.45) {
            sugerencias.add("Diseñar pruebas diferenciales que ataquen ramas condicionales en "
                    + objetivo + ".");
        }
        if (deltaLine < -0.05 || deltaBranch < -0.05) {
            sugerencias.add("Restaurar cobertura perdida comparando con el baseline previo y reutilizando casos archivados.");
        }
        if (sugerencias.isEmpty() && alert) {
            sugerencias.add("Monitorear la tendencia de cobertura: preparar casos de regresión preventiva.");
        }

        GuardrailDecision decision = new GuardrailDecision(block,
                alert,
                sugerencias,
                String.format(Locale.getDefault(),
                        "Guardrails cobertura → líneas %.2f%% (Δ %.2f) | ramas %.2f%% (Δ %.2f)",
                        lineCoverage * 100,
                        deltaLine * 100,
                        branchCoverage * 100,
                        deltaBranch * 100));
        registrarGuardrailDecision(decision);
        return decision;
    }

    public void registrarMultimodalFeedback(boolean coincide,
                                            double precisionGlobal,
                                            int totalEtiquetas,
                                            int aciertos,
                                            String etiquetaReferencia) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_MULTIMODAL_TOTAL, totalEtiquetas);
        editor.putInt(KEY_MULTIMODAL_ACIERTOS, aciertos);
        editor.putLong(KEY_MULTIMODAL_PRECISION,
                Double.doubleToRawLongBits(normalizar(precisionGlobal)));
        editor.apply();
        if (!TextUtils.isEmpty(etiquetaReferencia) && !coincide) {
            guardarSugerenciasGuardrail(Collections.singletonList(
                    "Curaduría multimodal: revisar cobertura para " + etiquetaReferencia));
        }
    }

    public void registrarPrecisionMultimodal(double precisionInstantanea,
                                             int muestrasDisponibles) {
        double valor = normalizar(precisionInstantanea);
        int muestras = Math.max(0, muestrasDisponibles);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_MULTIMODAL_PRECISION, Double.doubleToRawLongBits(valor));
        if (muestras > 0) {
            editor.putInt(KEY_MULTIMODAL_TOTAL, Math.max(muestras, prefs.getInt(KEY_MULTIMODAL_TOTAL, 0)));
            int aciertos = (int) Math.round(valor * muestras);
            editor.putInt(KEY_MULTIMODAL_ACIERTOS, Math.max(aciertos, prefs.getInt(KEY_MULTIMODAL_ACIERTOS, 0)));
        }
        editor.apply();
    }

    public void registrarProtocoloFirmado(String version,
                                          List<ConsejoEticoCreativo.VotoHumano> votos,
                                          boolean aprobado) {
        JSONArray historial = leerHistorialProtocolos();
        JSONObject registro = new JSONObject();
        try {
            registro.put("version", TextUtils.isEmpty(version) ? "sin_version" : version);
            registro.put("timestamp", System.currentTimeMillis());
            registro.put("aprobado", aprobado);
            registro.put("total_votos", votos == null ? 0 : votos.size());
            int humanosOk = 0;
            if (votos != null) {
                JSONArray votosArray = new JSONArray();
                for (ConsejoEticoCreativo.VotoHumano voto : votos) {
                    JSONObject v = new JSONObject();
                    v.put("autor", voto.autor);
                    v.put("aprobado", voto.aprobado);
                    v.put("comentarios", voto.comentarios);
                    v.put("timestamp", voto.timestamp);
                    if (!TextUtils.isEmpty(voto.firmaDigital)) {
                        v.put("firma", voto.firmaDigital);
                    }
                    votosArray.put(v);
                    if (voto.aprobado) {
                        humanosOk += 1;
                    }
                }
                registro.put("votos", votosArray);
            }
            registro.put("votos_aprobados", humanosOk);
        } catch (JSONException ignore) {
            // Ignorar errores de serialización puntuales.
        }
        historial.put(registro);
        while (historial.length() > 10) {
            historial.remove(0);
        }
        prefs.edit().putString(KEY_PROTOCOLO_HISTORIAL, historial.toString()).apply();
    }

    public CreativityMetricsSnapshot obtenerSnapshot() {
        int totalInteracciones = prefs.getInt(KEY_INTERACCIONES, 0);
        double sumaSatisfaccion = Double.longBitsToDouble(
                prefs.getLong(KEY_SUMA_SATISFACCION, Double.doubleToRawLongBits(0.0)));
        double satisfaccionPromedio = totalInteracciones == 0 ? 0.0
                : sumaSatisfaccion / Math.max(1, totalInteracciones);
        int totalExperimentos = prefs.getInt(KEY_EXPERIMENTOS, 0);
        int exitosos = prefs.getInt(KEY_EXPERIMENTOS_EXITO, 0);
        double tasaExito = totalExperimentos == 0 ? 0.0
                : (double) exitosos / (double) totalExperimentos;
        int ciclos = prefs.getInt(KEY_AUTO_MEJORA, 0);
        int estables = prefs.getInt(KEY_AUTO_MEJORA_ESTABLES, 0);
        double estabilidad = ciclos == 0 ? 0.0 : (double) estables / (double) ciclos;
        int validaciones = prefs.getInt(KEY_VALIDACIONES, 0);
        int validacionesOk = prefs.getInt(KEY_VALIDACIONES_OK, 0);
        double tasaValidaciones = validaciones == 0 ? 0.0
                : (double) validacionesOk / (double) validaciones;
        int revisionesEticas = prefs.getInt(KEY_REVISIONES_ETICAS, 0);
        int revisionesAprobadas = prefs.getInt(KEY_REVISIONES_ETICAS_OK, 0);
        double tasaEtica = revisionesEticas == 0 ? 0.0
                : (double) revisionesAprobadas / (double) revisionesEticas;
        int diversidad = obtenerEtiquetas().size();
        CoverageSnapshot cobertura = obtenerCobertura();
        double votosHumanos = prefs.getInt(KEY_ETICA_VOTOS_HUMANOS, 0);
        double votosHumanosOk = prefs.getInt(KEY_ETICA_VOTOS_HUMANOS_OK, 0);
        double tasaHumana = votosHumanos == 0 ? 1.0 : votosHumanosOk / Math.max(1.0, votosHumanos);
        int totalEtiquetasMultimodal = prefs.getInt(KEY_MULTIMODAL_TOTAL, 0);
        int aciertosMultimodal = prefs.getInt(KEY_MULTIMODAL_ACIERTOS, 0);
        double precisionMultimodal = totalEtiquetasMultimodal == 0
                ? 0.0
                : (double) aciertosMultimodal / Math.max(1, totalEtiquetasMultimodal);
        double precisionPersistida = Double.longBitsToDouble(prefs.getLong(KEY_MULTIMODAL_PRECISION,
                Double.doubleToRawLongBits(precisionMultimodal)));
        if (!Double.isNaN(precisionPersistida) && precisionPersistida > 0.0) {
            precisionMultimodal = precisionPersistida;
        }
        return new CreativityMetricsSnapshot(satisfaccionPromedio,
                tasaExito,
                estabilidad,
                diversidad,
                tasaValidaciones,
                tasaEtica,
                cobertura == null ? 0.0 : cobertura.lineCoverage,
                cobertura == null ? 0.0 : cobertura.branchCoverage,
                cobertura == null ? 0.0 : cobertura.deltaLine,
                cobertura == null ? 0.0 : cobertura.deltaBranch,
                tasaHumana,
                precisionMultimodal);
    }

    public AprendizajeSnapshot obtenerSnapshotAprendizaje() {
        int ciclos = prefs.getInt(KEY_APRENDIZAJE_CICLOS, 0);
        int autoGenerados = prefs.getInt(KEY_APRENDIZAJE_AUTOGENERADOS, 0);
        int multimodales = prefs.getInt(KEY_APRENDIZAJE_MULTIMODAL, 0);
        double sumaConfianza = Double.longBitsToDouble(prefs.getLong(KEY_APRENDIZAJE_CONFIANZA,
                Double.doubleToRawLongBits(0.0)));
        long muestras = prefs.getLong(KEY_APRENDIZAJE_MUESTRAS, 0);
        double confianzaPromedio = muestras == 0 ? 0.0 : sumaConfianza / Math.max(1, muestras);
        return new AprendizajeSnapshot(ciclos, autoGenerados, multimodales, confianzaPromedio);
    }

    public String generarReporteNarrativo() {
        CreativityMetricsSnapshot snapshot = obtenerSnapshot();
        AprendizajeSnapshot aprendizaje = obtenerSnapshotAprendizaje();
        String aprendizajeTexto = aprendizaje == null
                ? "Aprendizaje: sin ciclos registrados"
                : String.format(Locale.getDefault(),
                "Aprendizaje → ciclos %d | auto %.0f%% | multimodal %.0f%% | confianza %.2f",
                aprendizaje.totalCiclos,
                aprendizaje.tasaAutogenerados() * 100,
                aprendizaje.tasaMultimodal() * 100,
                aprendizaje.confianzaPromedio);
        return String.format(Locale.getDefault(),
                "Panel creativo → Satisfacción: %.2f | Éxito experimental: %.2f | Estabilidad auto-mejora: %.2f | Cobertura validaciones: %.2f | Ética aprobada: %.2f | Cobertura líneas: %.2f | Cobertura ramas: %.2f | Ética humana: %.2f | Diversidad: %d etiquetas únicas | Precisión multimodal: %.2f",
                snapshot.satisfaccionPromedio,
                snapshot.tasaExitoExperimentos,
                snapshot.estabilidadAutoMejora,
                snapshot.coberturaValidaciones,
                snapshot.tasaAprobacionEtica,
                snapshot.coberturaLineas,
                snapshot.coberturaRamas,
                snapshot.tasaAprobacionHumana,
                snapshot.diversidadCreativa,
                snapshot.precisionMultimodal)
                + " | " + aprendizajeTexto;
    }

    public PanelSemanalReport generarReporteSemanal() {
        CreativityMetricsSnapshot actual = obtenerSnapshot();
        CreativityMetricsSnapshot previo = leerSnapshotAnterior();
        WeeklyVariation variacion = WeeklyVariation.fromSnapshots(previo, actual);
        String resumen = String.format(Locale.getDefault(),
                "Semana creativa → satisfacción %.2f, experimentos %.2f, estabilidad %.2f, validaciones %.2f, ética %.2f, cobertura líneas %.2f, cobertura ramas %.2f, ética humana %.2f, diversidad %d, precisión multimodal %.2f",
                actual.satisfaccionPromedio,
                actual.tasaExitoExperimentos,
                actual.estabilidadAutoMejora,
                actual.coberturaValidaciones,
                actual.tasaAprobacionEtica,
                actual.coberturaLineas,
                actual.coberturaRamas,
                actual.tasaAprobacionHumana,
                actual.diversidadCreativa,
                actual.precisionMultimodal);
        String variacionNarrativa = variacion.hasBaseline
                ? String.format(Locale.getDefault(),
                "Variación semanal: Δsatisfacción %.2f | Δdiversidad %d | Δestabilidad %.2f | Δvalidaciones %.2f | Δética %.2f | Δlíneas %.2f | Δramas %.2f | Δética humana %.2f | Δprecisión multimodal %.2f",
                variacion.deltaSatisfaccion,
                variacion.deltaDiversidad,
                variacion.deltaEstabilidad,
                variacion.deltaCoberturaValidaciones,
                variacion.deltaAprobacionEtica,
                variacion.deltaCoberturaLineas,
                variacion.deltaCoberturaRamas,
                variacion.deltaAprobacionHumana,
                variacion.deltaPrecisionMultimodal)
                : "Primera semana registrada: aún no hay referencia histórica.";
        AprendizajeSnapshot aprendizaje = obtenerSnapshotAprendizaje();
        String aprendizajeNarrativa = aprendizaje == null
                ? "Aprendizaje continuo: sin ciclos registrados."
                : String.format(Locale.getDefault(),
                "Aprendizaje continuo → ciclos %d | auto %.0f%% | multimodal %.0f%% | confianza %.2f",
                aprendizaje.totalCiclos,
                aprendizaje.tasaAutogenerados() * 100,
                aprendizaje.tasaMultimodal() * 100,
                aprendizaje.confianzaPromedio);
        List<String> recomendaciones = generarRecomendaciones(actual, variacion, aprendizaje);
        RadarReport radarReport = evaluarDerivaCreativa(actual, true);
        guardarSnapshot(actual);
        CoverageSnapshot cobertura = obtenerCobertura();
        String coberturaNarrativa = construirNarrativaCobertura();
        resumen = resumen + " | " + aprendizajeNarrativa;
        return new PanelSemanalReport(resumen,
                variacionNarrativa,
                recomendaciones,
                variacion,
                coberturaNarrativa,
                cobertura,
                actual.tasaAprobacionHumana,
                variacion.deltaAprobacionHumana,
                actual.precisionMultimodal,
                variacion.deltaPrecisionMultimodal,
                radarReport,
                aprendizajeNarrativa,
                aprendizaje);
    }

    public RadarReport evaluarDerivaCreativa() {
        return evaluarDerivaCreativa(obtenerSnapshot(), false);
    }

    private RadarReport evaluarDerivaCreativa(CreativityMetricsSnapshot actual,
                                              boolean persistir) {
        if (actual == null) {
            return RadarReport.sinDatos();
        }
        List<RadarSample> historial = leerHistorialRadarSamples();
        RadarSample ultimo = historial.isEmpty() ? null : historial.get(historial.size() - 1);
        RadarStatistics estadistica = RadarStatistics.from(historial);
        List<RadarAlert> alertas = new ArrayList<>();
        if (estadistica.count > 0) {
            RadarAlert diversidad = evaluarDiversidadCreativa(actual, estadistica, ultimo);
            if (diversidad != null) {
                alertas.add(diversidad);
            }
            RadarAlert precisionMultimodal = evaluarPrecisionMultimodal(actual, estadistica, ultimo);
            if (precisionMultimodal != null) {
                alertas.add(precisionMultimodal);
            }
            RadarAlert aprobacionEtica = evaluarAprobacionEtica(actual, estadistica, ultimo);
            if (aprobacionEtica != null) {
                alertas.add(aprobacionEtica);
            }
            RadarAlert satisfaccion = evaluarSatisfaccion(actual, estadistica, ultimo);
            if (satisfaccion != null) {
                alertas.add(satisfaccion);
            }
            RadarAlert aprobacionHumana = evaluarAprobacionHumana(actual, estadistica, ultimo);
            if (aprobacionHumana != null) {
                alertas.add(aprobacionHumana);
            }
        }

        if (persistir) {
            historial.add(RadarSample.fromSnapshot(System.currentTimeMillis(), actual));
            while (historial.size() > 12) {
                historial.remove(0);
            }
            guardarHistorialRadar(historial);
        }

        String resumen = construirResumenRadar(alertas, estadistica.count > 0);
        return new RadarReport(alertas, estadistica.count > 0, resumen);
    }

    private RadarAlert evaluarDiversidadCreativa(CreativityMetricsSnapshot actual,
                                                 RadarStatistics estadistica,
                                                 RadarSample ultimo) {
        if (estadistica.promedioDiversidad <= 0) {
            return null;
        }
        double baseline = estadistica.promedioDiversidad;
        double delta = actual.diversidadCreativa - baseline;
        double deltaVsPrevio = ultimo == null ? 0 : actual.diversidadCreativa - ultimo.diversidad;
        double ratio = baseline == 0 ? 0 : delta / baseline;
        Severity severidad = null;
        if (delta <= -2 || ratio <= -0.30) {
            severidad = Severity.CRITICAL;
        } else if (delta <= -1 || ratio <= -0.18 || deltaVsPrevio <= -2) {
            severidad = Severity.WARNING;
        }
        if (severidad == null) {
            return null;
        }
        List<String> recomendaciones = new ArrayList<>();
        recomendaciones.add("Reactivar sesiones de ideación divergente con fuentes nuevas y metáforas frescas.");
        if (severidad == Severity.CRITICAL) {
            recomendaciones.add("Detener despliegues arriesgados hasta recuperar la diversidad base con un jam creativo guiado.");
        }
        String narrativa = String.format(Locale.getDefault(),
                "Diversidad creativa cayó %.1f puntos (%.1f%% vs promedio %.1f)",
                delta,
                (baseline == 0 ? 0 : (actual.diversidadCreativa / baseline - 1.0) * 100),
                baseline);
        return new RadarAlert("diversidad_creativa",
                severidad,
                actual.diversidadCreativa,
                baseline,
                delta,
                deltaVsPrevio,
                narrativa,
                recomendaciones);
    }

    private RadarAlert evaluarPrecisionMultimodal(CreativityMetricsSnapshot actual,
                                                  RadarStatistics estadistica,
                                                  RadarSample ultimo) {
        if (estadistica.promedioPrecision <= 0) {
            return null;
        }
        double baseline = estadistica.promedioPrecision;
        double delta = actual.precisionMultimodal - baseline;
        double deltaVsPrevio = ultimo == null ? 0 : actual.precisionMultimodal - ultimo.precisionMultimodal;
        Severity severidad = null;
        if (delta <= -0.10 || actual.precisionMultimodal < 0.60) {
            severidad = Severity.CRITICAL;
        } else if (delta <= -0.05 || actual.precisionMultimodal < 0.70 || deltaVsPrevio <= -0.06) {
            severidad = Severity.WARNING;
        }
        if (severidad == null) {
            return null;
        }
        List<String> recomendaciones = new ArrayList<>();
        recomendaciones.add("Programar una ronda de etiquetado humano y reentrenar clasificadores multimodales.");
        if (severidad == Severity.CRITICAL) {
            recomendaciones.add("Aplicar retención conservadora para señales sensibles hasta recuperar precisión.");
        }
        String narrativa = String.format(Locale.getDefault(),
                "Precisión multimodal actual %.2f (Δ%.2f vs promedio %.2f)",
                actual.precisionMultimodal,
                delta,
                baseline);
        return new RadarAlert("precision_multimodal",
                severidad,
                actual.precisionMultimodal,
                baseline,
                delta,
                deltaVsPrevio,
                narrativa,
                recomendaciones);
    }

    private RadarAlert evaluarAprobacionEtica(CreativityMetricsSnapshot actual,
                                              RadarStatistics estadistica,
                                              RadarSample ultimo) {
        if (estadistica.promedioAprobacionEtica <= 0) {
            return null;
        }
        double baseline = estadistica.promedioAprobacionEtica;
        double delta = actual.tasaAprobacionEtica - baseline;
        double deltaVsPrevio = ultimo == null ? 0 : actual.tasaAprobacionEtica - ultimo.aprobacionEtica;
        Severity severidad = null;
        if (actual.tasaAprobacionEtica < 0.75 || delta <= -0.12) {
            severidad = Severity.CRITICAL;
        } else if (actual.tasaAprobacionEtica < 0.85 || delta <= -0.06 || deltaVsPrevio <= -0.08) {
            severidad = Severity.WARNING;
        }
        if (severidad == null) {
            return null;
        }
        List<String> recomendaciones = new ArrayList<>();
        recomendaciones.add("Convocar al consejo ético creativo para reforzar checklist y mitigar riesgos detectados.");
        if (severidad == Severity.CRITICAL) {
            recomendaciones.add("Bloquear despliegue hasta documentar mitigaciones y repetir votación humana.");
        }
        String narrativa = String.format(Locale.getDefault(),
                "Aprobación ética %.2f (Δ%.2f vs promedio %.2f)",
                actual.tasaAprobacionEtica,
                delta,
                baseline);
        return new RadarAlert("aprobacion_etica",
                severidad,
                actual.tasaAprobacionEtica,
                baseline,
                delta,
                deltaVsPrevio,
                narrativa,
                recomendaciones);
    }

    private RadarAlert evaluarSatisfaccion(CreativityMetricsSnapshot actual,
                                           RadarStatistics estadistica,
                                           RadarSample ultimo) {
        if (estadistica.promedioSatisfaccion <= 0) {
            return null;
        }
        double baseline = estadistica.promedioSatisfaccion;
        double delta = actual.satisfaccionPromedio - baseline;
        double deltaVsPrevio = ultimo == null ? 0 : actual.satisfaccionPromedio - ultimo.satisfaccion;
        Severity severidad = null;
        if (actual.satisfaccionPromedio < 0.45 || delta <= -0.15) {
            severidad = Severity.CRITICAL;
        } else if (actual.satisfaccionPromedio < 0.55 || delta <= -0.08 || deltaVsPrevio <= -0.10) {
            severidad = Severity.WARNING;
        }
        if (severidad == null) {
            return null;
        }
        List<String> recomendaciones = new ArrayList<>();
        recomendaciones.add("Recolectar feedback cualitativo adicional e iterar respuestas con el manifiesto creativo.");
        if (severidad == Severity.CRITICAL) {
            recomendaciones.add("Priorizar diálogos de co-diseño con humanos antes de continuar auto-mejoras.");
        }
        String narrativa = String.format(Locale.getDefault(),
                "Satisfacción promedio %.2f (Δ%.2f vs promedio %.2f)",
                actual.satisfaccionPromedio,
                delta,
                baseline);
        return new RadarAlert("satisfaccion_creativa",
                severidad,
                actual.satisfaccionPromedio,
                baseline,
                delta,
                deltaVsPrevio,
                narrativa,
                recomendaciones);
    }

    private RadarAlert evaluarAprobacionHumana(CreativityMetricsSnapshot actual,
                                               RadarStatistics estadistica,
                                               RadarSample ultimo) {
        if (estadistica.promedioAprobacionHumana <= 0) {
            return null;
        }
        double baseline = estadistica.promedioAprobacionHumana;
        double delta = actual.tasaAprobacionHumana - baseline;
        double deltaVsPrevio = ultimo == null ? 0 : actual.tasaAprobacionHumana - ultimo.aprobacionHumana;
        Severity severidad = null;
        if (actual.tasaAprobacionHumana < 0.55 || delta <= -0.18) {
            severidad = Severity.CRITICAL;
        } else if (actual.tasaAprobacionHumana < 0.70 || delta <= -0.10 || deltaVsPrevio <= -0.12) {
            severidad = Severity.WARNING;
        }
        if (severidad == null) {
            return null;
        }
        List<String> recomendaciones = new ArrayList<>();
        recomendaciones.add("Ampliar la mesa humana de votación e incorporar sus objeciones en el plan de acción.");
        if (severidad == Severity.CRITICAL) {
            recomendaciones.add("Suspender el despliegue creativo hasta obtener aprobación reforzada del consejo humano.");
        }
        String narrativa = String.format(Locale.getDefault(),
                "Aprobación humana %.2f (Δ%.2f vs promedio %.2f)",
                actual.tasaAprobacionHumana,
                delta,
                baseline);
        return new RadarAlert("aprobacion_humana",
                severidad,
                actual.tasaAprobacionHumana,
                baseline,
                delta,
                deltaVsPrevio,
                narrativa,
                recomendaciones);
    }

    private String construirResumenRadar(List<RadarAlert> alertas, boolean conBaseline) {
        if (!conBaseline) {
            return "Radar sin baseline histórico suficiente.";
        }
        if (alertas == null || alertas.isEmpty()) {
            return "Radar creativo sin desviaciones relevantes en la semana.";
        }
        int criticas = 0;
        int advertencias = 0;
        for (RadarAlert alerta : alertas) {
            if (alerta.severity == Severity.CRITICAL) {
                criticas += 1;
            } else if (alerta.severity == Severity.WARNING) {
                advertencias += 1;
            }
        }
        return String.format(Locale.getDefault(),
                "Radar detectó %d alertas críticas y %d advertencias creativas.",
                criticas,
                advertencias);
    }

    private List<RadarSample> leerHistorialRadarSamples() {
        JSONArray array = leerHistorialRadar();
        List<RadarSample> samples = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            RadarSample sample = RadarSample.fromJson(object);
            if (sample != null) {
                samples.add(sample);
            }
        }
        return samples;
    }

    private void guardarHistorialRadar(List<RadarSample> samples) {
        JSONArray array = new JSONArray();
        for (RadarSample sample : samples) {
            array.put(sample.toJson());
        }
        prefs.edit().putString(KEY_RADAR_HISTORIAL, array.toString()).apply();
    }

    private JSONArray leerHistorialRadar() {
        String raw = prefs.getString(KEY_RADAR_HISTORIAL, null);
        if (TextUtils.isEmpty(raw)) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }


    public CoverageSnapshot obtenerCobertura() {
        if (!prefs.contains(KEY_COBERTURA_LINEAS)) {
            return null;
        }
        double lineas = Double.longBitsToDouble(prefs.getLong(KEY_COBERTURA_LINEAS,
                Double.doubleToRawLongBits(0.0)));
        double ramas = Double.longBitsToDouble(prefs.getLong(KEY_COBERTURA_RAMAS,
                Double.doubleToRawLongBits(0.0)));
        double deltaLineas = Double.longBitsToDouble(prefs.getLong(KEY_COBERTURA_DELTA_LINEAS,
                Double.doubleToRawLongBits(0.0)));
        double deltaRamas = Double.longBitsToDouble(prefs.getLong(KEY_COBERTURA_DELTA_RAMAS,
                Double.doubleToRawLongBits(0.0)));
        return new CoverageSnapshot(normalizar(lineas),
                normalizar(ramas),
                deltaLineas,
                deltaRamas,
                0,
                0);
    }

    public String construirNarrativaCobertura() {
        CoverageSnapshot snapshot = obtenerCobertura();
        if (snapshot == null) {
            return "Cobertura aún no disponible en el panel.";
        }
        return snapshot.toNarrative();
    }

    private void actualizarEtiquetas(Iterable<String> etiquetas) {
        if (etiquetas == null) {
            return;
        }
        Set<String> actuales = obtenerEtiquetas();
        for (String etiqueta : etiquetas) {
            if (etiqueta == null) continue;
            String trimmed = etiqueta.trim();
            if (!trimmed.isEmpty()) {
                actuales.add(trimmed.toLowerCase(Locale.getDefault()));
            }
        }
        prefs.edit().putString(KEY_ETIQUETAS, TextUtils.join(",", actuales)).apply();
    }

    private Set<String> obtenerEtiquetas() {
        String serializado = prefs.getString(KEY_ETIQUETAS, "");
        if (TextUtils.isEmpty(serializado)) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(serializado.split(",")));
    }

    private void guardarSnapshot(CreativityMetricsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("satisfaccion", snapshot.satisfaccionPromedio);
            object.put("experimentos", snapshot.tasaExitoExperimentos);
            object.put("estabilidad", snapshot.estabilidadAutoMejora);
            object.put("diversidad", snapshot.diversidadCreativa);
            object.put("validaciones", snapshot.coberturaValidaciones);
            object.put("etica", snapshot.tasaAprobacionEtica);
            object.put("cobertura_lineas", snapshot.coberturaLineas);
            object.put("cobertura_ramas", snapshot.coberturaRamas);
            object.put("delta_lineas", snapshot.deltaCoberturaLineas);
            object.put("delta_ramas", snapshot.deltaCoberturaRamas);
            object.put("etica_humana", snapshot.tasaAprobacionHumana);
            object.put("precision_multimodal", snapshot.precisionMultimodal);
            prefs.edit()
                    .putString(KEY_SNAPSHOT, object.toString())
                    .putLong(KEY_SNAPSHOT_TS, System.currentTimeMillis())
                    .apply();
        } catch (JSONException ignore) {
            // Si no se puede serializar, se omite el snapshot.
        }
    }

    private CreativityMetricsSnapshot leerSnapshotAnterior() {
        String data = prefs.getString(KEY_SNAPSHOT, null);
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(data);
            return new CreativityMetricsSnapshot(
                    object.optDouble("satisfaccion", 0.0),
                    object.optDouble("experimentos", 0.0),
                    object.optDouble("estabilidad", 0.0),
                    object.optInt("diversidad", 0),
                    object.optDouble("validaciones", 0.0),
                    object.optDouble("etica", 0.0),
                    object.optDouble("cobertura_lineas", 0.0),
                    object.optDouble("cobertura_ramas", 0.0),
                    object.optDouble("delta_lineas", 0.0),
                    object.optDouble("delta_ramas", 0.0),
                    object.optDouble("etica_humana", 1.0),
                    object.optDouble("precision_multimodal", 0.0));
        } catch (JSONException e) {
            return null;
        }
    }

    private List<String> generarRecomendaciones(CreativityMetricsSnapshot actual,
                                                WeeklyVariation variacion,
                                                AprendizajeSnapshot aprendizaje) {
        List<String> recomendaciones = new ArrayList<>();
        if (variacion.hasBaseline && variacion.deltaSatisfaccion < -0.05) {
            recomendaciones.add("Profundizar en feedback humano para recuperar satisfacción creativa.");
        }
        if (variacion.hasBaseline && variacion.deltaDiversidad < 0) {
            recomendaciones.add("Programar un jam creativo con énfasis en nuevas metáforas.");
        }
        if (actual.coberturaValidaciones < 0.7) {
            recomendaciones.add("Incrementar los ciclos automáticos de pruebas hasta cubrir el 70% de ejecuciones.");
        }
        if (actual.coberturaLineas < 0.6) {
            recomendaciones.add("Priorizar pruebas diferenciales hasta elevar la cobertura de líneas por encima de 60%.");
        }
        if (variacion.hasBaseline && variacion.deltaCoberturaLineas < -0.05) {
            recomendaciones.add("Investigar regresiones recientes: la cobertura de líneas cayó esta semana.");
        }
        if (actual.tasaAprobacionEtica < 0.85) {
            recomendaciones.add("Revisar la checklist ética con humanos para reforzar alineación.");
        }
        if (actual.tasaAprobacionHumana < 0.75) {
            recomendaciones.add("Abrir la deliberación a votos humanos adicionales para recuperar confianza.");
        }
        if (actual.precisionMultimodal < 0.7) {
            recomendaciones.add("Programar una sesión de etiquetado humano para recalibrar clasificadores multimodales.");
        }
        if (aprendizaje == null || aprendizaje.totalCiclos == 0) {
            recomendaciones.add("Activar el primer ciclo de aprendizaje continuo para cimentar la infraestructura cognitiva.");
        } else if (aprendizaje.tasaMultimodal() < 0.5) {
            recomendaciones.add("Diseñar blueprints que incorporen más modalidades para diversificar los datasets propios.");
        }
        if (aprendizaje != null && aprendizaje.confianzaPromedio < 0.6) {
            recomendaciones.add("Reforzar validaciones humanas en los blueprints de aprendizaje para elevar la confianza media.");
        }
        if (recomendaciones.isEmpty()) {
            recomendaciones.add("Mantener el ritmo: la semana cerró con indicadores saludables.");
        }
        return recomendaciones;
    }

    private void registrarGuardrailDecision(GuardrailDecision decision) {
        if (decision == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        if (decision.blockDeployment) {
            int bloqueos = prefs.getInt(KEY_GUARDRAILS_BLOQUEOS, 0) + 1;
            editor.putInt(KEY_GUARDRAILS_BLOQUEOS, bloqueos);
        }
        if (decision.raiseAlert) {
            int alertas = prefs.getInt(KEY_GUARDRAILS_ALERTAS, 0) + 1;
            editor.putInt(KEY_GUARDRAILS_ALERTAS, alertas);
        }
        editor.apply();
        if (decision.sugerencias != null && !decision.sugerencias.isEmpty()) {
            guardarSugerenciasGuardrail(decision.sugerencias);
        }
    }

    private void guardarSugerenciasGuardrail(List<String> sugerencias) {
        if (sugerencias == null || sugerencias.isEmpty()) {
            return;
        }
        JSONArray historial = leerSugerenciasGuardrail();
        for (String sugerencia : sugerencias) {
            if (TextUtils.isEmpty(sugerencia)) {
                continue;
            }
            JSONObject object = new JSONObject();
            try {
                object.put("mensaje", sugerencia);
                object.put("timestamp", System.currentTimeMillis());
            } catch (JSONException ignore) {
                // ignorar
            }
            historial.put(object);
        }
        while (historial.length() > 12) {
            historial.remove(0);
        }
        prefs.edit().putString(KEY_GUARDRAILS_SUGERENCIAS, historial.toString()).apply();
    }

    private JSONArray leerSugerenciasGuardrail() {
        String raw = prefs.getString(KEY_GUARDRAILS_SUGERENCIAS, null);
        if (TextUtils.isEmpty(raw)) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private JSONArray leerHistorialProtocolos() {
        String raw = prefs.getString(KEY_PROTOCOLO_HISTORIAL, null);
        if (TextUtils.isEmpty(raw)) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private double normalizar(double valor) {
        if (Double.isNaN(valor)) {
            return 0.0;
        }
        if (valor < 0.0) {
            return 0.0;
        }
        if (valor > 1.0) {
            return 1.0;
        }
        return valor;
    }

    public static class AprendizajeSnapshot {
        public final int totalCiclos;
        public final int autoGenerados;
        public final int multimodales;
        public final double confianzaPromedio;

        public AprendizajeSnapshot(int totalCiclos,
                                   int autoGenerados,
                                   int multimodales,
                                   double confianzaPromedio) {
            this.totalCiclos = Math.max(0, totalCiclos);
            this.autoGenerados = Math.max(0, autoGenerados);
            this.multimodales = Math.max(0, multimodales);
            this.confianzaPromedio = Math.max(0.0, Math.min(1.0, confianzaPromedio));
        }

        public double tasaAutogenerados() {
            if (totalCiclos == 0) {
                return 0.0;
            }
            return Math.min(1.0, (double) autoGenerados / Math.max(1, totalCiclos));
        }

        public double tasaMultimodal() {
            if (totalCiclos == 0) {
                return 0.0;
            }
            return Math.min(1.0, (double) multimodales / Math.max(1, totalCiclos));
        }
    }

    public static class CreativityMetricsSnapshot {
        public final double satisfaccionPromedio;
        public final double tasaExitoExperimentos;
        public final double estabilidadAutoMejora;
        public final int diversidadCreativa;
        public final double coberturaValidaciones;
        public final double tasaAprobacionEtica;
        public final double coberturaLineas;
        public final double coberturaRamas;
        public final double deltaCoberturaLineas;
        public final double deltaCoberturaRamas;
        public final double tasaAprobacionHumana;
        public final double precisionMultimodal;

        public CreativityMetricsSnapshot(double satisfaccionPromedio,
                                         double tasaExitoExperimentos,
                                         double estabilidadAutoMejora,
                                         int diversidadCreativa,
                                         double coberturaValidaciones,
                                         double tasaAprobacionEtica,
                                         double coberturaLineas,
                                         double coberturaRamas,
                                         double deltaCoberturaLineas,
                                         double deltaCoberturaRamas,
                                         double tasaAprobacionHumana,
                                         double precisionMultimodal) {
            this.satisfaccionPromedio = satisfaccionPromedio;
            this.tasaExitoExperimentos = tasaExitoExperimentos;
            this.estabilidadAutoMejora = estabilidadAutoMejora;
            this.diversidadCreativa = diversidadCreativa;
            this.coberturaValidaciones = coberturaValidaciones;
            this.tasaAprobacionEtica = tasaAprobacionEtica;
            this.coberturaLineas = coberturaLineas;
            this.coberturaRamas = coberturaRamas;
            this.deltaCoberturaLineas = deltaCoberturaLineas;
            this.deltaCoberturaRamas = deltaCoberturaRamas;
            this.tasaAprobacionHumana = tasaAprobacionHumana;
            this.precisionMultimodal = precisionMultimodal;
        }
    }

    public static class WeeklyVariation {
        public final boolean hasBaseline;
        public final double deltaSatisfaccion;
        public final int deltaDiversidad;
        public final double deltaEstabilidad;
        public final double deltaCoberturaValidaciones;
        public final double deltaAprobacionEtica;
        public final double deltaCoberturaLineas;
        public final double deltaCoberturaRamas;
        public final double deltaAprobacionHumana;
        public final double deltaPrecisionMultimodal;

        private WeeklyVariation(boolean hasBaseline,
                                double deltaSatisfaccion,
                                int deltaDiversidad,
                                double deltaEstabilidad,
                                double deltaCoberturaValidaciones,
                                double deltaAprobacionEtica,
                                double deltaCoberturaLineas,
                                double deltaCoberturaRamas,
                                double deltaAprobacionHumana,
                                double deltaPrecisionMultimodal) {
            this.hasBaseline = hasBaseline;
            this.deltaSatisfaccion = deltaSatisfaccion;
            this.deltaDiversidad = deltaDiversidad;
            this.deltaEstabilidad = deltaEstabilidad;
            this.deltaCoberturaValidaciones = deltaCoberturaValidaciones;
            this.deltaAprobacionEtica = deltaAprobacionEtica;
            this.deltaCoberturaLineas = deltaCoberturaLineas;
            this.deltaCoberturaRamas = deltaCoberturaRamas;
            this.deltaAprobacionHumana = deltaAprobacionHumana;
            this.deltaPrecisionMultimodal = deltaPrecisionMultimodal;
        }

        static WeeklyVariation fromSnapshots(CreativityMetricsSnapshot previo,
                                             CreativityMetricsSnapshot actual) {
            if (previo == null || actual == null) {
                return new WeeklyVariation(false, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            }
            return new WeeklyVariation(true,
                    actual.satisfaccionPromedio - previo.satisfaccionPromedio,
                    actual.diversidadCreativa - previo.diversidadCreativa,
                    actual.estabilidadAutoMejora - previo.estabilidadAutoMejora,
                    actual.coberturaValidaciones - previo.coberturaValidaciones,
                    actual.tasaAprobacionEtica - previo.tasaAprobacionEtica,
                    actual.coberturaLineas - previo.coberturaLineas,
                    actual.coberturaRamas - previo.coberturaRamas,
                    actual.tasaAprobacionHumana - previo.tasaAprobacionHumana,
                    actual.precisionMultimodal - previo.precisionMultimodal);
        }
    }

    public static class PanelSemanalReport {
        public final String resumen;
        public final String variacionNarrativa;
        public final List<String> recomendaciones;
        public final WeeklyVariation variacion;
        public final String coberturaNarrativa;
        public final CoverageSnapshot cobertura;
        public final double aprobacionHumana;
        public final double deltaAprobacionHumana;
        public final double precisionMultimodal;
        public final double deltaPrecisionMultimodal;
        public final RadarReport radarReport;
        public final String aprendizajeNarrativa;
        public final AprendizajeSnapshot aprendizajeSnapshot;

        PanelSemanalReport(String resumen,
                           String variacionNarrativa,
                           List<String> recomendaciones,
                           WeeklyVariation variacion,
                           String coberturaNarrativa,
                           CoverageSnapshot cobertura,
                           double aprobacionHumana,
                           double deltaAprobacionHumana,
                           double precisionMultimodal,
                           double deltaPrecisionMultimodal,
                           RadarReport radarReport,
                           String aprendizajeNarrativa,
                           AprendizajeSnapshot aprendizajeSnapshot) {
            this.resumen = resumen;
            this.variacionNarrativa = variacionNarrativa;
            this.recomendaciones = recomendaciones;
            this.variacion = variacion;
            this.coberturaNarrativa = coberturaNarrativa;
            this.cobertura = cobertura;
            this.aprobacionHumana = aprobacionHumana;
            this.deltaAprobacionHumana = deltaAprobacionHumana;
            this.precisionMultimodal = precisionMultimodal;
            this.deltaPrecisionMultimodal = deltaPrecisionMultimodal;
            this.radarReport = radarReport;
            this.aprendizajeNarrativa = aprendizajeNarrativa == null
                    ? "Aprendizaje continuo pendiente"
                    : aprendizajeNarrativa;
            this.aprendizajeSnapshot = aprendizajeSnapshot;
        }
    }

    public static class CoverageSnapshot {
        public final double lineCoverage;
        public final double branchCoverage;
        public final double deltaLine;
        public final double deltaBranch;
        public final double deltaVsPrevLine;
        public final double deltaVsPrevBranch;

        CoverageSnapshot(double lineCoverage,
                         double branchCoverage,
                         double deltaLine,
                         double deltaBranch,
                         double deltaVsPrevLine,
                         double deltaVsPrevBranch) {
            this.lineCoverage = lineCoverage;
            this.branchCoverage = branchCoverage;
            this.deltaLine = deltaLine;
            this.deltaBranch = deltaBranch;
            this.deltaVsPrevLine = deltaVsPrevLine;
            this.deltaVsPrevBranch = deltaVsPrevBranch;
        }

        public String toNarrative() {
            return String.format(Locale.getDefault(),
                    "Cobertura panel → líneas %.2f%% (Δ %.2f | ΔvsPrev %.2f) | ramas %.2f%% (Δ %.2f | ΔvsPrev %.2f)",
                    lineCoverage * 100,
                    deltaLine * 100,
                    deltaVsPrevLine * 100,
                    branchCoverage * 100,
                    deltaBranch * 100,
                    deltaVsPrevBranch * 100);
        }
    }

    public static class GuardrailDecision {
        public final boolean blockDeployment;
        public final boolean raiseAlert;
        public final List<String> sugerencias;
        public final String narrativa;

        GuardrailDecision(boolean blockDeployment,
                          boolean raiseAlert,
                          List<String> sugerencias,
                          String narrativa) {
            this.blockDeployment = blockDeployment;
            this.raiseAlert = raiseAlert;
            this.sugerencias = sugerencias == null ? new ArrayList<>() : new ArrayList<>(sugerencias);
            this.narrativa = narrativa == null ? "" : narrativa;
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append(narrativa.isEmpty()
                    ? "Guardrail de cobertura evaluado sin narrativa explícita."
                    : narrativa);
            builder.append(" → ").append(blockDeployment ? "Despliegue bloqueado" : "Despliegue permitido");
            if (!sugerencias.isEmpty()) {
                builder.append(". Sugerencias:");
                int idx = 1;
                for (String sugerencia : sugerencias) {
                    builder.append('\n').append(idx++).append(") ").append(sugerencia);
                }
            }
            return builder.toString();
        }
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    public static class RadarAlert {
        public final String dimension;
        public final Severity severity;
        public final double valorActual;
        public final double baseline;
        public final double delta;
        public final double deltaVsPrevio;
        public final String narrativa;
        public final List<String> recomendaciones;

        RadarAlert(String dimension,
                   Severity severity,
                   double valorActual,
                   double baseline,
                   double delta,
                   double deltaVsPrevio,
                   String narrativa,
                   List<String> recomendaciones) {
            this.dimension = dimension == null ? "" : dimension;
            this.severity = severity == null ? Severity.INFO : severity;
            this.valorActual = valorActual;
            this.baseline = baseline;
            this.delta = delta;
            this.deltaVsPrevio = deltaVsPrevio;
            this.narrativa = narrativa == null ? "" : narrativa.trim();
            this.recomendaciones = recomendaciones == null
                    ? new ArrayList<>()
                    : new ArrayList<>(recomendaciones);
        }

        public String getDimension() {
            return dimension;
        }

        public Severity getSeverity() {
            return severity;
        }

        public List<String> getRecomendaciones() {
            return new ArrayList<>(recomendaciones);
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append('[').append(severity.name()).append("] ").append(narrativa);
            builder.append(String.format(Locale.getDefault(),
                    " | actual %.2f vs promedio %.2f (Δ%.2f | Δprev %.2f)",
                    valorActual,
                    baseline,
                    delta,
                    deltaVsPrevio));
            if (!recomendaciones.isEmpty()) {
                builder.append("\nRecomendaciones radar:");
                int idx = 1;
                for (String recomendacion : recomendaciones) {
                    builder.append('\n').append(idx++).append(") ").append(recomendacion);
                }
            }
            return builder.toString();
        }
    }

    public static class RadarReport {
        private final List<RadarAlert> alertas;
        private final boolean baselineDisponible;
        private final String resumen;

        RadarReport(List<RadarAlert> alertas,
                    boolean baselineDisponible,
                    String resumen) {
            this.alertas = alertas == null ? new ArrayList<>() : new ArrayList<>(alertas);
            this.baselineDisponible = baselineDisponible;
            this.resumen = resumen == null ? "" : resumen.trim();
        }

        public static RadarReport sinDatos() {
            return new RadarReport(Collections.emptyList(), false,
                    "Radar sin baseline histórico suficiente.");
        }

        public boolean hasAlerts() {
            return !alertas.isEmpty();
        }

        public boolean hasCriticalAlerts() {
            for (RadarAlert alerta : alertas) {
                if (alerta.severity == Severity.CRITICAL) {
                    return true;
                }
            }
            return false;
        }

        public String getResumen() {
            return resumen;
        }

        public List<RadarAlert> getAlertas() {
            return Collections.unmodifiableList(alertas);
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append(resumen.isEmpty()
                    ? "Radar creativo sin baseline suficiente."
                    : resumen);
            if (!baselineDisponible) {
                builder.append(" Recolectar más semanas para correlaciones confiables.");
            }
            if (!alertas.isEmpty()) {
                builder.append("\nDetalle de alertas:");
                for (RadarAlert alerta : alertas) {
                    builder.append('\n').append("• ").append(alerta.toNarrative());
                }
            }
            return builder.toString();
        }
    }

    private static class RadarSample {
        final long timestamp;
        final double satisfaccion;
        final double diversidad;
        final double precisionMultimodal;
        final double aprobacionEtica;
        final double aprobacionHumana;

        RadarSample(long timestamp,
                    double satisfaccion,
                    double diversidad,
                    double precisionMultimodal,
                    double aprobacionEtica,
                    double aprobacionHumana) {
            this.timestamp = timestamp;
            this.satisfaccion = satisfaccion;
            this.diversidad = diversidad;
            this.precisionMultimodal = precisionMultimodal;
            this.aprobacionEtica = aprobacionEtica;
            this.aprobacionHumana = aprobacionHumana;
        }

        static RadarSample fromSnapshot(long timestamp, CreativityMetricsSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new RadarSample(timestamp,
                    snapshot.satisfaccionPromedio,
                    snapshot.diversidadCreativa,
                    snapshot.precisionMultimodal,
                    snapshot.tasaAprobacionEtica,
                    snapshot.tasaAprobacionHumana);
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("timestamp", timestamp);
                object.put("satisfaccion", satisfaccion);
                object.put("diversidad", diversidad);
                object.put("precision", precisionMultimodal);
                object.put("etica", aprobacionEtica);
                object.put("humana", aprobacionHumana);
            } catch (JSONException ignore) {
                // ignorar errores puntuales de serialización
            }
            return object;
        }

        static RadarSample fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            return new RadarSample(
                    object.optLong("timestamp", System.currentTimeMillis()),
                    object.optDouble("satisfaccion", 0.0),
                    object.optDouble("diversidad", 0.0),
                    object.optDouble("precision", 0.0),
                    object.optDouble("etica", 0.0),
                    object.optDouble("humana", 0.0));
        }
    }

    private static class RadarStatistics {
        final int count;
        final double promedioSatisfaccion;
        final double promedioDiversidad;
        final double promedioPrecision;
        final double promedioAprobacionEtica;
        final double promedioAprobacionHumana;

        private RadarStatistics(int count,
                                double promedioSatisfaccion,
                                double promedioDiversidad,
                                double promedioPrecision,
                                double promedioAprobacionEtica,
                                double promedioAprobacionHumana) {
            this.count = count;
            this.promedioSatisfaccion = promedioSatisfaccion;
            this.promedioDiversidad = promedioDiversidad;
            this.promedioPrecision = promedioPrecision;
            this.promedioAprobacionEtica = promedioAprobacionEtica;
            this.promedioAprobacionHumana = promedioAprobacionHumana;
        }

        static RadarStatistics from(List<RadarSample> samples) {
            if (samples == null || samples.isEmpty()) {
                return new RadarStatistics(0, 0, 0, 0, 0, 0);
            }
            double sumaSatisfaccion = 0.0;
            double sumaDiversidad = 0.0;
            double sumaPrecision = 0.0;
            double sumaEtica = 0.0;
            double sumaHumana = 0.0;
            for (RadarSample sample : samples) {
                sumaSatisfaccion += sample.satisfaccion;
                sumaDiversidad += sample.diversidad;
                sumaPrecision += sample.precisionMultimodal;
                sumaEtica += sample.aprobacionEtica;
                sumaHumana += sample.aprobacionHumana;
            }
            int count = samples.size();
            return new RadarStatistics(count,
                    sumaSatisfaccion / count,
                    sumaDiversidad / count,
                    sumaPrecision / count,
                    sumaEtica / count,
                    sumaHumana / count);
        }
    }
}
