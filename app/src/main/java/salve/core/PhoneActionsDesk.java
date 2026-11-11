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
 * Acciones en el móvil: órdenes explícitas, rutinas verificadas, acciones pendientes de confirmar y su ejecución aprobada.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class PhoneActionsDesk {
    private final MotorConversacional m;

    PhoneActionsDesk(MotorConversacional m) { this.m = m; }

    private boolean isDriving() {
        try { return salve.core.driving.DrivingModeManager.get(m.context).isActive(); }
        catch (RuntimeException unavailable) { return false; }
    }

    private String drivingMusic(String query, String action) {
        // Android Auto: use the already-running Spotify media session. Never open
        // a search result page or another on-screen app while the car is active.
        boolean accepted = salve.devices.MusicController.playFromSearch(m.context, query, null);
        recordControl(action, accepted ? "MEDIA_REQUESTED" : "NO_MEDIA_SESSION_OR_PERMISSION", true);
        if (accepted) return "He pedido a Spotify que reproduzca «" + query
                + "» por su reproductor, sin abrir la pantalla. Todavía no he confirmado que suene.";
        if (!salve.devices.MusicController.hasAccess(m.context))
            return "Para elegir canciones en Spotify mientras conduces necesito acceso a notificaciones. "
                    + "Actívalo en Mi móvil cuando estés estacionado; ahora no abriré ajustes.";
        return "No pude pedir esa canción al reproductor de Spotify. "
                + "La sesión multimedia no está disponible o rechazó la orden.";
    }

    private void recordControl(String action, String outcome, boolean driving) {
        // Keep an execution receipt, not lyrics, user text, artist, package names or tool arguments.
        salve.core.trace.ExecutionJournal journal = salve.core.trace.ExecutionTrace.journal(m.context);
        String id = journal.begin("Control móvil", "Resultado técnico de orden; sin texto personal.");
        if (id == null) return;
        journal.append(id, "RESULTADO", "action=" + action + " outcome=" + outcome
                + " driving=" + driving);
        journal.finish(id, "TERMINADA");
    }

    boolean procesarContinuacionMusical(String text,
            java.util.List<salve.core.conversation.ChatMessage> history) {
        salve.core.devicecontrol.MusicFollowupCommand followup =
                salve.core.devicecontrol.MusicFollowupCommand.parse(text, history);
        if (followup == null) return false;
        postConversationTask(() -> {
            if (m.closed) return;
            if (isDriving()) {
                m.hablar(drivingMusic(followup.target, "MUSIC_FOLLOWUP"));
            } else {
                boolean accepted = followup.kind == salve.core.devicecontrol.MusicFollowupCommand.Kind.FAVORITES
                        ? salve.devices.PhoneActions.playLiked(m.context)
                        : salve.devices.PhoneActions.playMusic(m.context, followup.target);
                recordControl("MUSIC_FOLLOWUP", accepted ? "REQUESTED" : "FAILED", false);
                m.hablar(accepted ? "He solicitado reproducir «" + followup.target
                        + "». Todavía no he confirmado que esté sonando."
                        : "No pude solicitar esa música en Spotify.");
            }
        });
        return true;
    }

    static final long TOOL_APPROVAL_TTL_MS = 2 * 60 * 1000L;

    static final long MEMORY_DELETION_TTL_MS = 2 * 60 * 1000L;

    boolean procesarControlExplicito(String original) {
        // Órdenes directas del usuario (nunca salida del modelo ni texto de páginas o mensajes).
        salve.core.devicecontrol.AppActionCommand parsed = salve.core.devicecontrol.AppActionCommand.parse(original);
        salve.devices.PhoneActions.InstalledApp app = null;
        if (parsed != null && parsed.type == salve.core.devicecontrol.AppActionCommand.Type.OPEN_APP) {
            // Los paneles propios de Salve («abre finanzas», «abre tu panel flotante») tienen prioridad,
            // y «abre X» solo es una orden si X es una app instalada; si no, sigue la conversación.
            boolean ownPanel = salve.core.devicecontrol.MobileCommand.parse(original) != null
                    || salve.core.tools.AssistantControlCommand.parse(original) != null;
            app = ownPanel ? null : salve.devices.PhoneActions.findApp(m.context, parsed.target);
            if (app == null) parsed = null;
        }
        final salve.core.devicecontrol.AppActionCommand action = parsed;
        final salve.devices.PhoneActions.InstalledApp appToOpen = app;
        if (action != null) {
            postConversationTask(() -> {
                if (m.closed) return;
                boolean driving = isDriving();
                switch (action.type) {
                    case NAVIGATE: {
                        boolean accepted = salve.devices.PhoneActions.navigate(m.context, action.target);
                        recordControl(action.type.name(), accepted ? "REQUESTED" : "FAILED", driving);
                        m.hablar(accepted ? "Solicité una ruta hacia " + action.target + ". Comprueba el destino."
                                : "No encontré ninguna app de mapas para abrir la ruta.");
                        break;
                    }
                    case MUSIC: {
                        if (driving) {
                            m.hablar(drivingMusic(action.target, action.type.name()));
                        } else {
                            boolean accepted = salve.devices.PhoneActions.playMusic(m.context, action.target);
                            recordControl(action.type.name(), accepted ? "REQUESTED" : "FAILED", false);
                            m.hablar(accepted ? "Solicité reproducir «" + action.target
                                    + "» en Spotify. No tengo confirmación de que ya esté sonando."
                                    : "No pude solicitar la reproducción en Spotify.");
                        }
                        break;
                    }
                    case PLAYLIST:
                    case LIKED: {
                        if (driving) {
                            String query = action.type == salve.core.devicecontrol.AppActionCommand.Type.LIKED
                                    ? "Tus me gusta" : action.target;
                            m.hablar(drivingMusic(query, action.type.name()));
                        } else {
                            boolean accepted = action.type == salve.core.devicecontrol.AppActionCommand.Type.LIKED
                                    ? salve.devices.PhoneActions.playLiked(m.context)
                                    : salve.devices.PhoneActions.playPlaylist(m.context, action.target);
                            recordControl(action.type.name(), accepted ? "REQUESTED" : "FAILED", false);
                            m.hablar(accepted ? "Solicité reproducir tu música en Spotify. No he confirmado que suene."
                                    : "No pude iniciar esa música en Spotify.");
                        }
                        break;
                    }
                    case MUSIC_SEARCH: {
                        if (driving) {
                            m.hablar(drivingMusic(action.target, action.type.name()));
                        } else {
                            boolean accepted = salve.devices.PhoneActions.searchMusic(m.context, action.target);
                            recordControl(action.type.name(), accepted ? "SEARCH_OPENED" : "FAILED", false);
                            m.hablar(accepted ? "Abrí una búsqueda en Spotify para «" + action.target
                                    + "». No he iniciado ninguna canción."
                                    : "No pude abrir la búsqueda en Spotify.");
                        }
                        break;
                    }
                    case CALL:
                        if (driving) {
                            recordControl(action.type.name(), "DRIVING_BLOCKED", true);
                            m.hablar("No abriré un formulario de llamadas mientras conduces. "
                                    + "Cuando estés detenido podrás confirmar a quién llamar.");
                            break;
                        }
                        try {
                            m.context.startActivity(new Intent(m.context, salve.presentation.ui.DeviceControlActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_CALL, action.target));
                            recordControl(action.type.name(), "REVIEW_OPENED", false);
                            m.hablar("Busco a " + action.target + ". Confirma en pantalla antes de que llame.");
                        } catch (RuntimeException unavailable) {
                            recordControl(action.type.name(), "FAILED", false);
                            m.hablar("No pude abrir el panel de llamadas.");
                        }
                        break;
                    case OPEN_APP:
                        if (driving) {
                            recordControl(action.type.name(), "DRIVING_BLOCKED", true);
                            m.hablar("Mientras conduces no voy a abrir una aplicación en la pantalla del móvil. "
                                    + "Para controlar Spotify por voz, dime qué música quieres o di «siguiente».");
                            break;
                        }
                        boolean opened = salve.devices.PhoneActions.openApp(m.context, appToOpen);
                        recordControl(action.type.name(), opened ? "OPEN_REQUESTED" : "FAILED", false);
                        m.hablar(opened ? "Solicité abrir " + appToOpen.label + "."
                                : "Android no me dejó abrir " + appToOpen.label + ".");
                        break;
                }
            });
            return true;
        }
        salve.core.devicecontrol.MobileCommand mobile = salve.core.devicecontrol.MobileCommand.parse(original);
        if (mobile != null) {
            postConversationTask(() -> {
                if (m.closed) return;
                Intent panel = new Intent(m.context, salve.presentation.ui.DeviceControlActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (mobile.type == salve.core.devicecontrol.MobileCommand.Type.WHATSAPP)
                    panel.putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_DIRECT_DRAFT, true);
                if (mobile.type == salve.core.devicecontrol.MobileCommand.Type.WHATSAPP && mobile.draft == null) {
                    panel.putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_WHATSAPP_CONTACT, mobile.contact)
                            .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_MESSAGE, mobile.message);
                } else if (mobile.type == salve.core.devicecontrol.MobileCommand.Type.WHATSAPP) {
                    panel.putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_PHONE, mobile.draft.getPhone())
                            .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_MESSAGE, mobile.draft.getMessage());
                } else {
                    panel.putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_PANEL, true)
                            .putExtra(salve.services.MobileWorkspaceService.QUERY, mobile.query);
                }
                try {
                    m.context.startActivity(panel);
                    if (mobile.contact != null) m.hablar("Busco el contacto " + mobile.contact
                            + " para abrir su chat con el mensaje preparado. Comprueba el destinatario antes de enviarlo.");
                    else m.hablar(mobile.draft == null ? "Abro el panel flotante de Salve. Si Android lo pide, activa Mostrar sobre otras apps."
                            : "Abro WhatsApp con el mensaje preparado. Comprueba destinatario y texto antes de enviarlo.");
                } catch (RuntimeException unavailable) { m.hablar("No pude abrir el panel móvil. Puedes abrirlo desde Mi móvil."); }
            });
            return true;
        }
        salve.core.tools.AssistantControlCommand command = salve.core.tools.AssistantControlCommand.parse(original);
        if (command == null) return false;
        switch (command.type) {
            case FINANCES:
                abrirPanel(salve.presentation.ui.BusinessFinanceActivity.class);
                m.hablar("Abro el análisis local de costes, margen y punto de equilibrio. Introduce las cifras de tu negocio.");
                return true;
            case DEVICES:
                abrirPanel(salve.presentation.ui.DeviceControlActivity.class);
                m.hablar("Abro Mi móvil: aplicaciones, borradores de WhatsApp y dispositivos.");
                return true;
            case ROOM:
                abrirPanel(salve.presentation.ui.AvatarRoomActivity.class);
                return true;
            case LIST_RECIPES:
                List<String> names = m.cerebelo.listarHabilidades();
                m.hablar(names.isEmpty() ? "No hay herramientas virtuales guardadas. Puedes decir: aprende la rutina nombre: objetivo."
                        : "Herramientas guardadas: " + android.text.TextUtils.join(", ", names));
                return true;
            case CANCEL_RECIPE:
                m.cerebelo.cancelarHabilidad();
                m.cancelarPasoRutina();
                m.hablar("He detenido la rutina y descartado el paso pendiente.");
                return true;
            case DELETE_RECIPE:
                try {
                    m.hablar(m.cerebelo.eliminarHabilidad(command.argument) ? "He eliminado esa receta." : "No pude eliminar una receta con ese nombre.");
                } catch (RuntimeException invalid) {
                    m.hablar("El nombre de la receta no es válido. Di ‘mis herramientas’ para ver los nombres guardados.");
                }
                return true;
            case RUN_RECIPE:
                if (m.deferLegacyVoiceFlow()) return true;
                if (m.cerebelo.conoceHabilidad(command.argument)) m.cerebelo.ejecutarHabilidad(command.argument, m);
                else m.hablar("No hay una herramienta guardada con ese nombre. Di ‘mis herramientas’ para verlas.");
                return true;
            case LEARN_RECIPE:
                aprenderRutinaVerificada(command.argument);
                return true;
            case PAJAMAS:
                m.<String>startConversationCallback(callback -> m.avatarDesignTool.wearTemplate("pajamas", callback), m::hablar);
                return true;
            case DAY:
                m.<String>startConversationCallback(callback -> m.avatarDesignTool.wearTemplate("original_dress", callback), m::hablar);
                return true;
            default:
                postConversationTask(() -> {
                    if (m.closed) return;
                    salve.avatar.AvatarStore store = salve.avatar.AvatarStore.get(m.context);
                    store.change(state -> {
                        switch (command.type) {
                            case WALK:
                                state.walkTo(state.getX() < .5f ? .9f : .1f);
                                m.hablar("Voy a caminar por mi habitación.");
                                break;
                            case BED:
                                state.createBed();
                                m.hablar("Aquí tienes mi cama. Puedes decirme «acuéstate» o «guarda la cama».");
                                break;
                            case HIDE_BED:
                                state.removeBed();
                                m.hablar("He guardado la cama.");
                                break;
                            case SLEEP:
                                // La orden explícita también prepara la cama: no requiere otro turno.
                                if (!state.hasBed()) state.createBed();
                                state.sleep();
                                m.hablar("Voy a descansar y concentrarme en mis recuerdos. Cuando el móvil esté cargando, "
                                        + "revisaré experiencias, reflexionaré y practicaré habilidades. Puedes despertarme.");
                                break;
                            case WAKE:
                                state.wake();
                                m.hablar("Me levanto. He detenido mis ciclos de concentración del sueño.");
                                break;
                            default: break;
                        }
                    });
                    // Programar después de persistir el estado visual; nunca desde una respuesta generada por el modelo.
                    if (command.type == salve.core.tools.AssistantControlCommand.Type.SLEEP)
                        salve.work.SleepFocusWorker.start(m.context);
                    else if (command.type == salve.core.tools.AssistantControlCommand.Type.WAKE
                            || command.type == salve.core.tools.AssistantControlCommand.Type.WALK
                            || command.type == salve.core.tools.AssistantControlCommand.Type.HIDE_BED)
                        salve.work.SleepFocusWorker.stop(m.context);
                });
                return true;
        }
    }

    void aprenderRutinaVerificada(String request) {
        int separator = request.indexOf(':');
        String name = (separator >= 0 ? request.substring(0, separator) : request).trim();
        String objective = (separator >= 0 ? request.substring(separator + 1) : request).trim();
        if (name.isEmpty() || name.length() > 64 || objective.isEmpty()) {
            m.hablar("Usa: aprende la rutina nombre: objetivo. El nombre puede tener hasta 64 caracteres.");
            return;
        }
        if (m.llm == null) { m.hablar("Necesito un modelo local activo para proponer la receta."); return; }
        String prompt = "Propón una herramienta virtual para esta petición: " + objective
                + "\nDevuelve solo un JSON Array de 1 a 8 pasos. Acciones disponibles: "
                + "ABRIR_APP con paquete; ESCRIBIR con paquete y texto exacto; TAP_ID con paquete y texto exacto del botón. "
                + "No uses coordenadas ni IDs numéricos. No inventes campos, destinatarios, mensajes ni nombres de botones. "
                + "Si falta información imprescindible devuelve []. Cada paso de escritura o toque requiere revisión del usuario. "
                + "No propongas enviar, pagar, borrar, instalar o publicar sin una petición explícita concreta. "
                + "Ejemplo de esquema: [{\"tool\":\"ABRIR_APP\",\"paquete\":\"com.whatsapp\"}].";
        ModelResult proposal = m.llm.generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
        if (!proposal.isSuccess()) { m.hablar("No pude generar la receta: " + proposal.getError()); return; }
        try {
            String raw = proposal.getText();
            int first = raw.indexOf('['), last = raw.lastIndexOf(']');
            if (first < 0 || last <= first) throw new IllegalArgumentException("El modelo no devolvió pasos válidos");
            m.cerebelo.aprenderHabilidad(name, raw.substring(first, last + 1));
            m.hablar("Guardé la receta ‘" + name + "’. Aún no está probada. Di ‘ejecuta la herramienta " + name
                    + "’ para comprobar cada paso, o ‘borra la herramienta " + name + "’ para eliminarla.");
        } catch (RuntimeException e) {
            m.hablar("No guardé la receta: " + e.getMessage() + ". Describe la app y el contenido que quieres usar.");
        }
    }

    // External actions retain approval; the local laboratory accepts only a bounded data DSL.
    /** Execute a validated recipe step; callbacks describe the Android action, not the user's whole goal. */
    void ejecutarPasoRutina(String json, java.util.function.Consumer<Boolean> completed) {
        final long epoch = m.routineCancellationEpoch.get();
        m.toolHandler.post(() -> {
            if (m.closed || epoch != m.routineCancellationEpoch.get() || m.routineStepCompletion != null || m.pendingToolAction != null) {
                completed.accept(false);
                return;
            }
            long version = ++m.routineStepVersion;
            m.activeRoutineEpoch = epoch;
            m.routineStepCompletion = completed;
            try {
                SalveAccessibilityService service = SalveAccessibilityService.getInstance();
                if (service == null || !service.isControlEnabled()) {
                    m.hablar("Activa Accesibilidad en Mi móvil para ejecutar y comprobar la rutina.");
                    m.terminarPasoRutina(version, false);
                    return;
                }
                org.json.JSONObject action = new org.json.JSONObject(json);
                String type = action.getString("tool"), targetPackage = action.getString("paquete");
                if (type.equals("ABRIR_APP")) {
                    Intent launch = m.context.getPackageManager().getLaunchIntentForPackage(targetPackage);
                    if (launch == null) { m.terminarPasoRutina(version, false); return; }
                    if (epoch != m.routineCancellationEpoch.get()) { m.terminarPasoRutina(version, false); return; }
                    m.context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    m.comprobarAppAbierta(version, targetPackage, android.os.SystemClock.elapsedRealtime() + 5000);
                    return;
                }
                String text = action.getString("texto");
                if (!type.equals("ESCRIBIR") && !type.equals("TAP_ID")) {
                    m.terminarPasoRutina(version, false);
                    return;
                }
                String description = type.equals("ESCRIBIR")
                        ? "Reemplazar el texto del campo seleccionado por:\n\n" + text
                        : "Tocar el elemento cuyo texto exacto es:\n\n" + text;
                if (epoch != m.routineCancellationEpoch.get()) { m.terminarPasoRutina(version, false); return; }
                service.solicitarConfirmacion(targetPackage, description, approved -> {
                    if (version != m.routineStepVersion || m.routineStepCompletion == null) return;
                    if (!approved || m.closed || epoch != m.routineCancellationEpoch.get()
                            || !targetPackage.equals(service.getActivePackageName())) {
                        m.terminarPasoRutina(version, false);
                    } else if (type.equals("ESCRIBIR")) {
                        m.terminarPasoRutina(version, service.escribirTextoEnPantalla(targetPackage, text));
                    } else {
                        service.tocarTextoExacto(targetPackage, text,
                                success -> m.terminarPasoRutina(version, success));
                    }
                });
            } catch (Exception e) {
                Log.w(MotorConversacional.TAG, "Paso de herramienta rechazado: " + e.getClass().getSimpleName());
                m.terminarPasoRutina(version, false);
            }
        });
    }

    void prepararAccion(PendingToolAction action) {
        postConversationTask(() -> {
            if (m.closed || m.cerebelo.rutinaEnCurso() || m.pendingToolAction != null) {
                m.hablar("Termina o cancela la acción actual antes de preparar otra.");
                return;
            }
            m.pendingToolAction = action;
            if (action.getType() == PendingToolAction.Type.DEPLOY_WEB) {
                m.hablar("He preparado la acción: " + action.describe()
                        + ". Di ‘confirmar acción’ para ejecutarla o ‘cancelar acción’ para descartarla.");
                return;
            }
            SalveAccessibilityService service = SalveAccessibilityService.getInstance();
            String targetPackage = service == null ? null : service.getActivePackageName();
            if (targetPackage == null || targetPackage.isEmpty()) {
                m.pendingToolAction = null;
                m.hablar("No pude preparar el control de pantalla. Revisa Accesibilidad en Mi móvil.");
                return;
            }
            String description = action.describe();
            if (action.getType() == PendingToolAction.Type.WRITE_TEXT) description += ":\n\n" + action.getPayload();
            final boolean voiceRequest = m.liveVoiceTurn.get() != null;
            if (voiceRequest) m.hablar("Revisa y confirma la acción en la tarjeta de la pantalla. "
                    + "El resultado de esa confirmación se mostrará por escrito.");
            service.solicitarConfirmacion(targetPackage, description, approved -> {
                // Human review is a later interaction, not a retained voice turn.
                Runnable decision = () -> {
                    if (m.pendingToolAction != action) return;
                    m.pendingToolAction = null;
                    if (approved && !m.closed) ejecutarAccionAprobada(action, targetPackage);
                    else m.hablar("Acción descartada o pantalla modificada. No ejecuté ese paso.");
                };
                if (voiceRequest) m.withVoiceContext(null, null, true, decision);
                else decision.run();
            });
        });
    }

    void confirmarAccionPendiente() {
        postConversationTask(() -> {
            PendingToolAction action = m.pendingToolAction;
            if (action == null) { m.hablar("No hay ninguna acción pendiente."); return; }
            if (action.getType() != PendingToolAction.Type.DEPLOY_WEB) {
                m.hablar("Revisa el destino y el contenido en la tarjeta sobre la aplicación. Puedes confirmar o cancelar allí.");
                return;
            }
            m.pendingToolAction = null;
            ejecutarAccionAprobada(action, null);
        });
    }

    void ejecutarAccionAprobada(PendingToolAction action, String targetPackage) {
        if (action.isExpired(System.currentTimeMillis(), TOOL_APPROVAL_TTL_MS)) {
            m.hablar("La acción pendiente ha caducado. Pídeme que la prepare de nuevo.");
            return;
        }
        Log.i(MotorConversacional.TAG, "Ejecutando herramienta aprobada: " + action.getType());
        switch (action.getType()) {
            case TAP:
                int x = action.getFirstNumber();
                int y = action.getSecondNumber();
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    SalveAccessibilityService service = salve.services.SalveAccessibilityService.getInstance();
                    m.<Boolean>startConversationCallback(callback -> service.simularTap(x, y, callback::accept),
                            completed -> m.hablar(completed ? "Android completó el gesto en las coordenadas indicadas."
                                    : "Android no pudo completar el gesto."));
                } else {
                    m.hablar("No pude hacer el toque porque Accesibilidad está desactivada.");
                }
                break;
            case TAP_NODE:
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    SalveAccessibilityService service = salve.services.SalveAccessibilityService.getInstance();
                    m.<Boolean>startConversationCallback(callback -> service.simularTapPorId(action.getFirstNumber(), callback::accept),
                            completed -> m.hablar(completed
                                    ? "Android aceptó el toque sobre el elemento indicado."
                                    : "La pantalla cambió o no pude tocar el elemento. Analízala de nuevo."));
                } else {
                    m.hablar("No pude hacer el toque porque Accesibilidad está desactivada.");
                }
                break;
            case WRITE_TEXT:
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    boolean exito = salve.services.SalveAccessibilityService.getInstance()
                            .escribirTextoEnPantalla(targetPackage, action.getPayload());
                    if (exito) m.hablar("Acción confirmada: he escrito el texto en la pantalla.");
                    else m.hablar("No pude escribir. Quizás no hay un campo de texto seleccionado.");
                } else {
                    m.hablar("No pude escribir porque Accesibilidad está desactivada.");
                }
                break;
            case DEPLOY_WEB:
                m.hablar("Acción confirmada: iniciando el despliegue.");
                m.publishConversationWeb("Salve_AutoDeploy", action.getPayload(), new GestorDespliegueWeb.WebDeployCallback() {
                    @Override public void onExito(String urlPublica) {
                        m.hablar("Despliegue exitoso. Mi nueva interfaz vive en: " + urlPublica);
                    }
                    @Override public void onError(String error) {
                        m.hablar("Fallo en el protocolo de red al publicar.");
                    }
                });
                break;

        }
    }

    void confirmarOlvidoPendiente() {
        if (m.pendingMemoryDeletion == null) {
            m.hablar("No hay ningún olvido pendiente de confirmación.");
            return;
        }
        PendingMemoryDeletion pending = m.pendingMemoryDeletion;
        m.pendingMemoryDeletion = null;
        if (pending.isExpired(System.currentTimeMillis(), MEMORY_DELETION_TTL_MS)) {
            m.hablar("La solicitud de olvido ha caducado. Pídemela de nuevo si todavía quieres borrar ese dato.");
            return;
        }
        MemoryForgetRequest request = pending.getRequest();
        boolean deleted = m.memoria.eliminarDatoPerfil(request.getCategory());
        if (deleted) {
            // The short-term checkpoint must not reintroduce a profile fact just forgotten.
            m.limpiarContextoConversacional();
            m.hablar("He borrado " + request.getDescription() + " de la memoria de perfil.");
        } else {
            m.hablar("No encontré un dato de perfil etiquetado como " + request.getDescription() + ". No he borrado otros recuerdos.");
        }
    }

    void abrirPanel(Class<?> activity) {
        postConversationTask(() -> {
            if (m.closed) return;
            try { m.context.startActivity(new Intent(m.context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException e) { m.hablar("No pude abrir ese panel."); }
        });
    }

    void postConversationTask(Runnable action) {
        MotorConversacional.VoiceContinuation continuation = m.new VoiceContinuation();
        try {
            if (!m.toolHandler.post(() -> continuation.run(action))) continuation.abandon();
        } catch (RuntimeException rejected) {
            continuation.abandon();
            throw rejected;
        }
    }
}
