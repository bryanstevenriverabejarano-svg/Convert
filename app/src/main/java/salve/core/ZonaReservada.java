package salve.core;

public class ZonaReservada {
    private float intensidad01;
    private float estabilidad01;

    public ZonaReservada() {
        intensidad01 = 0f;
        estabilidad01 = 0.5f;
    }

    public void registrarIntensidad(int intensidad) {
        intensidad01 = clamp01(intensidad / 10f);
        estabilidad01 = clamp01(1f - (intensidad01 * 0.5f));
    }

    public float getIntensidad() {
        return intensidad01;
    }

    public float getEstabilidad() {
        return estabilidad01;
    }

    private float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }
}
