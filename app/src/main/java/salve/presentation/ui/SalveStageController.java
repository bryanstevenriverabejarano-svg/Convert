package salve.presentation.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.salve.app.R;
import java.util.ArrayList;
import salve.avatar.AvatarView;
import salve.sketch.SketchLibrary;
import salve.sketch.SketchProtocol;
import salve.sketch.SketchScene;
import salve.sketch.SketchView;
import salve.sketch.Sketches;
import salve.sketch.StoryIllustrator;

/** Presentation-only state. The conversation engine remains the single owner of memory and voice. */
final class SalveStageController {
    private final AppCompatActivity activity;
    private final View stage, scrim, chat, options, reflections;
    private final EditText composer;
    private final TextView transcript, caption;
    private final NestedScrollView scroll;
    private final BottomSheetBehavior<View> chatBehavior, optionsBehavior;
    private final ArrayList<String> messages = new ArrayList<>();
    /** Hora de cada mensaje (0 = desconocida, p. ej. restaurado de una versión anterior). */
    private final ArrayList<Long> times = new ArrayList<>();
    private String active = "";
    private boolean keyboardWanted;
    private boolean chatFullScreen;
    private final ImageButton expandChat;
    private final OnBackPressedCallback back = new OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { closePanels(); }
    };

    SalveStageController(AppCompatActivity activity, Bundle saved) {
        this.activity = activity;
        stage = activity.findViewById(R.id.salveStage);
        scrim = activity.findViewById(R.id.stageScrim);
        chat = activity.findViewById(R.id.chatSheet);
        options = activity.findViewById(R.id.optionsSheet);
        reflections = activity.findViewById(R.id.panelReflexion);
        composer = activity.findViewById(R.id.inputChat);
        transcript = activity.findViewById(R.id.chatTranscript);
        caption = activity.findViewById(R.id.salveCaption);
        scroll = activity.findViewById(R.id.chatScroll);
        expandChat = activity.findViewById(R.id.btnExpandChat);
        chatBehavior = configure(chat, "chat");
        optionsBehavior = configure(options, "options");
        activity.getOnBackPressedDispatcher().addCallback(activity, back);
        // Cuando el repertorio de trazos termina de cargar, los dibujos pendientes aparecen.
        Runnable sketchesChanged = () -> { if (!activity.isFinishing()) renderMessages(); };
        Sketches.addListener(sketchesChanged);
        activity.getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
            @Override public void onDestroy(androidx.lifecycle.LifecycleOwner owner) { Sketches.removeListener(sketchesChanged); }
        });
        AvatarView avatar = activity.findViewById(R.id.imagenSalve);
        avatar.setImmersiveMode(true);
        avatar.setOnClickListener(v -> showChat(true));
        avatar.setOnLongClickListener(v -> { showOptions(); return true; });
        caption.setOnClickListener(v -> showChat(true));
        caption.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        activity.findViewById(R.id.btnStageOptions).setOnClickListener(v -> showOptions());
        activity.findViewById(R.id.btnCloseChat).setOnClickListener(v -> closePanels());
        expandChat.setOnClickListener(v -> setChatExpanded(!chatFullScreen));
        activity.findViewById(R.id.chatDragHandle).setOnClickListener(v -> cycleChatHeight());
        // El clip de la caja de texto abre lo mismo que la tarjeta «Documentos».
        activity.findViewById(R.id.btnAttachInline).setOnClickListener(v -> activity.findViewById(R.id.btnAdjuntar).performClick());
        // El halo se centra a la altura del pecho de Salve, sea cual sea la pantalla.
        View halo = activity.findViewById(R.id.salveHalo);
        activity.findViewById(R.id.avatarFrame).addOnLayoutChangeListener((frame, l, t, r, b, ol, ot, or, ob) ->
                halo.setTranslationY(Math.max(0, (b - t) * 0.30f - halo.getHeight() / 2f)));
        activity.findViewById(R.id.btnCloseOptions).setOnClickListener(v -> closePanels());
        activity.findViewById(R.id.btnMenuChat).setOnClickListener(v -> showChat(true));
        activity.findViewById(R.id.btnMenuVoice).setOnClickListener(v -> {
            closePanels(); activity.findViewById(R.id.btnHablar).performClick();
        });
        scrim.setOnClickListener(v -> closePanels());
        if (saved != null) {
            ArrayList<String> restored = saved.getStringArrayList("stage_messages");
            long[] restoredTimes = saved.getLongArray("stage_times");
            if (restored != null) {
                for (int i = 0; i < restored.size(); i++) {
                    messages.add(restored.get(i)); serials.add(++serialCounter);
                    times.add(restoredTimes != null && i < restoredTimes.length ? restoredTimes[i] : 0L);
                }
                restoring = true; trimMessages(); renderMessages(); restoring = false;
            }
            CharSequence last = saved.getCharSequence("stage_caption");
            if (last != null) caption.setText(last);
            String restorePanel = saved.getString("stage_panel", "");
            // Let the hierarchy finish restoring before positioning a sheet.
            stage.post(() -> {
                if ("chat".equals(restorePanel)) {
                    showChat(false);
                    if (saved.getBoolean("stage_chat_expanded", false)) setChatExpanded(true);
                }
                else if ("options".equals(restorePanel)) showOptions();
            });
        }
        installKeyboardInsets();
        activity.findViewById(R.id.mainLayout).addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
            float density = activity.getResources().getDisplayMetrics().density;
            int usable = b - t - topInset;
            float heightDp = (usable - keyboardInset) / density;
            // BottomSheetBehavior adds its own top offset. Use height, never a top margin,
            // otherwise a relayout can push the composer below the window.
            fitSheetHeight(chat, usable);
            fitSheetHeight(options, usable - (heightDp < 480 ? 0 : Math.round(96 * density)));
            chatBehavior.setPeekHeight(Math.min(dp(290), Math.max(dp(220), Math.round(usable * 0.32f))));
            availableHeightDp = heightDp;
            updateChatExtras();
        });
        updateChrome();
    }

    private int topInset, keyboardInset;
    private boolean keyboardVisible;
    private float availableHeightDp = 700;
    
    private void updateChatExtras() {
        boolean compact = "chat".equals(active) && chatBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;
        activity.findViewById(R.id.chatActions).setVisibility(
                !compact && !keyboardVisible && availableHeightDp >= 360 ? View.VISIBLE : View.GONE);
        activity.findViewById(R.id.chatSuggestions).setVisibility(
                !compact && !keyboardVisible && availableHeightDp >= 450 ? View.VISIBLE : View.GONE);
        activity.findViewById(R.id.chatResizeHint).setVisibility(
                !compact && !keyboardVisible && availableHeightDp >= 450 ? View.VISIBLE : View.GONE);
    }

    /**
     * El teclado nunca tapa lo que escribes: la app dibuja de borde a borde y mide ella misma las
     * barras del sistema y el teclado. La hoja del chat reserva abajo exactamente el alto del teclado,
     * así la caja de texto queda siempre encima y ves lo que tecleas (como en otras apps de chat).
     * Antes dependía de que Android encogiera la ventana (adjustResize), y con la hoja deslizante no
     * siempre lo hacía.
     */
    private void installKeyboardInsets() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View root = activity.findViewById(R.id.mainLayout);
        root.setFitsSystemWindows(false);
        float density = activity.getResources().getDisplayMetrics().density;
        int base = Math.round(16 * density);
        View overlay = activity.findViewById(R.id.downloadOverlayContainer);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            boolean wasVisible = keyboardVisible;
            keyboardVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) && ime.bottom > bars.bottom;
            topInset = bars.top;
            chatBehavior.setExpandedOffset(bars.top);
            keyboardInset = keyboardVisible ? ime.bottom - bars.bottom : 0;
            // Con el teclado presente, la caja de texto nunca queda por debajo de media pantalla.
            if (keyboardVisible && "chat".equals(active)
                    && chatBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                chatBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            int bottom = Math.max(bars.bottom, ime.bottom);
            stage.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            chat.setPadding(base + bars.left, base, base + bars.right, base + bottom);
            options.setPadding(base + bars.left, base, base + bars.right, base + bars.bottom);
            if (reflections != null) reflections.setPadding(reflections.getPaddingLeft(), reflections.getPaddingTop(),
                    reflections.getPaddingRight(), Math.round(20 * density) + bottom);
            if (overlay != null) overlay.setPadding(bars.left, bars.top, bars.right, bottom);
            updateChatExtras();
            // Al abrir el teclado, el último mensaje sigue a la vista justo encima de lo que escribes.
            if (keyboardVisible && !wasVisible) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            v.requestLayout();
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(root);
    }

    private void fitSheetHeight(View sheet, int height) {
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        height = Math.max(0, height);
        if (params.height != height) { params.height = height; sheet.setLayoutParams(params); }
    }

    private BottomSheetBehavior<View> configure(View sheet, String name) {
        sheet.setSaveEnabled(false);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        if ("chat".equals(name)) {
            behavior.setFitToContents(false);
            behavior.setHalfExpandedRatio(0.58f);
            behavior.setExpandedOffset(topInset);
            behavior.setDraggable(true);
            behavior.setPeekHeight(dp(250));
        }
        behavior.setHideable(true);
        behavior.setSkipCollapsed(!"chat".equals(name));
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(View view, int state) {
                if (state == BottomSheetBehavior.STATE_HIDDEN && active.equals(name)) {
                    active = ""; hideKeyboard(); updateChrome();
                }
                // El teclado sólo se puede abrir cuando el panel ya terminó de subir.
                if ("chat".equals(name)) {
                    if (state == BottomSheetBehavior.STATE_EXPANDED
                            || state == BottomSheetBehavior.STATE_HALF_EXPANDED
                            || state == BottomSheetBehavior.STATE_COLLAPSED) {
                        chatFullScreen = state == BottomSheetBehavior.STATE_EXPANDED;
                        updateExpandButton();
                        updateChatExtras();
                        if (chatFullScreen && keyboardWanted) focusComposer();
                    }
                }
            }
            @Override public void onSlide(View view, float offset) { }
        });
        return behavior;
    }

    boolean hasOpenPanel() { return !active.isEmpty(); }

    void showVoiceDetails() {
        showChat(false); // en modo voz no se abre el teclado
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    void setVoiceDetails(CharSequence text) {
        TextView details = activity.findViewById(R.id.voiceDetails);
        details.setText(text);
        details.setVisibility(View.VISIBLE);
    }

    void showChat(boolean keyboard) {
        boolean alreadyOpen = "chat".equals(active);
        if (!alreadyOpen) {
            closePanels();
            active = "chat";
            // El chat se abre dejando ver a Salve. Si el usuario quiere escribir,
            // se expande solo: el teclado jamás tapa el compositor.
            chatBehavior.setState(keyboard
                    ? BottomSheetBehavior.STATE_EXPANDED
                    : BottomSheetBehavior.STATE_HALF_EXPANDED);
        } else if (keyboard) {
            chatBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        keyboardWanted = keyboard;
        updateChrome();
        scrollToEnd();
        if (keyboard) {
            composer.post(this::focusComposer);
            composer.postDelayed(this::focusComposer, 350);
        }
    }

    private void cycleChatHeight() {
        if (chatBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            setChatExpanded(false);
        } else {
            setChatExpanded(!chatFullScreen);
        }
    }

    private void setChatExpanded(boolean expanded) {
        if (!"chat".equals(active)) return;
        keyboardWanted = false;
        if (!expanded) hideKeyboard();
        chatBehavior.setState(expanded
                ? BottomSheetBehavior.STATE_EXPANDED
                : BottomSheetBehavior.STATE_HALF_EXPANDED);
    }

    private void updateExpandButton() {
        expandChat.setImageResource(chatFullScreen ? R.drawable.ic_salve_shrink : R.drawable.ic_salve_expand);
        expandChat.setContentDescription(activity.getString(chatFullScreen
                ? R.string.salve_chat_shrink : R.string.salve_chat_expand));
        TextView hint = activity.findViewById(R.id.chatResizeHint);
        hint.setText(chatFullScreen ? R.string.salve_chat_full_hint : R.string.salve_chat_drag_hint);
        hint.setCompoundDrawablesRelativeWithIntrinsicBounds(
                chatFullScreen ? R.drawable.ic_salve_chevron_down : R.drawable.ic_salve_chevron_up, 0, 0, 0);
        updateScrim();
    }

    /** Cursor en la caja de texto y teclado abierto (sólo con el chat a la vista). */
    void focusComposer() {
        if (!"chat".equals(active) || activity.isFinishing()) return;
        composer.requestFocus();
        composer.setSelection(composer.getText() == null ? 0 : composer.getText().length());
        androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(), composer)
                .show(androidx.core.view.WindowInsetsCompat.Type.ime());
        InputMethodManager ime = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (ime != null) ime.showSoftInput(composer, InputMethodManager.SHOW_IMPLICIT);
    }

    private void showOptions() {
        closePanels(); active = "options";
        optionsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        updateChrome();
    }

    void showReflections() {
        closePanels(); active = "reflections";
        reflections.setVisibility(View.VISIBLE);
        updateChrome();
    }

    void closePanels() {
        active = "";
        chatBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        optionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        reflections.setVisibility(View.GONE);
        keyboardWanted = false;
        hideKeyboard(); updateChrome();
    }

    private void hideKeyboard() {
        composer.clearFocus();
        InputMethodManager ime = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (ime != null) ime.hideSoftInputFromWindow(composer.getWindowToken(), 0);
    }

    private void updateChrome() {
        boolean open = !active.isEmpty();
        back.setEnabled(open);
        scrim.setVisibility(open ? View.VISIBLE : View.GONE);
        updateScrim();
        stage.setImportantForAccessibility(open ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        chat.setImportantForAccessibility("chat".equals(active) ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        options.setImportantForAccessibility("options".equals(active) ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        updateChatExtras();
    }

    /**
     * Con el chat a media pantalla Salve se ve entera y luminosa por encima (como en el diseño): la
     * cortina sigue recogiendo el toque para cerrar, pero no oscurece. En las demás hojas sí oscurece.
     */
    private void updateScrim() {
        scrim.setAlpha("chat".equals(active) && !chatFullScreen ? 0f : 1f);
    }

    /** Borrador de Salve mientras escribe; no se guarda en el historial ni sobrevive a una recreación. */
    private String draft;

    /** Muestra (o quita, con null) lo que Salve está escribiendo ahora mismo. */
    void showDraft(String text) {
        String next = text == null || text.trim().isEmpty() ? null : text;
        if (next == null && draft == null) return;
        if (next != null) {
            // Mientras escribe un cuento, cada párrafo terminado que nombra algo dibujable se ilustra ya:
            // ella dibuja esa parte mientras sigue escribiendo la siguiente.
            SketchLibrary library = Sketches.loaded();
            if (library != null) next = StoryIllustrator.annotate(next, library, true);
        }
        if (next != null) {
            View row = activity.findViewById(R.id.thinkingRow);
            if (row != null && row.getVisibility() == View.VISIBLE) showThinking("Escribiendo…");
        }
        draft = next == null ? null : next.length() > 12000 ? next.substring(0, 12000) + "…" : next;
        renderMessages();
        if (draft != null) {
            String plain = plain(draft);
            caption.setText(plain.length() > 180 ? "…" + plain.substring(plain.length() - 180) : plain);
            scrollToEnd();
        }
    }

    // ───────── Salve pensando: puntos de luz y lo que hace ahora ─────────
    private final android.os.Handler thinkingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable thinkingTimeout = this::hideThinking;

    /** Muestra que Salve está trabajando y en qué («Buscando referencias de «gorila»…»). */
    void showThinking(String status) {
        View row = activity.findViewById(R.id.thinkingRow);
        TextView label = activity.findViewById(R.id.thinkingLabel);
        if (row == null || label == null) return;
        String text = status == null || status.trim().isEmpty() ? "Pensando…" : status.trim();
        label.setText(text);
        label.setContentDescription("Salve: " + text);
        if (row.getVisibility() != View.VISIBLE) { row.setVisibility(View.VISIBLE); scrollToEnd(); }
        thinkingHandler.removeCallbacks(thinkingTimeout);
        thinkingHandler.postDelayed(thinkingTimeout, 2 * 60 * 1000L); // sin novedades en 2 min, se quita
    }

    /** Actualiza lo que hace, sólo si ya está pensando en algo que pidió Bryan. */
    void updateThinking(String status) {
        View row = activity.findViewById(R.id.thinkingRow);
        if (row != null && row.getVisibility() == View.VISIBLE) showThinking(status);
    }

    void hideThinking() {
        thinkingHandler.removeCallbacks(thinkingTimeout);
        View row = activity.findViewById(R.id.thinkingRow);
        if (row != null) row.setVisibility(View.GONE);
    }

    void appendMessage(boolean fromSalve, String text) {
        if (fromSalve) hideThinking();
        if (text == null || text.trim().isEmpty()) { if (fromSalve) { draft = null; renderMessages(); } return; }
        // Bound recreation state so a long session cannot exceed the Android Binder limit.
        String visible = text.length() > 12000 ? text.substring(0, 12000) + "…" : text;
        long serial = ++serialCounter;
        if (fromSalve) {
            // Los dibujos que ya se trazaban en el borrador continúan sin reiniciarse en el mensaje final.
            int ordinal = 0;
            java.util.Set<String> drafted = new java.util.HashSet<>();
            for (String[] seg : SketchProtocol.segments(visible)) {
                if (!"sketch".equals(seg[0])) continue;
                SketchView view = sketchViews.remove("draft:" + seg[1]);
                if (view != null) { sketchViews.put(serial + ":" + ordinal, view); drafted.add(seg[1]); }
                ordinal++;
            }
            draft = null;
            sketchViews.keySet().removeIf(id -> id.startsWith("draft:"));
            int visibleSegments = firstPendingSketch(SketchProtocol.segments(visible), drafted);
            if (visibleSegments >= 0) { reveal.put(serial, visibleSegments); paceTimeout(serial, visibleSegments); }
        }
        messages.add((fromSalve ? "Salve" : "Tú") + "\n" + visible);
        serials.add(serial);
        times.add(System.currentTimeMillis());
        trimMessages(); renderMessages();
        if (fromSalve && keyboardWanted && "chat".equals(active)) composer.postDelayed(this::focusComposer, 120);
        if (fromSalve) {
            String plain = plain(text);
            caption.setText(plain.length() > 180 ? plain.substring(0, 180) + "…" : plain);
        }
        scrollToEnd();
    }

    /** Segmentos visibles al llegar: hasta el primer dibujo nuevo incluido (-1 = todo visible). */
    private static int firstPendingSketch(java.util.List<String[]> segments, java.util.Set<String> drafted) {
        for (int i = 0; i < segments.size(); i++)
            if ("sketch".equals(segments.get(i)[0]) && !drafted.contains(segments.get(i)[1]))
                return i + 1 < segments.size() ? i + 1 : -1;
        return -1;
    }

    private void trimMessages() {
        int chars = 0;
        for (String message : messages) chars += message.length();
        while (messages.size() > 40 || chars > 48000) {
            chars -= messages.remove(0).length();
            if (!times.isEmpty()) times.remove(0);
            long gone = serials.remove(0);
            reveal.remove(gone);
            String prefix = gone + ":";
            sketchViews.keySet().removeIf(id -> id.startsWith(prefix));
        }
    }

    // ───────── hilo de conversación: bloques de texto y lienzos de dibujo ─────────
    private final ArrayList<Long> serials = new ArrayList<>();
    private long serialCounter;
    /** Segmentos visibles de un mensaje que aún está contando (el resto espera al dibujo anterior). */
    private final java.util.Map<Long, Integer> reveal = new java.util.HashMap<>();
    private final java.util.Map<String, SketchView> sketchViews = new java.util.HashMap<>();
    private final java.util.Map<String, SketchScene> scenes = new java.util.HashMap<>();
    private final java.util.Set<String> staged = new java.util.HashSet<>();
    private final ArrayList<TextView> textBlocks = new ArrayList<>();
    private boolean restoring;

    private void renderMessages() {
        ViewGroup thread = activity.findViewById(R.id.chatThread);
        if (textBlocks.isEmpty()) textBlocks.add(transcript);
        if (messages.isEmpty() && draft == null) {
            styleBubble(transcript, true);
            transcript.setText(R.string.salve_chat_empty);
            MessageRow welcome = rowFor(transcript, true);
            welcome.show(true, null);
            if (thread.getChildCount() != 1 || thread.getChildAt(0) != welcome.root) {
                thread.removeAllViews();
                if (welcome.root.getParent() instanceof ViewGroup) ((ViewGroup) welcome.root.getParent()).removeView(welcome.root);
                thread.addView(welcome.root);
            }
            return;
        }
        java.util.List<View> desired = new ArrayList<>();
        int[] textIndex = {0};
        java.util.List<String> shown = new ArrayList<>(messages);
        if (draft != null) shown.add("Salve · escribiendo\n" + draft + " ▍");
        for (int m = 0; m < shown.size(); m++) {
            boolean isDraft = m == messages.size();
            String message = shown.get(m);
            int heading = message.indexOf('\n');
            String head = heading > 0 ? message.substring(0, heading) : message;
            boolean fromSalve = head.startsWith("Salve");
            // Quién habla lo dicen el lado, el color y la cara de Salve; la burbuja lleva solo el mensaje.
            SpannableStringBuilder text = new SpannableStringBuilder();
            firstOfMessage = true;
            lastRowOfMessage = null;
            String body = heading > 0 ? message.substring(heading + 1) : "";
            long serial = isDraft ? -1 : serials.get(m);
            Integer limit = isDraft ? null : reveal.get(serial);
            java.util.List<String[]> segments = SketchProtocol.segments(body);
            int ordinal = 0;
            for (int i = 0; i < segments.size(); i++) {
                if (limit != null && i >= limit) break;
                String[] seg = segments.get(i);
                if ("text".equals(seg[0])) {
                    text.append(seg[1].replaceAll("^\\n+|\\n+$", ""));
                    continue;
                }
                SketchScene scene = scene(seg[1]);
                String id = isDraft ? "draft:" + seg[1] : serial + ":" + ordinal;
                ordinal++;
                if (scene == null) continue;
                flushText(text, desired, textIndex, fromSalve);
                lastRowOfMessage = null; // tras un dibujo, la hora ya no va en el bloque anterior
                SketchView view = sketchViews.get(id);
                if (view == null) {
                    view = new SketchView(activity);
                    sketchViews.put(id, view);
                    view.bind(scene, restoring, finished -> onSketchFinished(id));
                    if (!restoring && staged.add(id)) Sketches.show(scene);
                } else view.bind(scene, false, finished -> onSketchFinished(id));
                desired.add(view);
            }
            flushText(text, desired, textIndex, fromSalve);
            // La hora va bajo el último bloque de texto del mensaje (como en cualquier app de mensajería).
            if (lastRowOfMessage != null) {
                long when = isDraft ? 0L : m < times.size() ? times.get(m) : 0L;
                lastRowOfMessage.show(lastRowOfMessage.avatarShown, when > 0 ? timeLabel(when, fromSalve) : null);
            }
        }
        // Reutiliza vistas y dibujos existentes para no reiniciar animaciones.
        int same = 0;
        while (same < desired.size() && same < thread.getChildCount()
                && thread.getChildAt(same) == desired.get(same)) same++;
        if (thread.getChildCount() > same) thread.removeViews(same, thread.getChildCount() - same);
        for (int i = same; i < desired.size(); i++) {
            View v = desired.get(i);
            if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
            thread.addView(v);
        }
    }

    private int dp(float n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }

    private void styleBubble(TextView block, boolean fromSalve) {
        // Evita recrear el fondo/medidas en cada token del texto progresivo.
        if (Boolean.valueOf(fromSalve).equals(block.getTag())) return;
        block.setTag(Boolean.valueOf(fromSalve));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = fromSalve ? Gravity.START : Gravity.END;
        params.setMargins(dp(2), dp(6), dp(2), dp(4));
        block.setLayoutParams(params);
        block.setMaxWidth(Math.max(dp(200), activity.getResources().getDisplayMetrics().widthPixels - dp(fromSalve ? 120 : 96)));
        block.setBackgroundResource(fromSalve ? R.drawable.salve_chat_bubble_salve : R.drawable.salve_chat_bubble_user);
        block.setTextColor(fromSalve ? 0xFFE8F2F6 : 0xFFFFFFFF);
        block.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17);
        block.setPadding(dp(18), dp(14), dp(18), dp(14));
        block.setTextIsSelectable(true);
    }

    private void flushText(SpannableStringBuilder text, java.util.List<View> desired,
                           int[] index, boolean fromSalve) {
        if (text.length() == 0) return;
        TextView block;
        if (index[0] < textBlocks.size()) block = textBlocks.get(index[0]);
        else {
            block = new TextView(activity);
            block.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, transcript.getTextSize());
            block.setLineSpacing(transcript.getLineSpacingExtra(), transcript.getLineSpacingMultiplier());
            textBlocks.add(block);
        }
        styleBubble(block, fromSalve);
        index[0]++;
        CharSequence value = new SpannableStringBuilder(text);
        if (!value.toString().equals(block.getText().toString())) block.setText(value);
        MessageRow row = rowFor(block, fromSalve);
        row.show(fromSalve && firstOfMessage, null);
        firstOfMessage = false;
        lastRowOfMessage = row;
        desired.add(row.root);
        text.clear();
        text.clearSpans();
    }

    // ───────── filas de mensaje: cara de Salve, burbuja y hora ─────────
    private boolean firstOfMessage;
    private MessageRow lastRowOfMessage;
    private final java.util.Map<TextView, MessageRow> rows = new java.util.IdentityHashMap<>();

    /** Una fila del chat: [cara de Salve] [burbuja + hora] a la izquierda, o [burbuja + hora ✓✓] a la derecha. */
    private final class MessageRow {
        final LinearLayout root, column;
        final ImageView avatar;
        final TextView time;
        boolean fromSalve, avatarShown;

        MessageRow(TextView bubble) {
            root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            avatar = new ImageView(activity);
            avatar.setImageResource(R.drawable.salve_chat_avatar);
            avatar.setContentDescription("Salve");
            LinearLayout.LayoutParams face = new LinearLayout.LayoutParams(dp(42), dp(42));
            face.setMargins(0, dp(8), dp(8), 0);
            avatar.setLayoutParams(face);
            column = new LinearLayout(activity);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            time = new TextView(activity);
            time.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            time.setTextColor(0xFF8FA6B5);
            time.setVisibility(View.GONE);
            if (bubble.getParent() instanceof ViewGroup) ((ViewGroup) bubble.getParent()).removeView(bubble);
            column.addView(bubble);
            column.addView(time);
            root.addView(avatar);
            root.addView(column);
        }

        void bind(boolean salve) {
            fromSalve = salve;
            avatar.setVisibility(salve ? View.INVISIBLE : View.GONE);
            column.setGravity(salve ? Gravity.START : Gravity.END);
            root.setPadding(salve ? 0 : dp(48), 0, salve ? dp(24) : 0, 0);
            LinearLayout.LayoutParams t = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            t.gravity = salve ? Gravity.START : Gravity.END;
            t.setMargins(salve ? dp(14) : 0, 0, salve ? 0 : dp(10), dp(6));
            time.setLayoutParams(t);
        }

        /** @param face cara de Salve visible (primer bloque de su mensaje); @param stamp hora, o null para ocultarla. */
        void show(boolean face, CharSequence stamp) {
            avatarShown = face;
            if (fromSalve) avatar.setVisibility(face ? View.VISIBLE : View.INVISIBLE);
            if (stamp == null) time.setVisibility(View.GONE);
            else { time.setText(stamp); time.setVisibility(View.VISIBLE); }
        }
    }

    private MessageRow rowFor(TextView bubble, boolean fromSalve) {
        MessageRow row = rows.get(bubble);
        if (row == null) { row = new MessageRow(bubble); rows.put(bubble, row); row.bind(fromSalve); }
        else if (row.fromSalve != fromSalve) row.bind(fromSalve);
        return row;
    }

    /** «4:03 p. m.» según el formato del teléfono; los mensajes de Bryan llevan ✓✓ como enviados. */
    private CharSequence timeLabel(long when, boolean fromSalve) {
        String clock = android.text.format.DateFormat.getTimeFormat(activity).format(new java.util.Date(when));
        if (fromSalve) return clock;
        SpannableStringBuilder label = new SpannableStringBuilder(clock).append("  ✓✓");
        label.setSpan(new ForegroundColorSpan(0xFF6ED6FF), clock.length() + 2, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        label.setSpan(new StyleSpan(Typeface.BOLD), clock.length() + 2, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return label;
    }

    private int sketchVersion = -1;

    private SketchScene scene(String key) {
        if (sketchVersion != Sketches.version()) { scenes.clear(); sketchVersion = Sketches.version(); }
        SketchScene cached = scenes.get(key);
        if (cached != null) return cached;
        SketchLibrary library = Sketches.loaded();
        SketchScene made = library == null ? null : SketchScene.fromKey(library, key);
        if (made != null) scenes.put(key, made);
        return made;
    }

    /** Terminado un dibujo, el relato sigue hasta el siguiente. */
    private void onSketchFinished(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || id.startsWith("draft:")) return;
        long serial;
        try { serial = Long.parseLong(id.substring(0, colon)); } catch (NumberFormatException e) { return; }
        Integer limit = reveal.get(serial);
        int index = serials.indexOf(serial);
        if (limit == null || index < 0) return;
        String message = messages.get(index);
        java.util.List<String[]> segments = SketchProtocol.segments(message.substring(message.indexOf('\n') + 1));
        int next = -1;
        for (int i = limit; i < segments.size(); i++) if ("sketch".equals(segments.get(i)[0])) { next = i + 1; break; }
        if (next < 0 || next >= segments.size()) reveal.remove(serial);
        else { reveal.put(serial, next); paceTimeout(serial, next); }
        renderMessages();
        scrollToEnd();
    }

    /** Si nadie mira el chat (o el dibujo queda fuera de pantalla), el relato no se queda esperando. */
    private void paceTimeout(long serial, int limit) {
        chat.postDelayed(() -> {
            Integer current = reveal.get(serial);
            if (current == null || current != limit) return;
            int index = serials.indexOf(serial);
            if (index < 0) return;
            String message = messages.get(index);
            java.util.List<String[]> segments = SketchProtocol.segments(message.substring(message.indexOf('\n') + 1));
            int ordinal = -1;
            for (int i = 0; i < limit && i < segments.size(); i++) if ("sketch".equals(segments.get(i)[0])) ordinal++;
            onSketchFinished(serial + ":" + Math.max(0, ordinal));
        }, 9500);
    }

    private static String plain(String text) {
        return text.replaceAll("\\s*\\[\\[salve_draw:[^\\]]*\\]\\]\\s*", " ").trim();
    }

    private void scrollToEnd() { scroll.post(() -> scroll.smoothScrollTo(0, scroll.getChildAt(0).getHeight())); }

    void save(Bundle state) {
        state.putStringArrayList("stage_messages", new ArrayList<>(messages));
        long[] stamps = new long[times.size()];
        for (int i = 0; i < stamps.length; i++) stamps[i] = times.get(i);
        state.putLongArray("stage_times", stamps);
        state.putCharSequence("stage_caption", caption.getText());
        state.putString("stage_panel", active);
        state.putBoolean("stage_chat_expanded", chatFullScreen);
    }
}
