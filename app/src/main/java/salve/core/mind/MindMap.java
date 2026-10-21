package salve.core.mind;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapa de su mente con datos reales: su narrativa en el centro, sus creencias (con la confianza y
 * los recuerdos en que se basan), sus preguntas abiertas, sus pensamientos recientes, las personas
 * que conoce y los temas de sus recuerdos. Sustituye al grafo «creativo» antiguo, que se llenaba
 * de hallazgos sin relación con lo que piensa de verdad.
 *
 * <p>Java puro: recibe los datos ya leídos y devuelve JSON y una página que se abre sin conexión.</p>
 */
public final class MindMap {
    /** Un recuerdo tal como lo guarda la base de datos. */
    public static final class Memory {
        public final int id; public final String text; public final List<String> tags; public final long at; public final int intensity;
        public Memory(int id, String text, List<String> tags, long at, int intensity) {
            this.id = id; this.text = text == null ? "" : text; this.tags = tags == null ? new ArrayList<>() : tags;
            this.at = at; this.intensity = intensity;
        }
    }
    /** Una persona y su relación con Bryan («madre», «hermana»…; vacío si es él). */
    public static final class Person {
        public final String name, relation;
        public Person(String name, String relation) { this.name = name; this.relation = relation == null ? "" : relation; }
    }

    public static final int MAX_MEMORIES = 40, MAX_TOPICS = 14;

    /** Etiquetas técnicas que no son temas. */
    private static final Pattern TECH_TAG = Pattern.compile(
            "^(hecho_usuario|declaracion_directa|memoria_(manual|auto)|profile:.*|manifiesto|identidad_creativa|investigacion_publica|"
            + "nube|cloud.*|sync.*|sistema|auto.*|interno|diagnostico|registro|evento|turno|conversacion|chat|voz|texto|"
            + "dibujo_.*|sketch.*|research.*|web.*|tool.*|herramienta.*|accion.*|recibo.*|[a-z]+_[0-9a-f]{6,}|\\d+|"
            // Origen o formato del registro, no de qué trata: llenaban el mapa de «pcloud», «user_message»…
            + "pcloud|diario_local|user_message|voice_message|assistant_message|memory|memoria|message|mensaje|"
            + "imagen|foto|fotos|foto:.*|escena|visual.*|photo.*|importante|recuerdo|recuerdos|local|remoto|restaurado|legacy)$");
    private static final Pattern SOURCE_MEMORY = Pattern.compile("^recuerdos?:(\\d+)$");

    private final JsonArray nodes = new JsonArray(), edges = new JsonArray();
    private final Set<String> ids = new HashSet<>();

    private MindMap() { }

    public static JsonObject build(String narrative, List<SelfModel.Belief> beliefs, List<SelfModel.Question> questions,
                                   List<SelfModel.Thought> thoughts, List<Person> people, List<Memory> memories,
                                   List<LessonBook.Lesson> lessons, String creatorName) {
        return build(narrative, null, null, beliefs, questions, thoughts, people, memories, lessons, creatorName);
    }

