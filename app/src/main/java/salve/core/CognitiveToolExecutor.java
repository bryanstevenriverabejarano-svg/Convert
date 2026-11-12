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
 * Ejecuta las herramientas que el modelo pide en un turno. El catálogo (CognitiveToolCatalog) decide cuáles hay; aquí se reparten: dibujo al taller, web a la mesa de investigación, y memoria, nube, estado, pantalla y vestuario aquí.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class CognitiveToolExecutor {
    private final MotorConversacional m;

    CognitiveToolExecutor(MotorConversacional m) { this.m = m; }

    /** Existing Android/UI executors remain here; registry validates tool name and
     * allowed argument keys before invoking any operation. External effects still
     * use their specific confirmation or per-tool policy. */
    CognitiveTurn.Receipt executeCognitiveTool(String name, JsonObject args, String input) {
        switch (name) {
            case "evidence.inspect": {
                // Arguments from Gemma are never an authorization. The real, verified
                // creator must ask in the current turn for private local evidence.
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Sin identidad verificada para informes privados.");
                if (salve.core.cognition.ConversationEvidenceIntent.classify(
                        input, m.conversationSession.snapshot())
                        == salve.core.cognition.ConversationEvidenceIntent.Scope.NONE)
                    throw new IllegalArgumentException("La petición actual no autoriza consultar este registro.");
                String scope = CognitiveReply.string(args, "scope");
                String verified = salve.core.cognition.ReadOnlyEvidenceDesk.render(m.context, scope);
                salve.core.trace.ExecutionTrace.note("FUENTE LOCAL VERIFICADA",
                        "Consulté " + scope + "; se entregó solo el registro local disponible.");
                m.hablar(verified);
                return CognitiveTurn.Receipt.handoff();
            }
            case "memory.lookup": {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Sin autorización para recuerdos privados");
                String requested = args.has("query") && args.get("query").isJsonPrimitive()
                        && args.get("query").getAsJsonPrimitive().isString()
                        ? args.get("query").getAsString() : null;
                String scoped = salve.core.conversation.MemoryProbePolicy.scoped(
                        input, m.conversationSession.snapshot(), requested);
                ConversationMemoryGrounding.Result result = m.memoria.recuperarContextoConversacional(scoped);
                String layers = m.mind().memoryLayersForPrompt(scoped, result);
                // Receipt distinguishes primary records from derived, source-checked
                // layers. Never replaces missing Room evidence with model beliefs.
                return new CognitiveTurn.Receipt(result.getStatus().name() + "\n"
                        + result.getDataContext() + (layers.isEmpty() ? "" : "\n" + layers));
            }
            case "web.search":
                return m.mesaInvestigacion.herramientaBuscarWeb(CognitiveReply.string(args, "query").trim(), input);
            case "sketch.draw":
                return m.dibujo.herramientaDibujar(CognitiveReply.string(args, "motivos").trim());
            case "cloud.sync": {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Nube privada requiere identidad comprobada");
                if (!salve.data.sync.CloudSyncManager.isPCloudConfigured(m.context))
                    return new CognitiveTurn.Receipt("NUBE_SIN_CONECTAR: pCloud no está conectada; Bryan debe autorizarla una vez en IA y cámara → Nube pCloud.");
                if (!salve.data.sync.CloudSyncManager.isEnabled(m.context)
                        && salve.data.sync.CloudSyncManager.pausedByUser(m.context))
                    return new CognitiveTurn.Receipt("NUBE_EN_PAUSA: Bryan la pausó; no la reactives sin que te lo pida.");
                if (!salve.data.sync.CloudSyncManager.isEnabled(m.context))
                    salve.data.sync.CloudSyncManager.setEnabled(m.context, true, false);
                salve.data.sync.CloudSyncManager.RestoreBatch batch =
                        salve.data.sync.CloudSyncManager.restoreMemoryBatch(m.context, 120);
                int files = salve.data.sync.CloudSyncManager.syncKnowledge(m.context);
                boolean mindRestored = salve.data.sync.CloudSyncManager.restoreMind(m.context, false);
                salve.data.sync.SyncWorker.restartNow(m.context);
                m.registrarAccion(salve.core.mind.SelfActionLog.Kind.SEARCH, "sincronizar con pCloud",
                        "traje " + batch.restored + " recuerdos y moví " + files + " archivos de lo que aprendí");
                return new CognitiveTurn.Receipt("NUBE_SINCRONIZADA: " + batch.restored
                        + " recuerdos nuevos; " + files + " archivos de dibujos/acciones sincronizados"
                        + (mindRestored ? "; mente restaurada" : "") + ". "
                        + salve.data.sync.CloudSyncManager.memoryStatus(m.context)
                        + (batch.more ? " Quedan más: sigo en segundo plano." : ""));
            }
            case salve.core.schema.Abstraction.TOOL: {
                String concept = CognitiveReply.string(args, "concepto").trim();
                if (concept.isEmpty() || concept.length() > 60) throw new IllegalArgumentException("Concepto vacío o demasiado largo");
                salve.core.schema.ConceptStore.Concept c = m.mind().abstractConcept(concept, input, true);
                if (c == null) {
                    // Sin anclar: al menos los esquemas que delata la pregunta, con sus consecuencias genéricas.
                    StringBuilder hint = new StringBuilder("SIN_ANCLAR: no pude descomponer «" + concept + "» ahora; lo haré al soñar.");
                    for (salve.core.schema.ImageSchemas.Schema s : salve.core.schema.ImageSchemas.detect(input))
                        hint.append("\n- ").append(s.label).append(": ").append(String.join("; ", s.infer(null, null)));
                    return new CognitiveTurn.Receipt(hint.toString());
                }
                m.registrarAccion(salve.core.mind.SelfActionLog.Kind.LEARN_LESSON, "pensar «" + concept + "» por dentro",
                        "lo descompuse en " + c.schemas.size() + (c.schemas.size() == 1 ? " esquema" : " esquemas"));
                return new CognitiveTurn.Receipt(salve.core.schema.ConceptStore.reasoning(c));
            }
            case "runtime.status": {
                ModelResult last = m.lastConversationResult;
                return new CognitiveTurn.Receipt(m.cognitiveState() + "\n" + m.getModelStatusDescription()
                        + (last == null ? "" : "\nÚltima inferencia: " + last.getStatus()
                        + "; latencia_ms=" + last.getLatencyMillis()));
            }
            case "SOLVE_CHALLENGE": {
                JsonObject challenge = args.getAsJsonObject("challenge");
                String family = salve.core.autonomy.AutonomousToolCommand.offeredFamilyFor(input);
                if (!family.equals(CognitiveReply.string(challenge, "family")))
                    throw new IllegalArgumentException("Familia distinta");
                JsonObject legacy = args.deepCopy();
                legacy.addProperty("tool", "SOLVE_CHALLENGE");
                String result = m.autonomousTools.respondToModel(legacy.toString(),
                        () -> m.closed || !m.isVoiceContextCurrent());
                m.deliverResponse(result, AvatarMotionProtocol.parse(""), false, true, true);
                return CognitiveTurn.Receipt.handoff();
            }
            case "whatsapp.draft": {
                // The language model chooses the capability but cannot invent who to
                // contact or what to say. The user's actual current turn is authority.
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("WhatsApp requiere al usuario verificado.");
                String contact = CognitiveReply.string(args, "contact").trim();
                String message = CognitiveReply.string(args, "message").trim();
                salve.core.cognition.WhatsAppDraftIntent.validate(input, contact, message);
                final boolean byNumber = contact.startsWith("+");
                final salve.devices.WhatsAppDraft draft = byNumber
                        ? salve.devices.WhatsAppDraft.create(contact, message) : null;
                m.mesaAcciones.postConversationTask(() -> {
                    if (m.closed) return;
                    Intent panel = new Intent(m.context, salve.presentation.ui.DeviceControlActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_DIRECT_DRAFT, true);
                    if (draft == null) {
                        panel.putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_WHATSAPP_CONTACT, contact)
                                .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_MESSAGE, message);
                    } else {
                        panel.putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_PHONE, draft.getPhone())
                                .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_MESSAGE, draft.getMessage());
                    }
                    try {
                        m.context.startActivity(panel);
                        m.hablar("He solicitado abrir el borrador de WhatsApp. Comprueba el contacto y el texto en WhatsApp. No he enviado nada.");
                    } catch (RuntimeException unavailable) {
                        m.hablar("Android no permitió abrir el borrador de WhatsApp. No se envió nada.");
                    }
                });
                // The activity can still fail to resolve a contact or be cancelled.
                // This is a handoff, NOT evidence of successful draft or delivery.
                return CognitiveTurn.Receipt.handoff();
            }
            case salve.core.sensors.DeviceSenses.TOOL: {
                // Solo lectura, pero de su teléfono: identidad verificada y pregunta real en este turno.
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Acceso privado requerido para los sensores.");
                if (!salve.core.sensors.DeviceSenses.offered(input))
                    throw new IllegalArgumentException("La petición actual no pregunta por los sensores.");
                String proposed = args.has("grupo") && args.get("grupo").isJsonPrimitive()
                        ? args.get("grupo").getAsString() : null;
                salve.core.sensors.DeviceSenses.Group group = salve.core.sensors.DeviceSenses.resolve(input, proposed);
                String measured = m.percibirDispositivo(group);
                salve.core.trace.ExecutionTrace.note("SENTIDOS DEL TELÉFONO",
                        "Ráfaga de sensores (" + group.name() + "); lecturas no guardadas.");
                return new CognitiveTurn.Receipt("PERCEPCION_REAL (medido ahora, no guardado):\n" + measured);
            }
            case salve.core.devicecontrol.SystemSetting.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Acceso privado requerido para los ajustes del sistema.");
                salve.core.devicecontrol.SystemSetting.Command setting =
                        salve.core.devicecontrol.SystemSetting.verified(input, CognitiveReply.string(args, "accion"));
                String state = m.closed ? "AJUSTE_RECHAZADO: la conversación se cerró."
                        : salve.devices.ShizukuBridge.apply(m.context, setting);
                salve.core.trace.ExecutionTrace.note("AJUSTE DEL SISTEMA", setting.action.name() + " → " + state);
                return new CognitiveTurn.Receipt(state);
            }
            case salve.core.cognition.DeviceControlIntent.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Acceso privado requerido para los mandos del teléfono.");
                salve.core.cognition.DeviceControlIntent.Command command =
                        salve.core.cognition.DeviceControlIntent.verified(input, CognitiveReply.string(args, "accion"));
                String observed = m.accionarDispositivo(command);
                salve.core.trace.ExecutionTrace.note("MANDO DEL TELÉFONO", command.action.name() + " → " + observed);
                return new CognitiveTurn.Receipt(observed);
            }
            case "device.media_control": {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Acceso privado requerido para controles multimedia.");
                salve.core.voice.MusicControlCommand.Action expected =
                        salve.core.cognition.MediaActionIntent.verifiedAction(
                                input, CognitiveReply.string(args, "action"));
                if (expected == salve.core.voice.MusicControlCommand.Action.NOW_PLAYING) {
                    String observed = salve.devices.MusicController.hasAccess(m.context)
                            ? salve.devices.MusicController.nowPlaying(m.context) : null;
                    return new CognitiveTurn.Receipt(observed != null
                            ? "REPRODUCTOR_INFORMA: " + observed
                            : "REPRODUCTOR_SIN_DATOS: no puedo confirmar qué está sonando; revisa acceso a notificaciones.");
                }
                m.mesaAcciones.postConversationTask(() -> {
                    if (m.closed) return;
                    if (!salve.core.identity.IdentityManager.get(m.context).privateAccess()) {
                        m.hablar("Ya no tengo permiso para controlar la música.");
                        return;
                    }
                    try {
                        boolean accepted;
                        switch (expected) {
                            case PLAY: accepted = salve.devices.MusicController.play(m.context); break;
                            case PAUSE: accepted = salve.devices.MusicController.pause(m.context); break;
                            case NEXT: accepted = salve.devices.MusicController.next(m.context); break;
                            case PREVIOUS: accepted = salve.devices.MusicController.previous(m.context); break;
                            default: accepted = false; break;
                        }
                        m.hablar(accepted
                                ? "He solicitado " + expected.name().toLowerCase(java.util.Locale.ROOT)
                                        + " al reproductor. Comprueba que el cambio se refleje en la música."
                                : "El reproductor no aceptó esa acción; no he confirmado ningún cambio.");
                    } catch (RuntimeException unavailable) {
                        m.hablar("Android no me dejó controlar la música. No hay resultado confirmado.");
                    }
                });
                return CognitiveTurn.Receipt.handoff();
            }
            case "device.open_app":
            case "device.navigate":
            case "device.play_music":
            case "device.search_music":
            case "device.call": {
                // The model proposes a target; only the current user request
                // may authorize an external effect. Recheck after scheduling.
                salve.core.cognition.DeviceActionIntent.Kind kind;
                switch (name) {
                    case "device.open_app":
                        kind = salve.core.cognition.DeviceActionIntent.Kind.OPEN_APP; break;
                    case "device.navigate":
                        kind = salve.core.cognition.DeviceActionIntent.Kind.NAVIGATE; break;
                    case "device.play_music":
                        kind = salve.core.cognition.DeviceActionIntent.Kind.PLAY_MUSIC; break;
                    case "device.search_music":
                        kind = salve.core.cognition.DeviceActionIntent.Kind.SEARCH_MUSIC; break;
                    default:
                        kind = salve.core.cognition.DeviceActionIntent.Kind.CALL; break;
                }
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Se necesita identidad verificada para controlar el móvil.");
                String target = salve.core.cognition.DeviceActionIntent.checkedTarget(
                        kind, input, CognitiveReply.string(args, "target"));
                m.mesaAcciones.postConversationTask(() -> {
                    if (m.closed) return;
                    if (!salve.core.identity.IdentityManager.get(m.context).privateAccess()) {
                        m.hablar("Ya no tengo autorización para esa acción. No la he realizado.");
                        return;
                    }
                    try {
                        boolean driving = salve.core.driving.DrivingModeManager.get(m.context).isActive();
                        // Voice, car and typed mode use the same app/screen launcher.
                        // Android itself may reject an activity, but Salve does not
                        // refuse the user's requested screen because of mode coche.
                        boolean opened;
                        switch (kind) {
                            case OPEN_APP: {
                                salve.devices.PhoneActions.InstalledApp app =
                                        salve.devices.PhoneActions.findApp(m.context, target);
                                opened = salve.devices.PhoneActions.openApp(m.context, app);
                                m.hablar(opened
                                        ? "Solicité abrir " + app.label + ". Comprueba que esté visible."
                                        : "No encontré una aplicación instalada que coincida claramente con «"
                                                + target + "». No la abrí.");
                                break;
                            }
                            case NAVIGATE:
                                opened = salve.devices.PhoneActions.navigate(m.context, target);
                                m.hablar(opened ? "Solicité abrir la ruta hacia " + target
                                        + ". Comprueba el destino en el mapa."
                                        : "No pude iniciar una ruta a «" + target + "».");
                                break;
                            case PLAY_MUSIC:
                                opened = driving
                                        ? salve.devices.MusicController.playFromSearch(m.context, target, null)
                                        : salve.devices.PhoneActions.playMusic(m.context, target);
                                m.hablar(opened ? "Solicité reproducir «" + target
                                        + "». Todavía no tengo confirmación de que esté sonando."
                                        : "No pude iniciar la reproducción de «" + target
                                                + "». No tengo confirmación de reproducción.");
                                break;
                            case SEARCH_MUSIC: {
                                opened = salve.devices.PhoneActions.searchMusic(m.context, target);
                                m.hablar(opened ? "Solicité abrir la búsqueda de «" + target
                                        + "» en Spotify. No he confirmado reproducción."
                                        : "No pude abrir la búsqueda de música.");
                                break;
                            }
                            case CALL:
                                m.context.startActivity(new Intent(m.context,
                                        salve.presentation.ui.DeviceControlActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_CALL, target));
                                m.hablar("Abrí la revisión de llamada a " + target
                                        + ". La llamada aún necesita tu confirmación.");
                                break;
                            default:
                                m.hablar("No tengo esa acción disponible.");
                        }
                    } catch (RuntimeException unavailable) {
                        m.hablar("Android no permitió completar la solicitud. No hay resultado confirmado.");
                    }
                });
                // Intent launch is not proof that an app opened, music played,
                // navigation started, or a call connected. All remain handoffs.
                return CognitiveTurn.Receipt.handoff();
            }
            case salve.core.cognition.SelfCommandIntent.TOOL: {
                // The model chooses WHICH of her own functions; the user's words decide
                // the details. An id the user did not ask for in this turn is rejected.
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Mis funciones propias requieren a Bryan verificado.");
                salve.core.cognition.SelfCommandIntent.Command command =
                        salve.core.cognition.SelfCommandIntent.verified(input, CognitiveReply.string(args, "id"));
                String words = salve.core.cognition.SelfCommandIntent.wordsFor(input, command);
                String reply = m.runSelfCommand(command, words);
                salve.core.trace.ExecutionTrace.note("FUNCIÓN PROPIA ELEGIDA POR EL MODELO",
                        command.name() + (command.readOnly ? " (lectura: el modelo lo explica)" : " (acción: entrega)")
                                + (words.equals(input) ? "" : "; entendida por significado como «" + words + "»"));
                if (command.readOnly) return new CognitiveTurn.Receipt(command.name() + "_REAL: " + reply);
                m.hablar(reply);
                // Opening a screen or toggling a mode is a handoff, not a verified goal.
                return CognitiveTurn.Receipt.handoff();
            }
            case "TAP": {
                m.prepararAccion(PendingToolAction.tap(coordinate(args, "x"),
                        coordinate(args, "y"), System.currentTimeMillis()));
                return CognitiveTurn.Receipt.handoff();
            }
            case "ESCRIBIR": {
                m.prepararAccion(PendingToolAction.writeText(requiredText(args, "texto", 2000),
                        System.currentTimeMillis()));
                return CognitiveTurn.Receipt.handoff();
            }
            case "DEPLOY_WEB": {
                m.prepararAccion(PendingToolAction.deployWeb(requiredText(args, "codigo", 20000),
                        System.currentTimeMillis()));
                return CognitiveTurn.Receipt.handoff();
            }
            case salve.core.mind.SelfCode.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Solo para Bryan verificado");
                String question = CognitiveReply.string(args, "pregunta");
                m.estado("Mirando mi propio código…");
                salve.core.mind.CodeReviewStore notes = salve.work.CodeReviewWorker.store(m.context);
                return new CognitiveTurn.Receipt(salve.core.mind.SelfCodeLoader.get(m.context)
                        .answer(question == null ? "" : question, notes::note));
            }
            case salve.core.mind.SelfSource.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Solo para Bryan verificado");
                if (!args.get("desde").isJsonPrimitive() || !args.getAsJsonPrimitive("desde").isNumber())
                    throw new IllegalArgumentException("desde debe ser un número de línea");
                int from = args.get("desde").getAsBigDecimal().intValue();
                String file = CognitiveReply.string(args, "archivo");
                m.estado("Leyendo mi código: " + (file.length() > 60 ? file.substring(0, 60) + "…" : file));
                return new CognitiveTurn.Receipt(salve.core.mind.SelfSource.read(
                        salve.core.mind.SelfCodeLoader.get(m.context), file, from,
                        path -> salve.core.mind.SelfCodeLoader.source(m.context, path)));
            }
            case salve.core.reports.SelfReport.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Solo para Bryan verificado");
                if (!salve.core.reports.SelfReport.requested(input))
                    throw new IllegalArgumentException("Bryan no ha pedido un informe en este turno");
                String body = salve.core.reports.SelfReport.compose(
                        CognitiveReply.string(args, "titulo"), CognitiveReply.string(args, "contenido"));
                m.estado("Guardando mi informe en PDF…");
                java.io.File saved = salve.core.reports.AutonomousReportWriter.saveText(m.context, body);
                if (saved == null) throw new IllegalStateException("No se pudo escribir el PDF");
                return new CognitiveTurn.Receipt("INFORME_GUARDADO: " + saved.getName()
                        + " en Registro de ejecución → Informes. Díselo a Bryan con ese nombre.");
            }
            case salve.core.mind.SelfFunctions.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Solo para Bryan verificado");
                String action = salve.core.mind.SelfFunctions.action(CognitiveReply.string(args, "accion"));
                if (action == null) throw new IllegalArgumentException("Función desconocida");
                // La función responde ella misma (con sus datos reales) y el turno termina aquí.
                switch (action) {
                    case "estado_memoria": m.hablar(m.mind().memoryHealth()); break;
                    case "examen": m.hablar(m.examen.run()); break;
                    case "mapa_mente": m.hablar(m.abrirMapaMente()); break;
                    default: m.hablar(m.examen.proposals());
                }
                return CognitiveTurn.Receipt.handoff();
            }
            case "AVATAR_LIST":
            case "AVATAR_WEAR":
            case "AVATAR_CREATE":
                return avatarAction(name, args);
            default:
                throw new IllegalArgumentException("Herramienta no autorizada");
        }
    }

    CognitiveTurn.Receipt avatarAction(String name, JsonObject args) {
        if (args.has("tool")) throw new IllegalArgumentException("Campo no permitido");
        JsonObject legacy = args.deepCopy(); legacy.addProperty("tool", name);
        salve.avatar.AvatarDesignRequest.parse(legacy.toString()); // validates BEFORE dispatch
        MotorConversacional.VoiceContinuation continuation = m.new VoiceContinuation();
        boolean accepted = m.avatarDesignTool.tryExecute(legacy.toString(), text -> continuation.run(() -> m.hablar(text)));
        if (!accepted) { continuation.abandon(); throw new IllegalArgumentException("Vestuario no disponible"); }
        return CognitiveTurn.Receipt.handoff();
    }

    static int coordinate(JsonObject args, String key) {
        if (!args.get(key).isJsonPrimitive() || !args.getAsJsonPrimitive(key).isNumber()) throw new IllegalArgumentException("Coordenada inválida");
        int value = args.get(key).getAsBigDecimal().intValueExact();
        if (value < 0 || value > 10000) throw new IllegalArgumentException("Coordenada fuera de rango");
        return value;
    }

    static String requiredText(JsonObject args, String key, int maximum) {
        String text = CognitiveReply.string(args, key);
        if (text.trim().isEmpty() || text.length() > maximum) throw new IllegalArgumentException("Texto vacío o demasiado largo");
        return text;
    }
}
