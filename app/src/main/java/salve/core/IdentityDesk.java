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
 * Quién le habla: verificación con el teléfono, puntos críticos antes de lo privado, presentaciones de otras personas y datos personales que Bryan le cuenta.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class IdentityDesk {
    private final MotorConversacional m;

    IdentityDesk(MotorConversacional m) { this.m = m; }

    static final java.util.regex.Pattern SELF_CONFIRMATION = java.util.regex.Pattern.compile(
            "^(?:s[ií][, ]+)?(?:claro[, ]+)?(?:s[ií][, ]+)?soy yo(?: mismo| misma)?(?:[, ]+tu creador)?[.!¡ ]*$",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

    static final java.util.regex.Pattern VERIFY_REQUEST = java.util.regex.Pattern.compile(
            "^\\s*(?:salve[, ]+)?(?:verif[ií]ca(?:me)?|comprueba que soy yo|confirma que soy yo|pon(?:go)? (?:mi )?huella|"
                    + "(?:confirma(?:r)?|verifica(?:r)?|comprueba|comprobar) (?:mi |la )?identidad|"
                    + "quiero verificarme|verif[ií]came con (?:la )?(?:huella|cara|rostro))\\b.*",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

    static final java.util.regex.Pattern NOT_CREATOR = java.util.regex.Pattern.compile(
            "\\b(?:no soy (?:bryan|tu creador)|te (?:est[aá] )?habla(?:ndo)? otra persona|soy otra persona|"
                    + "no es bryan quien (?:te )?habla)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

    /**
     * Órdenes explícitas sobre quién habla. La verificación usa el diálogo del sistema (huella,
     * rostro o PIN): es ligero y no compite con el modelo, y solo se abre cuando se pide.
     */
    String comandoIdentidad(String entrada) {
        salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(m.context);
        String first = m.mind().peopleGraph().creator().name.split(" ")[0];
        if (NOT_CREATOR.matcher(entrada).find()) {
            identity.session().onNotCreator();
            identity.clearVerification();
            // Otra persona: el historial y la foto de Bryan no pasan a su conversación.
            m.limpiarContextoConversacional();
            return "De acuerdo, no eres " + first + ". Encantada; ¿cómo te llamas?";
        }
        if (!VERIFY_REQUEST.matcher(entrada).matches()) return null;
        if (!identity.biometricAvailable())
            return "Este teléfono no tiene huella, rostro ni PIN configurados, así que no puedo verificarte con él. "
                    + "Puedes activarlo en los ajustes de seguridad de Android.";
        identity.requestVerification("Confirma que eres " + first, ok -> m.toolHandler.post(() -> m.hablar(ok
                ? "Ya está, " + first + ": eres tú. Puedo hablarte con toda confianza."
                : "No he podido confirmarlo. Si eres " + first + ", inténtalo de nuevo cuando quieras; mientras, guardo tus recuerdos.")));
        return "Abro la verificación del teléfono: pon tu huella o mira a la pantalla.";
    }

    /**
     * «Punto crítico»: una petición de algo privado (quién soy, mi familia, mis recuerdos, dinero…)
     * cuando solo se presume que habla el creador. Antes de que el turno cargue memoria o perfil,
     * se pide la verificación del sistema (huella, rostro con prueba de vida del propio Android o
     * PIN). Si se confirma, el mismo turno se repite ya verificado; si no, lo privado no se toca.
     * Sin biometría configurada en el teléfono se mantiene el comportamiento anterior.
     */
    String puntoCriticoPrivado(String entrada, boolean entradaPorVoz, boolean privateBudgetTurn) {
        if (!salve.core.identity.SensitiveRequest.needsVerification(entrada, privateBudgetTurn)) return null;
        salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(m.context);
        if (!identity.session().needsPrivateCheckpoint(identity.biometricAvailable(), System.currentTimeMillis())) return null;
        String first = m.mind().peopleGraph().creator().name.split(" ")[0];
        identity.requestVerification("Para hablar de algo privado de " + first, ok -> m.toolHandler.post(() -> {
            if (m.closed) return;
            if (ok) m.encolarEntrada(entrada, entradaPorVoz, privateBudgetTurn);
            else m.hablar("No he podido confirmar que eres " + first + ", así que eso lo guardo para él. "
                    + "Si eres tú, di «verifícame» cuando quieras.");
        }));
        return "Eso es privado de " + first + ". Antes de responder, confirma que eres tú: pon tu huella o mira a la pantalla.";
    }

    /**
     * Un nombre declarado distinto del creador conocido cambia el estado (familiar o invitado) en
     * lugar de sobrescribir el perfil. Devuelve true si quien habla no es el creador.
     */
    boolean registrarIdentidadDeclarada(String entrada) {
        if (entrada == null || entrada.trim().endsWith("?")) return !m.creatorSide();
        String declared = salve.core.mind.Interlocutor.extractName(entrada);
        String known = m.mind().interlocutor().name;
        salve.core.identity.SessionIdentity session = salve.core.identity.IdentityManager.get(m.context).session();
        boolean wasCreator = session.isCreatorSide();
        if (declared != null && (known != null || !session.isCreatorSide())) {
            salve.core.identity.PeopleGraph graph = m.mind().peopleGraph();
            session.onDeclaredName(declared, graph.creator().name, graph.familyPairs(), System.currentTimeMillis());
        }
        if (wasCreator && !session.isCreatorSide()) {
            // Cambio de interlocutor: lo hablado con Bryan no entra en el contexto de otra persona.
            m.limpiarContextoConversacional();
            m.conversationSession.addUser(entrada);
        }
        return !session.isCreatorSide();
    }

    /**
     * «Sí, soy yo» tras «si eres tú, dime…»: en su teléfono y sin nombre guardado, guarda el
     * nombre del creador. No sube a verificado: eso lo hace la huella o el rostro.
     */
    String confirmarQueEsElCreador(String entrada) {
        if (entrada == null || !SELF_CONFIRMATION.matcher(entrada.trim()).matches()) return null;
        if (!m.creatorSide() || m.mind().interlocutor().name != null) return null;
        String name = m.mind().peopleGraph().creator().name;
        m.memoria.guardarDatoPerfil("Me llamo " + name + ".", "name");
        m.memoria.guardarDatoPerfil("Soy tu creador.", "relation");
        m.mind().indexNewMemoriesAsync();
        salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(m.context);
        return "Hola, " + name + ". Te tengo de nuevo: lo guardo para no volver a preguntártelo."
                + (identity.biometricAvailable() && identity.session().state() != salve.core.identity.SessionIdentity.State.VERIFIED_CREATOR
                ? " Si quieres abrir mis reflexiones contigo, di «verifícame»." : "");
    }

    /**
     * Guarda nombre y familia aunque no vayan al principio de la frase («Jajaja sí, soy Bryan»,
     * «Bueno sí, me llamo Bryan…», «mi hermana se llama Ana»). Devuelve un acuse breve cuando el
     * mensaje es solo una presentación; si trae además una petición, devuelve null y responde el modelo.
     */
    String aprenderDatosPersonales(String entrada, MemoryProfileFact profileFact) {
        if (entrada == null || entrada.trim().endsWith("?")) return null;
        salve.core.mind.Interlocutor.Known before = m.mind().interlocutor();
        String name = salve.core.mind.Interlocutor.extractName(entrada);
        boolean nameIsNew = name != null && !name.equalsIgnoreCase(before.name == null ? "" : before.name);
        if (nameIsNew && (profileFact == null || !"name".equals(profileFact.getCategory())))
            m.memoria.guardarDatoPerfil("Me llamo " + name + ".", "name");
        java.util.List<salve.core.mind.FamilyFacts.Member> family = salve.core.mind.FamilyFacts.parse(entrada);
        for (salve.core.mind.FamilyFacts.Member member : family) m.memoria.guardarDatoPerfil(member.statement(), member.category());
        if (!nameIsNew && family.isEmpty()) return null;
        if (entrada.trim().split("\\s+").length > 16) return null;
        StringBuilder ack = new StringBuilder();
        if (nameIsNew) {
            boolean creator = before.declaredCreator || salve.core.mind.Interlocutor.declaresCreator(entrada);
            ack.append(before.name == null ? "Anotado: te llamas " + name : "Actualizo tu nombre: ahora te llamas " + name)
                    .append(creator ? " y te presentaste como mi creador" : "").append('.');
        }
        if (!family.isEmpty()) {
            java.util.List<String> parts = new java.util.ArrayList<>();
            for (salve.core.mind.FamilyFacts.Member member : family)
                parts.add(member.statement().replaceFirst("^Mi ", "tu ").replaceAll("[.]$", ""));
            ack.append(ack.length() == 0 ? "Anotado: " : " También anoto: ").append(String.join("; ", parts)).append('.');
        }
        ack.append(" Lo recordaré en las próximas conversaciones.");
        return ack.toString();
    }
}
