package salve.core.synergy;

import java.text.Normalizer;
import java.util.Locale;

/** Explicit request to inspect the computational regulator. */
public final class SynergyRequests {
    private SynergyRequests() {}
    public static boolean isStatus(String input) {
        if(input==null)return false;
        String s=Normalizer.normalize(input.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
        return s.matches(".*\\b(estado de (tu |la )?sinergia|estado sinergico|tu energia cognitiva|tu energia interna|nucleo sinergico)\\b.*");
    }
}