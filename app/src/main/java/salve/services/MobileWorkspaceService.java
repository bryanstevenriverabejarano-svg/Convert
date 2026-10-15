package salve.services;

import android.accessibilityservice.AccessibilityService;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.*;
import android.net.http.SslError;
import android.os.*;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.io.ByteArrayInputStream;
import salve.core.devicecontrol.BrowserRecovery;
import salve.core.devicecontrol.MobileBrowserPolicy;
import salve.core.web.WebSearchSettings;
import salve.presentation.ui.DeviceControlActivity;

/** Visible, explicitly started workspace. Web pages have no bridge to Android actions or memory. */
public final class MobileWorkspaceService extends Service {
    public static final String QUERY = "mobile_query";
    public static final String MEDIA = "mobile_media";
    public static final String MEDIA_AUTOPLAY = "mobile_media_autoplay";
    private static final String STOP = "salve.mobile.STOP";
    private static final String CHANNEL = "salve_mobile_workspace";
    private static final int NOTIFICATION = 3103;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BrowserRecovery recovery = new BrowserRecovery();
    private WindowManager manager;
    private WindowManager.LayoutParams layout;
    private LinearLayout panel, body, browserPage, controlPage;
    private TextView status, observation, trace;
    private final java.util.ArrayDeque<String> readings = new java.util.ArrayDeque<>();
    /** Búsqueda de YouTube cuyo primer vídeo hay que abrir; null si no hay reproducción pendiente. */
    private String mediaQuery;
    private boolean mediaAutoplay;
    private int mediaAttempts;
    private final salve.core.web.SearchLive.Listener live = new salve.core.web.SearchLive.Listener() {
        @Override public void onSearch(String value) {
            if (destroyed || query == null) return;
            readings.clear(); showReadings();
            query.setText(value); search(); report("Salve está buscando: " + value);
        }
        @Override public void onReading(String url, String title) {
            if (destroyed) return;
            String host = Uri.parse(url).getHost();
            readings.addLast("· " + (host == null ? url : host) + (title.isEmpty() ? "" : " — " + title));
            while (readings.size() > 4) readings.removeFirst();
            showReadings();
        }
    };
    private EditText query, text, label;
    private volatile WebView web;
    private boolean added, collapsed, destroyed, receiverRegistered, networkRegistered, reviewing;
    private boolean pageFailed, paused;
    private ConnectivityManager connectivity;
    private String pendingText = "";
    private long nextRetry;
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { visibility(); }
    };
    private final ConnectivityManager.NetworkCallback network = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network value) { main.post(MobileWorkspaceService.this::tick); }
        @Override public void onLost(Network value) { main.post(MobileWorkspaceService.this::tick); }
        @Override public void onCapabilitiesChanged(Network value, NetworkCapabilities caps) { main.post(MobileWorkspaceService.this::tick); }
    };
    private final Runnable clock = new Runnable() {
        @Override public void run() { if (!destroyed) { tick(); main.postDelayed(this, 1000); } }
    };
    /** Abre YouTube en la mini ventana; con autoplay pone el primer resultado. */
    public static boolean openMedia(Context context, String query, boolean autoplay) {
        if (!Settings.canDrawOverlays(context)) return false;
        try {
            context.startForegroundService(new Intent(context, MobileWorkspaceService.class)
                    .putExtra(MEDIA, query).putExtra(MEDIA_AUTOPLAY, autoplay));
            return true;
        } catch (RuntimeException unavailable) { return false; }
    }
    public static void close(Context context) {
        try { context.startService(new Intent(context, MobileWorkspaceService.class).setAction(STOP)); }
        catch (RuntimeException notRunning) { /* No estaba abierta. */ }
    }
    public static boolean open(Context context, String query) {
        if (!Settings.canDrawOverlays(context)) return false;
        try {
            context.startForegroundService(new Intent(context, MobileWorkspaceService.class).putExtra(QUERY, query));
            return true;
        } catch (RuntimeException unavailable) { return false; }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        try {
            if (panel == null) { notification(); createPanel(); observeEnvironment(); salve.core.web.SearchLive.register(live); }
            String media = intent == null ? null : intent.getStringExtra(MEDIA);
            String search = intent == null ? null : intent.getStringExtra(QUERY);
            if (media != null && !media.trim().isEmpty()) playMedia(media, intent.getBooleanExtra(MEDIA_AUTOPLAY, true));
            else if (search != null && !search.trim().isEmpty()) { mediaQuery = null; query.setText(search); search(); }
        } catch (RuntimeException unavailable) {
            Toast.makeText(this, "No pude abrir la ventana. Revisa el permiso y Android System WebView.", Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void notification() {
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "Panel móvil de Salve", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, DeviceControlActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 0, new Intent(this, MobileWorkspaceService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle("Salve · panel flotante activo").setContentText("Búsquedas y controles visibles. Cierra el panel para detenerlo.")
                .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null, "Cerrar", stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION, notification);
    }
    private void createPanel() {
        manager = getSystemService(WindowManager.class);
        panel = column(); panel.setPadding(dp(8), dp(6), dp(8), dp(6)); panel.setBackgroundColor(Color.rgb(19, 36, 49));
        LinearLayout header = row(panel);
        TextView title = message(header, "Salve · arrastra aquí"); title.setTextSize(16);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 2)); title.setGravity(Gravity.CENTER_VERTICAL);
        Button minimize = button(header, "−", () -> collapse(!collapsed)); minimize.setContentDescription("Minimizar o ampliar panel");
        Button close = button(header, "×", this::stopSelf); close.setContentDescription("Cerrar panel y detener búsqueda");
        installDrag(title);
        body = column(); panel.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        status = message(body, "Elige Web o Móvil. Puedes minimizar esta ventana mientras usas otras apps.");
        status.setMaxLines(4); status.setTextSize(13);
        LinearLayout tabs = row(body);
        button(tabs, "Web", () -> tab(true)); button(tabs, "Móvil", () -> tab(false)); button(tabs, "Pausar", this::pause);
        browserPage = column(); body.addView(browserPage, new LinearLayout.LayoutParams(-1, 0, 1));
        query = input(browserPage, "Busca o pega una dirección HTTPS", 1000);
        LinearLayout searchButtons = row(browserPage);
        button(searchButtons, "Buscar", this::search);
        button(searchButtons, "Atrás web", () -> { recovery.pause(); if (web != null && web.canGoBack()) { paused = false; web.goBack(); } });
        button(searchButtons, "Navegador", this::externalBrowser);
        trace = message(browserPage, ""); trace.setTextSize(12); trace.setMaxLines(4); trace.setVisibility(View.GONE);
        createWebView();
        controlPage = column(); body.addView(controlPage, new LinearLayout.LayoutParams(-1, 0, 1)); controlPage.setVisibility(View.GONE);
        ScrollView scroll = new ScrollView(this); controlPage.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout controls = column(); scroll.addView(controls);
        message(controls, "Prepara el texto; selecciona después el campo en la otra app y pulsa Revisar escritura. Cada acción se revisa sobre su destino.");
        text = input(controls, "Texto para escribir (reemplaza el campo)", 4000); text.setMaxLines(3);
        button(controls, "Preparar texto", () -> {
            pendingText = text.getText().toString(); releaseFocus();
            report(pendingText.isEmpty() ? "Escribe primero el texto." : "Texto preparado. Toca el campo en la otra app y vuelve a Revisar escritura.");
        });
        button(controls, "Revisar escritura", () -> actOnApp(true));
        label = input(controls, "Etiqueta exacta del botón, por ejemplo Enviar", 160);
        button(controls, "Revisar toque", () -> actOnApp(false));
        LinearLayout navigation = row(controls);
        button(navigation, "Atrás", () -> global(AccessibilityService.GLOBAL_ACTION_BACK));
        button(navigation, "Inicio", () -> global(AccessibilityService.GLOBAL_ACTION_HOME));
        button(navigation, "Recientes", () -> global(AccessibilityService.GLOBAL_ACTION_RECENTS));
        button(controls, "Leer pantalla actual", this::readScreen);
        button(controls, "Apps y borrador de WhatsApp", this::openDevices);
        observation = message(controls, "La lectura de pantalla aparecerá aquí solo cuando la solicites."); observation.setTextIsSelectable(true);
        layout = new WindowManager.LayoutParams(dp(350), dp(530), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);
        layout.gravity = Gravity.TOP | Gravity.LEFT; layout.x = dp(10); layout.y = dp(80);
        layout.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        panel.setOnTouchListener((v, event) -> { if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) releaseFocus(); return false; });
        bounds(); attach(); visibility();
    }
    @SuppressWarnings("SetJavaScriptEnabled") private void createWebView() {
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false); settings.setSupportMultipleWindows(false);
        // Solo la reproducción pedida a Salve («pon música…») puede sonar sin tocar la pantalla.
        settings.setMediaPlaybackRequiresUserGesture(mediaQuery == null); settings.setGeolocationEnabled(false); settings.setSaveFormData(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setOnTouchListener((v, event) -> { if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { recovery.userInteraction(); paused = false; focus(); } return false; });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) { callback.onReceiveValue(null); report("Para adjuntar archivos, abre la página en tu navegador."); return true; }
        });
        web.setDownloadListener((url, agent, disposition, type, length) -> report("Para descargar este archivo, abre la página en tu navegador."));
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (view != web) return true;
                String url = request.getUrl().toString();
                if (request.isForMainFrame()) recovery.navigation(request.getMethod(), url);
                if (MobileBrowserPolicy.allowed(url)) return false;
                if (request.isForMainFrame()) report("Esta ventana abre páginas HTTPS. Usa la app correspondiente para otros enlaces.");
                return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (view != web) return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                String url = request.getUrl().toString();
                if (request.isForMainFrame()) recovery.navigation(request.getMethod(), url);
                if (MobileBrowserPolicy.allowed(url)) return null;
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                if (view != web || paused) return;
                pageFailed = false;
                if (!MobileBrowserPolicy.allowed(url)) { pageFailed = true; view.stopLoading(); recovery.failed(false); report("Dirección no compatible con esta ventana."); return; }
                recovery.navigation("GET", url); report("Cargando · " + Uri.parse(url).getHost());
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (view != web || pageFailed || paused || recovery.state() == BrowserRecovery.State.WAITING_NETWORK
                        || recovery.state() == BrowserRecovery.State.FAILED) return;
                recovery.ready();
                if (mediaQuery != null && mediaAutoplay && url.contains("/results")) { mediaAttempts = 0; findFirstVideo(); return; }
                if (mediaQuery != null && url.contains("/watch")) { startVideo(); return; }
                report("Página cargada · " + Uri.parse(url).getHost() + ". Puedes abrir los resultados. Si pide verificación, complétala aquí o en tu navegador.");
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (view != web || !request.isForMainFrame() || paused) return;
                pageFailed = true;
                boolean retry = error.getErrorCode() == ERROR_HOST_LOOKUP || error.getErrorCode() == ERROR_CONNECT || error.getErrorCode() == ERROR_TIMEOUT;
                recovery.failed(retry); nextRetry = SystemClock.elapsedRealtime() + 3000;
                report(recovery.state() == BrowserRecovery.State.WAITING_NETWORK ? "Conexión interrumpida. Recuperaré esta búsqueda si vuelve la red (máximo 2 intentos)." : "No se pudo cargar esta página. Puedes volver a buscar o abrir el navegador.");
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (view == web && request.isForMainFrame()) { pageFailed = true; recovery.failed(false); report("El sitio respondió HTTP " + response.getStatusCode() + ". No repetiré esta petición automáticamente."); }
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); if (view == web) { pageFailed = true; recovery.failed(false); report("El certificado HTTPS no es válido. Carga detenida."); }
            }
            @Override public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                dontResend.sendToTarget();
                if (view == web) { recovery.pause(); pageFailed = true; report("Formulario no reenviado. Revisa su estado en la página."); }
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view == web) { recovery.pause(); paused = true; web = null; browserPage.removeView(view); view.destroy(); report("Android cerró el navegador de la ventana, posiblemente por memoria. Pulsa Buscar para volver a abrirlo."); }
                return true;
            }
        });
        browserPage.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
    }
    private void search() {
        if (reviewing || locked()) { report("Termina la acción pendiente y desbloquea el móvil para buscar."); return; }
        try {
            mediaQuery = null; // Una búsqueda normal nunca reproduce sola.
            String value = query.getText().toString();
            String url = MobileBrowserPolicy.searchUrl(WebSearchSettings.load(this, value), value);
            disposeBrowser(); createWebView(); // Isolates callbacks from the old search and any old forms.
            paused = false; pageFailed = false;
            recovery.begin(url, online(), SystemClock.elapsedRealtime());
            tab(true); collapse(false); releaseFocus();
            if (recovery.state() == BrowserRecovery.State.LOADING) web.loadUrl(url);
            else report("Sin conexión. Buscaré cuando vuelva la red durante los próximos 2 minutos.");
        } catch (IllegalArgumentException invalid) { report(invalid.getMessage()); }
        catch (RuntimeException unavailable) { recovery.pause(); report("No pude iniciar WebView. Comprueba que Android System WebView esté instalado y actualizado."); }
    }
    private void showReadings() {
        if (trace == null) return;
        trace.setVisibility(readings.isEmpty() ? View.GONE : View.VISIBLE);
        trace.setText(readings.isEmpty() ? "" : "Salve está leyendo:\n" + String.join("\n", readings));
    }
    /** YouTube en la ventana: busca y, si se pidió reproducir, abre el primer vídeo. */
    private void playMedia(String value, boolean autoplay) {
        if (reviewing || locked()) { report("Desbloquea el móvil para poner música."); return; }
        String url = salve.core.web.MediaRequest.searchUrl(value);
        mediaQuery = value; mediaAutoplay = autoplay; readings.clear(); showReadings();
        disposeBrowser(); createWebView();
        paused = false; pageFailed = false;
        recovery.begin(url, online(), SystemClock.elapsedRealtime());
        tab(true); collapse(false); releaseFocus(); query.setText(value);
        if (recovery.state() == BrowserRecovery.State.LOADING) { web.loadUrl(url); report("Buscando en YouTube: " + value); }
        else report("Sin conexión. Pondré «" + value + "» cuando vuelva la red.");
    }
    /** Los resultados de YouTube se dibujan tras cargar: se reintenta unos segundos. */
    private void findFirstVideo() {
        WebView view = web;
        if (view == null || destroyed || mediaQuery == null) return;
        view.evaluateJavascript("(function(){var a=document.querySelector('a[href*=\"/watch?v=\"]');return a?a.getAttribute('href'):'';})()", value -> {
            if (destroyed || web != view || mediaQuery == null) return;
            String id = salve.core.web.MediaRequest.videoId(value == null ? "" : value.replace("\\u0026", "&").replace("\"", ""));
            if (id != null) { report("Abriendo el primer resultado de «" + mediaQuery + "»…"); view.loadUrl("https://m.youtube.com/watch?v=" + id); return; }
            if (++mediaAttempts < 12) main.postDelayed(this::findFirstVideo, 700);
            else report("Estos son los resultados de «" + mediaQuery + "». Toca el que quieras.");
        });
    }
    private void startVideo() {
        WebView view = web;
        if (view == null) return;
        report("Sonando: " + mediaQuery + ". Puedes minimizar la ventana y seguir usando el móvil; × la cierra.");
        main.postDelayed(() -> { if (web == view && !destroyed)
            view.evaluateJavascript("(function(){var v=document.querySelector('video');if(v){v.play();return 'ok';}return '';})()", null); }, 1500);
    }
    private void observeEnvironment() {
        connectivity = getSystemService(ConnectivityManager.class);
        if (connectivity != null) try { connectivity.registerDefaultNetworkCallback(network); networkRegistered = true; } catch (RuntimeException ignored) { }
        IntentFilter filter = new IntentFilter(); filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON); filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, filter, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(screen, filter);
        receiverRegistered = true; main.post(clock);
    }
    private boolean online() {
        ConnectivityManager cm = connectivity == null ? getSystemService(ConnectivityManager.class) : connectivity;
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
    private void tick() {
        if (destroyed || panel == null || web == null || locked()) return;
        long now = SystemClock.elapsedRealtime();
        if (recovery.timedOut(now)) {
            pageFailed = true; recovery.failed(true); web.stopLoading(); nextRetry = now + 3000;
            report(recovery.state() == BrowserRecovery.State.WAITING_NETWORK ? "La carga agotó 25 segundos. Revisando la conexión para recuperar la búsqueda." : "La carga agotó 25 segundos. Pulsa Buscar para iniciar otra búsqueda.");
        }
        if (recovery.state() == BrowserRecovery.State.WAITING_NETWORK && now >= nextRetry) {
            if (recovery.retry(online(), now)) {
                // New view means delayed callbacks from a failed attempt cannot complete the next one.
                disposeBrowser();
                try { createWebView(); pageFailed = false; report("Recuperando búsqueda · intento " + recovery.retries() + "/2"); web.loadUrl(recovery.url()); }
                catch (RuntimeException unavailable) { recovery.pause(); report("No pude recuperar WebView. Pulsa Buscar para intentarlo de nuevo."); }
            } else if (recovery.state() == BrowserRecovery.State.FAILED) report("Recuperación detenida. Pulsa Buscar cuando quieras iniciar una nueva búsqueda.");
        }
    }
    private void pause() {
        paused = true; recovery.pause(); if (web != null) web.stopLoading(); pendingText = "";
        SalveAccessibilityService service = SalveAccessibilityService.getInstance();
        if (reviewing && service != null) service.cancelarConfirmacion();
        releaseFocus(); report("Panel pausado. Búsqueda y acción pendiente detenidas; pulsa Buscar para empezar de nuevo.");
    }
    private void actOnApp(boolean writing) {
        String payload = writing ? pendingText : label.getText().toString().trim();
        if (payload.isEmpty()) { report(writing ? "Pulsa Preparar texto antes de seleccionar el campo de destino." : "Escribe la etiqueta exacta del botón."); return; }
        withApp(service -> {
            String destination = service.getActivePackageName();
            if (destination == null || getPackageName().equals(destination)) { restorePanel(); report("Abre la app de destino y selecciona su campo; después repite la revisión."); return; }
            String description = writing ? "Reemplazar el campo seleccionado por:\n\n" + payload
                    : "Tocar el botón exacto «" + payload + "».\nSi es Enviar, comprueba el destinatario y el mensaje visibles en la aplicación. El toque puede enviar el mensaje.";
            service.solicitarConfirmacion(destination, description, approved -> {
                if (destroyed) return;
                boolean accepted = false;
                if (approved) accepted = writing ? service.escribirTextoEnPantalla(destination, payload) : service.tocarTextoExacto(destination, payload, null);
                if (approved) { if (writing) pendingText = ""; else label.setText(""); }
                restorePanel();
                report(!approved ? "Revisión cancelada, caducada o pantalla modificada. Selecciona de nuevo el destino y revisa otra vez."
                        : accepted ? "Android aceptó la acción. Comprueba el resultado en la aplicación. No se repetirá automáticamente."
                        : "No pude aplicar la acción: cambió el destino, falta un campo o la etiqueta no es única. Vuelve a preparar y revisar.");
            });
        });
    }
    private void readScreen() {
        withApp(service -> { String result = service.escanearPantallaParaLLM(); restorePanel(); observation.setText(result); report("Pantalla leída a petición tuya. El texto queda en este panel hasta cerrarlo o leer otra pantalla."); });
    }
    private void global(int action) {
        withApp(service -> { boolean accepted = service.ejecutarAccionGlobal(action); restorePanel(); report(accepted ? "Android aceptó la navegación." : "Control pausado, móvil bloqueado o acción no disponible."); });
    }
    private void withApp(java.util.function.Consumer<SalveAccessibilityService> action) {
        if (reviewing) return;
        SalveAccessibilityService service = SalveAccessibilityService.getInstance();
        if (service == null || !service.isControlEnabled()) { report("Activa Sistema Motor de Salve en Accesibilidad y los controles en Mi móvil."); return; }
        if (locked()) return;
        reviewing = true; releaseFocus(); detach();
        // Our overlay must not become the destination. The existing guard re-reads app/window/focus.
        main.postDelayed(() -> {
            if (destroyed) return;
            if (locked() || SalveAccessibilityService.getInstance() != service) { restorePanel(); report("La app o el servicio dejó de estar disponible. Repite la revisión."); return; }
            try { action.accept(service); } catch (RuntimeException unavailable) { restorePanel(); report("La pantalla dejó de estar disponible. No repito la acción."); }
        }, 250);
    }
    private void restorePanel() {
        reviewing = false;
        if (!destroyed) try { attach(); visibility(); } catch (RuntimeException unavailable) { stopSelf(); }
    }
    private void externalBrowser() {
        String url = web == null ? "" : web.getUrl();
        if (!MobileBrowserPolicy.allowed(url)) { report("Primero abre una búsqueda HTTPS."); return; }
        try { recovery.pause(); startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); collapse(true); }
        catch (RuntimeException unavailable) { report("No hay un navegador disponible para esta página."); }
    }
    private void openDevices() {
        try { releaseFocus(); collapse(true); startActivity(new Intent(this, DeviceControlActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException unavailable) { report("No pude abrir Mi móvil."); }
    }
    private boolean locked() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        PowerManager power = getSystemService(PowerManager.class);
        return keyguard == null || keyguard.isKeyguardLocked() || power == null || !power.isInteractive();
    }
    private void visibility() {
        if (panel == null) return;
        boolean hidden = locked(); panel.setVisibility(hidden ? View.GONE : View.VISIBLE);
        if (hidden) { pause(); if (web != null) web.onPause(); }
        else if (web != null) web.onResume();
    }
    private void focus() {
        if (layout == null || !added) return;
        layout.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; update();
    }
    private void releaseFocus() {
        if (layout == null) return;
        InputMethodManager input = getSystemService(InputMethodManager.class);
        if (input != null && panel != null) input.hideSoftInputFromWindow(panel.getWindowToken(), 0);
        panel.clearFocus(); layout.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; update();
    }
    private void tab(boolean browser) { browserPage.setVisibility(browser ? View.VISIBLE : View.GONE); controlPage.setVisibility(browser ? View.GONE : View.VISIBLE); releaseFocus(); }
    private void collapse(boolean value) { collapsed = value; body.setVisibility(value ? View.GONE : View.VISIBLE); releaseFocus(); bounds(); update(); }
    private void bounds() {
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics(); manager.getDefaultDisplay().getRealMetrics(metrics);
        int left = 0, top = dp(28), right = 0, bottom = dp(32), width = metrics.widthPixels, height = metrics.heightPixels;
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowMetrics current = manager.getCurrentWindowMetrics(); Rect area = current.getBounds(); width = area.width(); height = area.height();
            android.graphics.Insets bars = current.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
        }
        layout.width = Math.min(dp(collapsed ? 270 : 350), Math.max(dp(100), width - left - right));
        layout.height = Math.min(dp(collapsed ? 60 : 530), Math.max(dp(60), height - top - bottom));
        layout.x = Math.max(left, Math.min(layout.x, width - right - layout.width));
        layout.y = Math.max(top, Math.min(layout.y, height - bottom - layout.height));
    }
    private void installDrag(View handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            float x, y; int startX, startY;
            @Override public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { x = event.getRawX(); y = event.getRawY(); startX = layout.x; startY = layout.y; releaseFocus(); return true; }
                if (event.getActionMasked() == MotionEvent.ACTION_MOVE) { layout.x = startX + Math.round(event.getRawX() - x); layout.y = startY + Math.round(event.getRawY() - y); bounds(); update(); return true; }
                return event.getActionMasked() == MotionEvent.ACTION_UP;
            }
        });
    }
    private void attach() { if (!added && !destroyed) { manager.addView(panel, layout); added = true; } }
    private void detach() { if (added) { try { manager.removeView(panel); } catch (RuntimeException ignored) { } added = false; } }
    private void update() { if (added && !destroyed) try { manager.updateViewLayout(panel, layout); } catch (RuntimeException gone) { stopSelf(); } }
    private void report(String value) { if (status != null && !destroyed) status.setText(value); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row(LinearLayout parent) { LinearLayout view = new LinearLayout(this); parent.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view; }
    private TextView message(LinearLayout parent, String text) { TextView view = new TextView(this); view.setText(text); view.setTextColor(Color.WHITE); view.setTextSize(14); parent.addView(view); return view; }
    private EditText input(LinearLayout parent, String hint, int max) {
        EditText view = new EditText(this); view.setTextColor(Color.WHITE); view.setHintTextColor(Color.LTGRAY); view.setHint(hint); view.setTextSize(14); view.setMaxLines(2);
        view.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)}); view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        view.setOnTouchListener((v, event) -> { if (event.getActionMasked() == MotionEvent.ACTION_DOWN) focus(); return false; });
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    private Button button(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false); button.setTextSize(12); button.setMinWidth(0); button.setMinimumWidth(0); button.setPadding(dp(3), 0, dp(3), 0);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, parent.getOrientation() == LinearLayout.HORIZONTAL ? new LinearLayout.LayoutParams(0, dp(48), 1) : new LinearLayout.LayoutParams(-1, dp(48)));
        return button;
    }
    private void disposeBrowser() {
        WebView old = web; web = null;
        if (old != null) { browserPage.removeView(old); old.stopLoading(); old.destroy(); }
    }
    @Override public void onConfigurationChanged(Configuration configuration) { super.onConfigurationChanged(configuration); if (layout != null) { bounds(); update(); } }
    @Override public void onDestroy() {
        destroyed = true; main.removeCallbacksAndMessages(null); recovery.pause(); salve.core.web.SearchLive.unregister(live);
        if (receiverRegistered) unregisterReceiver(screen);
        if (networkRegistered) try { connectivity.unregisterNetworkCallback(network); } catch (RuntimeException ignored) { }
        if (reviewing && SalveAccessibilityService.getInstance() != null) SalveAccessibilityService.getInstance().cancelarConfirmacion();
        detach(); disposeBrowser(); pendingText = ""; stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
