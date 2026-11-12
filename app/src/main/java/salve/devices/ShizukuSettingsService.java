package salve.devices;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import salve.core.devicecontrol.SystemSetting;

/**
 * Servicio de usuario de Shizuku: corre en un proceso aparte con identidad de shell (la de ADB).
 *
 * Entiende dos transacciones: «aplica el ajuste nº N con el valor V» y «cierra las apps en segundo
 * plano» (para que Salve tenga memoria para sus 7k). Las órdenes las saca de la
 * tabla fija de {@link SystemSetting}; no acepta texto de shell de nadie, tampoco de la app. Devuelve
 * el código de salida y la lectura del estado después del cambio.
 */
public class ShizukuSettingsService extends Binder {
    static final String DESCRIPTOR = "salve.devices.ShizukuSettingsService";
    static final int APPLY = IBinder.FIRST_CALL_TRANSACTION;
    /** «Cierra las apps en segundo plano» (am kill-all): orden fija, sin texto de nadie. */
    static final int FREE_MEMORY = IBinder.FIRST_CALL_TRANSACTION + 1;
    /** Código que Shizuku usa para pedir al servicio que termine. */
    static final int DESTROY = 16777115;

    public ShizukuSettingsService() { }

    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == DESTROY) { System.exit(0); return true; }
        if (code == FREE_MEMORY) {
            data.enforceInterface(DESCRIPTOR);
            int exit;
            // Solo procesos en caché/segundo plano que Android puede cerrar sin riesgo; nunca el de
            // primer plano ni servicios persistentes.
            try { exit = run(new String[]{"am", "kill-all"}, null); } catch (Exception failed) { exit = -2; }
            if (reply != null) { reply.writeNoException(); reply.writeInt(exit); }
            return true;
        }
        if (code != APPLY) return super.onTransact(code, data, reply, flags);
        data.enforceInterface(DESCRIPTOR);
        int action = data.readInt(), value = data.readInt();
        int exit = -1;
        String read = "";
        String[][] commands = SystemSetting.commands(action, value);
        String[] verify = SystemSetting.verification(action);
        if (commands != null && verify != null) {
            try {
                exit = 0;
                for (String[] command : commands) {
                    int result = run(command, null);
                    if (result != 0) { exit = result; break; }
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                // Wifi y Bluetooth tardan un instante en reflejar el cambio.
                Thread.sleep(700L);
                run(verify, out);
                read = out.toString("UTF-8").trim();
            } catch (Exception failed) { exit = -2; }
        }
        if (reply != null) {
            reply.writeNoException();
            reply.writeInt(exit);
            reply.writeString(read.length() > 200 ? read.substring(0, 200) : read);
        }
        return true;
    }

    private static int run(String[] command, ByteArrayOutputStream output) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            int count, total = 0;
            while ((count = in.read(buffer)) != -1) {
                if (output != null && total < 4096) output.write(buffer, 0, count);
                total += count;
            }
        }
        if (!process.waitFor(8, TimeUnit.SECONDS)) { process.destroy(); return -3; }
        return process.exitValue();
    }
}
