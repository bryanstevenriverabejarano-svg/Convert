package salve.presentation.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
    private String active = "";
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
        caption.setOnClickListener(v -> showChat(false));
        caption.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        activity.findViewById(R.id.btnStageOptions).setOnClickListener(v -> showOptions());
        activity.findViewById(R.id.btnCloseChat).setOnClickListener(v -> closePanels());
        activity.findViewById(R.id.btnCloseOptions).setOnClickListener(v -> closePanels());
        activity.findViewById(R.id.btnMenuChat).setOnClickListener(v -> showChat(true));
        activity.findViewById(R.id.btnMenuVoice).setOnClickListener(v -> {
            closePanels(); activity.findViewById(R.id.btnHablar).performClick();
        });
        scrim.setOnClickListener(v -> closePanels());
        if (saved != null) {
            ArrayList<String> restored = saved.getStringArrayList("stage_messages");
            if (restored != null) {
                for (String m : restored) { messages.add(m); serials.add(++serialCounter); }
                restoring = true; trimMessages(); renderMessages(); restoring = false;
            }
            CharSequence last = saved.getCharSequence("stage_caption");
            if (last != null) caption.setText(last);
            String restorePanel = saved.getString("stage_panel", "");
            // Let the hierarchy finish restoring before positioning a sheet.
            stage.post(() -> {
                if ("chat".equals(restorePanel)) showChat(false);
                else if ("options".equals(restorePanel)) showOptions();
            });
        }
        activity.findViewById(R.id.mainLayout).addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
            // Leave room for the composer when the IME resizes a short/landscape window.
            float density = activity.getResources().getDisplayMetrics().density;
            float heightDp = (b - t) / density;
            // BottomSheetBehavior adds its own top offset. Use height, never a top margin,
            // otherwise a relayout can push the composer below the window.
            fitSheetHeight(chat, b - t - (heightDp < 480 ? 0 : Math.round(56 * density)));
            fitSheetHeight(options, b - t - (heightDp < 480 ? 0 : Math.round(96 * density)));
            activity.findViewById(R.id.chatActions).setVisibility(heightDp < 360 ? View.GONE : View.VISIBLE);
        });
        updateChrome();
    }

    private void fitSheetHeight(View sheet, int height) {
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        height = Math.max(0, height);
        if (params.height != height) { params.height = height; sheet.setLayoutParams(params); }
    }

    private BottomSheetBehavior<View> configure(View sheet, String name) {
        sheet.setSaveEnabled(false);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(View view, int state) {
                if (state == BottomSheetBehavior.STATE_HIDDEN && active.equals(name)) {
                    active = ""; hideKeyboard(); updateChrome();
                }
            }
            @Override public void onSlide(View view, float offset) { }
        });
        return behavior;
    }

    boolean hasOpenPanel() { return !active.isEmpty(); }

    void showVoiceDetails() {
        showChat(false);
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    void setVoiceDetails(CharSequence text) {
        TextView details = activity.findViewById(R.id.voiceDetails);
        details.setText(text);
        details.setVisibility(View.VISIBLE);
    }

    void showChat(boolean keyboard) {
        closePanels(); active = "chat";
        chatBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        updateChrome();
        scrollToEnd();
        if (keyboard) composer.post(() -> {
            if (!"chat".equals(active)) return;
            composer.requestFocus();
            InputMethodManager ime = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (ime != null) ime.showSoftInput(composer, InputMethodManager.SHOW_IMPLICIT);
        });
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
        stage.setImportantForAccessibility(open ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        chat.setImportantForAccessibility("chat".equals(active) ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        options.setImportantForAccessibility("options".equals(active) ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
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
        draft = next == null ? null : next.length() > 12000 ? next.substring(0, 12000) + "…" : next;
        renderMessages();
        if (draft != null) {
            String plain = plain(draft);
            caption.setText(plain.length() > 180 ? "…" + plain.substring(plain.length() - 180) : plain);
            scrollToEnd();
        }
    }

    void appendMessage(boolean fromSalve, String text) {
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
        trimMessages(); renderMessages();
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
            transcript.setText(R.string.salve_chat_empty);
            if (thread.getChildCount() > 1) thread.removeViews(1, thread.getChildCount() - 1);
            return;
        }
        java.util.List<View> desired = new ArrayList<>();
        SpannableStringBuilder text = new SpannableStringBuilder();
        int[] textIndex = {0};
        java.util.List<String> shown = new ArrayList<>(messages);
        if (draft != null) shown.add("Salve · escribiendo\n" + draft + " ▍");
        for (int m = 0; m < shown.size(); m++) {
            boolean isDraft = m == messages.size();
            String message = shown.get(m);
            int heading = message.indexOf('\n');
            if (text.length() > 0) text.append("\n\n");
            int start = text.length();
            String head = heading > 0 ? message.substring(0, heading) : message;
            text.append(head).append('\n');
            text.setSpan(new StyleSpan(Typeface.BOLD), start, start + head.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String body = heading > 0 ? message.substring(heading + 1) : "";
            long serial = isDraft ? -1 : serials.get(m);
            Integer limit = isDraft ? null : reveal.get(serial);
            java.util.List<String[]> segments = SketchProtocol.segments(body);
            int ordinal = 0;
            for (int i = 0; i < segments.size(); i++) {
                if (limit != null && i >= limit) break;
                String[] seg = segments.get(i);
                if ("text".equals(seg[0])) { text.append(seg[1].replaceAll("^\\n+|\\n+$", "")); continue; }
                SketchScene scene = scene(seg[1]);
                String id = isDraft ? "draft:" + seg[1] : serial + ":" + ordinal;
                ordinal++;
                if (scene == null) continue;
                flushText(text, desired, textIndex);
                SketchView view = sketchViews.get(id);
                if (view == null) {
                    view = new SketchView(activity);
                    sketchViews.put(id, view);
                    view.bind(scene, restoring, finished -> onSketchFinished(id));
                    if (!restoring && staged.add(id)) Sketches.show(scene);
                } else view.bind(scene, false, finished -> onSketchFinished(id));
                desired.add(view);
            }
        }
        flushText(text, desired, textIndex);
        // Reutiliza las vistas que no cambian: un dibujo a medias no se reinicia al llegar más texto.
        int same = 0;
        while (same < desired.size() && same < thread.getChildCount() && thread.getChildAt(same) == desired.get(same)) same++;
        if (thread.getChildCount() > same) thread.removeViews(same, thread.getChildCount() - same);
        for (int i = same; i < desired.size(); i++) {
            View v = desired.get(i);
            if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
            thread.addView(v);
        }
    }

    private void flushText(SpannableStringBuilder text, java.util.List<View> desired, int[] index) {
        if (text.length() == 0) return;
        TextView block;
        if (index[0] < textBlocks.size()) block = textBlocks.get(index[0]);
        else {
            block = new TextView(activity);
            block.setTextColor(transcript.getCurrentTextColor());
            block.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, transcript.getTextSize());
            block.setLineSpacing(transcript.getLineSpacingExtra(), transcript.getLineSpacingMultiplier());
            block.setPadding(transcript.getPaddingLeft(), transcript.getPaddingTop(), transcript.getPaddingRight(), transcript.getPaddingBottom());
            block.setTextIsSelectable(true);
            textBlocks.add(block);
        }
        index[0]++;
        CharSequence value = new SpannableStringBuilder(text);
        if (!value.toString().equals(block.getText().toString())) block.setText(value);
        desired.add(block);
        text.clear();
        text.clearSpans();
    }

    private SketchScene scene(String key) {
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
        state.putCharSequence("stage_caption", caption.getText());
        state.putString("stage_panel", active);
    }
}
