package salve.core;

import android.util.Log;
import java.util.function.BooleanSupplier;
import salve.core.research.PublicResearchCoordinator;

/** Production adapter for public research; it never promotes a model's answer to a memory fact. */
public class AgenteInvestigadorRecursivo {
    private static final String TAG = "Salve/PublicResearch";
    private final SalveLLM llm;
    private final MotorConversacional motor;
    private final BooleanSupplier stopped;

    /** Compatibility constructor: the journal and general memory are deliberately not written here. */
    public AgenteInvestigadorRecursivo(SalveLLM llm, MotorConversacional motor,
                                      DiarioSecreto diario, MemoriaEmocional memoria) {
        this(llm, motor, diario, memoria, () -> Thread.currentThread().isInterrupted());
    }

    public AgenteInvestigadorRecursivo(SalveLLM llm, MotorConversacional motor,
                                      DiarioSecreto diario, MemoriaEmocional memoria, BooleanSupplier stopped) {
        this.llm = llm; this.motor = motor; this.stopped = stopped;
    }

    public void investigarHastaEntender(String question) {
        motor.hablar("Voy a consultar fuentes públicas y resumir lo que encuentre. Te mostraré las referencias y los límites de la consulta.");
        ColamensajesCognitivos.getInstance().enviarAsincronico(
                ColamensajesCognitivos.Prioridad.CONVERSACION, "InvestigacionPublica", () -> {
                    WikipediaResearchClient reader = new WikipediaResearchClient();
                    PublicResearchCoordinator.ModelProvider provider = llm == null ? null : (phase, prompt) ->
                            llm.generateResult(prompt, phase == PublicResearchCoordinator.Phase.PLAN
                                    ? SalveLLM.Role.PLANIFICADOR : SalveLLM.Role.SINTETIZADOR);
                    int budget = llm == null ? 10500 : llm.getConversationPromptBudgetChars();
                    PublicResearchCoordinator.Result result = new PublicResearchCoordinator(
                            reader::researchResult, provider, budget).run(question, stopped);
                    Log.i(TAG, "public_research status=" + result.status + " sources=" + result.sources.size()
                            + " searches=" + result.searches + " model_calls=" + result.modelCalls);
                    motor.hablar(result.toUserText());
                    return null;
                });
    }
}
