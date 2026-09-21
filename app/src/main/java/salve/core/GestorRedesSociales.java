package salve.core;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.util.Locale;

/** Opens an explicitly requested app. Screen analysis is a separate user action. */
public class GestorRedesSociales {
    private final Context context;
    public GestorRedesSociales(Context context) { this.context = context.getApplicationContext(); }

    public void explorarRedSocial(String nombreApp) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            String packageName;
            switch (nombreApp == null ? "" : nombreApp.toLowerCase(Locale.ROOT)) {
                case "x": case "twitter": packageName = "com.twitter.android"; break;
                case "instagram": packageName = "com.instagram.android"; break;
                case "facebook": packageName = "com.facebook.katana"; break;
                case "telegram": packageName = "org.telegram.messenger"; break;
                default: packageName = "";
            }
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent == null) throw new IllegalArgumentException("Aplicación no disponible");
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException unavailable) {
                Toast.makeText(context, "No pude abrir esa aplicación. Comprueba Mi móvil.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
