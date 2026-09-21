package salve.devices;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real, read-only GATT inspection of a device the user has already paired and selected. */
public final class BluetoothInspector {
    public interface Listener {
        void onStatus(String status);
        void onServices(List<String> serviceIds);
    }

    private static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private BluetoothGatt current;
    private int generation;
    private Runnable timeout;

    public BluetoothInspector(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void connect(BluetoothDevice device) {
        disconnect();
        if (!hasPermission()) { listener.onStatus("Falta permiso para conectar por Bluetooth."); return; }
        final int attempt = generation;
        try {
            if (device == null || device.getBondState() != BluetoothDevice.BOND_BONDED) {
                listener.onStatus("Selecciona un dispositivo ya emparejado en los ajustes de Android.");
                return;
            }
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                listener.onStatus("Este dispositivo usa Bluetooth clásico. Este inspector solo admite servicios BLE/GATT.");
                return;
            }
            listener.onStatus("Solicitando conexión BLE; esperando respuesta del dispositivo…");
            current = device.connectGatt(context, false, new BluetoothGattCallback() {
                @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
                    main.post(() -> {
                        if (!isCurrent(attempt, gatt)) return;
                        if (status != BluetoothGatt.GATT_SUCCESS) { fail("La conexión Bluetooth falló (" + status + ")."); return; }
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            try {
                                listener.onStatus("Conexión GATT establecida. Consultando servicios…");
                                if (!gatt.discoverServices()) { fail("El dispositivo no aceptó la consulta de servicios."); return; }
                                deadline("El dispositivo no respondió a la consulta de servicios.", 15_000);
                            } catch (SecurityException error) { fail("El permiso Bluetooth fue revocado."); }
                        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            fail("El dispositivo se desconectó.");
                        }
                    });
                }

                @Override public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    main.post(() -> {
                        if (!isCurrent(attempt, gatt)) return;
                        cancelDeadline();
                        if (status != BluetoothGatt.GATT_SUCCESS) { fail("No se pudieron consultar los servicios (" + status + ")."); return; }
                        List<String> services = new ArrayList<>();
                        for (BluetoothGattService service : gatt.getServices()) services.add(service.getUuid().toString());
                        listener.onServices(services);
                        BluetoothGattService battery = gatt.getService(BATTERY_SERVICE);
                        BluetoothGattCharacteristic level = battery == null ? null : battery.getCharacteristic(BATTERY_LEVEL);
                        if (level == null || (level.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
                            listener.onStatus("Servicios consultados. No ofrece lectura de batería estándar. No se han escrito datos.");
                            return;
                        }
                        try {
                            if (!gatt.readCharacteristic(level)) { fail("El dispositivo no aceptó la lectura de batería."); return; }
                            listener.onStatus("Leyendo batería estándar…");
                            deadline("La lectura de batería agotó el tiempo de espera.", 8_000);
                        } catch (SecurityException error) { fail("El permiso Bluetooth fue revocado."); }
                    });
                }

                @Override public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    byte[] value = characteristic.getValue();
                    readResult(attempt, gatt, characteristic.getUuid(), value == null ? null : value.clone(), status);
                }

                @Override public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
                    readResult(attempt, gatt, characteristic.getUuid(), value == null ? null : value.clone(), status);
                }
            }, BluetoothDevice.TRANSPORT_LE);
            if (current == null) { fail("Android no pudo iniciar la conexión GATT."); return; }
            deadline("El dispositivo no respondió a la conexión BLE.", 15_000);
        } catch (SecurityException error) {
            fail("No se pudo conectar: falta permiso Bluetooth.");
        } catch (RuntimeException error) {
            fail("Android no pudo iniciar esta conexión Bluetooth.");
        }
    }

    private void readResult(int attempt, BluetoothGatt gatt, UUID characteristic, byte[] value, int status) {
        main.post(() -> {
            if (!isCurrent(attempt, gatt) || !BATTERY_LEVEL.equals(characteristic)) return;
            cancelDeadline();
            if (status != BluetoothGatt.GATT_SUCCESS || value == null || value.length != 1 || (value[0] & 0xff) > 100) {
                listener.onStatus("El dispositivo no devolvió un nivel de batería válido.");
                return;
            }
            listener.onStatus("Batería informada por el dispositivo: " + (value[0] & 0xff) + " %. Conexión de solo lectura activa.");
        });
    }

    private boolean hasPermission() {
        String permission = Build.VERSION.SDK_INT >= 31 ? Manifest.permission.BLUETOOTH_CONNECT : Manifest.permission.BLUETOOTH;
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isCurrent(int attempt, BluetoothGatt gatt) { return generation == attempt && current == gatt; }

    private void deadline(String message, long milliseconds) {
        cancelDeadline();
        timeout = () -> fail(message);
        main.postDelayed(timeout, milliseconds);
    }

    private void cancelDeadline() {
        if (timeout != null) main.removeCallbacks(timeout);
        timeout = null;
    }

    private void fail(String message) { disconnect(); listener.onStatus(message); }

    public void disconnect() {
        generation++;
        cancelDeadline();
        BluetoothGatt previous = current;
        current = null;
        if (previous != null) {
            try { previous.disconnect(); } catch (RuntimeException ignored) { }
            try { previous.close(); } catch (RuntimeException ignored) { }
        }
    }
}
