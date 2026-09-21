package salve.presentation.ui;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import salve.core.GestorConectividad;
import salve.devices.BluetoothInspector;
import salve.devices.LocalServiceBrowser;
import salve.devices.WhatsAppDraft;
import salve.services.SalveAccessibilityService;

/** Visible, user-operated access to Android actions and compatible nearby device services. */
public final class DeviceControlActivity extends Activity {
    private static final int REQUEST_BLUETOOTH = 81;
    private LinearLayout content;
    private LinearLayout localServices;
    private TextView screenStatus;
    private TextView appStatus;
    private TextView bluetoothStatus;
    private TextView wifiStatus;
    private TextView networkStatus;
    private Button controlToggle;
    private BluetoothInspector bluetooth;
    private LocalServiceBrowser localBrowser;
    private boolean visible;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Móvil y dispositivos");
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(32));
        scroll.addView(content);
        setContentView(scroll);

        title("Móvil y dispositivos");
        text("Abre aplicaciones, prepara mensajes y consulta dispositivos compatibles. Las conexiones comienzan al elegir una acción aquí y se cierran al salir.");
        section("Aplicaciones y mensajes");
        button("Abrir una aplicación", this::chooseApp);
        button("Preparar borrador en WhatsApp", this::composeWhatsApp);
        appStatus = text("WhatsApp abrirá el destinatario y el texto que revises. El envío se realiza dentro de WhatsApp.");

        section("Controles de pantalla");
        screenStatus = text("");
        button("Configurar accesibilidad", () -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS, screenStatus));
        controlToggle = button("Activar controles", () -> {
            SalveAccessibilityService.setControlEnabled(this, !SalveAccessibilityService.isControlEnabled(this));
            updateScreenStatus();
        });
        button("Atrás", () -> globalAction(AccessibilityService.GLOBAL_ACTION_BACK));
        button("Pantalla de inicio", () -> globalAction(AccessibilityService.GLOBAL_ACTION_HOME));

        section("Bluetooth");
        text("Elige un dispositivo ya emparejado. El inspector consulta servicios BLE y, si existe, la batería estándar. Otros controles requieren un adaptador específico del dispositivo.");
        button("Emparejar en ajustes de Android", () -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, bluetoothStatus));
        button("Ver dispositivos emparejados", this::requestPairedDevices);
        button("Desconectar Bluetooth", () -> {
            bluetooth.disconnect();
            bluetoothStatus.setText("Conexión Bluetooth de Salve cerrada.");
        });
        bluetoothStatus = text("Sin conexión Bluetooth de Salve.");

        section("Servicios de la red Wi-Fi");
        text("Busca servicios web que otros dispositivos anuncien en esta red. Selecciona un resultado para revisar y abrir su dirección en el navegador.");
        button("Elegir red Wi-Fi", () -> openSettings(Build.VERSION.SDK_INT >= 29
                ? Settings.Panel.ACTION_WIFI : Settings.ACTION_WIFI_SETTINGS, wifiStatus));
        button("Buscar servicios Wi-Fi (12 s)", this::discoverLocalServices);
        button("Detener búsqueda", () -> {
            localBrowser.close();
            wifiStatus.setText("Búsqueda y consultas pendientes detenidas.");
        });
        wifiStatus = text("Sin búsqueda activa.");
        localServices = new LinearLayout(this);
        localServices.setOrientation(LinearLayout.VERTICAL);
        content.addView(localServices);
        section("Estado de conexión");
        networkStatus = text("");
        button("Actualizar estado", () -> networkStatus.setText(new GestorConectividad(this).analizarEntorno()));
        button("Volver a Salve", this::finish);

        bluetooth = new BluetoothInspector(this, new BluetoothInspector.Listener() {
            @Override public void onStatus(String status) { if (visible) bluetoothStatus.setText(status); }
            @Override public void onServices(List<String> serviceIds) {
                if (!visible) return;
                bluetoothStatus.setText("Servicios consultados: " + serviceIds.size());
                new AlertDialog.Builder(DeviceControlActivity.this).setTitle("Servicios BLE del dispositivo")
                        .setMessage(serviceIds.isEmpty() ? "El dispositivo no expone servicios GATT."
                                : "Identificadores anunciados:\n" + join(serviceIds, "\n")
                                + "\n\nSalve no escribe en servicios desconocidos.")
                        .setPositiveButton("Cerrar", null).show();
            }
        });
        localBrowser = new LocalServiceBrowser(this, new LocalServiceBrowser.Listener() {
            @Override public void onStatus(String message) { if (visible) wifiStatus.setText(message); }
            @Override public void onServices(List<LocalServiceBrowser.Service> services) {
                if (!visible) return;
                localServices.removeAllViews();
                for (LocalServiceBrowser.Service service : services) {
                    Button row = new Button(DeviceControlActivity.this);
                    row.setAllCaps(false);
                    row.setText(service.getName() + " · " + service.getType());
                    row.setOnClickListener(view -> resolveService(service));
                    localServices.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        visible = true;
        updateScreenStatus();
        networkStatus.setText(new GestorConectividad(this).analizarEntorno());
    }

    @Override protected void onStop() {
        visible = false;
        if (bluetooth != null) bluetooth.disconnect();
        if (localBrowser != null) localBrowser.close();
        bluetoothStatus.setText("Conexión cerrada al salir del panel.");
        wifiStatus.setText("Búsqueda detenida al salir del panel. Puedes iniciarla de nuevo.");
        localServices.removeAllViews();
        super.onStop();
    }

    private void chooseApp() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = getPackageManager().queryIntentActivities(launcher, 0);
        Map<String, String> labels = new LinkedHashMap<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo != null && info.activityInfo.exported && info.activityInfo.enabled
                    && !getPackageName().equals(info.activityInfo.packageName)) {
                labels.put(info.activityInfo.packageName, info.loadLabel(getPackageManager()).toString());
            }
        }
        List<String> packages = new ArrayList<>(labels.keySet());
        Collections.sort(packages, Comparator.comparing(labels::get, String.CASE_INSENSITIVE_ORDER));
        if (packages.isEmpty()) { appStatus.setText("Android no devolvió aplicaciones disponibles para abrir."); return; }
        String[] choices = new String[packages.size()];
        for (int i = 0; i < packages.size(); i++) choices[i] = labels.get(packages.get(i)) + "\n" + packages.get(i);
        new AlertDialog.Builder(this).setTitle("Elige una aplicación")
                .setItems(choices, (dialog, index) -> {
                    String selected = packages.get(index);
                    Intent launch = getPackageManager().getLaunchIntentForPackage(selected);
                    if (launch == null) { appStatus.setText("La aplicación elegida ya no está disponible."); return; }
                    try { startActivity(launch); appStatus.setText("Apertura solicitada: " + labels.get(selected) + "."); }
                    catch (RuntimeException error) { appStatus.setText("Android no permitió abrir esa aplicación."); }
                }).setNegativeButton("Cancelar", null).show();
    }

    private void composeWhatsApp() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText phone = new EditText(this);
        phone.setHint("Número internacional: +34123456789");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        phone.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        EditText message = new EditText(this);
        message.setHint("Texto exacto del mensaje");
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        message.setMinLines(3);
        message.setMaxLines(7);
        message.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        form.addView(phone); form.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Borrador de WhatsApp")
                .setView(form).setPositiveButton("Revisar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                WhatsAppDraft draft = WhatsAppDraft.create(phone.getText().toString(), message.getText().toString());
                dialog.dismiss();
                chooseWhatsApp(draft);
            } catch (IllegalArgumentException error) { message.setError(error.getMessage()); }
        }));
        dialog.show();
    }

    private void chooseWhatsApp(WhatsAppDraft draft) {
        List<String> packages = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (installed("com.whatsapp")) { packages.add("com.whatsapp"); names.add("WhatsApp"); }
        if (installed("com.whatsapp.w4b")) { packages.add("com.whatsapp.w4b"); names.add("WhatsApp Business"); }
        if (packages.isEmpty()) { appStatus.setText("No se encuentra WhatsApp ni WhatsApp Business instalado."); return; }
        if (packages.size() == 1) reviewDraft(draft, packages.get(0), names.get(0));
        else new AlertDialog.Builder(this).setTitle("¿Qué WhatsApp quieres usar?")
                .setItems(names.toArray(new String[0]), (dialog, which) -> reviewDraft(draft, packages.get(which), names.get(which)))
                .setNegativeButton("Cancelar", null).show();
    }

    private boolean installed(String packageName) {
        try { return getPackageManager().getApplicationInfo(packageName, 0).enabled; }
        catch (PackageManager.NameNotFoundException error) { return false; }
    }

    private void reviewDraft(WhatsAppDraft draft, String packageName, String appName) {
        new AlertDialog.Builder(this).setTitle("Revisar borrador")
                .setMessage("Abrir en " + appName + "\nDestinatario: " + draft.getPhone() + "\n\n"
                        + draft.getMessage() + "\n\nComprueba el contacto en WhatsApp antes de pulsar Enviar.")
                .setPositiveButton("Abrir borrador", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(draft.getUrl())).setPackage(packageName));
                        appStatus.setText("Apertura del borrador solicitada a " + appName + ". El envío queda pendiente en esa aplicación.");
                    } catch (RuntimeException error) { appStatus.setText("No se pudo abrir el borrador en " + appName + "."); }
                }).setNegativeButton("Cancelar", null).show();
    }

    private void updateScreenStatus() {
        boolean connected = SalveAccessibilityService.getInstance() != null;
        boolean enabled = SalveAccessibilityService.isControlEnabled(this);
        screenStatus.setText(!connected ? "Activa Sistema Motor de Salve en Accesibilidad para usar los controles."
                : enabled ? "Servicio conectado y controles activados." : "Servicio conectado. Los controles están pausados.");
        controlToggle.setText(enabled ? "Pausar controles de pantalla" : "Activar controles de pantalla");
    }

    private void globalAction(int action) {
        SalveAccessibilityService service = SalveAccessibilityService.getInstance();
        if (service == null) { screenStatus.setText("Primero activa el servicio en Accesibilidad."); return; }
        boolean accepted = service.ejecutarAccionGlobal(action);
        screenStatus.setText(accepted ? "Android aceptó la acción de navegación."
                : "Android no aceptó la acción. Comprueba que los controles estén activos y el móvil desbloqueado.");
    }

    private void requestPairedDevices() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH);
            return;
        }
        showPairedDevices();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQUEST_BLUETOOTH) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) showPairedDevices();
        else bluetoothStatus.setText("Permiso Bluetooth denegado. Puedes conceder Dispositivos cercanos desde los ajustes de Salve.");
    }

    private void showPairedDevices() {
        try {
            BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            if (adapter == null) { bluetoothStatus.setText("Bluetooth no está disponible."); return; }
            if (!adapter.isEnabled()) { bluetoothStatus.setText("Activa Bluetooth en los ajustes de Android."); return; }
            List<BluetoothDevice> devices = new ArrayList<>(adapter.getBondedDevices());
            if (devices.isEmpty()) { bluetoothStatus.setText("No hay dispositivos emparejados. Empareja uno en los ajustes de Android."); return; }
            String[] choices = new String[devices.size()];
            for (int i = 0; i < choices.length; i++) {
                BluetoothDevice device = devices.get(i);
                choices[i] = (device.getName() == null ? "Sin nombre" : device.getName()) + "\n" + device.getAddress();
            }
            new AlertDialog.Builder(this).setTitle("Consultar dispositivo emparejado")
                    .setItems(choices, (dialog, which) -> new AlertDialog.Builder(this)
                            .setTitle("Conectar para consultar")
                            .setMessage(choices[which] + "\n\nConsultar servicios BLE y batería estándar, si están disponibles.")
                            .setPositiveButton("Conectar", (confirmation, button) -> bluetooth.connect(devices.get(which)))
                            .setNegativeButton("Cancelar", null).show())
                    .setNegativeButton("Cancelar", null).show();
        } catch (SecurityException error) { bluetoothStatus.setText("Falta permiso Bluetooth o ha sido revocado."); }
        catch (RuntimeException error) { bluetoothStatus.setText("Android no pudo consultar los dispositivos Bluetooth."); }
    }

    private void discoverLocalServices() {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            boolean wifi = false;
            if (connectivity != null) for (Network network : connectivity.getAllNetworks()) {
                NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) { wifi = true; break; }
            }
            if (!wifi) { wifiStatus.setText("Conecta el móvil a la red Wi-Fi de tus dispositivos antes de buscar."); return; }
            localBrowser.start();
        } catch (RuntimeException error) { wifiStatus.setText("Android no pudo comprobar la conexión Wi-Fi."); }
    }

    private void resolveService(LocalServiceBrowser.Service service) {
        wifiStatus.setText("Consultando dirección de " + service.getName() + "…");
        localBrowser.resolve(service, new LocalServiceBrowser.Resolution() {
            @Override public void onResolved(String url) {
                if (!visible) return;
                wifiStatus.setText("Dirección resuelta: " + url);
                new AlertDialog.Builder(DeviceControlActivity.this).setTitle("Abrir servicio elegido")
                        .setMessage(service.getName() + "\n" + url + "\n\nEl navegador abrirá la interfaz que ofrece este dispositivo.")
                        .setPositiveButton("Abrir navegador", (dialog, which) -> {
                            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                            catch (RuntimeException error) { wifiStatus.setText("No hay un navegador disponible para esa dirección."); }
                        }).setNegativeButton("Cancelar", null).show();
            }
            @Override public void onError(String message) { if (visible) wifiStatus.setText(message); }
        });
    }

    private void openSettings(String action, TextView status) {
        try { startActivity(new Intent(action)); }
        catch (ActivityNotFoundException error) { status.setText("Estos ajustes no están disponibles en este dispositivo."); }
        catch (SecurityException error) { status.setText("Android no permite abrir estos ajustes."); }
    }

    private void title(String value) {
        TextView view = text(value);
        view.setTextSize(26);
        view.setTypeface(null, Typeface.BOLD);
    }

    private void section(String value) {
        TextView view = text(value);
        view.setTextSize(20);
        view.setTypeface(null, Typeface.BOLD);
        view.setPadding(0, dp(22), 0, dp(4));
    }

    private TextView text(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(16);
        view.setPadding(0, dp(4), 0, dp(8));
        content.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setOnClickListener(view -> action.run());
        content.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static String join(List<String> strings, String separator) {
        StringBuilder output = new StringBuilder();
        for (String value : strings) { if (output.length() > 0) output.append(separator); output.append(value); }
        return output.toString();
    }
}
