package ai.mlc.mlcllm;

import android.util.Log;

import androidx.annotation.Nullable;

import org.apache.tvm.Function;
import org.apache.tvm.LibInfo;
import org.apache.tvm.TVMValue;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real JSON FFI bridge that talks to the packed TVM runtime.
 */
public class JSONFFIEngine {

    public interface KotlinFunction {
        void invoke(String arg);
    }

    private static final String TAG = "JSONFFIEngine";
    private static final String JSON_FFI_MARKER = "json";

    private KotlinFunction streamCallback;
    private final AtomicBoolean functionsReady = new AtomicBoolean(false);
    private Function reloadFunc;
    private Function chatCompletionFunc;
    private Function runBackgroundLoopFunc;
    private Function runBackgroundStreamBackLoopFunc;
    private Function exitBackgroundLoopFunc;
    private Function resetFunc;
    private Function unloadFunc;

    public JSONFFIEngine() {
        LibInfo.ensureLoaded();
    }

    public void initBackgroundEngine(KotlinFunction callback) {
        this.streamCallback = callback;
        ensureFunctions();
        Log.i(TAG, "engine initialized (callbacks registered)");
    }

    public void reload(String engineConfigJSONStr) {
        ensureFunctions();
        if (reloadFunc == null) {
            throw new IllegalStateException("No reload function available in the runtime.");
        }
        TVMValue value = reloadFunc.invoke(engineConfigJSONStr);
        Log.i(TAG, "model reloaded OK: " + value.asString());
    }

    public void chatCompletion(String requestJSONStr, String requestId) {
        ensureFunctions();
        if (chatCompletionFunc == null) {
            throw new IllegalStateException("No chat completion function available in the runtime.");
        }

        String payload = requestJSONStr;
        if (requestId != null && !requestId.isEmpty()) {
            payload = injectRequestId(requestJSONStr, requestId);
        }

        TVMValue result = chatCompletionFunc.invoke(payload);
        String response = result.asString();
        if (streamCallback != null) {
            streamCallback.invoke(response);
        } else {
            Log.w(TAG, "Stream callback is null; response dropped");
        }
    }

    public void runBackgroundLoop() {
        ensureFunctions();
        if (runBackgroundLoopFunc != null) {
            runBackgroundLoopFunc.invoke();
        }
    }

    public void runBackgroundStreamBackLoop() {
        ensureFunctions();
        if (runBackgroundStreamBackLoopFunc != null) {
            runBackgroundStreamBackLoopFunc.invoke();
        }
    }

    public void exitBackgroundLoop() {
        if (exitBackgroundLoopFunc != null) {
            exitBackgroundLoopFunc.invoke();
        }
    }

    public void unload() {
        if (unloadFunc != null) {
            unloadFunc.invoke();
        }
    }

    public void reset() {
        if (resetFunc != null) {
            resetFunc.invoke();
        }
    }

    private void ensureFunctions() {
        if (functionsReady.get()) {
            return;
        }
        synchronized (this) {
            if (functionsReady.get()) {
                return;
            }
            Map<String, Function> candidates = discoverJsonFFIFunctions();
            reloadFunc = candidates.get("reload");
            chatCompletionFunc = candidates.get("chat_completion");
            runBackgroundLoopFunc = candidates.get("run_background_loop");
            runBackgroundStreamBackLoopFunc = candidates.get("run_background_stream_back_loop");
            exitBackgroundLoopFunc = candidates.get("exit_background_loop");
            resetFunc = candidates.get("reset");
            unloadFunc = candidates.get("unload");
            functionsReady.set(true);
        }
    }

    private Map<String, Function> discoverJsonFFIFunctions() {
        List<String> globals = Function.listGlobalNames();
        Map<String, Function> map = new HashMap<>();

        for (String name : globals) {
            if (name == null || !name.toLowerCase(Locale.US).contains(JSON_FFI_MARKER)) {
                continue;
            }
            if (name.contains("reload")) {
                map.put("reload", Function.getGlobal(name));
            } else if (name.contains("chat") && name.contains("completion")) {
                map.put("chat_completion", Function.getGlobal(name));
            } else if (name.contains("run") && name.contains("background") && name.contains("stream_back")) {
                map.put("run_background_stream_back_loop", Function.getGlobal(name));
            } else if (name.contains("run") && name.contains("background")) {
                map.put("run_background_loop", Function.getGlobal(name));
            } else if (name.contains("exit") && name.contains("background")) {
                map.put("exit_background_loop", Function.getGlobal(name));
            } else if (name.contains("reset")) {
                map.put("reset", Function.getGlobal(name));
            } else if (name.contains("unload")) {
                map.put("unload", Function.getGlobal(name));
            }
        }

        if (!map.containsKey("reload")) {
            throw new IllegalStateException("No JSON FFI reload function found in runtime globals.");
        }
        if (!map.containsKey("chat_completion")) {
            throw new IllegalStateException("No JSON FFI chat completion function found in runtime globals.");
        }
        return map;
    }

    private String injectRequestId(String json, String requestId) {
        // Append the request id in a simple way without altering existing fields.
        String trimmed = json.trim();
        if (trimmed.endsWith("}")) {
            String prefix = trimmed.substring(0, trimmed.length() - 1);
            return prefix + ",\"request_id\":\"" + requestId + "\"}";
        }
        return json;
    }
}
