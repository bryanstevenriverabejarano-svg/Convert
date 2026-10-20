package salve.core.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Google Imágenes mirado como lo miraría una persona: Salve abre la búsqueda en su ventana en
 * directo, lee las miniaturas que se ven en pantalla y se queda con unas pocas como referencia.
 *
 * <p>No usa ninguna API de Google ni descarga nada aparte: las miniaturas son las que la ventana
 * ya muestra (incrustadas en la página o recortadas de la pantalla). Si Google pide verificación
 * o consentimiento, no lo resuelve sola: se lo deja a Bryan en la ventana.</p>
 */
public final class GoogleImages {
    private GoogleImages() { }

    /** Miniatura que Salve ve en la página. */
    public static final class Spot {
        public final int index;
        public final String alt, href;
        /** Imagen incrustada en la página (data:), o null si hay que recortarla de la pantalla. */
        public final byte[] bytes;
        /** Posición en la pantalla de la página (px CSS). */
        public final float x, y, w, h;
        Spot(int index, String alt, String href, byte[] bytes, float x, float y, float w, float h) {
            this.index = index; this.alt = alt; this.href = href; this.bytes = bytes;
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
        /** ¿Está entera dentro de la parte visible? */
        public boolean visible(float viewportW, float viewportH) {
            return x >= 0 && y >= 0 && w >= 40 && h >= 40 && x + w <= viewportW + 1 && y + h <= viewportH + 1;
        }
    }

    /** Lo que la página enseña: tamaño visible y miniaturas en orden. */
    public static final class Page {
        public final float viewportW, viewportH;
        public final List<Spot> spots;
        Page(float viewportW, float viewportH, List<Spot> spots) { this.viewportW = viewportW; this.viewportH = viewportH; this.spots = spots; }
    }

    /** Tamaño máximo de una miniatura incrustada que se acepta. */
    static final int MAX_THUMB_BYTES = 300_000;

    /** Búsqueda de imágenes con SafeSearch: «gorila» → «gorila dibujo». */
    public static String url(String subject) {
        String clean = subject == null ? "" : subject.replaceAll("[\\r\\n\"«»<>]+", " ").replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("¿Qué imágenes busco?");
        if (clean.length() > 80) clean = clean.substring(0, 80).trim();
        String q = clean.toLowerCase(Locale.ROOT).matches(".*\\b(dibujo|silueta|ilustraci[oó]n|drawing)\\b.*") ? clean : clean + " dibujo";
        try { return "https://www.google.com/search?udm=2&safe=active&hl=es&q=" + URLEncoder.encode(q, "UTF-8"); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new IllegalStateException(impossible); }
    }

    /** ¿Es una página de Google Imágenes (y no la verificación o el consentimiento)? */
    public static Blocker blocker(String url) {
        if (url == null) return Blocker.OTHER;
        String u = url.toLowerCase(Locale.ROOT);
        if (u.contains("google.") && u.contains("/sorry/")) return Blocker.VERIFICATION;
        if (u.startsWith("https://consent.google.") || u.contains("consent.google.")) return Blocker.CONSENT;
        if (u.contains("google.") && u.contains("/search")) return Blocker.NONE;
        return Blocker.OTHER;
    }

    public enum Blocker { NONE, VERIFICATION, CONSENT, OTHER }

