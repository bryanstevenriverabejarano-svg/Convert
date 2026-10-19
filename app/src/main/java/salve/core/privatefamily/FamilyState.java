package salve.core.privatefamily;

/** Debounced connectivity, independent of the language model's readiness. */
public final class FamilyState {
    public long lastSeen, lastCheck, lastFailure;
    public int failures;
    public boolean alerted;
    public String status = "Sin comprobar";
    public PrivateLocation location;
    public boolean recent(long now) { return lastCheck > 0 && now >= lastCheck - 60 && now - lastCheck <= 1800; }
    public String displayStatus(boolean vpnAvailable, long now) {
        if (!vpnAvailable) return "Sin VPN disponible; estado desconocido";
        return recent(now) ? status : "Sin comprobación reciente";
    }
    public boolean observe(boolean reachable, boolean vpnAvailable, long now) {
        if (!vpnAvailable) { status = "Pausado: VPN desconectada"; return false; }
        lastCheck = now;
        if (reachable) {
            lastSeen = now; failures = 0; lastFailure = 0; alerted = false; status = "Disponible"; return false;
        }
        // Manual refresh cannot accelerate an incident; tolerate backward wall-clock adjustments.
        if (lastFailure == 0 || now - lastFailure >= 300 || now < lastFailure) { failures++; lastFailure = now; }
        status = failures >= 3 ? "Inaccesible" : "Sin respuesta; pendiente de confirmar";
        if (lastSeen > 0 && failures >= 3 && !alerted) { alerted = true; return true; }
        return false;
    }
}
