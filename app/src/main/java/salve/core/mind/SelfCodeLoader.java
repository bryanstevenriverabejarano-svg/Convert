package salve.core.mind;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Carga una vez el índice de su código que va dentro de la app (assets/self/codigo.json). */
public final class SelfCodeLoader {
    private static volatile SelfCode cached;

    private SelfCodeLoader() { }

    public static SelfCode get(Context context) {
        SelfCode value = cached;
        if (value != null) return value;
        synchronized (SelfCodeLoader.class) {
            if (cached == null) {
                String json = "{}";
                try (InputStream in = context.getAssets().open(SelfCode.ASSET)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[16384];
                    for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                    json = out.toString("UTF-8");
                } catch (Exception missing) { /* versión sin índice: responde que no lo tiene */ }
                cached = SelfCode.parse(json);
            }
            return cached;
        }
    }
}
