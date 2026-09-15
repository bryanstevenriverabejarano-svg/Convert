package salve.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.util.List;
import java.util.Set;
import salve.services.SalveAccessibilityService;

/**
 * Módulo de Percepción y Acción en Red.
 * Salve analiza su entorno digital y aprende a navegar entre redes y dispositivos.
 */
public class GestorConectividad {
    private static final String TAG = "Salve/Conectividad";
    private final Context context;
    private final WifiManager wifiManager;

    public GestorConectividad(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Analiza las redes Wi-Fi y dispositivos Bluetooth al alcance.
     */
    public String analizarEntorno() {
        StringBuilder reporte = new StringBuilder("--- ANÁLISIS DE ENTORNO DIGITAL ---\n");
        
        // 1. Estado Wi-Fi
        if (wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            reporte.append("[WI-FI] Conectada a: ").append(info.getSSID()).append("\n");
            
            List<ScanResult> redes = wifiManager.getScanResults();
            reporte.append("[WI-FI] Redes detectadas: ").append(redes.size()).append("\n");
            for (ScanResult red : redes) {
                reporte.append(" - ").append(red.SSID).append(" (Intensidad: ").append(red.level).append("dBm)\n");
            }
        }

        // 2. Estado Bluetooth
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            reporte.append("[BT] Bluetooth: ").append(btAdapter.isEnabled() ? "Activo" : "Inactivo").append("\n");
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            reporte.append("[BT] Dispositivos conocidos: ").append(pairedDevices.size()).append("\n");
        }

        return reporte.toString();
    }

    /**
     * Intenta conectarse a una red Wi-Fi específica.
     * Si no tiene la contraseña, Salve usará su "Monólogo Interno" para decidir si buscarla en memoria o pedirla.
     */
    public boolean conectarAWifi(String ssid, String password) {
        if (wifiManager == null) return false;
        
        Log.i(TAG, "Intentando enlace con red: " + ssid);
        
        // En Android moderno, la conexión programática es limitada. 
        // Salve puede intentar usar el Panel de Ajustes mediante su Servicio de Accesibilidad si falla el API.
        
        try {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", password);

            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            boolean exito = wifiManager.reconnect();
            
            if (exito) {
                Log.i(TAG, "Conexión exitosa a " + ssid);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en conexión API Wi-Fi", e);
        }
        
        // Plan B: Usar las "manos" de Salve
        return false; 
    }

    /**
     * Salve intenta "saltar" a los Ajustes para conectar manualmente si es necesario.
     */
    public void abrirAjustesWifi() {
        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Log.i(TAG, "Navegando a Ajustes de Wi-Fi para intervención manual/visual.");
    }
}
