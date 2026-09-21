package salve.core;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.provider.Settings;

/** Reports actual Android connectivity without scanning networks or claiming a connection. */
public class GestorConectividad {
    private final Context context;

    public GestorConectividad(Context context) { this.context = context.getApplicationContext(); }

    public String analizarEntorno() {
        StringBuilder report = new StringBuilder();
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = manager == null ? null : manager.getActiveNetwork();
            NetworkCapabilities capabilities = network == null ? null : manager.getNetworkCapabilities(network);
            if (capabilities == null) report.append("No hay una conexión de red activa.\n");
            else {
                String transport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi"
                        : capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "datos móviles"
                        : capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? "VPN" : "otra red";
                report.append("Red activa: ").append(transport).append(". ");
                report.append(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        ? "Android ha validado acceso a Internet.\n" : "Android no ha validado acceso a Internet.\n");
            }
        } catch (SecurityException error) { report.append("Android no permite consultar el estado de red.\n"); }
        try {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            if (adapter == null) report.append("Bluetooth no está disponible.");
            else if (context.checkSelfPermission(Build.VERSION.SDK_INT >= 31
                    ? Manifest.permission.BLUETOOTH_CONNECT : Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                report.append("Bluetooth: falta autorizar dispositivos cercanos en Móvil y dispositivos.");
            } else if (!adapter.isEnabled()) report.append("Bluetooth está desactivado.");
            else report.append("Bluetooth activo. Dispositivos emparejados: ").append(adapter.getBondedDevices().size()).append(".");
        } catch (SecurityException error) { report.append("El permiso Bluetooth no está disponible."); }
        catch (RuntimeException error) { report.append("Android no pudo consultar Bluetooth."); }
        return report.toString();
    }

    /** Retained for callers: opening system settings does not establish or verify a connection. */
    public boolean conectarAWifi(String ssid, String password) {
        abrirAjustesWifi();
        return false;
    }

    public void abrirAjustesWifi() {
        String action = Build.VERSION.SDK_INT >= 29 ? Settings.Panel.ACTION_WIFI : Settings.ACTION_WIFI_SETTINGS;
        try { context.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException ignored) { /* The caller must not claim that a connection was established. */ }
    }
}