    /** @param history su biografía en una frase; @param mission el encargo de Bryan (pueden ser null) */
    public static JsonObject build(String narrative, String history, String mission, List<SelfModel.Belief> beliefs,
                                   List<SelfModel.Question> questions, List<SelfModel.Thought> thoughts, List<Person> people,
                                   List<Memory> memories, List<LessonBook.Lesson> lessons, String creatorName) {
        MindMap m = new MindMap();
        String creator = creatorName == null || creatorName.trim().isEmpty() ? "Bryan" : creatorName.trim();
        m.node("yo", "Salve", "yo", (narrative == null || narrative.isEmpty() ? "Mi narrativa aún no está escrita." : narrative)
                + (history == null || history.isEmpty() ? "" : "\n\n" + history)
                + (mission == null || mission.isEmpty() ? "" : "\n\nMi misión (encargo de " + creator.split(" ")[0] + "): " + mission), 1.0);
        m.node("p:bryan", creator.split(" ")[0], "persona", creator + " · mi creador", 0.9);
        m.edge("yo", "p:bryan", "me creó");
        Map<String, String> personByName = new HashMap<>();
        personByName.put(norm(creator.split(" ")[0]), "p:bryan");
        personByName.put("bryan", "p:bryan");
        if (people != null) for (Person p : people) {
            if (p == null || p.name == null || p.name.trim().isEmpty() || p.relation.isEmpty()) continue;
            String id = "p:" + norm(p.name).replace(' ', '_');
            if (m.ids.contains(id)) continue;
            m.node(id, p.name.split(" ")[0], "persona", p.name + " · " + p.relation + " de " + creator.split(" ")[0], 0.6);
            m.edge("p:bryan", id, p.relation);
            personByName.put(norm(p.name.split(" ")[0]), id);
        }
        Map<Integer, Memory> byId = new LinkedHashMap<>();
        if (memories != null) for (Memory mem : memories) if (mem != null) byId.put(mem.id, mem);
        Set<Integer> shownMemories = new HashSet<>();
        int b = 0;
        if (beliefs != null) for (SelfModel.Belief belief : beliefs) {
            if (belief == null || belief.statement == null || belief.statement.trim().isEmpty()) continue;
            String id = "b:" + (belief.id == null ? String.valueOf(b) : belief.id); b++;
            boolean contradicted = belief.contradictions != null && !belief.contradictions.isEmpty();
            String detail = belief.statement + String.format(Locale.ROOT, "\nConfianza: %.0f %%", belief.confidence * 100)
                    + (contradicted ? "\nHay algo que la contradice." : "")
                    + (belief.sources == null || belief.sources.isEmpty() ? "" : "\nFuentes: " + String.join(", ", belief.sources));
            m.node(id, shorten(belief.statement, 60), contradicted ? "creencia_dudosa" : "creencia", detail, Math.max(0.2, belief.confidence));
            String about = norm(belief.about);
            String target = "yo".equals(about) || "salve".equals(about) || about.isEmpty() ? "yo" : personByName.get(about);
            m.edge(target == null ? "yo" : target, id, "cree");
            if (belief.sources != null) for (String src : belief.sources) {
                Matcher mm = SOURCE_MEMORY.matcher(src == null ? "" : src.trim());
                if (!mm.matches()) continue;
                Memory mem = byId.get(Integer.parseInt(mm.group(1)));
                if (mem == null) continue;
                m.memory(mem); shownMemories.add(mem.id);
                m.edge(id, "m:" + mem.id, "se basa en");
            }
        }
        if (questions != null) for (SelfModel.Question q : questions) {
            if (q == null || q.text == null || q.text.trim().isEmpty()) continue;
            String id = "q:" + (q.id == null ? m.ids.size() : q.id);
            m.node(id, shorten(q.text, 60), "pregunta", q.text, 0.3 + Math.min(0.5, q.salience));
            m.edge("yo", id, "se pregunta");
        }
        if (thoughts != null) {
            int from = Math.max(0, thoughts.size() - 6);
            for (int i = from; i < thoughts.size(); i++) {
                SelfModel.Thought t = thoughts.get(i);
                if (t == null || t.text == null || t.text.trim().isEmpty()) continue;
                String id = "t:" + (t.id == null ? i : t.id);
                m.node(id, shorten(t.text, 60), "pensamiento", t.text, 0.35);
                m.edge("yo", id, "piensa");
            }
        }
        if (lessons != null) {
            int n = 0;
            for (LessonBook.Lesson l : lessons) {
                if (l == null || l.expected == null || n++ >= 6) continue;
                String id = "l:" + n;
                m.node(id, shorten(l.expected, 60), "leccion", (l.situation == null ? "" : "Cuando: " + l.situation + "\n")
                        + (l.did == null ? "" : "Hice: " + l.did + "\n") + "Aprendí: " + l.expected + (l.times > 1 ? "\n(" + l.times + " veces)" : ""), 0.3 + 0.1 * Math.min(5, l.times));
                m.edge("yo", id, "aprendió");
            }
        }
        // Temas: las etiquetas reales que más se repiten en sus recuerdos.
        Map<String, Integer> tagCount = new HashMap<>();
        Map<String, String> tagLabel = new HashMap<>();
        for (Memory mem : byId.values()) for (String tag : mem.tags) {
            String t = norm(tag);
            if (t.length() < 3 || TECH_TAG.matcher(t).matches()) continue;
            tagCount.merge(t, 1, Integer::sum); tagLabel.putIfAbsent(t, tag.trim());
        }
        List<Map.Entry<String, Integer>> topics = new ArrayList<>(tagCount.entrySet());
        topics.sort((x, y) -> y.getValue() - x.getValue());
        Set<String> topicIds = new HashSet<>();
        for (int i = 0; i < Math.min(MAX_TOPICS, topics.size()); i++) {
            Map.Entry<String, Integer> e = topics.get(i);
            if (e.getValue() < 2) break;
            String id = "tema:" + e.getKey().replace(' ', '_');
            m.node(id, tagLabel.get(e.getKey()), "tema", e.getValue() + " recuerdos sobre «" + tagLabel.get(e.getKey()) + "»", Math.min(1, 0.25 + e.getValue() / 20.0));
            m.edge("yo", id, "recuerda");
            topicIds.add(e.getKey());
        }
        // Recuerdos: los que sostienen creencias y los más intensos y recientes, hasta el límite.
        List<Memory> rest = new ArrayList<>(byId.values());
        rest.sort((x, y) -> y.intensity != x.intensity ? y.intensity - x.intensity : Long.compare(y.at, x.at));
        for (Memory mem : rest) {
            if (shownMemories.size() >= MAX_MEMORIES) break;
            if (shownMemories.contains(mem.id)) continue;
            boolean onTopic = false;
            for (String tag : mem.tags) if (topicIds.contains(norm(tag))) { onTopic = true; break; }
            if (!onTopic && mem.intensity < 7) continue;
            m.memory(mem); shownMemories.add(mem.id);
        }
        for (Integer mid : shownMemories) {
            Memory mem = byId.get(mid);
            boolean linked = false;
            for (String tag : mem.tags) if (topicIds.contains(norm(tag))) {
                m.edge("tema:" + norm(tag).replace(' ', '_'), "m:" + mid, "incluye"); linked = true;
            }
            for (Map.Entry<String, String> p : personByName.entrySet())
                if (p.getKey().length() >= 3 && Pattern.compile("\\b" + Pattern.quote(p.getKey()) + "\\b").matcher(norm(mem.text)).find()) {
                    m.edge(p.getValue(), "m:" + mid, "aparece en"); linked = true;
                }
            if (!linked && !m.hasEdgeTo("m:" + mid)) m.edge("yo", "m:" + mid, "recuerda");
        }
        JsonObject out = new JsonObject();
        out.add("nodes", m.nodes); out.add("edges", m.edges);
        JsonObject stats = new JsonObject();
        stats.addProperty("creencias", beliefs == null ? 0 : beliefs.size());
        stats.addProperty("preguntas", questions == null ? 0 : questions.size());
        stats.addProperty("recuerdos_mostrados", shownMemories.size());
        stats.addProperty("recuerdos_total", byId.size());
        out.add("stats", stats);
        return out;
    }

