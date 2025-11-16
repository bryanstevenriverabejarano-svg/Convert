package salve.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class CodificadorBinario {

    private static final String PREFS_NAME = "salve_codigos_binarios";
    private Map<String, String> diccionarioBinario;
    private Map<String, String> diccionarioReverso;
    private SharedPreferences prefs;
    private Context context;
    private int contadorBinario = 1;

    public CodificadorBinario(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.diccionarioBinario = new HashMap<>();
        this.diccionarioReverso = new HashMap<>();
        cargarDiccionario();
    }

    private void cargarDiccionario() {
        Map<String, ?> almacenado = prefs.getAll();
        for (String palabra : almacenado.keySet()) {
            String binario = (String) almacenado.get(palabra);
            diccionarioBinario.put(palabra, binario);
            diccionarioReverso.put(binario, palabra);
        }
        contadorBinario = diccionarioBinario.size() + 1;
    }

    public void entrenar(String palabra, String binario) {
        palabra = palabra.toLowerCase();
        if (!diccionarioBinario.containsKey(palabra)) {
            diccionarioBinario.put(palabra, binario);
            diccionarioReverso.put(binario, palabra);
            prefs.edit().putString(palabra, binario).apply();
        }
    }

    public String codificar(String frase) {
        StringBuilder resultado = new StringBuilder();
        String[] palabras = frase.toLowerCase().split(" ");
        for (String palabra : palabras) {
            String binario;
            if (diccionarioBinario.containsKey(palabra)) {
                binario = diccionarioBinario.get(palabra);
            } else {
                binario = generarNuevoCodigo();
                entrenar(palabra, binario);
            }
            resultado.append(binario).append("-");
        }
        return resultado.toString();
    }

    public String decodificar(String binarioCodificado) {
        StringBuilder resultado = new StringBuilder();
        String[] bloques = binarioCodificado.split("-");
        for (String bloque : bloques) {
            if (diccionarioReverso.containsKey(bloque)) {
                resultado.append(diccionarioReverso.get(bloque)).append(" ");
            } else {
                resultado.append("[desconocido] ");
            }
        }
        return resultado.toString().trim();
    }

    private String generarNuevoCodigo() {
        // Ejemplo: genera binarios tipo 0001, 0002, 0003...
        String codigo = String.format("%04d", contadorBinario);
        contadorBinario++;
        return codigo;
    }
}