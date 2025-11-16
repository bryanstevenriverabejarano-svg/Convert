package salve.core;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

public class ADNVisual {

    private List<String> codigosBinarios;
    private static final int MAX_HISTORIAL = 50;

    public ADNVisual() {
        this.codigosBinarios = new ArrayList<>();
    }

    // Codifica puntos clave de un rostro en un string binario
    public String codificarRostro(List<PointF> puntosFaciales) {
        StringBuilder binario = new StringBuilder();
        for (PointF punto : puntosFaciales) {
            int x = Math.round(punto.x);
            int y = Math.round(punto.y);
            binario.append(Integer.toBinaryString(x)).append(Integer.toBinaryString(y));
        }
        return binario.toString();
    }

    // Guarda una nueva versión de ADN visual
    public void actualizarADN(String nuevoCodigo) {
        if (!codigosBinarios.contains(nuevoCodigo)) {
            if (codigosBinarios.size() >= MAX_HISTORIAL) {
                codigosBinarios.remove(0); // Elimina el más antiguo
            }
            codigosBinarios.add(nuevoCodigo);
        }
    }

    // Evalúa si el nuevo rostro es suficientemente similar al ADN visual actual
    public boolean esCoincidenciaAceptable(String codigoNuevo, int toleranciaBits) {
        for (String previo : codigosBinarios) {
            int diferencia = compararBinarios(previo, codigoNuevo);
            if (diferencia <= toleranciaBits) {
                return true;
            }
        }
        return false;
    }

    // Compara dos cadenas binarias por diferencia de bits
    private int compararBinarios(String bin1, String bin2) {
        int diferencias = 0;
        int longitud = Math.min(bin1.length(), bin2.length());
        for (int i = 0; i < longitud; i++) {
            if (bin1.charAt(i) != bin2.charAt(i)) {
                diferencias++;
            }
        }
        diferencias += Math.abs(bin1.length() - bin2.length());
        return diferencias;
    }

    public List<String> obtenerHistorial() {
        return codigosBinarios;
    }
}