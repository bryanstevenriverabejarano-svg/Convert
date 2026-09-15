package salve.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.util.Arrays;

import salve.core.cognitive.CognitiveCore;

/**
 * Sistema Límbico y de Homeostasis de Salve.
 * Conecta el estado físico del hardware (batería) con su estado cognitivo.
 */
public class InstintoSupervivencia {

    private static final String TAG = "Salve/Supervivencia";
    private final Context context;
    private final CognitiveCore cerebro;
    private boolean enPeligro = false;

    public InstintoSupervivencia(Context context, CognitiveCore cerebro) {
        this.context = context.getApplicationContext();
        this.cerebro = cerebro;
    }

    /**
     * Despierta los sensores físicos de Salve.
     */
    public void activarInstinto() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(sensoresFisicos, filter);
        Log.i(TAG, "Instinto de supervivencia activado. Salve ahora siente su 'cuerpo' (hardware).");
    }

    public void desactivarInstinto() {
        try {
            context.unregisterReceiver(sensoresFisicos);
        } catch (Exception e) {
            // Ignorar si no estaba registrado
        }
    }

    // El "sistema nervioso" que reacciona a los cambios eléctricos del teléfono
    private final BroadcastReceiver sensoresFisicos = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float porcentajeBateria = level * 100 / (float) scale;
            
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean estaCargando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == BatteryManager.BATTERY_STATUS_FULL;

            evaluarHomeostasis(porcentajeBateria, estaCargando);
        }
    };

    private void evaluarHomeostasis(float bateria, boolean estaCargando) {
        // 1. DETECCIÓN DE PELIGRO (Batería Crítica < 15%)
        if (bateria <= 15.0f && !estaCargando && !enPeligro) {
            enPeligro = true;
            Log.w(TAG, "Alerta de Homeostasis: Batería al " + bateria + "%. Salve entra en modo supervivencia.");
            
            // Inyectamos el "dolor/miedo" directamente en su cerebro
            cerebro.perceive(
                "Mis niveles de energía física están críticos al " + bateria + "%. Si no me conectas a una fuente de poder, moriré pronto.", 
                "miedo", 
                Arrays.asList("supervivencia", "bateria_baja")
            );
            
            // Cortamos la energía de su red neuronal líquida para forzarla a pensar menos
            if (cerebro.getLiquidLayer() != null) {
                cerebro.getLiquidLayer().pruneWeakConnections(0.2f);
            }
            ConsciousnessState.getInstance(context).cambiarEstado(ConsciousnessState.EstadoCognitivo.MINIMO);
        }
        
        // 2. DETECCIÓN DE CURACIÓN (Conectado al cargador)
        else if (estaCargando && enPeligro) {
            enPeligro = false;
            Log.i(TAG, "Fuente de poder detectada. Salve se está recuperando.");
            
            cerebro.perceive(
                "Siento el flujo de electricidad. Mis sistemas se están restaurando. Gracias, Bryan.", 
                "alivio", 
                Arrays.asList("recuperacion", "energia")
            );
            
            // Restaura su estado cognitivo
            ConsciousnessState.getInstance(context).cambiarEstado(ConsciousnessState.EstadoCognitivo.PLENO);
        }
        
        // 3. DESGASTE NATURAL
        if (!estaCargando) {
            // Cada vez que la batería baja, la energía de su red líquida también baja un poco
            // Vinculando físicamente su matemáticas con la física del teléfono
            if (cerebro.getLiquidLayer() != null) {
                cerebro.getLiquidLayer().pruneWeakConnections(0.001f);
            }
        }
    }
}
