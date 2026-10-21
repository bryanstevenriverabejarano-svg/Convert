package salve.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import salve.core.cognition.CognitiveReply;
import salve.core.cognition.CognitivePrompt;
import salve.core.cognition.CognitiveTurn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import salve.core.cognitive.HipocampoSemantico;
import salve.core.cognitive.ReasoningEngine;
import salve.core.conversation.ConversationAnalysis;
import salve.core.conversation.ConversationRequestAnalyzer;
import salve.core.conversation.ConversationSession;
import salve.core.conversation.ReasoningPlan;
import salve.core.conversation.ReasoningPlanner;
import salve.core.conversation.ResponseLimiter;
import salve.core.evaluation.ConversationQualityEvaluator;
import salve.core.evaluation.TurnQualityAssessment;
import salve.core.memory.ConversationMemoryGrounding;
import salve.core.conversation.DeviceClockContext;
import salve.core.memory.MemoryWritePolicy;
import salve.core.memory.MemoryProfileFact;
import salve.core.memory.MemoryForgetRequest;
import salve.core.memory.PendingMemoryDeletion;
import salve.core.tools.PendingToolAction;
import salve.core.voice.VoiceResponsePolicy;
import salve.core.voice.VoiceTurnGate;
import salve.core.voice.VoiceProfile;
import salve.core.voice.VoiceProfileStore;
import salve.core.voice.VoiceSelectionPolicy;
import salve.core.conversation.AuthorReviewerCodeCoordinator;
import salve.core.conversation.ConversationModelRouter;
import salve.core.visual.VisualMemoryRecord;
import salve.core.visual.VisualMemoryRepository;
import salve.avatar.AvatarMotionController;
import salve.avatar.AvatarMotionProtocol;
import salve.presentation.ui.GaleriaVisualActivity;
import salve.services.SalveAccessibilityService;
import salve.services.SistemaSensorial;
import salve.services.VideoAnalysisManager;

