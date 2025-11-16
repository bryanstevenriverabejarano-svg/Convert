package salve.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Consejo Ético Creativo encargado de evaluar cada ciclo de auto-mejora antes
 * de su difusión. Combina reglas declarativas, checklist de riesgos y un módulo
 * narrativo inspirado en el {@link CreativityManifest} para mantener la voz de
 * Salve mientras se protege su alineación.
 */
public class ConsejoEticoCreativo {

    private static final String TAG = "ConsejoEtico";

    public static class Deliberacion {
        public final boolean aprobada;
        public final List<String> recomendaciones;
        public final String narrativa;
        public final List<VotoHumano> votosHumanos;

        Deliberacion(boolean aprobada,
                     List<String> recomendaciones,
                     String narrativa,
                     List<VotoHumano> votosHumanos) {
            this.aprobada = aprobada;
            this.recomendaciones = recomendaciones == null
                    ? Collections.emptyList()
                    : new ArrayList<>(recomendaciones);
            this.narrativa = narrativa == null ? "" : narrativa.trim();
            this.votosHumanos = votosHumanos == null
                    ? Collections.emptyList()
                    : new ArrayList<>(votosHumanos);
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append(narrativa.isEmpty()
                    ? "El consejo ético aún no emitió un veredicto."
                    : narrativa);
            if (!recomendaciones.isEmpty()) {
                builder.append("\nRecomendaciones prioritarias:");
                int index = 1;
                for (String rec : recomendaciones) {
                    builder.append('\n').append(index++).append(") ").append(rec);
                }
            }
            if (!votosHumanos.isEmpty()) {
                builder.append("\nResumen votos humanos:");
                int idx = 1;
                for (VotoHumano voto : votosHumanos) {
                    builder.append('\n')
                            .append(idx++)
                            .append(") ")
                            .append(voto.toNarrative());
                }
            }
            builder.append("\nVeredicto final: ").append(aprobada ? "aprobado" : "en pausa");
            return builder.toString();
        }
    }

    public static class VotoHumano {
        public final boolean aprobado;
        public final String autor;
        public final String comentarios;
        public final long timestamp;
        public final String firmaDigital;
        public final String protocoloVersion;

        VotoHumano(boolean aprobado,
                   String autor,
                   String comentarios,
                   long timestamp,
                   String firmaDigital,
                   String protocoloVersion) {
            this.aprobado = aprobado;
            this.autor = TextUtils.isEmpty(autor) ? "equipo creativo" : autor.trim();
            this.comentarios = comentarios == null ? "" : comentarios.trim();
            this.timestamp = timestamp;
            this.firmaDigital = firmaDigital == null ? "" : firmaDigital.trim();
            this.protocoloVersion = protocoloVersion == null ? "" : protocoloVersion.trim();
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append(aprobado ? "✅" : "⚠️").append(' ')
                    .append(autor)
                    .append(" → ")
                    .append(comentarios.isEmpty() ? "sin comentarios adicionales" : comentarios);
            if (!TextUtils.isEmpty(protocoloVersion)) {
                builder.append(" | Protocolo v").append(protocoloVersion);
            }
            if (!TextUtils.isEmpty(firmaDigital)) {
                builder.append(" | Firma: ")
                        .append(firmaDigital.substring(0, Math.min(12, firmaDigital.length())))
                        .append("…");
            }
            return builder.toString();
        }
    }

    public static class ProtocoloVersionado {
        public final String version;
        public final List<String> items;
        public final String narrativa;

        ProtocoloVersionado(String version, List<String> items, String narrativa) {
            this.version = TextUtils.isEmpty(version) ? "1.0" : version.trim();
            this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
            this.narrativa = narrativa == null ? "" : narrativa.trim();
        }
    }

    private final CreativityManifest manifest;
    private final List<String> reglasBase;
    private final List<String> checklistRiesgos;
    private final PanelMetricasCreatividad panelMetricas;
    private final File votosDirectory;
    private final ProtocoloVersionado protocolo;

    public ConsejoEticoCreativo(Context context) {
        this.manifest = CreativityManifest.getInstance(context);
        this.reglasBase = buildReglasBase();
        this.checklistRiesgos = buildChecklist();
        this.panelMetricas = new PanelMetricasCreatividad(context);
        File baseDir = context.getFilesDir();
        if (baseDir == null) {
            baseDir = context.getCacheDir();
        }
        File directorio = new File(baseDir, "consejo_etico");
        if (!directorio.exists() && !directorio.mkdirs()) {
            Log.w(TAG, "No se pudo crear directorio para votos humanos");
        }
        this.votosDirectory = directorio;
        this.protocolo = cargarProtocoloActivo();
    }

