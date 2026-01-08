package salve.core;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public class GlifoReliquia {
    private final String id;
    private final long seed;
    private final String style;
    private final int colorArgb;
    private final int sizeDp;
    private final long createdAt;
    private final String titulo;
    private final String nota;

    public GlifoReliquia(String id,
                         long seed,
                         String style,
                         int colorArgb,
                         int sizeDp,
                         long createdAt,
                         String titulo,
                         String nota) {
        this.id = id;
        this.seed = seed;
        this.style = style;
        this.colorArgb = colorArgb;
        this.sizeDp = sizeDp;
        this.createdAt = createdAt;
        this.titulo = titulo;
        this.nota = nota;
    }

    public String getId() {
        return id;
    }

    public long getSeed() {
        return seed;
    }

    public String getStyle() {
        return style;
    }

    public int getColorArgb() {
        return colorArgb;
    }

    public int getSizeDp() {
        return sizeDp;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getNota() {
        return nota;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("seed", seed);
            json.put("style", style);
            json.put("colorArgb", colorArgb);
            json.put("sizeDp", sizeDp);
            json.put("createdAt", createdAt);
            json.put("titulo", titulo);
            json.put("nota", nota);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static @Nullable GlifoReliquia fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        try {
            String id = json.optString("id", null);
            long seed = json.optLong("seed", 0L);
            String style = json.optString("style", null);
            int colorArgb = json.optInt("colorArgb", 0xFF000000);
            int sizeDp = json.optInt("sizeDp", 120);
            long createdAt = json.optLong("createdAt", System.currentTimeMillis());
            String titulo = json.optString("titulo", null);
            String nota = json.optString("nota", null);
            if (id == null) {
                return null;
            }
            return new GlifoReliquia(id, seed, style, colorArgb, sizeDp, createdAt, titulo, nota);
        } catch (Exception ignored) {
            return null;
        }
    }
}
