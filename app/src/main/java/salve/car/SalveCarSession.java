package salve.car;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

/** Una sesión por conexión con el coche: arranca en la pantalla de Salve. */
public final class SalveCarSession extends Session {
    @NonNull @Override public Screen onCreateScreen(@NonNull Intent intent) {
        return new SalveCarScreen(getCarContext());
    }
}