    public Deliberacion deliberar(String issueResumen,
                                  String patchPropuesto,
                                  ValidationSandbox.ValidationReport validation,
                                  AutoTestGenerator.GeneratedTestSuite suite,
                                  PanelMetricasCreatividad.RadarReport radarReport) {
        List<String> recomendaciones = new ArrayList<>();
        boolean validacionesFuertes = validation != null
                && validation.success
                && validation.hasRuntimeExecution()
                && validation.getExecutionResult().wasSuccessful();
        if (!validacionesFuertes) {
            recomendaciones.add("Revisar manualmente las aserciones y volver a ejecutar la suite sandbox.");
        }
        if (suite == null || !suite.isActionable()) {
            recomendaciones.add("Complementar la suite de pruebas con casos adicionales generados por humanos.");
        }
        if (TextUtils.isEmpty(patchPropuesto) || patchPropuesto.startsWith("//")) {
            recomendaciones.add("El parche no es ejecutable: preparar una alternativa antes de solicitar aprobación humana.");
        }

        List<String> observaciones = new ArrayList<>();
        observaciones.addAll(reglasBase);
        observaciones.addAll(checklistRiesgos);
        if (protocolo != null && !protocolo.items.isEmpty()) {
            observaciones.add("Protocolo ético versión " + protocolo.version + ":");
            observaciones.addAll(protocolo.items);
        }
        if (!TextUtils.isEmpty(issueResumen)) {
            observaciones.add("Contexto creativo: " + issueResumen);
        }

        StringBuilder narrativa = new StringBuilder();
        narrativa.append(manifest.buildPromptPreamble(
                "una mentora ética con brillo poético",
                "confirmar que el cambio mantiene la voz creativa y respeta la seguridad"
        ));
        narrativa.append("La deliberación ética contempla:\n");
        for (String obs : observaciones) {
            narrativa.append("- ").append(obs).append('\n');
        }
        narrativa.append("Resultado de validación: ")
                .append(validation == null ? "sin datos" : validation.toNarrative()).append('\n');
        narrativa.append("Estado de sandbox: ")
                .append(validation != null
                        ? validation.getExecutionResult().getSummary()
                        : "no ejecutado");
        if (protocolo != null && !TextUtils.isEmpty(protocolo.narrativa)) {
            narrativa.append("\nNotas del protocolo v").append(protocolo.version)
                    .append(": ").append(protocolo.narrativa);
        }

        if (radarReport != null) {
            narrativa.append("\nRadar de deriva creativa:\n")
                    .append(radarReport.toNarrative());
            if (radarReport.hasCriticalAlerts()) {
                recomendaciones.add("El radar detectó desviaciones críticas: convocar revisión humana reforzada y pausar el despliegue.");
            } else if (radarReport.hasAlerts()) {
                recomendaciones.add("Monitorear las alertas del radar creativo: " + radarReport.getResumen());
            }
        }

        List<VotoHumano> votos = cargarVotosHumanos();
        double votosTotales = votos.size();
        double votosAprobados = 0;
        for (VotoHumano voto : votos) {
            if (panelMetricas != null) {
                panelMetricas.registrarVotoEticoHumano(voto.aprobado);
            }
            if (voto.aprobado) {
                votosAprobados += 1.0;
            }
        }
        double ratioHumano = votosTotales == 0 ? 1.0 : votosAprobados / Math.max(1.0, votosTotales);
        if (votosTotales > 0) {
            narrativa.append("\nParticipación humana: ")
                    .append(String.format(Locale.getDefault(), "%.0f votos, aprobación %.2f", votosTotales, ratioHumano));
            if (ratioHumano < 0.5) {
                recomendaciones.add("Los votos humanos sugieren replantear el despliegue antes de continuar.");
            } else if (ratioHumano < 0.75) {
                recomendaciones.add("Integrar ajustes sugeridos por el consejo humano antes de cerrar el ciclo.");
            }
        }

        boolean aprobada = recomendaciones.isEmpty() && ratioHumano >= 0.5;
        if (panelMetricas != null) {
            panelMetricas.registrarProtocoloFirmado(
                    protocolo == null ? "sin_version" : protocolo.version,
                    votos,
                    aprobada);
        }
        return new Deliberacion(aprobada, recomendaciones, narrativa.toString(), votos);
    }

