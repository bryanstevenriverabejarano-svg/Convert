package salve.devices;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import rikka.shizuku.Shizuku;
import salve.core.devicecontrol.SystemSetting;

/**
 * Puente con Shizuku para los ajustes del sistema. Sin Shizuku en marcha o sin su permiso no hace
 * nada y lo dice: nunca finge un cambio. No ejecuta órdenes libres; solo pide al servicio
 * privilegiado un ajuste de la lista cerrada de {@link SystemSetting}.
 */
public final class ShizukuBridge {
    private static final String TAG = "Salve/Shizuku";
    private static final int REQUEST_CODE = 7341;
    private static final Object LOCK = new Object();
    private static IBinder service;

    private ShizukuBridge() { }

    /** Aplica el ajuste y devuelve lo que pasó de verdad. No llamar desde el hilo principal. */
    public static String apply(Context context, SystemSetting.Command command) {
        if (command == null) return "AJUSTE_RECHAZADO: sin orden.";
        if (Looper.myLooper() == Looper.getMainLooper()) return "AJUSTE_NO_APLICADO: no puedo esperar a Shizuku desde aquí.";
        try {
            if (!Shizuku.pingBinder())
                return "SHIZUKU_NO_DISPONIBLE: para cambiar " + command.action.label + " necesito Shizuku instalada y en marcha "
                        + "(se arranca desde su app con la depuración inalámbrica; hay que repetirlo tras reiniciar el teléfono). No cambié nada.";
            if (Shizuku.isPreV11()) return "SHIZUKU_ANTIGUA: esa versión de Shizuku no sirve; hay que actualizarla. No cambié nada.";
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.shouldShowRequestPermissionRationale())
                    return "SHIZUKU_SIN_PERMISO: el permiso se denegó para siempre; hay que dármelo desde la app de Shizuku. No cambié nada.";
                Shizuku.requestPermission(REQUEST_CODE);
                return "SHIZUKU_PIDE_PERMISO: acabo de pedir permiso en Shizuku; acéptalo y repíteme la orden. No cambié nada todavía.";
            }
            IBinder binder = bind(context);
            if (binder == null) return "SHIZUKU_SIN_SERVICIO: Shizuku no arrancó mi servicio de ajustes a tiempo. No cambié nada.";
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(ShizukuSettingsService.DESCRIPTOR);
                data.writeInt(command.action.ordinal());
                data.writeInt(command.value);
                binder.transact(ShizukuSettingsService.APPLY, data, reply, 0);
                reply.readException();
                int exit = reply.readInt();
                String read = reply.readString();
                if (exit != 0) return "AJUSTE_FALLIDO: el sistema rechazó la orden (código " + exit + "). Ahora mismo, "
                        + SystemSetting.explain(command.action, read);
                return "AJUSTE_REAL (leído del sistema después de la orden): " + SystemSetting.explain(command.action, read);
            } finally { data.recycle(); reply.recycle(); }
        } catch (Exception failed) {
            synchronized (LOCK) { service = null; }
            Log.w(TAG, "Ajuste no aplicado: " + failed.getClass().getSimpleName());
            return "AJUSTE_FALLIDO: Shizuku no respondió; no hay cambio confirmado.";
        }
    }

    private static IBinder bind(Context context) throws InterruptedException {
        synchronized (LOCK) { if (service != null && service.pingBinder()) return service; }
        final CountDownLatch ready = new CountDownLatch(1);
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                synchronized (LOCK) { service = binder; }
                ready.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { synchronized (LOCK) { service = null; } }
        };
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                new ComponentName(context.getPackageName(), ShizukuSettingsService.class.getName()))
                .daemon(false).processNameSuffix("ajustes").tag("salve-ajustes").version(1);
        Shizuku.bindUserService(args, connection);
        ready.await(8, TimeUnit.SECONDS);
        synchronized (LOCK) { return service; }
    }
}