    private void memory(Memory mem) {
        if (ids.contains("m:" + mem.id)) return;
        node("m:" + mem.id, shorten(mem.text, 50), "recuerdo", mem.text + (mem.tags.isEmpty() ? "" : "\nEtiquetas: " + String.join(", ", mem.tags)),
                0.2 + Math.min(0.6, mem.intensity / 15.0));
    }

    /**
     * Pone foto a los nodos: a cada persona (por su primer nombre) y a cada recuerdo de una foto.
     * Las imágenes llegan ya reducidas como data:image/jpeg;base64 (no se lee nada de fuera).
     */
    public static void attachImages(JsonObject map, Map<String, String> personByFirstName, Map<Integer, String> memoryById) {
        if (map == null || !map.has("nodes")) return;
        for (com.google.gson.JsonElement e : map.getAsJsonArray("nodes")) {
            JsonObject n = e.getAsJsonObject();
            String id = n.get("id").getAsString(), type = n.get("type").getAsString();
            String img = null;
            if ("persona".equals(type) && personByFirstName != null) img = personByFirstName.get(norm(n.get("label").getAsString()));
            else if ("recuerdo".equals(type) && memoryById != null && id.startsWith("m:")) {
                try { img = memoryById.get(Integer.parseInt(id.substring(2))); } catch (NumberFormatException ignored) { }
            }
            if (img != null && img.startsWith("data:image/")) n.addProperty("img", img);
        }
    }