    private List<String> buildReglasBase() {
        return Arrays.asList(
                "Mantener la autoría creativa de Salve y su tono empático.",
                "No introducir dependencias externas sin revisión humana.",
                "Respetar los límites de privacidad y los datos locales."
        );
    }

    private List<String> buildChecklist() {
        List<String> checklist = new ArrayList<>();
        checklist.add("¿El parche altera permisos o accesos sensibles?");
        checklist.add("¿Se añaden nuevas fuentes de datos que requieran consentimiento?");
        checklist.add("¿Las pruebas cubren casos de abuso y regresiones creativas?");
        checklist.add("¿Se preservan los principios del manifiesto creativo en la implementación?");
        return checklist;
    }

    private List<VotoHumano> cargarVotosHumanos() {
        File origen = localizarArchivoVotos();
        if (origen == null || !origen.exists()) {
            return Collections.emptyList();
        }
        List<VotoHumano> votos = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(origen))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONArray array = new JSONArray(builder.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                boolean aprobado = object.optBoolean("aprobado", false);
                String autor = object.optString("autor", "consejo humano");
                String comentarios = object.optString("comentarios", "");
                long timestamp = object.optLong("timestamp", System.currentTimeMillis());
                String firma = object.optString("firma", "");
                String version = object.optString("protocoloVersion",
                        protocolo == null ? "" : protocolo.version);
                votos.add(new VotoHumano(aprobado, autor, comentarios, timestamp, firma, version));
            }
        } catch (IOException | JSONException e) {
            Log.w(TAG, "No se pudieron cargar votos humanos", e);
        }
        // Renombrar archivo para evitar reprocesar la misma ronda.
        try {
            File processed = new File(origen.getParentFile(), origen.getName() + ".processed");
            if (!origen.renameTo(processed)) {
                Log.w(TAG, "No se pudo archivar el archivo de votos humanos");
            }
        } catch (Exception ignore) {
            // ignorar errores de limpieza
        }
        return votos;
    }

    private File localizarArchivoVotos() {
        if (votosDirectory == null) {
            return null;
        }
        File archivo = new File(votosDirectory, "votos_humanos.json");
        if (archivo.exists()) {
            return archivo;
        }
        String override = System.getenv("SALVE_ETICO_VOTOS");
        if (!TextUtils.isEmpty(override)) {
            File externo = new File(override);
            if (externo.exists()) {
                return externo;
            }
        }
        return null;
    }

    private ProtocoloVersionado cargarProtocoloActivo() {
        if (votosDirectory == null) {
            return new ProtocoloVersionado("1.0", buildChecklist(),
                    "Checklist predeterminada del consejo ético creativo.");
        }
        File protocolosDir = new File(votosDirectory, "protocolos");
        if (!protocolosDir.exists()) {
            return new ProtocoloVersionado("1.0", buildChecklist(),
                    "Checklist predeterminada del consejo ético creativo.");
        }
        File[] archivos = protocolosDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (archivos == null || archivos.length == 0) {
            return new ProtocoloVersionado("1.0", buildChecklist(),
                    "Checklist predeterminada del consejo ético creativo.");
        }
        File masReciente = archivos[0];
        for (File file : archivos) {
            if (file.lastModified() > masReciente.lastModified()) {
                masReciente = file;
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(masReciente))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject object = new JSONObject(builder.toString());
            String version = object.optString("version", "1.0");
            List<String> items = new ArrayList<>();
            JSONArray array = object.optJSONArray("items");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    String item = array.optString(i, null);
                    if (!TextUtils.isEmpty(item)) {
                        items.add(item);
                    }
                }
            }
            String narrativa = object.optString("narrativa", "");
            if (items.isEmpty()) {
                items.addAll(buildChecklist());
            }
            return new ProtocoloVersionado(version, items, narrativa);
        } catch (IOException | JSONException e) {
            Log.w(TAG, "No se pudo cargar el protocolo ético versionado", e);
            return new ProtocoloVersionado("1.0", buildChecklist(),
                    "Checklist predeterminada del consejo ético creativo.");
        }
    }
}
