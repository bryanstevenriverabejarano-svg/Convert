package salve.core.privatefamily;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.work.*;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import salve.core.compute.*;
import salve.presentation.ui.PrivateFamilyActivity;

public final class FamilyMonitor extends Worker {
    private static final Object RUN_LOCK = new Object();
    public FamilyMonitor(Context context, WorkerParameters parameters) { super(context, parameters); }
    public static void schedule(Context context, boolean enabled) {
        WorkManager work = WorkManager.getInstance(context);
        if (!enabled) { work.cancelUniqueWork("salve-private-family"); return; }
        work.enqueueUniquePeriodicWork("salve-private-family", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(FamilyMonitor.class, 15, TimeUnit.MINUTES).build());
    }
    @Override public Result doWork() {
        try { if (new FamilyVault(getApplicationContext()).read().monitoring) check(getApplicationContext(), this::isStopped); }
        catch (RuntimeException unavailable) { return Result.failure(); }
        return Result.success();
    }
    public static void check(Context context, BooleanSupplier cancelled) {
        synchronized (RUN_LOCK) {
            FamilyVault vault = new FamilyVault(context); FamilyVault.Data settings = vault.read();
            ComputeNodeStore registry = new ComputeNodeStore(context); long registryRevision = registry.revision();
            PrivateVpn vpn = PrivateVpn.get(context); ComputeHttpTransport transport = new ComputeHttpTransport(context);
            BooleanSupplier stopped = () -> cancelled.getAsBoolean() || registry.revision() != registryRevision || vault.read().revision != settings.revision;
            List<ComputeNode> nodes = registry.load(); Set<String> existing = new HashSet<>(); for (ComputeNode n : nodes) existing.add(n.id);
            vault.edit(data -> { data.states.keySet().retainAll(existing); data.family.retainAll(existing); });
            for (ComputeNode node : nodes) {
                if (stopped.getAsBoolean()) return;
                if (!settings.family.contains(node.id)) continue;
                boolean connected = false; PrivateLocation location = null;
                try {
                    JsonObject status = transport.familyStatus(node, stopped);
                    if (status.get("protocol").getAsInt() != 1 || !status.get("available").getAsBoolean()) throw new IllegalArgumentException();
                    if (status.has("location") && !status.get("location").isJsonNull()) {
                        location = PrivateLocation.parse(status.getAsJsonObject("location"), System.currentTimeMillis() / 1000);
                        if (!location.fresh(System.currentTimeMillis() / 1000)) location = null;
                    }
                    connected = true;
                } catch (Exception inaccessible) { /* Nothing from the remote error enters a log or notification. */ }
                if (stopped.getAsBoolean()) return;
                final boolean reachable = connected, available = vpn.available(); final PrivateLocation fix = location;
                vault.edit(data -> {
                    if (data.revision != settings.revision || !data.family.contains(node.id) || registry.revision() != registryRevision) return;
                    FamilyState state = data.states.computeIfAbsent(node.id, unused -> new FamilyState());
                    if (reachable) state.location = fix; // An explicit null withdraws the last shared coordinates.
                    boolean alert = state.observe(reachable, available, System.currentTimeMillis() / 1000);
                    if (alert && data.monitoring) notifyInaccessible(context, node.id);
                });
            }
        }
    }
    private static void notifyInaccessible(Context context, String nodeId) {
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        if (!notifications.areNotificationsEnabled()) return;
        notifications.createNotificationChannel(new NotificationChannel("family-status", "Estado de nodos familiares", NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open = PendingIntent.getActivity(context, 8203, new Intent(context, PrivateFamilyActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(context, "family-status").setSmallIcon(android.R.drawable.ic_menu_myplaces)
                .setContentTitle("Un nodo familiar no responde").setContentText("Abre el mapa para revisar la conexión. No indica la situación de la persona.")
                .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build();
        try { notifications.notify(nodeId, 8203, note); } catch (SecurityException denied) { /* Map remains available. */ }
    }
}