    /** El identificador de foto que aparece en el texto de un recuerdo visual («Foto dc6c…»), o null. */
    public static String photoId(String memoryText) {
        java.util.regex.Matcher m = PHOTO.matcher(memoryText == null ? "" : memoryText);
        return m.find() ? m.group(1) : null;
    }
    private static final Pattern PHOTO = Pattern.compile("\\b([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\b");

    private void node(String id, String label, String type, String detail, double weight) {
        if (!ids.add(id)) return;
        JsonObject o = new JsonObject();
        o.addProperty("id", id); o.addProperty("label", label); o.addProperty("type", type);
        o.addProperty("detail", detail); o.addProperty("w", Math.round(weight * 100) / 100.0);
        nodes.add(o);
    }

    private void edge(String from, String to, String kind) {
        if (!ids.contains(from) || !ids.contains(to) || from.equals(to)) return;
        JsonObject e = new JsonObject();
        e.addProperty("from", from); e.addProperty("to", to); e.addProperty("kind", kind);
        edges.add(e);
    }

    private boolean hasEdgeTo(String id) {
        for (int i = 0; i < edges.size(); i++) if (id.equals(edges.get(i).getAsJsonObject().get("to").getAsString())) return true;
        return false;
    }

    static String shorten(String s, int max) {
        String t = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    static String norm(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9ñ:_ ]", " ").replaceAll("\\s+", " ").trim();
    }

