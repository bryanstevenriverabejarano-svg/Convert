package salve.core.conversation;

import java.util.function.Supplier;
import salve.core.ModelResult;

/** Routes user turns only to inference adapters. Experimental verbalizers are not providers. */
public final class ConversationModelRouter {
    private ConversationModelRouter() {}

    public static ModelResult generate(boolean requiresVision,
                                       Supplier<ModelResult> cloud,
                                       Supplier<ModelResult> local) {
        return generate(requiresVision, false, false, cloud, local);
    }

    /** Local mode never silently sends a turn/photo to a remote provider. */
    public static ModelResult generate(boolean requiresVision, boolean localOnly, boolean localSupportsVision,
                                       Supplier<ModelResult> cloud, Supplier<ModelResult> local) {
        if (localOnly) {
            if (requiresVision && !localSupportsVision) return ModelResult.failure(ModelResult.Status.UNAVAILABLE,
                    "El modelo local seleccionado no admite fotos", 0L);
            return invoke(local);
        }
        ModelResult remote = invoke(cloud);
        if (remote.isSuccess() || remote.getStatus() == ModelResult.Status.CANCELLED) return remote;
        // The current local adapter accepts text only. Never describe an unseen image.
        if (requiresVision) return remote;
        ModelResult offline = invoke(local);
        if (offline.getStatus() != ModelResult.Status.UNAVAILABLE) return offline;
        return cloud == null ? offline : remote;
    }

    private static ModelResult invoke(Supplier<ModelResult> provider) {
        if (Thread.currentThread().isInterrupted()) {
            return ModelResult.failure(ModelResult.Status.CANCELLED, "Turno cancelado", 0L);
        }
        if (provider == null) {
            return ModelResult.failure(ModelResult.Status.UNAVAILABLE, "No hay un modelo configurado", 0L);
        }
        try {
            ModelResult result = provider.get();
            if (result == null || (result.isSuccess() && result.getText().trim().isEmpty())) {
                return ModelResult.failure(ModelResult.Status.ERROR, "El modelo no devolvió texto", 0L);
            }
            return result;
        } catch (RuntimeException e) {
            return ModelResult.failure(Thread.currentThread().isInterrupted()
                    ? ModelResult.Status.CANCELLED : ModelResult.Status.ERROR,
                    "Falló el proveedor de inferencia", 0L);
        }
    }
}
