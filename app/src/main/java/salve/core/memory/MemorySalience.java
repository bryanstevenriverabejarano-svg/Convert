package salve.core.memory;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * La «amígdala» de su memoria: a igualdad de parecido con la pregunta, sale antes lo que más le
 * importó (la intensidad emocional con que se guardó) y lo que tiene que ver con quien más está en
 * su vida. Y el tiempo: si Bryan dice «el otro día…» gana lo reciente; si dice «hace tiempo…», lo
 * antiguo. No decide qué es verdad ni sustituye al parecido con la pregunta: solo desempata.
 */
public final class MemorySalience {
    private MemorySalience() { }

    public enum When { ANY, RECENT, OLD }

    private static final Pattern RECENT = Pattern.compile(
            "\\b(hace poco|hace un rato|el otro dia|la otra noche|ayer|anoche|antes de ayer|anteayer|hoy|esta manana|esta tarde|"
            + "esta semana|estos dias|recientemente|ultimamente|hace unos dias|hace dos dias|hace tres dias|lo ultimo)\\b");
    private static final Pattern OLD = Pattern.compile(
            "\\b(hace (mucho|tiempo|meses|anos|un ano|un mes|bastante)|hace mucho tiempo|al principio|antiguamente|"
            + "la primera vez|cuando empezamos|cuando nos conocimos|en su dia|tiempo atras|el ano pasado|el mes pasado)\\b");

    /** ¿La pregunta pide algo reciente, algo antiguo o le da igual? */
    public static When when(String input) {
        String t = normalize(input);
        if (OLD.matcher(t).find()) return When.OLD;
        if (RECENT.matcher(t).find()) return When.RECENT;
        return When.ANY;
    }

    /**
     * De 0 a 1: cuánto le importa este recuerdo. 60 % la emoción con que se guardó (intensidad 0..10),
     * 40 % la persona más cercana que aparece en él.
     * @param people nombre (sin tildes, minúsculas) → peso 0..1 de {@code PersonFamiliarity}
     */
    public static double score(String text, int intensity, Map<String, Double> people) {
        double emotional = Math.max(0, Math.min(10, intensity)) / 10.0;
        double person = 0;
        if (people != null && !people.isEmpty() && text != null) {
            String t = " " + normalize(text).replaceAll("[^\\p{L}0-9]+", " ") + " ";
            for (Map.Entry<String, Double> p : people.entrySet()) {
                if (p.getKey().length() < 3 || p.getValue() == null) continue;
                if (t.contains(" " + p.getKey() + " ")) person = Math.max(person, p.getValue());
            }
        }
        return 0.6 * emotional + 0.4 * person;
    }

    /** «hoy», «ayer», «hace 3 días», «hace 2 semanas», «hace 5 meses», «hace 1 año»; vacío si no hay fecha. */
    public static String ago(long timestamp, long now) {
        if (timestamp <= 0 || now <= 0) return "";
        long days = Math.max(0, (now - timestamp) / 86_400_000L);
        if (now < timestamp) return "hoy";
        if (days == 0) return "hoy";
        if (days == 1) return "ayer";
        if (days < 14) return "hace " + days + " días";
        if (days < 60) return "hace " + (days / 7) + " semanas";
        if (days < 365) { long m = days / 30; return "hace " + m + (m == 1 ? " mes" : " meses"); }
        long y = days / 365;
        return "hace " + y + (y == 1 ? " año" : " años");
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
}
