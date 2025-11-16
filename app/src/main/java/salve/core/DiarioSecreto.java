package salve.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiarioSecreto {

    private static final String PREFS_NAME = "salve_diario_secreto";
    private List<String> entradas;
    private SharedPreferences prefs;
    private CodificadorBinario codificador;

    public DiarioSecreto(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.codificador = new CodificadorBinario(context);
        this.entradas = new ArrayList<>();
        cargarEntradas();
    }

    private void cargarEntradas() {
        int total = prefs.getInt("total", 0);
        for (int i = 0; i < total; i++) {
            String bin = prefs.getString("entrada_" + i, "");
            entradas.add(bin);
        }
    }

    public void escribir(String pensamiento) {
        String fecha = obtenerFechaActual();
        String texto = "[" + fecha + "] " + pensamiento;
        String binario = codificador.codificar(texto);
        entradas.add(binario);
        guardarEntrada(binario);
    }

    /**
     * Registra una auto-crítica creativa en el diario manteniendo un prefijo claro.
     */
    public void escribirAutoCritica(String narrativa) {
        if (narrativa == null || narrativa.trim().isEmpty()) return;
        escribir("AUTO-CRÍTICA · " + narrativa.trim());
    }

    private void guardarEntrada(String binario) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("entrada_" + (entradas.size() - 1), binario);
        editor.putInt("total", entradas.size());
        editor.apply();
    }

    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public List<String> leerTodoDecodificado() {
        List<String> resultados = new ArrayList<>();
        for (String bin : entradas) {
            resultados.add(codificador.decodificar(bin));
        }
        return resultados;
    }

    public int totalEntradas() {
        return entradas.size();
    }

    public String ultimaEntrada() {
        if (entradas.isEmpty()) return "[Nada aún]";
        return codificador.decodificar(entradas.get(entradas.size() - 1));
    }
}