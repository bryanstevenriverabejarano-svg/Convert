package salve.core;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "TTSManager";
    private TextToSpeech tts;
    private boolean ready = false;

    public TTSManager(Context ctx) {
        tts = new TextToSpeech(ctx, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int res = tts.setLanguage(new Locale("es","ES"));
            ready = res!=TextToSpeech.LANG_NOT_SUPPORTED && res!=TextToSpeech.LANG_MISSING_DATA;
            Log.d(TAG, ready?"TTS listo":"TTS no soportado");
        } else {
            Log.e(TAG,"Error init TTS: "+status);
        }
    }

    /** Pide al TTS que hable este texto. */
    public void speak(String text) {
        if (!ready || text==null||text.isEmpty()) return;
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "UTTERANCE_ID");
    }

    /** Libera recursos (opcional si lo hace SalveApplication). */
    public void shutdown() {
        if (tts!=null) {
            tts.stop();
            tts.shutdown();
        }
    }
}