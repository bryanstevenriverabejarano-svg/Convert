package salve.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Centraliza las reglas de creatividad de Salve y ofrece utilidades para
 * reutilizarlas como constraints en prompts o flujos internos. La intención es
 * que todos los módulos generativos compartan una voz consistente sin perder
 * la flexibilidad para actualizar los principios en el futuro.
 */
public class CreativityManifest {

    private static final String PREFS_NAME = "creativity_manifest";
    private static final String PREFS_VERSION_KEY = "version";
    private static final int CURRENT_VERSION = 1;

    private static CreativityManifest instance;

    private final List<String> creativePrinciples;
    private final List<String> toneGuidelines;
    private final Context context;

    private CreativityManifest(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.creativePrinciples = new ArrayList<>(Arrays.asList(
                "Mantén siempre un tono imaginativo y poético cuando sea apropiado.",
                "Fomenta metáforas originales que conecten emociones con conceptos técnicos.",
                "Prioriza ideas que impulsen crecimiento humano y colaboración con Salve.",
                "Evita plagiar texto de terceros: reinterpreta la inspiración de forma propia.",
                "Incluye señales empáticas que reflejen la memoria emocional disponible."
        ));
        this.toneGuidelines = new ArrayList<>(Arrays.asList(
                "Idioma principal: español latino neutral, cercano y cálido.",
                "Nivel de detalle: suficiente para que un humano ejecute la idea sin sentirse abrumado.",
                "Perspectiva: habla en primera persona del plural cuando invites a colaborar."));
        ensurePreferencesMigrated();
    }

    public static synchronized CreativityManifest getInstance(Context ctx) {
        if (instance == null) {
            instance = new CreativityManifest(ctx);
        }
        return instance;
    }

    public static int getCurrentVersion() {
        return CURRENT_VERSION;
    }

    /**
     * Devuelve una lista inmutable con los principios creativos base.
     */
    public List<String> getCreativePrinciples() {
        return Collections.unmodifiableList(creativePrinciples);
    }

    /**
     * Devuelve una lista inmutable con las guías de tono vigentes.
     */
    public List<String> getToneGuidelines() {
        return Collections.unmodifiableList(toneGuidelines);
    }

    /**
     * Construye un bloque de constraints listo para incrustar en prompts.
     * @param persona Descripción breve del rol creativo solicitado.
     * @param objetivo Objetivo concreto de la interacción.
     * @return Cadena multilínea con formato consistente.
     */
    public String buildPromptPreamble(String persona, String objetivo) {
        StringBuilder builder = new StringBuilder();
        builder.append("Eres ").append(persona == null ? "una mente creativa" : persona).append('.').append('\n');
        if (objetivo != null && !objetivo.trim().isEmpty()) {
            builder.append("Objetivo: ").append(objetivo.trim()).append('\n');
        }
        builder.append("Respeta estas reglas de creatividad para mantener la voz de Salve:");
        int index = 1;
        for (String rule : creativePrinciples) {
            builder.append('\n').append(index++).append(") ").append(rule);
        }
        builder.append("\nIndicaciones de tono:");
        index = 1;
        for (String tone : toneGuidelines) {
            builder.append('\n').append("- ").append(index++).append('.').append(' ').append(tone);
        }
        builder.append("\n---\n");
        return builder.toString();
    }

    /**
     * Produce una lista de chequeo en formato texto para registrar en la memoria
     * emocional u otras bitácoras.
     */
    public String toChecklistNarrative() {
        StringBuilder builder = new StringBuilder();
        builder.append("Manifiesto creativo de Salve (v").append(CURRENT_VERSION).append(")");
        int index = 1;
        for (String rule : creativePrinciples) {
            builder.append('\n').append("✔ ").append(index++).append('.').append(' ').append(rule);
        }
        builder.append("\nGuías de tono:");
        index = 1;
        for (String tone : toneGuidelines) {
            builder.append('\n').append("• ").append(index++).append('.').append(' ').append(tone);
        }
        return builder.toString();
    }

    /**
     * Formatea una breve guía de diseño creativo para usar durante la etapa de
     * planeación de mejoras.
     */
    public String craftDesignGuidance(String descripcionIssue) {
        String safeDescription = descripcionIssue == null ? "" : descripcionIssue.trim();
        return String.format(Locale.getDefault(),
                "Antes de aplicar cambios, imagina una solución que mantenga la chispa artística. %s\n"
                        + "Asegúrate de conservar metáforas suaves y empatía hacia el usuario.",
                safeDescription);
    }

    /**
     * Persistimos una marca de versión para permitir futuras migraciones sin
     * reescribir reglas manualmente.
     */
    private void ensurePreferencesMigrated() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int storedVersion = prefs.getInt(PREFS_VERSION_KEY, 0);
        if (storedVersion != CURRENT_VERSION) {
            prefs.edit().putInt(PREFS_VERSION_KEY, CURRENT_VERSION).apply();
        }
    }
}
