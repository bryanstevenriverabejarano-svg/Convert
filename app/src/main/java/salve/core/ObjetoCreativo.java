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
        TRIANGULO,
        GLIFO
    }

    private final Forma forma;
    private final int colorArgb;
    private final float tamanoDp;
    private final long seed;
    private final String style;

    public ObjetoCreativo(Forma forma, int colorArgb, float tamanoDp) {
        this(forma, colorArgb, tamanoDp, 0L, null);
    }

    public ObjetoCreativo(Forma forma, int colorArgb, float tamanoDp, long seed, String style) {
        this.forma = forma;
        this.colorArgb = colorArgb;
        this.tamanoDp = tamanoDp;
        this.seed = seed;
        this.style = style;
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

    public long getSeed() {
        return seed;
    }

    public String getStyle() {
        return style;
    }
}
