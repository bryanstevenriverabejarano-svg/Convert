package salve.core.identity;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Con quién cree Salve que está hablando en esta sesión y con qué certeza.
 *
 * <p>En su propio teléfono parte de la hipótesis de que habla con su creador. La certeza sube
 * con la verificación del sistema (huella, rostro o PIN del dispositivo, que prueban que es el
 * dueño del teléfono) y baja cuando alguien dice ser otra persona, cancela la verificación o
 * empieza una conversación por voz sin verificación reciente. Una declaración («soy Bryan») o
 * una foto etiquetada son indicios, no pruebas.</p>
 */
public final class SessionIdentity {
    public enum State { VERIFIED_CREATOR, PRESUMED_CREATOR, UNCERTAIN, FAMILY, GUEST }
    public enum Method { DISPOSITIVO, BIOMETRIA, DECLARACION, NINGUNO }

    /** Una verificación biométrica vale este tiempo; después se vuelve a la presunción. */
    public static final long VERIFICATION_TTL = 6L * 60 * 60 * 1000;

    private State state = State.PRESUMED_CREATOR;
    private Method method = Method.DISPOSITIVO;
    private String name;
    private String relation;
    private long verifiedAt;
    private boolean claimedCreator;

    public synchronized State state() { return state; }
    public synchronized Method method() { return method; }
    public synchronized String name() { return name; }
    public synchronized String relation() { return relation; }
    public synchronized long verifiedAt() { return verifiedAt; }

    /** Restaura una verificación anterior (persistida) si sigue vigente. */
    public synchronized void restore(long verifiedAt, long now) {
        if (verifiedAt > 0 && now - verifiedAt < VERIFICATION_TTL) {
            this.verifiedAt = verifiedAt;
            state = State.VERIFIED_CREATOR;
            method = Method.BIOMETRIA;
        }
    }

    public synchronized void onBiometricSuccess(long now) {
        state = State.VERIFIED_CREATOR;
        method = Method.BIOMETRIA;
        verifiedAt = now;
        name = null;
        relation = null;
        claimedCreator = false;
    }

    /** Cancelación o fallo: si no hay verificación vigente, la identidad queda en duda. */
    public synchronized void onBiometricRejected(long now) {
        if (!verifiedRecently(now)) {
            state = State.UNCERTAIN;
            method = Method.NINGUNO;
        }
    }

    /** La voz puede venir de cualquiera en la habitación: sin verificación reciente, hay duda. */
    public synchronized void onVoiceSession(long now) {
        if (!verifiedRecently(now) && (state == State.PRESUMED_CREATOR)) {
            state = State.UNCERTAIN;
            method = Method.NINGUNO;
        }
    }

    /**
     * Alguien dice quién es. Si coincide con el creador, no sube a verificado (hace falta la
     * verificación del dispositivo), pero recupera la presunción si no había otra persona. Si
     * coincide con un familiar registrado, pasa a familiar; si no, a invitado.
     */
    public synchronized void onDeclaredName(String declared, String creatorName, List<String[]> family, long now) {
        if (declared == null || declared.trim().isEmpty()) return;
        if (matches(declared, creatorName)) {
            claimedCreator = true;
            if (state == State.VERIFIED_CREATOR && verifiedRecently(now)) return;
            if (state == State.GUEST || state == State.FAMILY || state == State.UNCERTAIN) {
                state = State.UNCERTAIN;
                method = Method.DECLARACION;
            } else {
                state = State.PRESUMED_CREATOR;
            }
            name = null;
            relation = null;
            return;
        }
        if (family != null) for (String[] member : family) {
            if (member.length >= 2 && matches(declared, member[1])) {
                state = State.FAMILY;
                method = Method.DECLARACION;
                name = member[1];
                relation = member[0];
                return;
            }
        }
        state = State.GUEST;
        method = Method.DECLARACION;
        name = declared.trim();
        relation = null;
    }

    /** «No soy Bryan», «te está hablando otra persona». */
    public synchronized void onNotCreator() {
        state = State.GUEST;
        method = Method.DECLARACION;
        name = null;
        relation = null;
    }

    public synchronized void expire(long now) {
        if (state == State.VERIFIED_CREATOR && !verifiedRecently(now)) {
            state = State.PRESUMED_CREATOR;
            method = Method.DISPOSITIVO;
        }
    }

    /**
     * Acceso a recuerdos privados, reflexiones y familia de Bryan. Con biometría disponible exige
     * verificación; sin ella (móvil sin huella ni rostro) basta la presunción del dispositivo.
     */
    public synchronized boolean privateAccess(boolean biometricAvailable, long now) {
        if (state == State.VERIFIED_CREATOR && verifiedRecently(now)) return true;
        return state == State.PRESUMED_CREATOR && !biometricAvailable;
    }

    /**
     * Ante una petición privada: ¿hay que pedir antes la verificación del teléfono? Solo cuando
     * podría ser el creador (presunción o duda), hay biometría y no hay verificación vigente. Un
     * familiar o invitado declarado no la necesita: ya recibe solo lo que le corresponde.
     */
    public synchronized boolean needsPrivateCheckpoint(boolean biometricAvailable, long now) {
        expire(now); // una verificación caducada vuelve a la presunción, aunque nadie la haya actualizado aún
        if (!biometricAvailable || privateAccess(true, now)) return false;
        return state == State.PRESUMED_CREATOR || state == State.UNCERTAIN;
    }

    public synchronized boolean isCreatorSide() {
        return state == State.VERIFIED_CREATOR || state == State.PRESUMED_CREATOR;
    }

    public synchronized boolean claimedCreator() { return claimedCreator; }

    private boolean verifiedRecently(long now) {
        return verifiedAt > 0 && now - verifiedAt < VERIFICATION_TTL;
    }

    /** Coincide si el nombre completo o el primer nombre son iguales (sin tildes ni mayúsculas). */
    public static boolean matches(String a, String b) {
        String x = normalize(a), y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        String fx = x.split(" ")[0], fy = y.split(" ")[0];
        return fx.equals(fy) && (x.split(" ").length == 1 || y.split(" ").length == 1 || y.startsWith(x) || x.startsWith(y));
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^\\p{L} ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
