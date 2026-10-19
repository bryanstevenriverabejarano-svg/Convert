package salve.core.compute;

import salve.core.compute.ConstellationModel.State;

/** Geometría, toque y estados de la constelación sin Android (scripts/test-compute-router.sh). */
public final class ConstellationChecks {
    public static void main(String[] args) {
        float[][] p = ConstellationModel.positions(4, 100, 100, 50);
        check(near(p[0][0], 100) && near(p[0][1], 50), "el primero arriba");
        check(near(p[1][0], 150) && near(p[1][1], 100), "sentido horario: derecha");
        check(near(p[2][1], 150) && near(p[3][0], 50), "abajo e izquierda");
        check(ConstellationModel.positions(0, 0, 0, 10).length == 0, "sin nodos");
        check(ConstellationModel.hit(p, 102, 53, 20) == 0, "toque cerca del primero");
        check(ConstellationModel.hit(p, 100, 100, 20) == -1, "el centro no es un nodo");
        check(ConstellationModel.hit(p, 140, 98, 30) == 1, "elige el más cercano");

        NodeStatusCheck.Result ok = new NodeStatusCheck.Result("PC", true, "m", 1, 20, "");
        NodeStatusCheck.Result busy = new NodeStatusCheck.Result("PC", true, "m", 0, 20, "");
        NodeStatusCheck.Result down = new NodeStatusCheck.Result("PC", false, "", 0, 0, "x");
        check(ConstellationModel.stateOf(ok, true) == State.ACTIVE, "activo");
        check(ConstellationModel.stateOf(busy, true) == State.BUSY, "ocupado");
        check(ConstellationModel.stateOf(down, true) == State.OFFLINE, "desconectado");
        check(ConstellationModel.stateOf(null, true) == State.CHECKING, "comprobando");
        check(ConstellationModel.stateOf(ok, false) == State.DISABLED, "desactivado manda");
        check(ConstellationModel.label(State.BUSY).equals("Ocupado"), "etiqueta");
        System.out.println("Constellation: OK");
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.01f; }
    private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
}