    /**
     * JavaScript que corre en la ventana: apunta las miniaturas visibles (sin logos ni anuncios),
     * les pone un número para poder señalarlas y devuelve dónde están.
     */
    public static final String COLLECT_JS = "(function(){try{"
            + "var out=[],seen={},imgs=document.images,vw=window.innerWidth,vh=window.innerHeight;"
            + "for(var i=0;i<imgs.length&&out.length<12;i++){"
            + "var im=imgs[i],r=im.getBoundingClientRect();"
            + "if(r.width<48||r.height<48||r.bottom<0||r.top>vh*3)continue;"
            + "var src=im.currentSrc||im.src||'';"
            + "if(!src||/googlelogo|\\/logos\\/|favicon|\\/images\\/branding\\//.test(src))continue;"
            + "if(im.closest&&im.closest('[aria-label*=\"atrocinad\"],[aria-label*=\"ponsored\"],[data-text-ad],[data-pla]'))continue;"
            + "if(seen[src])continue;seen[src]=1;"
            + "im.setAttribute('data-salve-idx',String(out.length));"
            + "var a=im.closest?im.closest('a'):null;"
            + "var data=(src.indexOf('data:image/jpeg')===0||src.indexOf('data:image/png')===0||src.indexOf('data:image/webp')===0)"
            + "&&src.length<420000?src:'';"
            + "out.push({alt:String(im.alt||'').slice(0,140),data:data,x:r.left,y:r.top,w:r.width,h:r.height,"
            + "href:a?String(a.href||'').slice(0,400):''});}"
            + "return JSON.stringify({vw:vw,vh:vh,items:out});}catch(e){return JSON.stringify({vw:0,vh:0,items:[],error:String(e)});}})()";

    /** Marca en la ventana la miniatura que está mirando (un borde), para que se vea qué elige. */
    public static String markJs(int index, boolean chosen) {
        int i = Math.max(0, Math.min(99, index));
        String color = chosen ? "#3ddc97" : "#58c4dc";
        return "(function(){var all=document.querySelectorAll('img[data-salve-idx]');"
                + "for(var k=0;k<all.length;k++){all[k].style.outline='';}"
                + "var im=document.querySelector('img[data-salve-idx=\"" + i + "\"]');"
                + "if(!im)return '';im.style.outline='4px solid " + color + "';im.style.outlineOffset='-4px';"
                + "im.scrollIntoView({block:'center',behavior:'smooth'});return 'ok';})()";
    }

    /**
     * Lee lo que devolvió {@link #COLLECT_JS}. WebView entrega el resultado como texto JSON entre
     * comillas, así que se desenvuelve primero. Nunca lanza: si algo no cuadra, página vacía.
     */
    public static Page parse(String evaluated) {
        List<Spot> spots = new ArrayList<>();
        try {
            JsonElement outer = JsonParser.parseString(evaluated == null ? "" : evaluated);
            String inner = outer.isJsonPrimitive() ? outer.getAsString() : outer.toString();
            JsonObject page = JsonParser.parseString(inner).getAsJsonObject();
            float vw = number(page, "vw"), vh = number(page, "vh");
            JsonElement items = page.get("items");
            if (items != null && items.isJsonArray()) {
                JsonArray list = items.getAsJsonArray();
                for (int i = 0; i < list.size() && spots.size() < 12; i++) {
                    if (!list.get(i).isJsonObject()) continue;
                    JsonObject o = list.get(i).getAsJsonObject();
                    String href = text(o, "href");
                    if (!href.isEmpty() && !href.startsWith("https://")) href = "";
                    spots.add(new Spot(i, text(o, "alt").replaceAll("\\s+", " ").trim(), href, decode(text(o, "data")),
                            number(o, "x"), number(o, "y"), number(o, "w"), number(o, "h")));
                }
            }
            return new Page(vw, vh, spots);
        } catch (RuntimeException odd) {
            return new Page(0, 0, spots);
        }
    }

    /** data:image/jpeg;base64,… → bytes (o null si no es una imagen incrustada válida). */
    static byte[] decode(String dataUri) {
        if (dataUri == null || !dataUri.matches("(?s)^data:image/(jpeg|png|webp);base64,.+")) return null;
        try {
            byte[] bytes = Base64.getMimeDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1).getBytes(StandardCharsets.US_ASCII));
            return bytes.length < 200 || bytes.length > MAX_THUMB_BYTES ? null : bytes; // < 200 B: marcador vacío
        } catch (IllegalArgumentException broken) { return null; }
    }

    private static float number(JsonObject o, String key) {
        JsonElement e = o.get(key);
        try { return e != null && e.isJsonPrimitive() ? e.getAsFloat() : 0; } catch (RuntimeException odd) { return 0; }
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }
}
