package salve.core.identity;

import java.util.regex.Pattern;

/**
 * «Puntos críticos»: peticiones que piden recuperar algo privado del creador (quién es, qué sabe
 * Salve de él, su familia, recuerdos, dónde vive, dinero, salud, claves).
 *
 * <p>Solo cuenta si se pide la información, no si se aporta: «mi hermana se llama Ana» es un dato
 * nuevo; «¿cómo se llama mi hermana?» es una petición. El detector decide cuándo pedir la
 * verificación del teléfono; la protección real sigue siendo el estado de {@link SessionIdentity}
 * y lo que el código deja entrar en el contexto, no lo que se le pide al modelo.</p>
 */
public final class SensitiveRequest {
    private static final Pattern SELF = Pattern.compile(
            "\\b(?:quien soy(?: yo)?|sabes quien soy|que (?:sabes|recuerdas|conoces|piensas) (?:de|sobre) mi"
                    + "|me conoces|como me llamo|cual es mi nombre)\\b");
    private static final Pattern PRIVATE_TOPIC = Pattern.compile(
            "\\b(?:mis? (?:familia|madre|padre|mama|papa|hermanas?|hermanos?|pareja|novia|novio|mujer|marido"
                    + "|esposa|esposo|hijas?|hijos?|abuelas?|abuelos?|tias?|tios?|primas?|primos?)"
                    + "|mis recuerdos|mi diario|tus reflexiones sobre mi|lo que te (?:he )?(?:contado|dicho|conte|dije)"
                    + "|donde vivo|mi (?:direccion|casa)|donde trabajo|mi trabajo"
                    + "|mi (?:contrasena|clave|pin|cuenta bancaria|tarjeta)|mis (?:contrasenas|claves|cuentas|tarjetas)"
                    + "|mi (?:presupuesto|sueldo|salario|dinero|saldo)|mis (?:gastos|ingresos|deudas|ahorros|finanzas)"
                    + "|mi salud|mis (?:medicamentos|enfermedades)|mis datos(?: personales| privados)?)\\b");
    private static final Pattern ASKS = Pattern.compile(
            "^(?:que|quien|quienes|cual|cuales|como|donde|cuando|cuanto|cuanta|cuantos|cuantas|por que)\\b"
                    + "|\\b(?:dime|cuentame|recuerdame|muestrame|ensename|repiteme|leeme|sabes|recuerdas|conoces"
                    + "|hablame de|me dices|me cuentas)\\b");

    private SensitiveRequest() { }

    /**
     * @param privateBudgetTurn la interfaz ya marcó este turno como del presupuesto personal.
     */
    public static boolean needsVerification(String input, boolean privateBudgetTurn) {
        if (privateBudgetTurn) return true;
        if (input == null || input.trim().isEmpty() || input.length() > 2000) return false;
        String text = SessionIdentity.normalize(input);
        if (SELF.matcher(text).find()) return true;
        if (!PRIVATE_TOPIC.matcher(text).find()) return false;
        return input.indexOf('?') >= 0 || ASKS.matcher(text).find();
    }
}
