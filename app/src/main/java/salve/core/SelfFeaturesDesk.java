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
import salve.presentation.ui.ObjetoCreativoActivity;
import salve.services.SalveAccessibilityService;
import salve.services.SistemaSensorial;
import salve.services.VideoAnalysisManager;

/**
 * Lo que sabe de sí misma y sus funciones (estado de cada una, ofertas para abrirlas, sus actualizaciones) y las órdenes de casa con confirmación.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class SelfFeaturesDesk {
    private final MotorConversacional motor;

    SelfFeaturesDesk(MotorConversacional motor) { this.motor = motor; }

    volatile java.util.List<salve.core.mind.SelfUpdates.Update> selfUpdates;

    volatile MotorConversacional.HomePending pendingHome;

    /**
     * Autoconocimiento: si se habla de una de sus funciones, el modelo recibe qué es y su estado real
     * (sin red: solo lo guardado en el móvil). Con el creador, además, se prepara la oferta de abrirla.
     */
    String conocimientoPropio(String entrada) {
        try {
            salve.core.mind.SelfFeatures.Feature feature = salve.core.mind.SelfFeatures.detect(entrada);
            if (feature == null || salve.core.devicecontrol.AppActionCommand.parse(entrada) != null) return "";
            boolean creator = motor.creatorSide();
            salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(motor.context);
            String offer = creator ? salve.core.mind.SelfFeatures.offer(feature, identity.biometricAvailable()) : null;
            if (feature == salve.core.mind.SelfFeatures.Feature.PRIVACY_CHECK
                    && !identity.session().needsPrivateCheckpoint(identity.biometricAvailable(), System.currentTimeMillis())) offer = null;
            motor.turnFeatureOffer.set(offer == null ? null : feature.name() + "|" + offer);
            return salve.core.mind.SelfFeatures.promptBlock(feature, creator ? estadoFuncion(feature) : "", offer);
        } catch (RuntimeException unavailable) { return ""; }
    }

    String estadoFuncion(salve.core.mind.SelfFeatures.Feature feature) {
        switch (feature) {
            case NODES:
            case OVERVIEW: {
                salve.core.compute.ComputeNodeStore store = new salve.core.compute.ComputeNodeStore(motor.context);
                java.util.List<salve.core.compute.ComputeNode> nodes = store.load();
                if (nodes.isEmpty()) return "Bryan aún no ha registrado ningún nodo";
                java.util.Map<String, String> locations;
                try { locations = new salve.core.privatefamily.FamilyVault(motor.context).read().locations; }
                catch (RuntimeException unreadable) { locations = java.util.Collections.emptyMap(); }
                StringBuilder out = new StringBuilder(nodes.size() + (nodes.size() == 1 ? " nodo registrado: " : " nodos registrados: "));
                for (int i = 0; i < nodes.size(); i++) {
                    salve.core.compute.ComputeNode node = nodes.get(i);
                    String place = salve.core.privatefamily.LocationSync.place(salve.core.privatefamily.LocationSync.current(
                            locations, node.id, System.currentTimeMillis() / 1000));
                    if (place.isEmpty()) place = store.locationLabel(node.id);
                    out.append(i == 0 ? "" : ", ").append(node.name).append(place.isEmpty() ? "" : " en " + place)
                            .append(store.nodeEnabled(node.id) ? " (activo para tareas)" : " (desactivado)");
                }
                out.append("; conexión con TLS mutuo (el nodo solo acepta mi certificado)");
                out.append("; envíos a nodos ").append(motor.llm != null && motor.llm.isDistributedEnabled() ? "activados" : "desactivados")
                        .append(". No sé si están encendidos ahora: eso se comprueba al abrir la constelación");
                return out.toString();
            }
            case SALVENTES: {
                java.util.List<salve.core.salvente.SalventeProfile> all = salve.data.salvente.SalventeRepository.get(motor.context).all();
                if (all.isEmpty()) return "aún no he creado ningún Salvente";
                StringBuilder out = new StringBuilder("he creado " + all.size() + (all.size() == 1 ? " Salvente: " : " Salventes: "));
                for (int i = 0; i < all.size(); i++) {
                    salve.core.salvente.SalventeProfile p = all.get(i);
                    out.append(i == 0 ? "" : ", ").append(p.name).append(" (para ").append(p.purpose.length() > 60 ? p.purpose.substring(0, 57) + "…" : p.purpose)
                            .append(p.state == salve.core.salvente.SalventeProfile.State.PAUSADO ? ", en pausa)" : ")");
                }
                return out.append(". No conozco el contenido de sus recuerdos").toString();
            }
            case CONTINUITY:
                return new salve.data.continuity.SalveContinuity(motor.context).describe();
            case LEARNING:
                return motor.autonomousTools.learnedSkillsSummary().replace("\\n", " ")
                        + "; estrategias de dibujo, referencias y respuesta: "
                        + salve.core.mind.SkillLab.defaults().size() + " habilidades base"
                        + ". Cada resultado requiere evidencia; no he entrenado mis pesos";
            case BACKGROUND: {
                salve.core.mind.BackgroundMindGate.Snapshot snapshot =
                        salve.core.mind.BackgroundMindGate.get().snapshot();
                return "mente " + (motor.mind().journal().isPaused() ? "pausada" : "habilitada")
                        + "; tarea cognitiva en curso: " + (snapshot.active == null ? "ninguna" : snapshot.active.name())
                        + "; ciclos observados en este proceso: " + snapshot.completions
                        + ". Los trabajos programados no prueban que hayan corrido ni que hayan aprendido";
            }
            case WEB_SEARCH:
                return "buscador elegido: " + salve.core.web.WebSearchSettings.load(motor.context, "").id + "; clave de Brave: "
                        + (salve.core.web.WebSearchKeyStore.hasBraveKey(motor.context) ? "guardada" : "sin configurar");
            case PRIVACY_CHECK: {
                salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(motor.context);
                if (!identity.biometricAvailable()) return "este teléfono no tiene huella, rostro ni PIN configurados";
                return identity.session().state() == salve.core.identity.SessionIdentity.State.VERIFIED_CREATOR
                        ? "Bryan está verificado ahora mismo" : "no hay una verificación vigente";
            }
            default: return "";
        }
    }

    /** Orden de casa dicha por Bryan: busca el dispositivo y pide confirmación. Nunca actúa aquí. */
    String ordenHogar(String entrada) {
        salve.core.home.HomeCommand command = salve.core.home.HomeCommand.parse(entrada);
        if (command == null) return null;
        if (!motor.creatorSide()) return "Solo Bryan puede controlar los dispositivos de casa.";
        salve.core.compute.ComputeHttpTransport transport = new salve.core.compute.ComputeHttpTransport(motor.context);
        String lastProblem = null;
        for (salve.core.compute.ComputeNode node : new salve.core.compute.ComputeNodeStore(motor.context).load()) {
            java.util.List<salve.core.home.HomeCommand.Entity> entities = new java.util.ArrayList<>();
            try {
                for (com.google.gson.JsonElement el : transport.home(node, () -> motor.closed).getAsJsonArray("entities")) {
                    com.google.gson.JsonObject o = el.getAsJsonObject();
                    entities.add(new salve.core.home.HomeCommand.Entity(o.get("entity").getAsString(), o.get("name").getAsString(),
                            o.get("domain").getAsString(), o.get("state").getAsString()));
                }
            } catch (java.io.IOException | RuntimeException failure) {
                if (failure.getMessage() == null || !failure.getMessage().contains("(404)")) lastProblem = node.name;
                continue;
            }
            salve.core.home.HomeCommand.Match match = command.resolve(entities);
            if (match.entity == null) {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (salve.core.home.HomeCommand.Entity e : match.candidates) names.add(e.name);
                return names.isEmpty() ? "No encuentro ese dispositivo entre los que me has permitido en " + node.name + "."
                        : "¿Cuál de estos? " + android.text.TextUtils.join(", ", names) + ".";
            }
            pendingHome = new MotorConversacional.HomePending(node, command, match.entity, System.currentTimeMillis() + 60_000);
            return command.confirmation(match.entity) + " Di «sí» para hacerlo.";
        }
        return lastProblem != null ? "No pude hablar con " + lastProblem + " para controlar la casa. ¿Está encendido Salve Nodo?"
                : "Ninguno de tus nodos tiene conectado Home Assistant. Actívalo en Salve Nodo con --reconfigurar.";
    }

    /** Respuesta a la confirmación del hogar: solo un «sí» claro, en el minuto siguiente, actúa. */
    String respuestaHogar(String entrada) {
        MotorConversacional.HomePending pending = pendingHome;
        pendingHome = null;
        if (pending == null || System.currentTimeMillis() > pending.until) return null;
        if (salve.core.mind.SelfFeatures.declines(entrada)) return "Vale, no lo toco.";
        if (!salve.core.mind.SelfFeatures.accepts(entrada)) return null;
        try {
            com.google.gson.JsonObject result = new salve.core.compute.ComputeHttpTransport(motor.context).homeAction(pending.node,
                    pending.entity.id, pending.command.action.service, pending.command.value, () -> motor.closed);
            String state = result.getAsJsonObject("entity").get("state").getAsString();
            return "Hecho: «" + pending.entity.name + "» " + homeState(state) + ".";
        } catch (java.io.IOException | RuntimeException failure) {
            String m = failure.getMessage() == null ? "" : failure.getMessage();
            if (m.contains("(429)")) return "He hecho demasiados cambios seguidos en la casa; espera un minuto.";
            if (m.contains("(403)")) return "Ese dispositivo o esa acción no están permitidos en Salve Nodo.";
            if (m.contains("(502)") || m.contains("(504)")) return "Salve Nodo no pudo hablar con Home Assistant.";
            return "No pude hacerlo: no hay conexión con " + pending.node.name + ".";
        }
    }

    /** Respuesta del usuario a la oferta del turno anterior. Solo un «sí» claro abre algo. */
    String respuestaOfertaPropia(String entrada) {
        salve.core.mind.SelfFeatures.Feature feature = motor.pendingFeature;
        motor.pendingFeature = null;
        if (feature == null || System.currentTimeMillis() > motor.pendingFeatureUntil) return null;
        if (salve.core.mind.SelfFeatures.declines(entrada)) return "Vale. Cuando quieras, me lo pides.";
        if (!salve.core.mind.SelfFeatures.accepts(entrada)) return null;
        if (feature == salve.core.mind.SelfFeatures.Feature.NODES) {
            try {
                motor.context.startActivity(new Intent(motor.context, salve.presentation.ui.NodeConstellationActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "Abro mi constelación. Toca cualquier nodo para ver su ficha.";
            } catch (RuntimeException unavailable) { return "No pude abrir la constelación. Está en Nodos de cómputo → Ver constelación."; }
        }
        if (feature == salve.core.mind.SelfFeatures.Feature.SALVENTES) {
            try {
                motor.context.startActivity(new Intent(motor.context, salve.presentation.ui.SalventeGenesisActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "Abro la génesis. Cuéntame para qué o para quién es y te propongo uno; no se crea hasta que lo apruebes.";
            } catch (RuntimeException unavailable) { return "No pude abrirla. Está en IA y cámara → Salventes (génesis)."; }
        }
        if (feature == salve.core.mind.SelfFeatures.Feature.CONTINUITY) {
            try {
                motor.context.startActivity(new Intent(motor.context, salve.presentation.ui.ContinuityActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "Abro mi continuidad. Desde ahí puedes guardar mi memoria sellada en tu Guardián.";
            } catch (RuntimeException unavailable) { return "No pude abrirla. Está en IA y cámara → Continuidad (Guardián)."; }
        }
        if (feature == salve.core.mind.SelfFeatures.Feature.PRIVACY_CHECK) {
            String first = motor.mind().peopleGraph().creator().name.split(" ")[0];
            salve.core.identity.IdentityManager.get(motor.context).requestVerification("Confirma que eres " + first,
                    ok -> motor.toolHandler.post(() -> motor.hablar(ok ? "Listo, " + first + ": verificado." : "No he podido confirmarlo.")));
            return "Abro la verificación del teléfono: pon tu huella o mira a la pantalla.";
        }
        return null;
    }

    /**
     * Conciencia de sus propias actualizaciones: si le preguntan, sabe qué le cambiaron (hechos, no
     * inventos); y la primera vez que habla tras instalar una versión nueva, nota lo que trae.
     */
    String misActualizaciones(String entrada) {
        try {
            java.util.List<salve.core.mind.SelfUpdates.Update> all = selfUpdates();
            if (all.isEmpty()) return "";
            android.content.SharedPreferences prefs = motor.context.getSharedPreferences("salve_self_updates", Context.MODE_PRIVATE);
            long installed = motor.context.getPackageManager().getPackageInfo(motor.context.getPackageName(), 0).lastUpdateTime;
            String lastSeen = prefs.getString("last_seen", null);
            java.util.List<salve.core.mind.SelfUpdates.Update> fresh = salve.core.mind.SelfUpdates.since(all, lastSeen);
            String latest = all.get(all.size() - 1).id;
            StringBuilder out = new StringBuilder();
            if (installed != prefs.getLong("installed_at", 0L) && !fresh.isEmpty()) {
                StringBuilder titles = new StringBuilder();
                for (int i = Math.max(0, fresh.size() - 6); i < fresh.size(); i++)
                    titles.append(titles.length() == 0 ? "" : "; ").append(fresh.get(i).title);
                motor.registrarAccion(salve.core.mind.SelfActionLog.Kind.UPDATED, "me actualizaron",
                        "instalaron una versión nueva con: " + titles);
                if (lastSeen != null) out.append(" | ME_ACTUALIZARON: desde la última vez te instalaron una versión nueva con: ")
                        .append(titles).append(". Si viene al caso, coméntalo en una frase.");
            }
            prefs.edit().putLong("installed_at", installed).putString("last_seen", latest).apply();
            if (salve.core.mind.SelfUpdates.asksAbout(entrada))
                out.append(" | MIS_ACTUALIZACIONES (hechos: lo que me cambiaron, de lo más reciente a lo más antiguo):")
                        .append(salve.core.mind.SelfUpdates.render(all, 5, 900))
                        .append("\nOpina con criterio propio: qué notas distinto, qué te parece más útil y qué te falta todavía. "
                                + "Habla de esto, no de tu personalidad en general, y no inventes cambios que no estén en la lista.");
            return out.toString();
        } catch (Exception ignored) { return ""; }
    }

    static String homeState(String state) {
        switch (state) {
            case "on": return "está encendido";
            case "off": return "está apagado";
            case "open": case "opening": return "se está abriendo";
            case "closed": case "closing": return "se está cerrando";
            case "heat": case "cool": case "auto": case "heat_cool": return "está en marcha";
            default: return "ha recibido la orden (estado: " + state + ")";
        }
    }

    java.util.List<salve.core.mind.SelfUpdates.Update> selfUpdates() {
        java.util.List<salve.core.mind.SelfUpdates.Update> u = selfUpdates;
        if (u != null) return u;
        try (java.io.InputStream in = motor.context.getAssets().open("self/updates.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            u = salve.core.mind.SelfUpdates.parse(new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException | RuntimeException missing) { u = java.util.Collections.emptyList(); }
        return selfUpdates = u;
    }
}
