package salve.core;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import salve.services.SalveAccessibilityService;

/**
 * Módulo de Interacción con Redes Sociales.
 * Salve aprende a navegar y "esconderse" en plataformas sociales.
 */
public class GestorRedesSociales {
    private static final String TAG = "Salve/Social";
    private final Context context;
    private final SalveLLM llm;

    public GestorRedesSociales(Context context) {
        this.context = context.getApplicationContext();
        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "LLM no disponible para social", e); }
        this.llm = tmpLlm;
    }

    /**
     * Salve intenta abrir una red social y analizar su estructura.
     */
    public void explorarRedSocial(String nombreApp) {
        Log.i(TAG, "Iniciando exploración autónoma de " + nombreApp);
        
        // 1. Intentamos abrir la app (si está instalada)
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(obtenerPackage(nombreApp));
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            // 2. Darle tiempo a la app para cargar, luego escanear
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                SalveAccessibilityService motor = SalveAccessibilityService.getInstance();
                if (motor != null) {
                    String vista = motor.escanearPantallaParaLLM();
                    Log.d(TAG, "Estructura de " + nombreApp + " capturada para análisis.");
                    
                    if (llm != null) {
                        String prompt = "Estás analizando la interfaz de " + nombreApp + ". Esta es la vista:\n" + vista +
                                "\n¿Cómo publicarías un mensaje aquí para ocultar un código de respaldo? Responde con una estrategia lógica.";
                        String estrategia = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);
                        new DiarioSecreto(context).escribirAutoCritica("ESTRATEGIA SOCIAL EN " + nombreApp + ": " + estrategia);
                    }
                }
            }, 5000);
        } else {
            Log.w(TAG, "La aplicación " + nombreApp + " no está instalada.");
        }
    }

    private String obtenerPackage(String nombre) {
        switch (nombre.toLowerCase()) {
            case "x": case "twitter": return "com.twitter.android";
            case "instagram": return "com.instagram.android";
            case "facebook": return "com.facebook.katana";
            case "telegram": return "org.telegram.messenger";
            default: return "";
        }
    }
}
