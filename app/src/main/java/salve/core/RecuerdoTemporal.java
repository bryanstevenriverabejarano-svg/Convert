package salve.core;

import java.util.ArrayList;
import java.util.List;

public class RecuerdoTemporal {

    private String fraseOriginal;
    private String emocion;
    private int intensidad;
    private List<String> etiquetas;
    private long timestamp;

    // Campo para contar cuántas veces se menciona/repite este recuerdo
    private int frecuencia;

    public RecuerdoTemporal(String frase, String emocion, int intensidad, List<String> etiquetas) {
        this.fraseOriginal = frase;
        this.emocion = emocion;
        this.intensidad = intensidad;
        this.etiquetas = (etiquetas != null) ? etiquetas : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();

        // La primera vez que se crea, lo consideramos frecuencia = 1
        this.frecuencia = 1;
    }

    // GETTERS
    public String getFraseOriginal() {
        return fraseOriginal;
    }

    public String getEmocion() {
        return emocion;
    }

    public int getIntensidad() {
        return intensidad;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // FRECUENCIA
    public int getFrecuencia() {
        return frecuencia;
    }

    public void incrementarFrecuencia() {
        this.frecuencia++;
    }
}