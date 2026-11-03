package salve.car;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * La pantalla de Salve en el coche: su imagen, cómo va la voz («Te escucho…», «Pensando…»,
 * «Hablando…») y un solo botón: «Hablar con Salve» o «Terminar». Siempre dos filas y un botón, para
 * que cada cambio de estado sea un refresco y no un paso nuevo (Android Auto limita los pasos).
 */
final class SalveCarScreen extends Screen {
    private static final long START_CHECK_MS = 5000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final CarVoiceStatus.Listener statusListener = () -> main.post(this::refresh);
    private String notice;

    SalveCarScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override public void onStart(@NonNull LifecycleOwner owner) {
                CarVoiceStatus.addListener(statusListener);
                refresh();
            }
            @Override public void onStop(@NonNull LifecycleOwner owner) {
                CarVoiceStatus.removeListener(statusListener);
                main.removeCallbacksAndMessages(null);
            }
        });
    }

    private void refresh() {
        if (CarVoiceStatus.active()) notice = null;
        invalidate();
    }

    @NonNull @Override public Template onGetTemplate() {
        boolean active = CarVoiceStatus.active();
        Pane.Builder pane = new Pane.Builder()
                .addRow(new Row.Builder().setTitle(CarVoiceStatus.line()).addText(CarVoiceStatus.hint()).build())
                .addRow(new Row.Builder().setTitle(notice != null ? notice
                        : active ? "Te oigo por el micrófono del móvil." : "Su voz sale por los altavoces del coche.").build())
                .addAction(new Action.Builder()
                        .setTitle(active ? "Terminar" : "Hablar con Salve")
                        .setBackgroundColor(active ? CarColor.RED : CarColor.BLUE)
                        .setOnClickListener(active ? this::end : this::talk)
                        .build());
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            pane.setImage(new CarIcon.Builder(
                    IconCompat.createWithResource(getCarContext(), com.salve.app.R.drawable.salve_car)).build());
        }
        return new PaneTemplate.Builder(pane.build())
                .setTitle("Salve")
                .setHeaderAction(Action.APP_ICON)
                .build();
    }

    /** Abre la conversación por voz de Salve (la misma que «Toca para hablar» de la notificación). */
    private void talk() {
        notice = CarVoiceControl.talk(getCarContext())
                ? "Abriendo la conversación…" : "Ábrela con la notificación «Modo conducción» del móvil.";
        invalidate();
        // Android puede bloquear abrir la app desde segundo plano sin avisar: si no empieza, decirlo.
        main.postDelayed(() -> {
            if (!CarVoiceStatus.active()) {
                notice = "Si no empieza: antes de arrancar, toca «Modo conducción» en el móvil.";
                invalidate();
            }
        }, START_CHECK_MS);
    }

    private void end() {
        CarVoiceControl.end();
        notice = null;
        invalidate();
    }
}