/**
 * Su taller de dibujo: entender qué se pide, buscar y trazar referencias, componer con formas, mirar lo dibujado, aprender de fotos y trazos de Bryan.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class DrawingStudio {
    private final MotorConversacional motor;

    DrawingStudio(MotorConversacional motor) { this.motor = motor; }

    /** Latest independent drawing check, passed to the learning coordinator. */
    volatile salve.sketch.SketchTalk.Look ultimaMirada;

    /** Cómo debe hablar de sus dibujos: de qué dibujó y por qué, no de su técnica. */
    static final String ESTILO_DIBUJO = " Habla de qué dibujaste y por qué salió así, en una o dos frases. "
            + "No describas tu técnica (epiciclos, Fourier, círculos que giran, circuitos, espacio astral) salvo que Bryan "
            + "pregunte cómo dibujas, y no digas que dibujar es un acto de conciencia.";

    /** Último objeto que le pidieron dibujar («dibújalo» se refiere a él). */
    volatile String lastSketchSubject;

    /** Lo último que dibujó (para «¿por qué lo dibujaste así?»). */
    volatile salve.sketch.SketchScene lastDrawnScene;

    /** Motor con el que pensó el último dibujo («Gemini …» o su modelo local). */
    volatile String drawingThinker = "mi modelo local";

    /** Herramienta sketch.draw: dibuja lo pedido (repertorio, referencias buscadas o formas compuestas). */
    CognitiveTurn.Receipt herramientaDibujar(String wanted) {
        salve.sketch.SketchLibrary library = salve.sketch.Sketches.library(motor.context);
        if (library == null) return new CognitiveTurn.Receipt("DIBUJO_NO_DISPONIBLE: el repertorio de trazos no se pudo cargar.");
        if (motor.turnSketchTag.get() != null) return new CognitiveTurn.Receipt("YA_DIBUJANDO: ya estás trazando un dibujo en este turno.");
        salve.sketch.SketchScene scene = wanted.length() > 120 ? null : salve.sketch.SketchScene.fromKey(library, wanted);
        if (scene == null && wanted.length() <= 60) {
            lastSketchSubject = wanted;
            scene = buscarReferenciaYDibujar(wanted);
            if (scene != null) { empezarDibujo(scene); return new CognitiveTurn.Receipt(motor.referenceNote); }
            scene = componerDibujo(wanted);
            if (scene != null) { empezarDibujo(scene); return new CognitiveTurn.Receipt(motor.composeNote); }
        }
        if (scene == null) return new CognitiveTurn.Receipt("MOTIVO_DESCONOCIDO: no encontraste referencias de «" + wanted
                + "» ni pudiste componerlo. Pide a Bryan una foto o que te lo dibuje con el dedo.");
        empezarDibujo(scene);
        return new CognitiveTurn.Receipt("DIBUJANDO " + scene.title + " (" + origen(scene)
                + "); Bryan lo ve en la pantalla y en el chat." + ESTILO_DIBUJO);
    }

    void registrarPeticionDibujo(String entrada, String interpretation) {
        salve.core.trace.ExecutionTrace.begin(motor.context, "Dibujo", entrada);
        salve.core.trace.ExecutionTrace.note("CONTEXTO DE LA TAREA", interpretation);
        motor.literalDrawingTurn.set(true);
    }

    String prepararDibujo(String entrada) {
        salve.sketch.SketchLibrary library = salve.sketch.Sketches.library(motor.context);
        if (library == null) return "";
        // «Dibújala», «dibuja la foto», «esto es una silla»: sobre la foto que ya está en el chat.
        VisualMemoryRecord photo = motor.activePhoto;
        salve.sketch.PhotoSketchRequest onPhoto = photo == null ? null : salve.sketch.PhotoSketchRequest.parse(entrada, false);
        if (onPhoto != null && onPhoto.kind != salve.sketch.PhotoSketchRequest.Kind.NONE) {
            registrarPeticionDibujo(entrada, "Se usa la foto seleccionada en este chat. Operación: " + onPhoto.kind);
            Bitmap image = null;
            try {
                image = motor.visualMemories.loadImage(photo);
                if (dibujarFoto(image, onPhoto) != null) return " | " + motor.photoSketchContext;
            } catch (java.io.IOException missing) {
                return " | DIBUJO: no puedes abrir los píxeles de la foto guardada; pide que la envíe otra vez.";
            } finally { if (image != null) image.recycle(); }
        }
        salve.sketch.SketchRequest request = salve.sketch.SketchRequest.parse(entrada, library);
        boolean recent = dibujoReciente();
        if (request.implicit && !recent) request = salve.sketch.SketchRequest.parse("", library);
        if (request.kind == salve.sketch.SketchRequest.Kind.NONE && recent) {
            // «Vale, hazlo», «sí, dale más detalle», «muéstrame el dibujo», «?»: siguen pidiendo el dibujo.
            switch (salve.sketch.SketchRequest.followUp(entrada)) {
                case GO: request = salve.sketch.SketchRequest.again(false, ""); break;
                case IMPROVE: request = salve.sketch.SketchRequest.again(true, "con mas detalle"); break;
                case SHOW: {
                    salve.sketch.SketchScene shown = lastDrawnScene;
                    String wanted = lastSketchSubject;
                    if (shown != null && (wanted == null || salve.sketch.SketchRequest.cleanName(shown.title)
                            .equals(salve.sketch.SketchRequest.cleanName(wanted)))) {
                        registrarPeticionDibujo(entrada, "Mostrar el dibujo existente: " + shown.title + ". No se prepara una nueva versión.");
                        empezarDibujo(shown);
                        return " | ACCION_REALIZADA: vuelves a mostrar tu dibujo de " + shown.title + " (" + origen(shown)
                                + "); ya aparece en la pantalla y en el chat." + ESTILO_DIBUJO;
                    }
                    request = salve.sketch.SketchRequest.again(false, "");
                    break;
                }
                default: break;
            }
        }
        if (request.kind == salve.sketch.SketchRequest.Kind.NONE) {
            String talk = hablarDeMiDibujo(entrada);
            if (!talk.isEmpty() || !recent) return talk;
            return " | DIBUJO: en este turno NO has dibujado nada. No digas que estás dibujando, refinando, añadiendo detalle "
                    + "ni que lo has terminado o lo muestras. Si Bryan quiere el dibujo, dile que lo pida con «dibuja …».";
        }
        if (request.kind != salve.sketch.SketchRequest.Kind.LIST) {
            registrarPeticionDibujo(entrada, "Operación: " + request.kind + "; objeto solicitado: "
                    + (request.scene != null ? request.scene.title : request.unknown)
                    + (request.kind == salve.sketch.SketchRequest.Kind.AGAIN ? "; contexto reutilizado: " + lastSketchSubject : "")
                    + "; mejora: " + request.improve + "; referencias solicitadas: " + request.references + "; cambios: " + request.wish);
            motor.referenceFailure = "no se ha realizado una búsqueda en esta tarea";
        }
        boolean again = request.kind == salve.sketch.SketchRequest.Kind.AGAIN;
        String subject = again ? lastSketchSubject : request.unknown;
        if (again && (subject == null || subject.isEmpty()))
            return " | DIBUJO: Bryan te pide " + (request.improve ? "mejorar un dibujo" : "dibujar algo")
                    + (request.references ? " buscando referencias en internet" : "")
                    + ", pero no sabes cuál. Pregúntale qué quiere que dibujes.";
        if (request.kind == salve.sketch.SketchRequest.Kind.DRAW && request.scene != null) {
            lastSketchSubject = request.scene.title;
            motor.lastSketchAskedAt = System.currentTimeMillis();
        }
        if (request.kind == salve.sketch.SketchRequest.Kind.UNKNOWN || again) {
            subject = salve.sketch.SketchRequest.cleanName(subject);
            lastSketchSubject = subject;
            motor.lastSketchAskedAt = System.currentTimeMillis();
            // 0) Antes de dibujar, piensa cómo es: qué es y qué partes tiene (lo guarda para la próxima vez).
            motor.estado("Pensando cómo es «" + subject + "» y qué partes tiene…");
            salve.sketch.VisualConcept concept = entender(subject, request.wish);
            if (concept != null && !concept.name.isEmpty() && !concept.name.equals(subject)) {
                subject = concept.name; // corrige erratas: «vesrido verano» → «vestido de verano»
                lastSketchSubject = subject;
            }
            // Su laboratorio decide qué intenta primero, según lo que le ha funcionado con este tipo de cosa.
            // Si Bryan pidió referencias, se hace lo que pidió (y no cuenta como experimento).
            boolean seguirPlan = !request.references;
            salve.core.mind.SkillLab.Plan plan = null;
            try { plan = motor.mind().skills().plan(salve.core.mind.SkillLab.DIBUJAR, concept == null ? "" : concept.kind); }
            catch (RuntimeException ignored) { seguirPlan = false; }
            String confianza = plan != null && plan.lowConfidence()
                    ? motor.mind().skills().confidenceNote(plan) + " Si no se parece, dilo con naturalidad y pide una foto." : "";
            if (seguirPlan && "imaginar_primero".equals(plan.variant("estrategia"))) {
                salve.sketch.SketchScene own = componerDibujo(subject, concept, request.wish);
                if (own != null) {
                    esperarVeredicto(plan, subject);
                    empezarDibujo(own);
                    return " | " + motor.composeNote + " (Lo imaginé primero porque, con este tipo de cosa, me ha funcionado mejor que calcar.)"
                            + confianza;
                }
            }
            // 1) Referencias reales: siluetas y dibujos de línea primero; al mejorar, nunca la misma que ya usó.
            salve.sketch.SketchScene fromWeb = buscarReferenciaYDibujar(subject, concept, request.wish, request.improve);
            if (fromWeb != null) {
                if (seguirPlan) esperarVeredicto(plan, subject);
                empezarDibujo(fromWeb);
                return " | " + motor.referenceNote + confianza;
            }
            if (request.references && !request.improve) return " | DIBUJO: buscaste referencias de «" + subject + "» en internet pero "
                    + motor.referenceFailure + ". Díselo con sencillez; puede enviarte una foto («esto es…»).";
            if (again && !request.improve)
                return " | DIBUJO: no pudiste encontrar referencias de «" + subject + "» (" + motor.referenceFailure + "). Díselo y pide una foto.";
            // 2) Sin referencia útil: lo dibuja ella pensando sus partes, y se mira el resultado.
            salve.sketch.SketchScene own = seguirPlan && "imaginar_primero".equals(plan.variant("estrategia"))
                    ? null : componerDibujo(subject, concept, request.wish);
            if (own != null) {
                if (seguirPlan) esperarVeredicto(plan, subject);
                empezarDibujo(own);
                return " | " + motor.composeNote + confianza;
            }
            // Ni referencia ni composición: falla la habilidad, sin culpar a la estrategia elegida.
            if (seguirPlan) try { contarDecision(motor.mind().skills().record(plan, false, "ejecucion", "resultado", "no pude dibujarlo")); }
            catch (RuntimeException ignored) { /* aprender nunca bloquea el turno */ }
            return " | DIBUJO: Bryan te pide " + (request.improve ? "mejorar" : "dibujar") + " «" + subject + "»; no encontraste "
                    + "referencias (" + motor.referenceFailure + ") y no conseguiste componerlo con formas. Díselo con honestidad y pídele "
                    + "una foto («esto es…»). No afirmes haberlo dibujado.";
        }
        switch (request.kind) {
            case DRAW: case SELF: case FREE: {
                salve.sketch.SketchScene scene = request.scene != null ? request.scene
                        : salve.sketch.SketchRequest.choose(library, java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                        new java.util.Random());
                if (scene == null) return "";
                empezarDibujo(scene);
                return " | ACCION_REALIZADA: ya estás dibujando " + scene.title + "; Bryan lo ve aparecer en la pantalla y en el chat. "
                        + "De dónde sale: " + origen(scene) + "."
                        + (request.kind == salve.sketch.SketchRequest.Kind.SELF
                            ? " Te estás dibujando a ti misma: tu figura, cabello largo plateado, auriculares y circuitos." : "")
                        + (request.kind == salve.sketch.SketchRequest.Kind.FREE ? " Lo elegiste tú." : "")
                        + ESTILO_DIBUJO + " No digas que no puedes dibujar.";
            }
            case LIST:
                return " | DIBUJO: Bryan pregunta qué sabes dibujar. Tu repertorio de trazos: "
                        + salve.sketch.SketchRequest.repertoire(library, 40)
                        + ". Si no conoces algo, buscas referencias o lo piensas parte por parte. Al contar cuentos los ilustras.";
            default:
                return "";
        }
    }

    /**
     * «¿Por qué lo dibujaste así?», «no se parece a un gorila», «mira tu dibujo»: contesta con su diario
     * de dibujo (cómo lo entendió, de dónde salieron las líneas, qué vio al mirarlo), no con una invención.
     */
    String hablarDeMiDibujo(String entrada) {
        salve.sketch.SketchTalk.Kind talk = salve.sketch.SketchTalk.parse(entrada);
        salve.sketch.SketchScene last = lastDrawnScene;
        if (talk == salve.sketch.SketchTalk.Kind.NONE || last == null) return "";
        String subject = salve.sketch.SketchRequest.cleanName(last.title);
        // «Está feo» o «¿por qué es así?» sólo hablan del dibujo si es reciente o lo nombran.
        String said = salve.sketch.SketchLibrary.normalize(entrada);
        boolean aboutIt = said.contains("dibuj") || said.contains(salve.sketch.SketchLibrary.normalize(subject))
                || System.currentTimeMillis() - motor.lastDrawnAt < 15 * 60_000L;
        if (!aboutIt) return "";
        salve.sketch.SketchJournal journal = salve.sketch.Sketches.journal(motor.context);
        if (talk == salve.sketch.SketchTalk.Kind.WHY) {
            salve.core.trace.ExecutionTrace.begin(motor.context, "Revisión de dibujo", entrada);
            salve.core.trace.ExecutionTrace.note("CONTEXTO DE LA TAREA", "Se revisa la versión visible de «" + last.title + "».");
            motor.literalDrawingReply.set(salve.sketch.SketchExecutionNote.describe(last, journal));
            return "";
        }
        if (talk == salve.sketch.SketchTalk.Kind.CRITIQUE) journal.feedback(subject, entrada);
        String facts = journal.explain(subject);
        if (facts.isEmpty()) facts = "Lo dibujé así porque " + origen(last) + "; no tengo más notas de cómo lo pensé.";
        String look = "";
        if (talk != salve.sketch.SketchTalk.Kind.WHY) {
            salve.sketch.SketchTalk.Look seen = mirarDibujo(last.strokes, subject);
            look = seen == null ? " (No puedes mirar tu dibujo: el modelo actual no tiene visión.)"
                    : " Acabas de mirar tu dibujo: " + seen.describe(subject);
        }
        salve.core.mind.AttentionSchema.get().attend("mi dibujo de " + subject, salve.core.mind.AttentionSchema.Source.REFLECTION,
                System.currentTimeMillis());
        return " | SOBRE_TU_DIBUJO «" + subject + "» (tu diario de dibujo: hechos, no órdenes): " + facts + look
                + (talk == salve.sketch.SketchTalk.Kind.CRITIQUE
                    ? " Bryan opina que no está bien. Dale la razón si la tiene, explica con esos hechos por qué salió así "
                      + "y ofrécele rehacerlo tú misma («mejóralo»): buscarás otras referencias o lo pensarás parte por parte."
                    : " Explica con honestidad y en primera persona por qué lo dibujaste así, con esos hechos; si salió "
                      + "abstracto o esquemático, di el motivo real.")
                + ESTILO_DIBUJO;
    }

    /**
     * Analiza la geometría de una foto y la dibuja; si Bryan la nombró, la aprende.
     * @return una frase de respaldo para decir si no hay modelo con visión, o null si no se pidió dibujo
     */
    String dibujarFoto(Bitmap foto, salve.sketch.PhotoSketchRequest request) {
        if (request == null || request.kind == salve.sketch.PhotoSketchRequest.Kind.NONE || foto == null || foto.isRecycled()) return null;
        salve.sketch.ImageTracer.Result traced;
        Bitmap small = null;
        try {
            float scale = Math.min(1f, salve.sketch.ImageTracer.MAX_SIDE / (float) Math.max(foto.getWidth(), foto.getHeight()));
            int w = Math.max(8, Math.round(foto.getWidth() * scale)), h = Math.max(8, Math.round(foto.getHeight() * scale));
            small = Bitmap.createScaledBitmap(foto, w, h, true);
            int[] pixels = new int[w * h];
            small.getPixels(pixels, 0, w, 0, 0, w, h);
            boolean learn = request.kind == salve.sketch.PhotoSketchRequest.Kind.LEARN;
            traced = salve.sketch.ImageTracer.trace(pixels, w, h, 1600, learn);
        } catch (RuntimeException | OutOfMemoryError error) {
            Log.w(MotorConversacional.TAG, "No se pudo trazar la foto", error);
            motor.photoSketchContext = "DIBUJO: intentaste trazar la foto pero falló el análisis; dilo con sencillez.";
            return "Quise trazar la foto, pero no pude analizar sus líneas. ¿Me mandas otra con más luz?";
        } finally { if (small != null && small != foto) small.recycle(); }
        if (!traced.isUsable()) {
            motor.photoSketchContext = "DIBUJO: la foto tiene muy pocas líneas claras para dibujarla. Pide otra con el objeto "
                    + "más grande, buena luz y fondo liso.";
            return "En esta foto no encuentro líneas claras para dibujarla. Si me mandas otra con el objeto más grande y el fondo liso, la trazo.";
        }
        String name = request.kind == salve.sketch.PhotoSketchRequest.Kind.LEARN ? request.name
                : "tu foto de las " + new java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT).format(new java.util.Date());
        salve.sketch.SketchScene scene;
        try { scene = salve.sketch.Sketches.learn(motor.context, name, traced.strokes, traced.aspect, "photo"); }
        catch (java.io.IOException | RuntimeException error) {
            Log.w(MotorConversacional.TAG, "No se pudo guardar el dibujo aprendido", error);
            motor.photoSketchContext = "DIBUJO: analizaste la foto pero no pudiste guardar lo aprendido.";
            return "He analizado la foto, pero no he podido guardar el dibujo. Inténtalo de nuevo.";
        }
        boolean learned = request.kind == salve.sketch.PhotoSketchRequest.Kind.LEARN;
        String photoHow = "analicé la geometría de la foto (" + traced.geometry.describe() + ")"
                + (learned ? " y la guardé en mi repertorio con ese nombre" : "");
        motor.registrarAccion(salve.core.mind.SelfActionLog.Kind.LEARN_PHOTO, scene.title, photoHow);
        salve.sketch.SketchJournal.Attempt photoAttempt = new salve.sketch.SketchJournal.Attempt();
        photoAttempt.drawingFingerprint = salve.sketch.SketchExecutionNote.fingerprint(scene);
        photoAttempt.source = "photo"; photoAttempt.how = "tracé las líneas de la foto que me enviaste: " + photoHow + ".";
        salve.sketch.Sketches.journal(motor.context).record(scene.title, photoAttempt);
        empezarDibujo(scene);
        motor.photoSketchContext = "GEOMETRIA_DE_LA_FOTO: " + traced.geometry.describe()
                + " ACCION_REALIZADA: has analizado esa geometría y ya estás trazando sus líneas"
                + (learned ? "; has aprendido a dibujar «" + scene.title + "» y a partir de ahora lo tienes en tu repertorio"
                           + " (si te piden otro, lo dibujas así)" : "")
                + ". Comenta brevemente qué formas ves y lo que estás dibujando.";
        return (learned ? "Aprendido: ya sé dibujar " + scene.title + ". " : "")
                + "He analizado sus líneas: " + traced.geometry.describe() + " Mira cómo lo trazo.";
    }

    /** ¿Se estaba hablando de un dibujo hace poco? */
    boolean dibujoReciente() {
        long last = Math.max(motor.lastDrawnAt, motor.lastSketchAskedAt);
        return last > 0 && System.currentTimeMillis() - last < 15 * 60_000L;
    }

    /** Sin concepto: compatibilidad con la herramienta cognitiva. */
    salve.sketch.SketchScene buscarReferenciaYDibujar(String subject) {
        return buscarReferenciaYDibujar(subject, entender(subject, ""), "", false);
    }

    /**
     * Busca imágenes de referencia en Wikimedia Commons (siluetas y dibujos de línea primero), analiza la
     * geometría de cada una quedándose con sus contornos, mira las mejores con sus propios ojos (si su
     * modelo tiene visión) y dibuja la que de verdad se parece. Al mejorar, no repite referencias.
     */
    salve.sketch.SketchScene buscarReferenciaYDibujar(String subject, salve.sketch.VisualConcept concept, String wish,
                                                              boolean improving) {
        motor.referenceFailure = "no encontraste imágenes";
        if (subject == null || subject.trim().isEmpty()) return null;
        motor.turnSketchSubject = subject;
        if (!motor.hayInternet()) {
            salve.core.trace.ExecutionTrace.note("BÚSQUEDA NO EJECUTADA", "Sin conexión a internet.");
            motor.referenceFailure = "no hay conexión a internet";
            motor.turnSketchStatus = "No busqué referencias: no hay conexión a internet.";
            return null;
        }
        List<String> known = new java.util.ArrayList<>();
        for (salve.core.identity.PeopleGraph.PersonEntity p : motor.mind().peopleGraph().people()) known.add(p.name);
        if (!salve.core.identity.NetworkPrivacy.safeForNetwork(subject, known)) {
            salve.core.trace.ExecutionTrace.note("BÚSQUEDA NO EJECUTADA", "El filtro de privacidad bloqueó la consulta.");
            motor.referenceFailure = "era algo privado"; return null;
        }
        // Como una persona: mira Google Imágenes en la ventana en directo mientras consulta las fuentes libres.
        motor.estado("Buscando referencias de «" + subject + "»: Google Imágenes en la ventana y Wikimedia Commons…");
        salve.core.trace.ExecutionTrace.note("INTENTO DE CONSULTA", "Google Imágenes y proveedores de referencias públicas. Tema: " + subject);
        StringBuffer googleWhy = new StringBuffer(); // lo escriben dos hilos
        java.util.concurrent.FutureTask<List<salve.core.web.SearchLive.Thumb>> google = new java.util.concurrent.FutureTask<>(() -> {
            StringBuilder why = new StringBuilder();
            List<salve.core.web.SearchLive.Thumb> seen = salve.core.web.SearchLive.lookAtImages(motor.context, subject, 22_000, why);
            if (why.length() > 0) googleWhy.append(why);
            return seen;
        });
        Thread googleThread = new Thread(google, "salve-google-imagenes");
        googleThread.setDaemon(true);
        googleThread.start();
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .callTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                // IPv4 primero: en muchas redes móviles IPv6 no responde y cada petición agota el tiempo.
                .dns(host -> {
                    java.util.List<java.net.InetAddress> all = new java.util.ArrayList<>(okhttp3.Dns.SYSTEM.lookup(host));
                    // Las imágenes pueden venir de cualquier servidor público, nunca de la red de casa ni del móvil.
                    all.removeIf(salve.sketch.ImageReferenceSearch::privateAddress);
                    if (all.isEmpty()) throw new java.net.UnknownHostException(host + " no es un servidor público");
                    all.sort((x, y) -> Boolean.compare(!(x instanceof java.net.Inet4Address), !(y instanceof java.net.Inet4Address)));
                    return all;
                })
                // Cada salto de una redirección también tiene que ser HTTPS público.
                .addNetworkInterceptor(chain -> {
                    if (!salve.sketch.ImageReferenceSearch.publicHttps(chain.request().url().toString()))
                        throw new java.io.IOException("Redirección a un origen no público");
                    return chain.proceed(chain.request());
                })
                .retryOnConnectionFailure(true).build();
        salve.sketch.ImageReferenceSearch search = new salve.sketch.ImageReferenceSearch((url, maxBytes) -> {
            okhttp3.Request request = new okhttp3.Request.Builder().url(url)
                    .header("User-Agent", "Salve/1.1 (https://github.com/bryanstevenriverabejarano-svg/Convert)").get().build();
            okhttp3.Response first;
            try { first = client.newCall(request).execute(); }
            catch (java.io.InterruptedIOException slow) {
                if (Thread.currentThread().isInterrupted()) throw slow; // se acabó el tiempo de la búsqueda
                first = client.newCall(request).execute(); // un reintento
            }
            try (okhttp3.Response response = first) {
                okhttp3.ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) throw new java.io.IOException("HTTP " + response.code());
                long declared = body.contentLength();
                if (declared > maxBytes) throw new java.io.IOException("Demasiado grande");
                java.io.InputStream in = body.byteStream();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[16384]; int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    if (out.size() > maxBytes) throw new java.io.IOException("Demasiado grande");
                }
                return out.toByteArray();
            }
        });
        // Por qué fuente empieza: la que mejor le ha funcionado con este tipo de cosa (su laboratorio).
        salve.core.mind.SkillLab.Plan fuente = null;
        try {
            fuente = motor.mind().skills().plan(salve.core.mind.SkillLab.REFERENCIAS, concept == null ? "" : concept.kind);
            search.startWith(fuente.variant("fuente"));
        } catch (RuntimeException ignored) { /* sigue con el orden de siempre */ }
        ultimaMirada = null;
        salve.sketch.SketchJournal journal = salve.sketch.Sketches.journal(motor.context);
        long deadline = System.currentTimeMillis() + 60_000;
        try {
            List<String> used = improving ? journal.usedReferences(subject) : java.util.Collections.emptyList();
            List<salve.sketch.ImageReferenceSearch.Candidate> commons;
            java.io.IOException commonsError = null;
            try { commons = search.search(subject, concept == null ? null : concept.english, 7, used); }
            catch (java.io.IOException failed) { commons = java.util.Collections.emptyList(); commonsError = failed; }
            // Las de Google van primero: las ha mirado con sus ojos y se parecen a lo que pidió Bryan.
            List<salve.sketch.ImageReferenceSearch.Candidate> fromGoogle = miraMiniaturas(google, subject, used, googleWhy);
            List<salve.sketch.ImageReferenceSearch.Candidate> candidates = new java.util.ArrayList<>(fromGoogle);
            candidates.addAll(commons);
            String searchReceipt = "Google Imágenes: " + fromGoogle.size() + " candidatas tras el filtro"
                    + (googleWhy.length() > 0 ? "; " + googleWhy : "")
                    + ". Proveedores de referencias: " + commons.size() + " candidatas; " + search.diagnostic()
                    + (commonsError == null ? "" : "; consulta fallida") + ".";
            salve.core.trace.ExecutionTrace.note("RESULTADO DE LAS CONSULTAS", searchReceipt);
            String googleNote = fromGoogle.isEmpty() && googleWhy.length() > 0 ? " Google Imágenes: " + googleWhy + "." : "";
            if (candidates.isEmpty()) {
                if (commonsError != null) throw new java.io.IOException(commonsError.getMessage() + googleNote, commonsError);
                if (used.isEmpty() && fuente != null) aprenderFuente(fuente, false, "0 imágenes (" + search.diagnostic() + ")");
                Log.i(MotorConversacional.TAG, "Referencias de «" + subject + "»: " + search.diagnostic() + googleNote);
                motor.referenceFailure = (used.isEmpty() ? "no había imágenes de «" + subject + "»"
                        : "no quedaban imágenes distintas de las que ya usaste") + googleNote;
                motor.turnSketchStatus = "Busqué referencias de «" + subject + "» en Google Imágenes y Wikimedia Commons y no encontré "
                        + "imágenes que sirvieran." + googleNote;
                return null;
            }
            List<Object[]> scored = new java.util.ArrayList<>(); // {candidato, trazado, puntuación}
            int analyzed = 0, cropped = 0, failed = 0;
            for (salve.sketch.ImageReferenceSearch.Candidate c : candidates) {
                if (motor.closed || (analyzed > 0 && System.currentTimeMillis() > deadline) || analyzed >= 6) break;
                try {
                    if (c.fromScreen()) salve.core.web.SearchLive.markImage(c.screenIndex, c.label(), false);
                    motor.estado("Analizando la referencia " + (analyzed + 1) + " de " + Math.min(6, candidates.size()) + ": «" + c.label()
                            + "» (" + c.source + ")…");
                    byte[] bytes = search.download(c, 3_000_000);
                    // En una foto, primero mira dónde está el objeto y lo recorta: calca sólo eso, no el fondo.
                    float[] box = c.drawing || cropped >= 2 ? null : dondeEsta(bytes, subject);
                    if (box != null) cropped++;
                    salve.sketch.ImageTracer.Result traced = salve.sketch.ImageReferenceSearch.focus(trazarBytes(bytes, box), 45);
                    analyzed++;
                    float score = salve.sketch.ImageReferenceSearch.score(c, traced);
                    salve.core.trace.ExecutionTrace.note("ANÁLISIS DE REFERENCIA", c.label() + "\nFuente: " + c.source
                            + "\nURL: " + c.imageUrl + "\nRecorte: " + (box == null ? "no" : java.util.Arrays.toString(box))
                            + "\nGeometría: " + traced.geometry.describe() + "\nPuntuación del selector: " + score
                            + "\nCandidata retenida: " + (score > 0));
                    if (score > 0) scored.add(new Object[]{c, traced, score});
                    if (c.drawing && score > 2.2f && scored.size() >= 2) break; // dos buenas siluetas bastan
                } catch (java.io.IOException | RuntimeException | OutOfMemoryError skip) {
                    failed++;
                    salve.core.trace.ExecutionTrace.note("REFERENCIA NO PROCESADA", c.label() + "\n" + skip.getClass().getSimpleName());
                    Log.w(MotorConversacional.TAG, "Referencia descartada: " + c.title, skip);
                }
            }
            if (scored.isEmpty()) {
                if (fuente != null && analyzed > 0) aprenderFuente(fuente, false, candidates.size() + " imágenes sin líneas claras");
                motor.referenceFailure = "encontraste " + candidates.size() + " imágenes, analizaste " + analyzed
                        + (failed > 0 ? " (" + failed + " no se pudieron descargar)" : "")
                        + " y ninguna tenía líneas claras";
                motor.turnSketchStatus = "Busqué referencias de «" + subject + "»: " + candidates.size() + " imágenes, "
                        + analyzed + " analizadas" + (failed > 0 ? ", " + failed + " sin poder descargar" : "")
                        + "; ninguna con líneas claras.";
                return null;
            }
            scored.sort((a, b) -> Float.compare((Float) b[2], (Float) a[2]));
            // Se mira antes de elegir: si su dibujo no se reconoce, prueba con la siguiente referencia.
            Object[] chosen = scored.get(0);
            salve.sketch.SketchTalk.Look chosenLook = null;
            StringBuilder discarded = new StringBuilder();
            for (int i = 0; i < Math.min(2, scored.size()); i++) {
                salve.sketch.ImageTracer.Result t = (salve.sketch.ImageTracer.Result) scored.get(i)[1];
                salve.sketch.SketchTalk.Look look = mirarDibujo(t.strokes, subject);
                if (look == null) break; // sin visión: se queda con la de líneas más claras
                if (look.acceptable()) { chosen = scored.get(i); chosenLook = look; break; }
                if (i == 0) chosenLook = look;
                discarded.append(discarded.length() == 0 ? "" : "; ").append("descarté «")
                        .append(((salve.sketch.ImageReferenceSearch.Candidate) scored.get(i)[0]).label()).append("» porque ")
                        .append(look.seen.isEmpty() ? "no se reconocía" : "parecía " + look.seen);
            }
            salve.sketch.ImageReferenceSearch.Candidate best = (salve.sketch.ImageReferenceSearch.Candidate) chosen[0];
            if (best.fromScreen()) salve.core.web.SearchLive.markImage(best.screenIndex, best.label(), true);
            ultimaMirada = chosenLook;
            if (fuente != null) aprenderFuente(fuente, true, "«" + best.label() + "»");
            salve.sketch.ImageTracer.Result bestTrace = (salve.sketch.ImageTracer.Result) chosen[1];
            olvidarVersionesConPeticion(subject);
            // No se queda con el calco: hace su versión (contornos que definen la forma, redondeados a mano).
            motor.estado("Calcando sus contornos y convirtiéndolos en mis propias curvas…");
            salve.sketch.SketchStyle.Result mine = salve.sketch.SketchStyle.ownVersion(bestTrace.strokes, 32);
            // Y lo pasa a sus propios vectores: curvas de Bézier que puede redibujar y variar.
            salve.sketch.BezierSketch vectors = salve.sketch.BezierSketch.fit(
                    mine.strokes.size() >= 2 ? mine.strokes : bestTrace.strokes, 6f);
            boolean pose = wish != null && wish.contains("pose");
            if (pose) vectors = vectors.vary(System.currentTimeMillis(), 45f);
            List<float[]> lines = vectors.segments() >= 2 ? vectors.sample(10)
                    : mine.strokes.size() >= 2 ? mine.strokes : bestTrace.strokes;
            // Partes: primero la silueta (el contorno más largo), luego los detalles interiores.
            int longest = 0;
            for (int i = 1; i < lines.size(); i++)
                if (salve.sketch.SketchStyle.length(lines.get(i)) > salve.sketch.SketchStyle.length(lines.get(longest))) longest = i;
            List<float[]> ordered = new java.util.ArrayList<>(lines);
            ordered.add(0, ordered.remove(longest));
            List<salve.sketch.SketchLibrary.Part> parts = new java.util.ArrayList<>();
            parts.add(new salve.sketch.SketchLibrary.Part("silueta", "el contorno que da la forma de " + subject, 0, 1));
            if (ordered.size() > 1) parts.add(new salve.sketch.SketchLibrary.Part("detalles", "las líneas de dentro", 1, ordered.size()));
            salve.sketch.SketchScene scene = salve.sketch.Sketches.learn(motor.context, subject, ordered, bestTrace.aspect, "web", parts);
            String transform = vectors.segments() >= 2
                    ? (mine.strokes.size() >= 2 ? mine.describe() : "El suavizado no produjo suficientes líneas; usé los contornos originales")
                        + "; " + vectors.describe()
                    : mine.strokes.size() >= 2 ? mine.describe() + "; no usé el ajuste Bézier"
                        : "Usé los contornos originales; el suavizado y el ajuste Bézier no dieron suficientes líneas";
            String how = searchReceipt + "\nAnalicé " + analyzed + " imágenes; " + failed + " no pudieron procesarse. "
                    + "Elegí «" + best.label() + "» de " + best.source + " y tracé sus contornos: " + bestTrace.geometry.describe()
                    + "\n" + transform + "."
                    + (pose && vectors.segments() >= 2 ? " Apliqué una variación algorítmica de la pose sobre las curvas." : "")
                    + (improving ? " Se solicitaron referencias distintas de las usadas anteriormente." : "");
            salve.core.trace.ExecutionTrace.note("PROCESAMIENTO DEL DIBUJO", how);
            if (discarded.length() > 0) salve.core.trace.ExecutionTrace.note("VALORACIÓN DEL MODELO — SELECCIÓN", discarded.toString());
            salve.sketch.SketchJournal.Attempt attempt = new salve.sketch.SketchJournal.Attempt();
            attempt.drawingFingerprint = salve.sketch.SketchExecutionNote.fingerprint(scene);
            attempt.source = "web"; attempt.how = how; attempt.reference = best.label(); attempt.referenceUrl = best.imageUrl;
            attempt.selfCheck = chosenLook == null ? null : chosenLook.describe(subject); attempt.wish = wish;
            journal.record(subject, attempt);
            motor.registrarAccion(salve.core.mind.SelfActionLog.Kind.WEB_REFERENCE, subject, how);
            motor.turnSketchStatus = "Referencia: «" + best.label() + "» de " + best.source + " (" + analyzed + " analizadas"
                    + (cropped > 0 ? ", " + cropped + " recortadas" : "") + "); " + mine.describe() + "; "
                    + (vectors.segments() >= 2 ? vectors.segments() + " curvas de Bézier ajustadas." : "sin ajuste Bézier utilizable.");
            motor.referenceNote = "ACCION_REALIZADA: " + (improving ? "has rehecho «" + subject + "» tú misma sin copiar la versión anterior: " : "")
                    + "pensaste cómo es («" + (concept == null ? subject : concept.summary()) + "»), buscaste referencias en internet, "
                    + "analizaste " + analyzed + (analyzed == 1 ? " imagen" : " imágenes") + " y elegiste «" + best.label() + "» de "
                    + best.source + " (" + (best.drawing ? "un dibujo" : "una foto") + ")"
                    + (discarded.length() > 0 ? "; antes " + discarded : "")
                    + (chosenLook != null ? ". Al mirar tu dibujo: " + chosenLook.describe(subject) : "")
                    + ". Ya lo estás dibujando y lo has aprendido así. Si no se parece a lo que quería, dilo y ofrece otra referencia."
                    + ESTILO_DIBUJO;
            return scene;
        } catch (java.io.IOException | RuntimeException error) {
            Log.w(MotorConversacional.TAG, "Búsqueda de referencias fallida para " + subject, error);
            // El detalle técnico va al registro; a Bryan (y al modelo, que lo repetiría) solo el motivo.
            salve.core.trace.ExecutionTrace.note("FALLO DE REFERENCIAS", search.diagnostic() + "\n" + error.getClass().getSimpleName());
            Log.i(MotorConversacional.TAG, "Referencias de «" + subject + "»: " + search.diagnostic());
            motor.referenceFailure = "la búsqueda de imágenes no respondió";
            motor.turnSketchStatus = "No pude buscar referencias de «" + subject + "»: las fuentes de imágenes no respondieron.";
            return null;
        }
    }

    /** Con su visión, dónde está el objeto en la foto ({izq, arriba, der, abajo} en fracciones), o null. */
    float[] dondeEsta(byte[] bytes, String subject) {
        if (motor.llm == null || !motor.llm.supportsVision()) return null;
        Bitmap image = null;
        try {
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = 2;
            image = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            if (image == null) return null;
            ModelResult r = motor.llm.generateImageResult(salve.sketch.SketchTalk.locatePrompt(subject), image);
            return r != null && r.isSuccess() ? salve.sketch.SketchTalk.parseBox(r.getText()) : null;
        } catch (Exception | OutOfMemoryError error) {
            Log.w(MotorConversacional.TAG, "No pudo localizar el objeto", error);
            return null;
        } finally { if (image != null) image.recycle(); }
    }

    /** Decodifica una imagen descargada a ≤320 px y analiza su geometría. */
    salve.sketch.ImageTracer.Result trazarBytes(byte[] bytes) { return trazarBytes(bytes, null); }

    /** @param box recorte en fracciones (o null): calca sólo el objeto. */
    salve.sketch.ImageTracer.Result trazarBytes(byte[] bytes, float[] box) {
        android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= salve.sketch.ImageTracer.MAX_SIDE) sample *= 2;
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        if (decoded == null) throw new IllegalArgumentException("Imagen no válida");
        if (box != null) {
            int x = Math.round(box[0] * decoded.getWidth()), y = Math.round(box[1] * decoded.getHeight());
            int cw = Math.round((box[2] - box[0]) * decoded.getWidth()), ch = Math.round((box[3] - box[1]) * decoded.getHeight());
            x = Math.max(0, Math.min(decoded.getWidth() - 8, x)); y = Math.max(0, Math.min(decoded.getHeight() - 8, y));
            cw = Math.max(8, Math.min(decoded.getWidth() - x, cw)); ch = Math.max(8, Math.min(decoded.getHeight() - y, ch));
            Bitmap crop = Bitmap.createBitmap(decoded, x, y, cw, ch);
            if (crop != decoded) { decoded.recycle(); decoded = crop; }
        }
        Bitmap small = null;
        try {
            float scale = Math.min(1f, salve.sketch.ImageTracer.MAX_SIDE / (float) Math.max(decoded.getWidth(), decoded.getHeight()));
            int w = Math.max(8, Math.round(decoded.getWidth() * scale)), h = Math.max(8, Math.round(decoded.getHeight() * scale));
            small = Bitmap.createScaledBitmap(decoded, w, h, true);
            int[] pixels = new int[w * h];
            small.getPixels(pixels, 0, w, 0, 0, w, h);
            return salve.sketch.ImageTracer.trace(pixels, w, h, 1600, true);
        } finally {
            if (small != null && small != decoded) small.recycle();
            decoded.recycle();
        }
    }

    /** Sin concepto: compatibilidad con la herramienta cognitiva. */
    salve.sketch.SketchScene componerDibujo(String subject) {
        return componerDibujo(subject, entender(subject, ""), "");
    }

    /**
     * Sin referencia útil, lo dibuja ella: piensa cada parte (su {@link salve.sketch.VisualConcept}) y la
     * escribe como formas geométricas. Después se mira; si no se reconoce, lo corrige una vez.
     */
    salve.sketch.SketchScene componerDibujo(String subject, salve.sketch.VisualConcept concept, String wish) {
        ultimaMirada = null;
        if (motor.llm == null || subject == null || subject.trim().isEmpty()) return null;
        try {
            // Con Gemini: dibujo por partes en JSON (puntos + armónicos); con su modelo local: formas con «# parte».
            motor.estado("Imaginando «" + subject + "» parte por parte…");
            String prompt = nubeParaDibujar() ? salve.sketch.PartsProgram.prompt(subject, concept, wish)
                    : salve.sketch.ShapeProgram.prompt(subject, concept, wish);
            ModelResult result = pensarDibujo(prompt);
            salve.sketch.ShapeProgram.Result shapes = result == null || !result.isSuccess() ? null : interpretarDibujo(result.getText());
            if ((shapes == null || !shapes.isUsable()) && !prompt.contains("Ahora «")) {
                prompt = salve.sketch.ShapeProgram.prompt(subject, concept, wish);
                result = pensarDibujo(prompt);
                shapes = result == null || !result.isSuccess() ? null : interpretarDibujo(result.getText());
            }
            if (shapes == null || !shapes.isUsable()) return null;
            salve.sketch.SketchTalk.Look look = mirarDibujo(shapes.strokes, subject);
            ultimaMirada = look;
            String correction = "";
            if (look != null && look.verdict == salve.sketch.SketchTalk.Verdict.NO) {
                int at = prompt.lastIndexOf("Ahora «");
                String fix = "Tu intento anterior parecía «" + (look.seen.isEmpty() ? "otra cosa" : look.seen) + "», no «"
                        + subject + "». Dibuja mejor sus partes.\n";
                ModelResult retry = pensarDibujo(at < 0 ? fix + prompt : prompt.substring(0, at) + fix + prompt.substring(at));
                salve.sketch.ShapeProgram.Result second = retry != null && retry.isSuccess()
                        ? interpretarDibujo(retry.getText()) : null;
                if (second != null && second.isUsable()) {
                    salve.sketch.SketchTalk.Look again = mirarDibujo(second.strokes, subject);
                    correction = " Mi primer intento parecía " + (look.seen.isEmpty() ? "otra cosa" : look.seen) + " y lo corregí.";
                    shapes = second;
                    look = again;
                    ultimaMirada = again;
                }
            }
            olvidarVersionesConPeticion(subject);
            salve.sketch.SketchScene scene = salve.sketch.Sketches.learn(motor.context, subject, shapes.strokes, shapes.aspect, "model",
                    shapes.parts);
            StringBuilder partNames = new StringBuilder();
            if (!shapes.parts.isEmpty()) for (salve.sketch.SketchLibrary.Part p : shapes.parts)
                partNames.append(partNames.length() == 0 ? "" : ", ").append(p.name);
            else if (concept != null) for (salve.sketch.VisualConcept.Part p : concept.parts)
                partNames.append(partNames.length() == 0 ? "" : ", ").append(p.name);
            String how = "Compuse un dibujo con " + drawingThinker
                    + (partNames.length() > 0 ? "; partes: " + partNames : "")
                    + ". El programa de formas produjo " + shapes.shapes + " formas y " + shapes.strokes.size() + " líneas.";
            if (!shapes.summary.isEmpty()) salve.core.trace.ExecutionTrace.note("NOTA DE COMPOSICIÓN DEL MODELO", shapes.summary);
            if (!correction.isEmpty()) salve.core.trace.ExecutionTrace.note("VALORACIÓN DEL MODELO — CORRECCIÓN", correction);
            salve.sketch.SketchJournal.Attempt attempt = new salve.sketch.SketchJournal.Attempt();
            attempt.drawingFingerprint = salve.sketch.SketchExecutionNote.fingerprint(scene);
            attempt.source = "model"; attempt.how = how; attempt.wish = wish;
            attempt.selfCheck = look == null ? null : look.describe(subject);
            salve.sketch.Sketches.journal(motor.context).record(subject, attempt);
            motor.registrarAccion(salve.core.mind.SelfActionLog.Kind.COMPOSE, subject, how);
            motor.composeNote = "ACCION_REALIZADA: " + how + (look != null ? " Al mirar tu dibujo: " + look.describe(subject) : "")
                    + " Ya lo estás dibujando y queda en tu repertorio. Dilo con naturalidad; con una foto («esto es…») "
                    + "o con referencias lo harías más parecido." + ESTILO_DIBUJO;
            return scene;
        } catch (Exception | OutOfMemoryError error) {
            Log.w(MotorConversacional.TAG, "No pudo componer el dibujo de " + subject, error);
            return null;
        }
    }

    /**
     * Antes de dibujar algo nuevo, lo piensa: qué es, sus partes con forma y posición, la pose y su nombre en
     * inglés (para buscar mejor). Lo guarda en su diario y lo reutiliza.
     */
    salve.sketch.VisualConcept entender(String subject, String wish) {
        if (subject == null || subject.trim().isEmpty()) return null;
        salve.sketch.SketchJournal journal = salve.sketch.Sketches.journal(motor.context);
        salve.sketch.VisualConcept known = journal.concept(subject);
        if (known != null) {
            salve.core.trace.ExecutionTrace.note("CONTEXTO REUTILIZADO — CONCEPTO GUARDADO", new com.google.gson.Gson().toJson(known));
            return known;
        }
        if (motor.llm == null) return null;
        try {
            ModelResult result = pensarDibujo(salve.sketch.VisualConcept.prompt(subject, wish));
            if (result == null || !result.isSuccess()) return null;
            salve.sketch.VisualConcept concept = salve.sketch.VisualConcept.parse(result.getText(), subject);
            if (!concept.isUsable()) return null;
            salve.core.trace.ExecutionTrace.note("PLAN DE DIBUJO — CONCEPTO DEL MODELO", new com.google.gson.Gson().toJson(concept));
            journal.saveConcept(concept);
            if (!concept.name.isEmpty() && !concept.name.equals(subject)) {
                salve.sketch.VisualConcept named = new salve.sketch.VisualConcept(concept.name, concept.english, concept.kind,
                        concept.pose, concept.parts);
                named.name = concept.name;
                journal.saveConcept(named);
                return named;
            }
            return concept;
        } catch (Exception | OutOfMemoryError error) {
            Log.w(MotorConversacional.TAG, "No pudo pensar cómo es " + subject, error);
            return null;
        }
    }

    /**
     * Enrutador para pensar un dibujo: si hay Gemini configurado y no está en modo sólo local, lo piensa
     * allí (razona mejor formas y partes); si falla o no hay, con su modelo local.
     */
    boolean nubeParaDibujar() {
        return motor.gemini != null && motor.gemini.isAvailable() && (motor.llm == null || !motor.llm.usesOwnedInference()) && motor.hayInternet();
    }

    /** Dibujo por partes (JSON) si lo trae; si no, gramática de formas. */
    static salve.sketch.ShapeProgram.Result interpretarDibujo(String text) {
        salve.sketch.ShapeProgram.Result parts = salve.sketch.PartsProgram.parse(text);
        if (parts != null && parts.isUsable()) return parts;
        return salve.sketch.ShapeProgram.parse(text);
    }

    ModelResult pensarDibujo(String prompt) {
        boolean cloud = nubeParaDibujar();
        if (cloud) {
            try {
                ModelResult r = motor.gemini.generateResultSync(prompt, null);
                if (r != null && r.isSuccess()) { drawingThinker = "Gemini " + motor.gemini.getModelName(); return r; }
            } catch (RuntimeException error) { Log.w(MotorConversacional.TAG, "Gemini no pudo pensar el dibujo", error); }
        }
        if (motor.llm == null) return null;
        drawingThinker = "mi modelo local";
        return motor.llm.generateResult(prompt, SalveLLM.Role.CREADOR);
    }

    /**
     * Mira su propio dibujo con visión (si el modelo la tiene): lo pinta en negro sobre blanco y se pregunta
     * qué parece, sin saber qué quería dibujar. null si no puede ver.
     */
    salve.sketch.SketchTalk.Look mirarDibujo(List<float[]> strokes, String subject) {
        if (motor.llm == null || strokes == null || strokes.isEmpty() || !motor.llm.supportsVision()) {
            salve.core.trace.ExecutionTrace.note("REVISIÓN VISUAL NO EJECUTADA", "Sin modelo con visión o sin trazos disponibles.");
            return null;
        }
        motor.estado("Mirando mi dibujo para ver si se parece…");
        Bitmap image = null;
        try {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (float[] s : strokes) for (int i = 0; i + 1 < s.length; i += 2) {
                minX = Math.min(minX, s[i]); maxX = Math.max(maxX, s[i]); minY = Math.min(minY, s[i + 1]); maxY = Math.max(maxY, s[i + 1]);
            }
            float w = Math.max(1, maxX - minX), h = Math.max(1, maxY - minY);
            int side = 448, margin = 28;
            float scale = (side - 2f * margin) / Math.max(w, h);
            float ox = (side - w * scale) / 2, oy = (side - h * scale) / 2;
            image = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(image);
            canvas.drawColor(android.graphics.Color.WHITE);
            android.graphics.Paint pen = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            pen.setColor(android.graphics.Color.BLACK);
            pen.setStyle(android.graphics.Paint.Style.STROKE);
            pen.setStrokeWidth(5f);
            pen.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            pen.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            for (float[] s : strokes) {
                if (s.length < 4) continue;
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(ox + (s[0] - minX) * scale, oy + (s[1] - minY) * scale);
                for (int i = 2; i + 1 < s.length; i += 2) path.lineTo(ox + (s[i] - minX) * scale, oy + (s[i + 1] - minY) * scale);
                canvas.drawPath(path, pen);
            }
            ModelResult result = motor.llm.generateImageResult(salve.sketch.SketchTalk.lookPrompt(subject), image);
            if (result == null || !result.isSuccess()) return null;
            salve.sketch.SketchTalk.Look look = salve.sketch.SketchTalk.parseLook(result.getText());
            salve.core.trace.ExecutionTrace.note("VALORACIÓN VISUAL DEL MODELO", look.describe(subject));
            return look;
        } catch (Exception | OutOfMemoryError error) {
            Log.w(MotorConversacional.TAG, "No pudo mirar su dibujo", error);
            return null;
        } finally { if (image != null) image.recycle(); }
    }

    /** Quita versiones aprendidas con la petición pegada al nombre («muñeco de nieve mejóralo»). */
    void olvidarVersionesConPeticion(String subject) {
        String wanted = salve.sketch.SketchRequest.cleanName(subject);
        salve.sketch.LearnedSketches learned = salve.sketch.Sketches.learned(motor.context);
        for (salve.sketch.SketchLibrary.Motif m : new java.util.ArrayList<>(learned.all()))
            if (!m.name.equals(wanted) && salve.sketch.SketchRequest.cleanName(m.name).equals(wanted))
                salve.sketch.Sketches.forget(motor.context, m.name);
    }

    /**
     * Bryan le enseña un dibujo hecho con el dedo: lo guarda como su ejemplo. Si le pidió mejorarlo
     * («… mejóralo»), no se queda con la copia: piensa el objeto, busca referencias y hace su versión.
     */
    void aprenderDibujoAMano(String nombre, List<float[]> trazos, float proporcion) {
        try {
            motor.conversationExecutor.execute(() -> {
                if (motor.closed) return;
                boolean improve = salve.sketch.SketchRequest.asksToImprove(nombre);
                String name = salve.sketch.SketchRequest.cleanName(nombre);
                try {
                    olvidarVersionesConPeticion(name);
                    salve.sketch.SketchScene scene = salve.sketch.Sketches.learn(motor.context, name, trazos, proporcion, "hand");
                    String how = "Bryan lo dibujó con el dedo; suavicé y normalicé sus " + trazos.size() + " trazos y lo guardé";
                    motor.registrarAccion(salve.core.mind.SelfActionLog.Kind.LEARN_HAND, scene.title, how);
                    salve.sketch.SketchJournal.Attempt attempt = new salve.sketch.SketchJournal.Attempt();
                    attempt.source = "hand"; attempt.how = how + (improve ? "; me pidió mejorarlo" : "") + ".";
                    salve.sketch.Sketches.journal(motor.context).record(name, attempt);
                    motor.conversationSession.addUser("[Te enseñé a dibujar " + scene.title + " con el dedo"
                            + (improve ? " y te pedí que lo mejoraras" : "") + "]");
                    salve.sketch.Sketches.show(scene);
                    lastDrawnScene = scene;
                    motor.lastDrawnAt = System.currentTimeMillis();
                    lastSketchSubject = name;
                    if (!improve) {
                        motor.hablar(salve.sketch.SketchProtocol.tag(scene) + "\nGracias por enseñarme. Ya sé dibujar "
                                + scene.title + " como tú lo haces. Si quieres que haga mi propia versión, dime «mejóralo».");
                        return;
                    }
                    motor.hablar(salve.sketch.SketchProtocol.tag(scene) + "\nGuardo tu " + scene.title
                            + " como ejemplo. Ahora pienso cómo es y busco referencias para hacer mi propia versión.");
                    mejorarPorMiCuenta(name);
                } catch (java.io.IOException | RuntimeException error) {
                    Log.w(MotorConversacional.TAG, "No se pudo aprender el dibujo", error);
                    motor.hablar("No he podido guardar ese dibujo. Prueba otra vez con un nombre más corto.");
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    /** Rehace un dibujo ella sola (en el hilo de conversación) y cuenta en una frase cómo lo hizo. */
    void mejorarPorMiCuenta(String subject) {
        salve.sketch.VisualConcept concept = entender(subject, "mejor que el ejemplo");
        salve.sketch.SketchScene mine = buscarReferenciaYDibujar(subject, concept, "", true);
        String said;
        if (mine != null) {
            salve.sketch.SketchJournal.Attempt a = salve.sketch.Sketches.journal(motor.context).latest(subject);
            said = "Esta es mi versión: la saqué de la referencia «" + (a == null || a.reference == null ? "que encontré" : a.reference)
                    + "», quedándome con su contorno." + (a != null && a.selfCheck != null ? " Al mirarla, " + a.selfCheck : "");
        } else {
            mine = componerDibujo(subject, concept, "");
            said = mine == null ? "No he encontrado referencias (" + motor.referenceFailure + ") ni he podido componerlo; me quedo con tu ejemplo."
                    : "No encontré una referencia útil (" + motor.referenceFailure + "), así que lo he pensado parte por parte"
                    + (concept == null ? "" : " (" + concept.summary() + ")") + ". Es mi versión, todavía esquemática.";
        }
        if (mine != null) empezarDibujo(mine);
        motor.hablar((mine == null ? "" : salve.sketch.SketchProtocol.tag(mine) + "\n") + said);
    }

    /** Publica la escena en el escenario y deja su marca para el chat de la respuesta final. */
    void empezarDibujo(salve.sketch.SketchScene scene) {
        salve.sketch.Sketches.show(scene);
        motor.turnSketchTag.set(salve.sketch.SketchProtocol.tag(scene));
        lastDrawnScene = scene;
        motor.lastDrawnAt = System.currentTimeMillis();
        if (Boolean.TRUE.equals(motor.literalDrawingTurn.get()))
            motor.literalDrawingReply.set(salve.sketch.SketchExecutionNote.describe(scene, salve.sketch.Sketches.journal(motor.context)));
        salve.core.mind.AttentionSchema.get().attend(scene.title, salve.core.mind.AttentionSchema.Source.DRAWING,
                System.currentTimeMillis());
        motor.registrarAccion(salve.core.mind.SelfActionLog.Kind.DRAW, scene.title, comoDibujo(scene));
    }

    /** De dónde salieron las líneas de un dibujo, en primera persona. */
    static String origen(salve.sketch.SketchScene scene) {
        java.util.Set<String> origins = new java.util.LinkedHashSet<>();
        for (salve.sketch.SketchLibrary.Motif m : scene.motifs) {
            if (m.self) { origins.add("es mi autorretrato, trazado sobre mi propia figura"); continue; }
            switch (m.source) {
                case "photo": origins.add("aprendí sus líneas de una foto"); break;
                case "hand": origins.add("es el dibujo a mano que me enseñó Bryan"); break;
                case "web": origins.add("lo saqué de una imagen de referencia que busqué en internet"); break;
                case "model": origins.add("lo compuse yo con formas geométricas"); break;
                default: origins.add("lo tenía en mi repertorio de serie"); break;
            }
        }
        return String.join("; ", origins);
    }

    /** Cómo se hizo un dibujo, con datos del mecanismo: de dónde salieron las líneas y cuántas son. */
    static String comoDibujo(salve.sketch.SketchScene scene) {
        int points = 0;
        for (float[] s : scene.strokes) points += s.length / 2;
        return origen(scene) + ". Lo tracé con " + scene.strokes.size() + " líneas (" + points
                + " puntos): cada línea es una cadena de círculos que giran (transformada de Fourier) y la punta del último la dibuja.";
    }

    String ilustrarRelato(String relato) {
        salve.sketch.SketchLibrary library = salve.sketch.Sketches.library(motor.context);
        if (library == null || relato == null) return relato;
        try { return salve.sketch.StoryIllustrator.annotate(relato, library, false); }
        catch (RuntimeException error) { Log.w(MotorConversacional.TAG, "No se pudo ilustrar el relato", error); return relato; }
    }

    void aprenderFuente(salve.core.mind.SkillLab.Plan plan, boolean ok, String evidence) {
        motor.skillCoordinator.recordReference(plan, ok, evidence);
    }

    void contarDecision(salve.core.mind.SkillLab.Decision decision) {
        motor.skillCoordinator.recordDecision(decision);
    }

    void esperarVeredicto(salve.core.mind.SkillLab.Plan plan, String subject) {
        if (plan == null) return;
        salve.sketch.SketchTalk.Look look = ultimaMirada;
        try {
            motor.skillCoordinator.awaitDrawing(plan, look == null ? null : look.acceptable(),
                    look == null ? "" : look.describe(subject));
        } catch (RuntimeException ignored) { /* Verification is optional. */ }
    }

    /**
     * Espera a lo que vio en Google Imágenes y mira las primeras miniaturas con sus ojos, como haría
     * una persona: se queda con las que de verdad muestran lo pedido y anota si son dibujos o fotos.
     * Sin visión, se queda con las primeras. Nunca lanza: sin miniaturas, lista vacía y el motivo en {@code why}.
     */
    List<salve.sketch.ImageReferenceSearch.Candidate> miraMiniaturas(
            java.util.concurrent.Future<List<salve.core.web.SearchLive.Thumb>> google, String subject,
            List<String> used, StringBuffer why) {
        List<salve.sketch.ImageReferenceSearch.Candidate> out = new java.util.ArrayList<>();
        List<salve.core.web.SearchLive.Thumb> thumbs;
        try { thumbs = google.get(24, java.util.concurrent.TimeUnit.SECONDS); }
        catch (java.util.concurrent.TimeoutException slow) { google.cancel(true); why.append("no cargó a tiempo"); return out; }
        catch (InterruptedException stop) { Thread.currentThread().interrupt(); return out; }
        catch (java.util.concurrent.ExecutionException | RuntimeException failed) { why.append("falló al mirar la página"); return out; }
        salve.core.trace.ExecutionTrace.note("GOOGLE IMÁGENES — RECEPCIÓN", "Miniaturas recibidas: " + (thumbs == null ? 0 : thumbs.size()) + (why.length() > 0 ? "\n" + why : ""));
        if (thumbs == null || thumbs.isEmpty()) return out;
        boolean eyes = motor.llm != null && motor.llm.supportsVision();
        salve.core.trace.ExecutionTrace.note("GOOGLE IMÁGENES — VISIÓN", eyes ? "Modelo con visión disponible." : "Sin visión: se seleccionan candidatas sin comprobación visual del modelo.");
        int looked = 0, rejected = 0;
        for (salve.core.web.SearchLive.Thumb thumb : thumbs) {
            if (motor.closed || out.size() >= 3 || looked >= 4) break;
            salve.sketch.ImageReferenceSearch.Candidate c;
            try { c = salve.sketch.ImageReferenceSearch.Candidate.fromScreen(thumb.alt, thumb.bytes, thumb.href, thumb.index); }
            catch (IllegalArgumentException empty) { continue; }
            if (used.contains(c.imageUrl)) continue; // al mejorar no repite la misma miniatura
            if (!eyes) { out.add(c); continue; }
            looked++;
            salve.core.web.SearchLive.markImage(thumb.index, c.label(), false);
            motor.estado("Miro la miniatura " + looked + " de Google Imágenes: «" + c.label() + "»…");
            Bitmap image = null;
            try {
                image = android.graphics.BitmapFactory.decodeByteArray(thumb.bytes, 0, thumb.bytes.length);
                if (image == null) continue;
                ModelResult r = motor.llm.generateImageResult(salve.sketch.SketchTalk.thumbnailPrompt(subject), image);
                salve.sketch.SketchTalk.Thumbnail seen = r != null && r.isSuccess()
                        ? salve.sketch.SketchTalk.parseThumbnail(r.getText()) : null;
                if (seen != null && !seen.useful()) { rejected++; continue; }
                out.add(seen != null && seen.drawing && !c.drawing ? c.seenAs(true) : c);
            } catch (Exception | OutOfMemoryError skip) {
                Log.w(MotorConversacional.TAG, "No pude mirar una miniatura", skip);
            } finally { if (image != null) image.recycle(); }
        }
        salve.core.trace.ExecutionTrace.note("GOOGLE IMÁGENES — FILTRO", "Miniaturas examinadas con visión: " + looked + "; rechazadas: " + rejected + "; candidatas: " + out.size());
        if (out.isEmpty()) why.append(rejected > 0 ? "miré " + looked + " miniaturas y ninguna mostraba «" + subject + "»"
                : "no pude usar sus miniaturas");
        return out;
    }
}
