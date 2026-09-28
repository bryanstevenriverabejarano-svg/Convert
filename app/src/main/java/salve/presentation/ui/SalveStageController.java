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
            if (restored != null) { messages.addAll(restored); trimMessages(); renderMessages(); }
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

    void appendMessage(boolean fromSalve, String text) {
        if (text == null || text.trim().isEmpty()) return;
        // Bound recreation state so a long session cannot exceed the Android Binder limit.
        String visible = text.length() > 12000 ? text.substring(0, 12000) + "…" : text;
        messages.add((fromSalve ? "Salve" : "Tú") + "\n" + visible);
        trimMessages(); renderMessages();
        if (fromSalve) caption.setText(text.length() > 180 ? text.substring(0, 180) + "…" : text);
        scrollToEnd();
    }

    private void trimMessages() {
        int chars = 0;
        for (String message : messages) chars += message.length();
        while (messages.size() > 40 || chars > 48000) chars -= messages.remove(0).length();
    }

    private void renderMessages() {
        if (messages.isEmpty()) { transcript.setText(R.string.salve_chat_empty); return; }
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (String message : messages) {
            if (text.length() > 0) text.append("\n\n");
            int start = text.length();
            text.append(message);
            int heading = message.indexOf('\n');
            if (heading > 0) text.setSpan(new StyleSpan(Typeface.BOLD), start, start + heading, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        transcript.setText(text);
    }

    private void scrollToEnd() { scroll.post(() -> scroll.smoothScrollTo(0, scroll.getChildAt(0).getHeight())); }

    void save(Bundle state) {
        state.putStringArrayList("stage_messages", new ArrayList<>(messages));
        state.putCharSequence("stage_caption", caption.getText());
        state.putString("stage_panel", active);
    }
}
