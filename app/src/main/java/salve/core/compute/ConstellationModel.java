package salve.core.compute;

/**
 * Geometría y estados de la constelación de nodos, sin Android para poder probarla.
 * El núcleo (el móvil) está en el centro; los nodos, repartidos en un círculo a su alrededor.
 */
public final class ConstellationModel {
    public enum State { CHECKING, ACTIVE, BUSY, OFFLINE, DISABLED }

    private ConstellationModel() { }

    /** Posiciones {x, y} de {@code count} nodos; el primero arriba y en sentido horario. */
    public static float[][] positions(int count, float centerX, float centerY, float radius) {
        float[][] points = new float[Math.max(0, count)][2];
        for (int i = 0; i < points.length; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / points.length;
            points[i][0] = (float) (centerX + radius * Math.cos(angle));
            points[i][1] = (float) (centerY + radius * Math.sin(angle));
        }
        return points;
    }

    /** Índice del nodo más cercano dentro de {@code touchRadius}, o -1. */
    public static int hit(float[][] points, float x, float y, float touchRadius) {
        int best = -1;
        double bestDistance = touchRadius;
        for (int i = 0; i < points.length; i++) {
            double distance = Math.hypot(points[i][0] - x, points[i][1] - y);
            if (distance <= bestDistance) { best = i; bestDistance = distance; }
        }
        return best;
    }

    /** Estado visible: desactivado manda; sin resultado aún, comprobando. */
    public static State stateOf(NodeStatusCheck.Result result, boolean enabled) {
        if (!enabled) return State.DISABLED;
        if (result == null) return State.CHECKING;
        if (!result.online) return State.OFFLINE;
        return result.freeSlots > 0 ? State.ACTIVE : State.BUSY;
    }

    public static String label(State state) {
        switch (state) {
            case ACTIVE: return "Activo";
            case BUSY: return "Ocupado";
            case OFFLINE: return "Desconectado";
            case DISABLED: return "Desactivado";
            default: return "Comprobando…";
        }
    }
}
