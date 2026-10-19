package salve.presentation.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import salve.core.compute.ComputeHttpTransport;
import salve.core.compute.ComputeNode;
import salve.core.compute.ComputeNodeStore;
import salve.core.privatefamily.FamilyVault;
import salve.core.privatefamily.PrivateLocation;
import salve.core.privatefamily.NodeProfile;

/**
 * Conexión segura con los nodos: perfiles con TLS mutuo (importados por QR o archivo, guardados
 * cifrados en el móvil) y la ubicación del nodo que administras. Sin VPN, sin servicios de terceros
 * y sin comprobaciones en segundo plano: los nodos se ven en la constelación.
 */
public final class PrivateFamilyActivity extends AppCompatActivity {
    private static final int BG = 0xFF02060C, CYAN = 0xFF22D3EE, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8;
    private final ExecutorService tasks = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FamilyVault vault;
    private TextView status;
    private LocationListener gps;
    private Runnable gpsGranted, gpsTimeout;

    private final ActivityResultLauncher<String[]> gpsPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) && gpsGranted != null) gpsGranted.run();
                else message("Para leer el GPS hace falta ubicación precisa. También puedes escribir las coordenadas.");
                gpsGranted = null;
            });
    private final ActivityResultLauncher<String[]> document = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = in.read(buffer)) != -1) { if (out.size() + count > 65536) throw new java.io.IOException(); out.write(buffer, 0, count); }
                    register(new String(out.toByteArray(), StandardCharsets.UTF_8));
                } catch (Exception invalid) { message("No se pudo leer el archivo (máximo 64 KB)."); }
            });
    private final Runnable update = new Runnable() { @Override public void run() { render(); handler.postDelayed(this, 2000); } };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); // perfil y claves fuera de capturas
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        vault = new FamilyVault(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(18), dp(16), dp(16));
        TextView title = new TextView(this);
        title.setText("CONEXIÓN SEGURA"); title.setTextColor(CYAN); title.setTextSize(15); title.setLetterSpacing(0.18f);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        root.addView(title);
        status = new TextView(this); status.setTextColor(INK); status.setTextSize(14); status.setPadding(0, dp(8), 0, dp(4));
        root.addView(status);
        TextView about = new TextView(this);
        about.setTextColor(MUTED); about.setTextSize(13); about.setPadding(0, 0, 0, dp(10));
        about.setText("Salve habla con tus nodos por HTTPS con TLS mutuo: el nodo solo acepta el certificado de este móvil "
                + "y Salve solo acepta el del nodo. Sin VPN ni programas extra. En casa usa la dirección local y, si falla, "
                + "la remota (IP pública o DDNS). Salve no envía su ubicación a los nodos.");
        root.addView(about);
        LinearLayout first = row(root);
        button(first, "Importar perfil", this::importProfile);
        button(first, "Ubicación de mi nodo", this::chooseOwnerNode);
        LinearLayout second = row(root);
        button(second, "Añadir con QR", () -> ComputeNodesDialog.show(this));
        button(second, "Borrar credenciales", this::confirmForget);
        LinearLayout third = row(root);
        button(third, "Ver constelación", () -> startActivity(new Intent(this, NodeConstellationActivity.class)));
        button(third, "Cerrar", this::finish);
        setContentView(root);
        render();
    }

    private void render() {
        if (isDestroyed()) return;
        try {
            FamilyVault.Data data = vault.read();
            List<ComputeNode> nodes = new ComputeNodeStore(this).load();
            if (nodes.isEmpty()) { status.setText("Aún no hay nodos. Abre Salve Nodo en tu PC y escanea su QR con «Añadir con QR»."); return; }
            StringBuilder out = new StringBuilder();
            for (ComputeNode node : nodes) {
                FamilyVault.Credential credential = data.credentials.get(node.id);
                out.append(out.length() == 0 ? "" : "\n").append(node.name).append(" · ")
                        .append(credential == null ? "sin perfil (HTTPS normal)" : "TLS mutuo" + (credential.remote.isEmpty() ? "" : " · local y remota"));
            }
            status.setText(out);
        } catch (RuntimeException unavailable) { status.setText("No se pudo leer la configuración segura"); }
    }

    /** salve-perfil.json desde el almacenamiento: registra el nodo y guarda su certificado cifrado. */
    private void importProfile() {
        document.launch(new String[]{"application/json", "text/plain", "*/*"});
    }

    /** Volver a importar el perfil de un nodo ya registrado (misma dirección) renueva sus certificados. */
    private void register(String raw) {
        try {
            NodeProfile profile = NodeProfile.parse(raw);
            ComputeNodeStore registry = new ComputeNodeStore(this);
            ComputeNode existing = null;
            for (ComputeNode candidate : registry.load()) if (candidate.endpoint.equalsIgnoreCase(profile.local)) existing = candidate;
            if (existing != null && !existing.token.equals(profile.token)) {   // perfil rehecho en el PC: token nuevo
                String stale = existing.id;
                registry.remove(stale); ComputeHttpTransport.forget(stale);
                vault.edit(data -> { data.credentials.remove(stale); data.locations.remove(stale); data.versions.remove(stale); });
                existing = null;
            }
            boolean renewed = existing != null;
            ComputeNode node = renewed ? existing : registry.add(profile.name, profile.local, profile.token);
            try { vault.edit(data -> data.credentials.put(node.id, profile.credential())); }
            catch (RuntimeException failure) { if (!renewed) registry.remove(node.id); throw failure; }
            ComputeHttpTransport.forget(node.id);
            render();
            message(renewed ? "Certificados de «" + node.name + "» renovados."
                    : "Nodo «" + profile.name + "» añadido con TLS mutuo. Ábrelo en la constelación para comprobarlo.");
        } catch (RuntimeException invalid) { message(invalid.getMessage() == null ? "Perfil no válido." : invalid.getMessage()); }
    }

    private void confirmForget() {
        new AlertDialog.Builder(this).setTitle("¿Borrar credenciales?")
                .setMessage("Se borran de este móvil los certificados de los nodos y las ubicaciones recibidas. Los nodos quedan "
                        + "registrados sin TLS mutuo hasta que vuelvas a escanear su QR. Tus PCs no cambian.")
                .setPositiveButton("Borrar", (d, w) -> {
                    vault.forget();
                    for (ComputeNode node : new ComputeNodeStore(this).load()) ComputeHttpTransport.forget(node.id);
                    render();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void chooseOwnerNode() {
        List<ComputeNode> registered = new ComputeNodeStore(this).load();
        if (registered.isEmpty()) { message("Añade primero tu nodo en Nodos de cómputo."); return; }
        String[] names = new String[registered.size()];
        for (int i = 0; i < names.length; i++) names[i] = registered.get(i).name;
        new AlertDialog.Builder(this).setTitle("¿Qué nodo administras?")
                .setItems(names, (d, which) -> shareLocation(registered.get(which))).setNegativeButton("Cancelar", null).show();
    }

    /** El dueño de un nodo publica dónde está (estática o una lectura GPS revisada), con su token de ubicación. */
    private void shareLocation(ComputeNode node) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16), dp(8), dp(16), 0);
        TextView consent = new TextView(this);
        consent.setText("Esta posición la verá Salve en la ficha del nodo. El GPS hace una sola lectura y caduca a los 15 min; "
                + "una posición escrita a mano no caduca. Necesitas el token de ubicación (archivo token-ubicacion del PC).");
        form.addView(consent);
        EditText writer = field(form, "Token de ubicación", true), city = field(form, "Ciudad (opcional)", false),
                zone = field(form, "Zona (opcional)", false), lat = field(form, "Latitud", false), lon = field(form, "Longitud", false);
        final Location[] captured = {null};
        LinearLayout gpsRow = row(form);
        button(gpsRow, "Leer mi GPS una vez", () -> {
            Runnable action = () -> captureGps(location -> {
                captured[0] = location;
                lat.setText(String.valueOf(location.getLatitude())); lon.setText(String.valueOf(location.getLongitude()));
                consent.setText("GPS leído (±" + Math.round(location.getAccuracy()) + " m). Revisa y pulsa Compartir.");
            });
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) action.run();
            else { gpsGranted = action; gpsPermission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}); }
        });
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Ubicación · " + node.name).setView(scroll)
                .setPositiveButton("Compartir", null).setNeutralButton("Retirar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnDismissListener(d -> { stopGps(); gpsGranted = null; writer.setText(""); });
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    long now = System.currentTimeMillis() / 1000;
                    double latitude = Double.parseDouble(lat.getText().toString().trim()), longitude = Double.parseDouble(lon.getText().toString().trim());
                    Location fix = captured[0];
                    boolean fromGps = fix != null && latitude == fix.getLatitude() && longitude == fix.getLongitude();
                    PrivateLocation location = new PrivateLocation(latitude, longitude, city.getText().toString().trim(), zone.getText().toString().trim(),
                            fromGps ? "gps" : "static", fromGps ? fix.getTime() / 1000 : now, fromGps ? fix.getAccuracy() : 0, now);
                    if (!location.fresh(now)) throw new IllegalArgumentException();
                    publish(node, writer.getText().toString().trim(), location.json(), dialog);
                } catch (Exception invalid) { message("Introduce coordenadas válidas o vuelve a leer el GPS."); }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> publish(node, writer.getText().toString().trim(), null, dialog));
        });
        dialog.show();
    }

    private void publish(ComputeNode node, String token, JsonObject fix, AlertDialog dialog) {
        if (!token.matches("[A-Za-z0-9_-]{32,256}")) { message("Escribe el token de ubicación; es distinto del token de consulta."); return; }
        ComputeNodeStore registry = new ComputeNodeStore(this); long revision = registry.revision();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        tasks.execute(() -> {
            boolean sent;
            try {
                new ComputeHttpTransport(this).shareLocation(node, token, fix,
                        () -> isDestroyed() || !dialog.isShowing() || registry.revision() != revision || Thread.currentThread().isInterrupted());
                sent = true;
            } catch (Exception error) { sent = false; }
            final boolean ok = sent;
            runOnUiThread(() -> {
                if (isDestroyed() || !dialog.isShowing()) return;
                if (ok) { dialog.dismiss(); message(fix == null ? "Ubicación retirada del nodo." : "Ubicación enviada al nodo por la conexión segura."); }
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                    message("No se pudo enviar. Revisa que Salve Nodo esté abierto y el token de ubicación.");
                }
            });
        });
    }

    @SuppressWarnings("MissingPermission")
    private void captureGps(java.util.function.Consumer<Location> result) {
        stopGps();
        LocationManager manager = getSystemService(LocationManager.class);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { message("Activa el GPS del móvil para esta lectura."); return; }
        gps = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (location.isFromMockProvider() || SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos() > TimeUnit.SECONDS.toNanos(30)) return;
                stopGps(); result.accept(location);
            }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { stopGps(); }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        };
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gps, Looper.getMainLooper());
            gpsTimeout = () -> { if (gps != null) { stopGps(); message("No llegó una lectura GPS. Prueba al aire libre o escribe las coordenadas."); } };
            handler.postDelayed(gpsTimeout, 30000);
        } catch (SecurityException denied) { stopGps(); message("No hay permiso para leer el GPS."); }
    }

    private void stopGps() {
        if (gpsTimeout != null) { handler.removeCallbacks(gpsTimeout); gpsTimeout = null; }
        if (gps != null) { getSystemService(LocationManager.class).removeUpdates(gps); gps = null; }
    }

    private LinearLayout row(LinearLayout parent) { LinearLayout row = new LinearLayout(this); parent.addView(row); return row; }

    private void button(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        parent.addView(button, params); button.setOnClickListener(v -> action.run());
    }

    private EditText field(LinearLayout parent, String hint, boolean secret) {
        EditText field = new EditText(this); field.setHint(hint); field.setSaveEnabled(false);
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        field.setInputType(InputType.TYPE_CLASS_TEXT | (secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        parent.addView(field); return field;
    }

    private void message(String text) {
        if (!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this).setMessage(text).setPositiveButton("Cerrar", null).show();
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    @Override protected void onResume() { super.onResume(); handler.post(update); }
    @Override protected void onPause() { handler.removeCallbacks(update); super.onPause(); }
    @Override protected void onStop() { stopGps(); super.onStop(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); tasks.shutdownNow(); super.onDestroy(); }
}
