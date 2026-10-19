package salve.presentation.ui;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.*;
import android.net.Uri;
import android.net.VpnService;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.*;
import salve.core.compute.*;
import salve.core.privatefamily.*;

/** All coordinates stay in the encrypted vault and offline map. No external location/map APIs. */
public final class PrivateFamilyActivity extends AppCompatActivity {
    private final ExecutorService tasks = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FamilyVault vault;
    private MapView map;
    private LinearLayout nodes;
    private TextView status;
    private EditText importTarget;
    private LocationListener gps;
    private Runnable gpsGranted;
    private boolean busy;
    private String rendered = "";
    private final List<Marker> markers = new ArrayList<>();
    private final ActivityResultLauncher<Intent> vpnPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) startVpn(); else message("Android no autorizó la VPN.");
    });
    private final ActivityResultLauncher<String[]> gpsPermission = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        if (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) && gpsGranted != null) gpsGranted.run();
        else message("Para esta lectura GPS se necesita ubicación precisa. Puedes introducir coordenadas estáticas.");
        gpsGranted = null;
    });
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (!granted) message("Los estados seguirán en el mapa; los avisos del sistema están desactivados.");
    });
    private final ActivityResultLauncher<String[]> document = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
        if (uri == null || importTarget == null) return;
        try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = in.read(buffer)) != -1) { if (out.size() + count > 65536) throw new IOException(); out.write(buffer, 0, count); }
            importTarget.setText(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception invalid) { message("No se pudo leer el archivo (máximo 64 KB)."); }
        importTarget = null;
    });
    private final Runnable update = new Runnable() { @Override public void run() { render(); handler.postDelayed(this, 3000); } };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        vault = new FamilyVault(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16, 8, 16, 8); setContentView(root);
        TextView title = new TextView(this); title.setText("Red privada · Familia"); title.setTextSize(23); root.addView(title);
        status = new TextView(this); root.addView(status);
        LinearLayout actions = row(root);
        button(actions, "Configurar VPN", this::configure);
        button(actions, "Conectar", () -> { Intent consent = VpnService.prepare(this); if (consent == null) startVpn(); else vpnPermission.launch(consent); });
        button(actions, "Desconectar", () -> {
            disconnectVpn(); render();
        });
        LinearLayout controls = row(root);
        button(controls, "Familia", this::selectFamily); button(controls, "Actualizar", this::refresh); button(controls, "Mi ubicación", this::chooseOwnerNode);
        Switch monitor = new Switch(this); monitor.setText("Comprobar cada 15 min aprox. y avisar");
        try { monitor.setChecked(vault.read().monitoring); } catch (RuntimeException error) { message(error.getMessage()); }
        monitor.setOnCheckedChangeListener((view, enabled) -> {
            try { vault.edit(data -> { data.monitoring = enabled; data.revision++; }); FamilyMonitor.schedule(this, enabled);
                if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } catch (RuntimeException error) { message(error.getMessage()); }
        }); root.addView(monitor);
        // Zero tile modules: even accidental setUseDataConnection(true) cannot download tiles.
        MapTileProviderArray tiles = new MapTileProviderArray(new XYTileSource("private-offline", 0, 18, 256, ".png", new String[0]),
                new SimpleRegisterReceiver(this), new MapTileModuleProviderBase[0]) {
            @Override public int getMinimumZoomLevel() { return 0; }
            @Override public int getMaximumZoomLevel() { return 18; }
            @Override public android.graphics.drawable.Drawable getMapTile(long tile) { return new android.graphics.drawable.ColorDrawable(Color.rgb(220, 233, 238)); }
        };
        tiles.setUseDataConnection(false); map = new MapView(this, tiles); map.setUseDataConnection(false);
        map.setMultiTouchControls(true); map.setMinZoomLevel(1.0); map.setMaxZoomLevel(18.0);
        map.getController().setZoom(2.0); map.getController().setCenter(new GeoPoint(10, 0));
        root.addView(map, new LinearLayout.LayoutParams(-1, 0, 1)); drawLand();
        TextView caption = new TextView(this); caption.setText("Mapa general sin conexión · Natural Earth (dominio público). Coordenadas declaradas por cada propietario; no seguimiento en directo."); root.addView(caption);
        ScrollView scroll = new ScrollView(this); nodes = new LinearLayout(this); nodes.setOrientation(LinearLayout.VERTICAL); scroll.addView(nodes);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, (int)(140 * getResources().getDisplayMetrics().density)));
        render();
    }
    private LinearLayout row(LinearLayout parent) { LinearLayout row = new LinearLayout(this); parent.addView(row); return row; }
    private void button(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(this); button.setText(text); button.setTextSize(12); button.setAllCaps(false);
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1)); button.setOnClickListener(v -> action.run());
    }
    private EditText field(LinearLayout parent, String hint, boolean secret) {
        EditText field = new EditText(this); field.setHint(hint); field.setSaveEnabled(false); field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        field.setInputType(InputType.TYPE_CLASS_TEXT | (secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_FLAG_MULTI_LINE)); parent.addView(field); return field;
    }
    private void startVpn() {
        try { ContextCompat.startForegroundService(this, new Intent(this, FamilyVpnService.class)); }
        catch (RuntimeException blocked) { message("No se pudo iniciar la VPN. Abre esta pantalla y vuelve a conectar."); }
    }
    private void disconnectVpn() {
        PrivateVpn.get(this).invalidate();
        if (FamilyVpnService.running) startService(new Intent(this, FamilyVpnService.class).setAction(FamilyVpnService.STOP));
    }
    private void configure() {
        if (FamilyVpnService.running) { message("Desconecta la VPN antes de cambiar el perfil."); return; }
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        TextView info = new TextView(this); info.setText("Importa tu perfil WireGuard (.conf) y la CA que firma el HTTPS de tus nodos. Una dirección propia privada /32, rutas privadas /32 y endpoints UDP numéricos; sin DNS. Se guardan cifrados."); form.addView(info);
        EditText config = field(form, "Perfil WireGuard", true), ca = field(form, "CA PEM de los nodos", false);
        try { FamilyVault.Data data = vault.read(); config.setText(data.config); ca.setText(data.ca); } catch (RuntimeException error) { message(error.getMessage()); }
        LinearLayout imports = row(form);
        button(imports, "Importar .conf", () -> { importTarget = config; document.launch(new String[]{"*/*"}); });
        button(imports, "Importar CA", () -> { importTarget = ca; document.launch(new String[]{"*/*"}); });
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("VPN y certificados propios").setView(scroll)
                .setPositiveButton("Guardar", null).setNegativeButton("Cerrar", null).setNeutralButton("Borrar red", (d, w) -> {
                    disconnectVpn(); FamilyMonitor.schedule(this, false);
                    vault.forget(); recreate();
                }).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                if (FamilyVpnService.running) throw new IllegalStateException();
                String profile = config.getText().toString(), pem = ca.getText().toString();
                PrivateVpn.validate(profile, getPackageName()); PrivateVpn.trust(pem);
                vault.edit(data -> { data.config = profile; data.ca = pem; data.revision++; data.states.clear(); });
                config.setText(""); dialog.dismiss(); render();
            } catch (Exception invalid) { message("Perfil o CA inválidos. Comprueba /32 privadas, claves, endpoints numéricos y vigencia de la CA."); }
        })); dialog.show();
    }
    private void selectFamily() {
        List<ComputeNode> registered = new ComputeNodeStore(this).load(); if (registered.isEmpty()) { message("Añade primero los nodos autorizados en Nodos de cómputo."); return; }
        FamilyVault.Data data = vault.read(); String[] names = new String[registered.size()]; boolean[] selected = new boolean[names.length];
        for (int i = 0; i < names.length; i++) { names[i] = registered.get(i).name; selected[i] = data.family.contains(registered.get(i).id); }
        new AlertDialog.Builder(this).setTitle("Supervisar nodos autorizados").setMultiChoiceItems(names, selected, (d, which, checked) -> selected[which] = checked)
                .setPositiveButton("Guardar", (d, w) -> { vault.edit(current -> { current.family.clear();
                    for (int i = 0; i < selected.length; i++) if (selected[i]) current.family.add(registered.get(i).id);
                    current.states.keySet().retainAll(current.family); current.revision++;
                }); render(); }).setNegativeButton("Cancelar", null).show();
    }
    private void refresh() {
        if (busy) return; busy = true; status.setText("Comprobando por VPN…");
        tasks.execute(() -> { try { FamilyMonitor.check(getApplicationContext(), () -> Thread.currentThread().isInterrupted() || isDestroyed()); }
            catch (RuntimeException ignored) { } finally { runOnUiThread(() -> { busy = false; if (!isDestroyed()) render(); }); } });
    }
    private void render() {
        if (map == null || isDestroyed()) return;
        try {
            FamilyVault.Data data = vault.read(); PrivateVpn vpn = PrivateVpn.get(this);
            if (!busy) status.setText(vpn.status() + (vpn.available() ? " · Consultas privadas disponibles" : " · Supervisión pausada"));
            // Avoid refreshing overlays while the user is inspecting a marker; expire GPS on time.
            String stable = new Gson().toJson(data.states) + data.family + new ComputeNodeStore(this).revision() + vpn.available() + System.currentTimeMillis() / 60000;
            if (stable.equals(rendered)) return; rendered = stable;
            nodes.removeAllViews(); for (Marker marker : markers) { marker.closeInfoWindow(); map.getOverlays().remove(marker); } markers.clear();
            long now = System.currentTimeMillis() / 1000;
            for (ComputeNode node : new ComputeNodeStore(this).load()) {
                if (!data.family.contains(node.id)) continue;
                FamilyState state = data.states.getOrDefault(node.id, new FamilyState());
                String label = node.name + " · " + (vpn.available() ? state.status : "VPN desconectada; estado desconocido")
                        + "\nÚltima respuesta: " + date(state.lastSeen);
                PrivateLocation fix = state.location;
                if (fix != null && fix.fresh(now)) {
                    label += "\n" + fix.city + " · " + fix.zone + " · " + (fix.source.equals("gps") ? "GPS puntual" : "Configuración estática")
                            + "\n" + fix.latitude + ", " + fix.longitude + " · compartida " + date(fix.capturedAt);
                    Marker marker = new Marker(map);
                    android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable(); dot.setShape(android.graphics.drawable.GradientDrawable.OVAL); dot.setSize(30, 30);
                    dot.setColor(!vpn.available() ? Color.GRAY : state.status.equals("Disponible") ? Color.rgb(26, 128, 75) : Color.rgb(192, 114, 27)); dot.setStroke(2, Color.WHITE); marker.setIcon(dot);
                    marker.setPosition(new GeoPoint(fix.latitude, fix.longitude)); marker.setTitle(node.name);
                    marker.setSnippet(label); marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); map.getOverlays().add(marker); markers.add(marker);
                } else label += "\nSin coordenadas compartidas vigentes";
                TextView item = new TextView(this); item.setText(label); item.setPadding(8, 10, 8, 16); nodes.addView(item);
                if (fix != null && fix.fresh(now)) item.setOnClickListener(v -> { map.getController().setCenter(new GeoPoint(fix.latitude, fix.longitude)); map.getController().setZoom(8.0); });
            }
            if (nodes.getChildCount() == 0) { TextView empty = new TextView(this); empty.setText("Selecciona los nodos de tu familia. Cada propietario decide si comparte ubicación."); nodes.addView(empty); }
            map.invalidate();
        } catch (RuntimeException unavailable) { status.setText("No se pudo leer la configuración privada"); }
    }
    private String date(long seconds) { return seconds == 0 ? "nunca" : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(seconds * 1000)); }
    private void drawLand() {
        try (Reader reader = new InputStreamReader(getAssets().open("maps/world.geojson"), StandardCharsets.UTF_8)) {
            for (JsonElement feature : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("features")) {
                JsonObject geometry = feature.getAsJsonObject().getAsJsonObject("geometry"); JsonArray coordinates = geometry.getAsJsonArray("coordinates");
                if (geometry.get("type").getAsString().equals("Polygon")) landPolygon(coordinates);
                else for (JsonElement polygon : coordinates) landPolygon(polygon.getAsJsonArray());
            }
        } catch (Exception unavailable) { message("No se pudo abrir el mapa general sin conexión."); }
    }
    private void landPolygon(JsonArray rings) {
        Polygon polygon = new Polygon(map); List<GeoPoint> points = new ArrayList<>();
        for (JsonElement p : rings.get(0).getAsJsonArray()) { JsonArray xy = p.getAsJsonArray(); points.add(new GeoPoint(xy.get(1).getAsDouble(), xy.get(0).getAsDouble())); }
        polygon.setPoints(points); polygon.getFillPaint().setColor(Color.rgb(205, 221, 203)); polygon.getOutlinePaint().setColor(Color.rgb(119, 148, 126)); polygon.getOutlinePaint().setStrokeWidth(1);
        polygon.setOnClickListener((overlay, view, point) -> false); map.getOverlays().add(polygon);
    }
    private void chooseOwnerNode() {
        List<ComputeNode> registered = new ComputeNodeStore(this).load(); if (registered.isEmpty()) { message("Añade primero tu nodo."); return; }
        String[] names = new String[registered.size()]; for (int i = 0; i < names.length; i++) names[i] = registered.get(i).name;
        new AlertDialog.Builder(this).setTitle("Nodo que tú administras").setItems(names, (d, which) -> shareLocation(registered.get(which))).setNegativeButton("Cancelar", null).show();
    }
    private void shareLocation(ComputeNode node) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        TextView consent = new TextView(this); consent.setText("Solo el propietario puede publicar aquí. Las personas autorizadas para este nodo podrán ver estas coordenadas. GPS hace una lectura puntual; caduca a los 15 min. Ciudad y zona las escribes tú."); form.addView(consent);
        EditText writer = field(form, "Token de ubicación del propietario", true), city = field(form, "Ciudad (opcional)", false), zone = field(form, "Zona (opcional)", false), lat = field(form, "Latitud", false), lon = field(form, "Longitud", false);
        final Location[] captured = {null};
        LinearLayout gpsRow = row(form);
        button(gpsRow, "Leer mi GPS una vez", () -> {
            Runnable action = () -> captureGps(location -> { captured[0] = location; lat.setText(String.valueOf(location.getLatitude())); lon.setText(String.valueOf(location.getLongitude())); consent.setText("GPS leído, precisión ±" + Math.round(location.getAccuracy()) + " m. Revisa las coordenadas y pulsa Compartir para enviarlas por VPN."); });
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) action.run();
            else { gpsGranted = action; gpsPermission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}); }
        });
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Mi ubicación · " + node.name).setView(scroll)
                .setPositiveButton("Compartir", null).setNeutralButton("Retirar ubicación", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnDismissListener(d -> { stopGps(); gpsGranted = null; writer.setText(""); });
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    long now = System.currentTimeMillis() / 1000; double latitude = Double.parseDouble(lat.getText().toString()), longitude = Double.parseDouble(lon.getText().toString());
                    Location gpsFix = captured[0]; boolean fromGps = gpsFix != null && latitude == gpsFix.getLatitude() && longitude == gpsFix.getLongitude();
                    PrivateLocation fix = new PrivateLocation(latitude, longitude, city.getText().toString(), zone.getText().toString(), fromGps ? "gps" : "static", fromGps ? gpsFix.getTime() / 1000 : now, fromGps ? gpsFix.getAccuracy() : 0, now);
                    if (!fix.fresh(now)) throw new IllegalArgumentException();
                    publish(node, writer.getText().toString(), fix.json(), dialog);
                } catch (Exception invalid) { message("Introduce coordenadas válidas o vuelve a leer el GPS."); }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> publish(node, writer.getText().toString(), null, dialog));
        }); dialog.show();
    }
    private void publish(ComputeNode node, String token, JsonObject fix, AlertDialog dialog) {
        if (!token.matches("[A-Za-z0-9_-]{32,256}")) { message("Introduce el token de ubicación del propietario; es distinto del token de consulta."); return; }
        ComputeNodeStore registry = new ComputeNodeStore(this); long revision = registry.revision();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        tasks.execute(() -> {
            boolean success;
            try { new ComputeHttpTransport(this).shareLocation(node, token, fix, () -> isDestroyed() || !dialog.isShowing() || registry.revision() != revision || Thread.currentThread().isInterrupted()); success = true; }
            catch (Exception error) { success = false; }
            final boolean sent = success;
            runOnUiThread(() -> { if (isDestroyed() || !dialog.isShowing()) return;
                if (sent) { dialog.dismiss(); message(fix == null ? "Ubicación retirada del nodo. Los otros móviles borrarán su copia al volver a consultarlo." : "Coordenadas compartidas por la VPN privada."); refresh(); }
                else { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true); message("No se pudo confirmar el envío. Revisa VPN, certificado y token de ubicación."); }
            });
        });
    }
    @SuppressWarnings("MissingPermission") private void captureGps(java.util.function.Consumer<Location> result) {
        stopGps(); LocationManager manager = getSystemService(LocationManager.class);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { message("Activa el GPS local del dispositivo para esta lectura."); return; }
        gps = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (location.isFromMockProvider() || (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) > TimeUnit.SECONDS.toNanos(30)) return;
                stopGps(); result.accept(location);
            }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { stopGps(); }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        };
        try { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gps, Looper.getMainLooper());
            handler.postDelayed(() -> { if (gps != null) { stopGps(); message("No llegó una lectura GPS. Prueba al aire libre o usa coordenadas estáticas."); } }, 30000);
        } catch (SecurityException denied) { stopGps(); message("No hay permiso para leer el GPS."); }
    }
    private void stopGps() { if (gps != null) { getSystemService(LocationManager.class).removeUpdates(gps); gps = null; } }
    private void message(String text) { if (!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this).setMessage(text).setPositiveButton("Cerrar", null).show(); }
    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); handler.post(update); }
    @Override protected void onPause() { handler.removeCallbacks(update); if (map != null) map.onPause(); super.onPause(); }
    @Override protected void onStop() { stopGps(); super.onStop(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); tasks.shutdownNow(); if (map != null) map.onDetach(); super.onDestroy(); }
}
