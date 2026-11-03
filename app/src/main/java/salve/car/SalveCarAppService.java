package salve.car;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;

/**
 * Salve en la pantalla del coche (Android Auto). Solo plantillas del sistema: su imagen, el estado de
 * la voz y «Hablar»/«Terminar». No muestra el chat ni nada que haya que leer conduciendo.
 * Instalada fuera de la Play Store, Android Auto solo la enseña con «Fuentes desconocidas» activado
 * en sus ajustes de desarrollador. Categorías: POI + IOT en el mismo filtro (Android Auto en el móvil
 * no lista apps que solo declaran IOT; con POI aparece y puede usar las plantillas generales).
 */
public final class SalveCarAppService extends CarAppService {
    @NonNull @Override public HostValidator createHostValidator() {
        // Solo los hosts oficiales de Android Auto / Automotive (lista de la propia librería).
        return new HostValidator.Builder(getApplicationContext())
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build();
    }

    @NonNull @Override public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        return new SalveCarSession();
    }
}
