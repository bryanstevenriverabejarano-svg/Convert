package salve.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Recuerdo.java
 * -------------
 * Representa un recuerdo almacenado en formato binario,
 * con metadatos: fecha, emoción, intensidad, ponderación,
 * etiquetas y conexiones a otros recuerdos.
 */
public class Recuerdo {

    // ====================== Campos principales ======================

    /** Fecha y hora de creación (formato yyyy-MM-dd HH:mm:ss) */
    private final String fecha;

    /** Emoción principal detectada (ej: "tristeza", "alegría") */
    private final String emocionPrincipal;

    /** Intensidad de la emoción, de 1 a 10 */
    private final int intensidad;

    /** Ponderación general del recuerdo, de 1 a 10, para priorización */
    private final int weight;

    /** Etiquetas clasificatorias (ej: "auto", "emocional", etc.) */
    private final List<String> etiquetas;

    /** Cadena binaria que codifica la frase original */
    private final String binarioCodificado;

    /** Conexiones a otros recuerdos, guardadas como sus binarios */
    private final List<String> conexiones;

    /** Visibilidad: false = interno, true = puede mostrarse */
    private boolean visible;

    // =================================================================

    /**
     * Constructor principal.
     *
     * @param fraseOriginal Texto original a codificar
     * @param emocion       Emoción asociada (p.ej. "tristeza")
     * @param intensidad     Intensidad de la emoción (1–10)
     * @param weight         Ponderación de importancia (1–10)
     * @param etiquetas      Lista de etiquetas clasificatorias
     * @param codificador    Instancia de CodificadorBinario
     */
    public Recuerdo(
            String fraseOriginal,
            String emocion,
            int intensidad,
            int weight,
            List<String> etiquetas,
            CodificadorBinario codificador
    ) {
        this.fecha = obtenerFechaActual();
        this.emocionPrincipal = emocion;
        this.intensidad = intensidad;
        this.weight = weight;

        // Lista inmutable de etiquetas
        if (etiquetas != null) {
            this.etiquetas = Collections.unmodifiableList(new ArrayList<>(etiquetas));
        } else {
            this.etiquetas = Collections.emptyList();
        }

        // Codifica la frase original a binario
        this.binarioCodificado = codificador.codificar(fraseOriginal);

        // Inicializa conexiones vacías
        this.conexiones = new ArrayList<>();

        // Por defecto, oculto
        this.visible = false;
    }

    // ===================== Métodos auxiliares =======================

    /**
     * Formatea la fecha/hora actual.
     */
    private String obtenerFechaActual() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Conecta este recuerdo con otro, guardando
     * su binario si no existe ya.
     */
    public void conectarCon(Recuerdo otroRecuerdo) {
        String otroBin = otroRecuerdo.getBinarioCodificado();
        if (!conexiones.contains(otroBin)) {
            conexiones.add(otroBin);
        }
    }

    /**
     * Decodifica y devuelve el texto original.
     */
    public String getTexto(CodificadorBinario codificador) {
        return codificador.decodificar(binarioCodificado);
    }

    /**
     * Genera un resumen legible del recuerdo:
     * fecha, ponderación, texto, emoción e intensidad.
     */
    public String resumenHumano(CodificadorBinario codificador) {
        String texto = getTexto(codificador);
        return String.format(
                Locale.getDefault(),
                "Recuerdo [%s] (w=%d) → %s | Emoción: %s (int=%d)",
                fecha, weight, texto, emocionPrincipal, intensidad
        );
    }

    // ========================== Getters =============================

    public String getFecha() {
        return fecha;
    }

    public String getEmocionPrincipal() {
        return emocionPrincipal;
    }

    public int getIntensidad() {
        return intensidad;
    }

    public int getWeight() {
        return weight;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public String getBinarioCodificado() {
        return binarioCodificado;
    }

    public List<String> getConexiones() {
        return Collections.unmodifiableList(conexiones);
    }

    public boolean esVisible() {
        return visible;
    }

    public String getEtiquetaPrincipal() {
        return etiquetas.isEmpty() ? "sin etiqueta" : etiquetas.get(0);
    }

    // ========================== Setter ==============================

    /**
     * Define la visibilidad del recuerdo (interno vs público).
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}