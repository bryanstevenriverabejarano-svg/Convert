package salve.core;

/**
 * ObjetoCreativo representa una forma visual simple que Salve puede
 * "forjar" en respuesta a ciertas interacciones. Esta clase
 * describe la forma (círculo, cuadrado, triángulo), el color y el tamaño
 * (en dp) del objeto.
 */
public class ObjetoCreativo {
    public enum Forma {
        CIRCULO,
        CUADRADO,
        TRIANGULO
    }

    private final Forma forma;
    private final int colorArgb;
    private final float tamanoDp;

    public ObjetoCreativo(Forma forma, int colorArgb, float tamanoDp) {
        this.forma = forma;
        this.colorArgb = colorArgb;
        this.tamanoDp = tamanoDp;
    }

    public Forma getForma() {
        return forma;
    }

    public int getColorArgb() {
        return colorArgb;
    }

    public float getTamanoDp() {
        return tamanoDp;
    }
}
