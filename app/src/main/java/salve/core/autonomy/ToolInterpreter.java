package salve.core.autonomy;

import com.google.gson.JsonObject;

/** Capability boundary: only the fixed, audited kernels and exact verifier are callable. */
public final class ToolInterpreter {
    private ToolInterpreter() { }
    public static final class Execution {
        public final JsonObject result;
        public final ToolVerifier.Verification verification;
        Execution(JsonObject result, ToolVerifier.Verification verification) {
            this.result = result; this.verification = verification;
        }
    }
    public static Execution run(JsonObject program, JsonObject input, OperationBudget budget) {
        budget.tick();
        String strategy = ToolProgram.strategy(program);
        String family = program.get("family").getAsString();
        // Input validation happens inside each kernel before work is performed.
        JsonObject result = ToolKernels.execute(family, strategy, input.deepCopy(), budget);
        ToolVerifier.Verification verified = ToolVerifier.verify(family, input, result, budget);
        return new Execution(result, verified);
    }
}