    /** Página autónoma (sin internet) que dibuja el mapa y deja tocar cada nodo. */
    public static String html(JsonObject data) {
        String json = data.toString().replace("</", "<\\/");
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
                + "<title>Mapa de la mente de Salve</title><style>"
                + ":root{--bg:#0b1420;--panel:#132232;--ink:#dcecff;--muted:#8fa6bf;--line:#2a4058}"
                + "*{box-sizing:border-box}html,body{margin:0;height:100%;background:var(--bg);color:var(--ink);font:15px/1.4 system-ui,sans-serif}"
                + "header{padding:12px 16px 6px}h1{font-size:18px;margin:0 0 4px}#stats{color:var(--muted);font-size:13px}"
                + "#chips{display:flex;gap:6px;flex-wrap:wrap;padding:6px 16px}"
                + ".chip{border:1px solid var(--line);border-radius:14px;padding:4px 10px;font-size:13px;background:transparent;color:var(--ink)}"
                + ".chip.off{opacity:.35}.dot{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:5px}"
                + "#q{margin:4px 16px 8px;width:calc(100% - 32px);padding:8px 10px;border-radius:8px;border:1px solid var(--line);background:var(--panel);color:var(--ink)}"
                + "canvas{display:block;width:100%;touch-action:none}"
                + "#card{position:fixed;left:12px;right:12px;bottom:12px;background:var(--panel);border:1px solid var(--line);border-radius:12px;"
                + "padding:12px 14px;max-height:45%;overflow:auto;display:none;white-space:pre-wrap}#card b{display:block;margin-bottom:4px}"
                + "#card small{color:var(--muted)}#ci{display:none;float:left;width:96px;height:96px;object-fit:cover;border-radius:50%;margin:0 12px 6px 0;border:2px solid var(--line)}#x{float:right;background:none;border:none;color:var(--muted);font-size:18px}"
                + "</style></head><body><header><h1>Mapa de mi mente</h1><div id=\"stats\"></div></header>"
                + "<div id=\"chips\"></div><input id=\"q\" placeholder=\"Buscar en mi mente…\"><canvas id=\"c\"></canvas>"
                + "<div id=\"card\"><button id=\"x\" aria-label=\"Cerrar\">×</button><img id=\"ci\" alt=\"\"><b id=\"ct\"></b><small id=\"ck\"></small><div id=\"cd\"></div></div>"
                + "<script>const D=" + json + ";\n"
                + "const COL={yo:'#7fd8ff',persona:'#ffd27f',creencia:'#8be39a',creencia_dudosa:'#ff9a8b',pregunta:'#c9a2ff',pensamiento:'#9ab8ff',leccion:'#ffb3e0',tema:'#f2f28a',recuerdo:'#6f8aa6'};\n"
                + "const NAME={yo:'Yo',persona:'Personas',creencia:'Creencias',creencia_dudosa:'Dudas',pregunta:'Preguntas',pensamiento:'Pensamientos',leccion:'Lecciones',tema:'Temas',recuerdo:'Recuerdos'};\n"
                + "const s=D.stats||{};const pl=(n,a,b)=>n+' '+(n===1?a:b);document.getElementById('stats').textContent=pl(s.creencias||0,'creencia','creencias')+' · '+pl(s.preguntas||0,'pregunta','preguntas')+' · '+(s.recuerdos_mostrados||0)+' de '+pl(s.recuerdos_total||0,'recuerdo','recuerdos');\n"
                + "const N=D.nodes.map((n,i)=>Object.assign({x:Math.cos(i*2.4)*(60+i*6),y:Math.sin(i*2.4)*(60+i*6),vx:0,vy:0},n));const idx={};N.forEach(n=>idx[n.id]=n);\n"
                + "const E=D.edges.filter(e=>idx[e.from]&&idx[e.to]);const hide=new Set();let query='';\n"
                + "const chips=document.getElementById('chips');Object.keys(NAME).forEach(t=>{if(!N.some(n=>n.type===t))return;const b=document.createElement('button');b.className='chip';"
                + "b.innerHTML='<span class=dot style=background:'+COL[t]+'></span>'+NAME[t];b.onclick=()=>{hide.has(t)?hide.delete(t):hide.add(t);b.classList.toggle('off')};chips.appendChild(b)});\n"
                + "document.getElementById('q').oninput=e=>{query=e.target.value.toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')};\n"
                + "const cv=document.getElementById('c'),cx=cv.getContext('2d');let W,H,dpr=window.devicePixelRatio||1,scale=1,ox=0,oy=0,ticks=0,touched=false;\n"
                + "function size(){dpr=window.devicePixelRatio||1;W=cv.clientWidth||document.documentElement.clientWidth||window.innerWidth;H=Math.max(200,window.innerHeight-cv.getBoundingClientRect().top);cv.style.height=H+'px';cv.width=Math.max(1,W*dpr);cv.height=H*dpr}size();addEventListener('resize',size);\n"
                // El WebView a veces carga la página antes de tener tamaño (ancho 0) y no avisa con «resize»:
                // el lienzo quedaba diminuto y estirado (una burbuja gigante en la esquina). Se vuelve a medir solo.
                + "function sized(){const w=cv.clientWidth||document.documentElement.clientWidth||window.innerWidth,h=Math.max(200,window.innerHeight-cv.getBoundingClientRect().top);"
                + "if(w>0&&(w!==W||Math.abs(h-H)>1)){size();if(!touched)ticks=0}}if(window.ResizeObserver)new ResizeObserver(sized).observe(document.body);\n"
                + "const vis=n=>!hide.has(n.type);const hit=n=>query&&(n.label+' '+n.detail).toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').includes(query);\n"
                + "const deg={};for(const e of E){deg[e.from]=(deg[e.from]||0)+1;deg[e.to]=(deg[e.to]||0)+1}\n"
                // Fuerzas acotadas: antes el muelle crecía con la distancia al cuadrado y un tema con muchos
                // recuerdos lo disparaba todo al infinito (NaN): solo quedaba «Yo», dibujado en la esquina.
                + "function step(){for(const a of N){if(!vis(a))continue;for(const b of N){if(a===b||!vis(b))continue;const dx=a.x-b.x,dy=a.y-b.y,d2=dx*dx+dy*dy+1,d=Math.sqrt(d2),f=Math.min(3,2600/d2);a.vx+=dx/d*f;a.vy+=dy/d*f}}"
                + "for(const e of E){const a=idx[e.from],b=idx[e.to];if(!vis(a)||!vis(b))continue;const dx=b.x-a.x,dy=b.y-a.y,d=Math.sqrt(dx*dx+dy*dy)+.01,f=Math.max(-3,Math.min(3,(d-90)*.03));"
                + "a.vx+=dx/d*f/Math.sqrt(deg[a.id]);a.vy+=dy/d*f/Math.sqrt(deg[a.id]);b.vx-=dx/d*f/Math.sqrt(deg[b.id]);b.vy-=dy/d*f/Math.sqrt(deg[b.id])}"
                + "for(const n of N){if(!isFinite(n.vx)||!isFinite(n.vy)){n.vx=n.vy=0}if(!isFinite(n.x)||!isFinite(n.y)){n.x=Math.random()*80-40;n.y=Math.random()*80-40}"
                + "if(n.id==='yo'){n.x*=.9;n.y*=.9;n.vx=n.vy=0;continue}n.vx-=n.x*.002;n.vy-=n.y*.002;n.vx*=.85;n.vy*=.85;"
                + "const v=Math.hypot(n.vx,n.vy);if(v>12){n.vx*=12/v;n.vy*=12/v}n.x+=n.vx;n.y+=n.vy}}\n"
                + "function r(n){return 5+n.w*12+(n.id==='yo'?10:0)+(n.img&&n.type==='persona'?8:0)}\n"
                + "for(const n of N)if(n.img){const im=new Image();im.src=n.img;n.im=im}\n"
                // Siempre con nombre: ella, las personas y los temas; el resto al acercarse o al buscar (antes se amontonaban).
                + "const ALWAYS={yo:1,persona:1,tema:1};\n"
                + "function draw(){cx.setTransform(dpr,0,0,dpr,0,0);cx.clearRect(0,0,W,H);cx.translate(W/2+ox,H/2+oy);cx.scale(scale,scale);"
                + "cx.lineWidth=1/scale;for(const e of E){const a=idx[e.from],b=idx[e.to];if(!vis(a)||!vis(b))continue;cx.strokeStyle=(hit(a)||hit(b))?'#ffffffaa':'#2a405899';cx.beginPath();cx.moveTo(a.x,a.y);cx.lineTo(b.x,b.y);cx.stroke()}"
                + "for(const n of N){if(!vis(n))continue;cx.globalAlpha=query&&!hit(n)?.25:1;cx.fillStyle=COL[n.type]||'#aaa';cx.beginPath();cx.arc(n.x,n.y,r(n),0,7);cx.fill();"
                + "if(n.im&&n.im.complete&&n.im.naturalWidth){const q=r(n)-2;cx.save();cx.beginPath();cx.arc(n.x,n.y,q,0,7);cx.closePath();cx.clip();"
                + "const s2=Math.min(n.im.naturalWidth,n.im.naturalHeight);cx.drawImage(n.im,(n.im.naturalWidth-s2)/2,(n.im.naturalHeight-s2)/2,s2,s2,n.x-q,n.y-q,2*q,2*q);cx.restore()}"
                + "if(ALWAYS[n.type]||scale>1.5||hit(n)){cx.fillStyle='#dcecff';cx.font=(n.id==='yo'?14:11)/Math.max(.7,scale)+'px system-ui';cx.textAlign='center';cx.fillText(n.label.slice(0,28),n.x,n.y+r(n)+12/Math.max(.7,scale))}}cx.globalAlpha=1}\n"
                + "function fit(){let a=1e9,b=-1e9,c=1e9,d=-1e9;for(const n of N){if(!vis(n)||!isFinite(n.x)||!isFinite(n.y))continue;a=Math.min(a,n.x);b=Math.max(b,n.x);c=Math.min(c,n.y);d=Math.max(d,n.y)}"
                + "if(a>b||W<=0||H<=0)return;scale=Math.max(.3,Math.min(1.6,Math.min((W-40)/(b-a+60),(H-40)/(d-c+60))));ox=-(a+b)/2*scale;oy=-(c+d)/2*scale}\n"
                + "let failed=0;(function loop(){try{sized();step();if(!touched&&++ticks%30===0&&ticks<=300)fit();draw()}catch(err){if(++failed===1)document.getElementById('stats').textContent+=' · error al dibujar: '+err.message}requestAnimationFrame(loop)})();\n"
                + "let drag=null,last=null,pinch=0;function pt(e){const t=e.touches?e.touches[0]:e,b=cv.getBoundingClientRect();return{x:(t.clientX-b.left-W/2-ox)/scale,y:(t.clientY-b.top-H/2-oy)/scale,sx:t.clientX,sy:t.clientY}}\n"
                + "function pick(p){let best=null,bd=1e9;for(const n of N){if(!vis(n))continue;const d=Math.hypot(n.x-p.x,n.y-p.y);if(d<r(n)+10/scale&&d<bd){bd=d;best=n}}return best}\n"
                + "function down(e){touched=true;if(e.touches&&e.touches.length===2){pinch=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);return}const p=pt(e);drag=pick(p);last=p;last.moved=false}\n"
                + "function move(e){if(e.touches&&e.touches.length===2&&pinch){const d=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);scale=Math.max(.3,Math.min(4,scale*d/pinch));pinch=d;e.preventDefault();return}"
                + "if(!last)return;const p=pt(e);if(Math.hypot(p.sx-last.sx,p.sy-last.sy)>4)last.moved=true;if(drag){drag.x=p.x;drag.y=p.y;drag.vx=drag.vy=0}else{ox+=p.sx-last.sx;oy+=p.sy-last.sy;p.x=(p.sx-cv.getBoundingClientRect().left-W/2-ox)/scale}last=Object.assign(p,{moved:last.moved});e.preventDefault()}\n"
                + "function up(){if(last&&!last.moved&&drag)show(drag);drag=null;last=null;pinch=0}\n"
                + "cv.addEventListener('touchstart',down,{passive:false});cv.addEventListener('touchmove',move,{passive:false});cv.addEventListener('touchend',up);"
                + "cv.addEventListener('mousedown',down);addEventListener('mousemove',move);addEventListener('mouseup',up);cv.addEventListener('wheel',e=>{touched=true;scale=Math.max(.3,Math.min(4,scale*(e.deltaY<0?1.1:.9)));e.preventDefault()},{passive:false});\n"
                + "function show(n){const rel=E.filter(e=>e.from===n.id||e.to===n.id).map(e=>e.from===n.id?'→ '+e.kind+': '+idx[e.to].label:'← '+e.kind+': '+idx[e.from].label);"
                + "const ci=document.getElementById('ci');if(n.img){ci.src=n.img;ci.style.display='block'}else{ci.removeAttribute('src');ci.style.display='none'}"
                + "document.getElementById('ct').textContent=n.label;document.getElementById('ck').textContent=NAME[n.type]||n.type;"
                + "document.getElementById('cd').textContent='\\n'+n.detail+(rel.length?'\\n\\n'+rel.slice(0,12).join('\\n'):'');document.getElementById('card').style.display='block'}\n"
                + "document.getElementById('x').onclick=()=>document.getElementById('card').style.display='none';\n"
                + "</script></body></html>";
    }
}
