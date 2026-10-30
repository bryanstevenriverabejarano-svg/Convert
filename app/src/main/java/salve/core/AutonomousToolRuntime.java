package salve.core;

import android.content.Context;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import salve.core.autonomy.AutonomousToolCommand;
import salve.core.autonomy.AutonomousToolLab;
import salve.core.autonomy.AutonomousToolReply;
import salve.core.autonomy.FileToolStateStore;
import salve.core.autonomy.InductiveSkillLab;
import salve.core.autonomy.InductiveSkillProposal;
import salve.core.autonomy.BlindSkillTransferExperiment;
import salve.core.agent.AgentRun;
import salve.core.agent.AgentSkillGapDetector;
import java.util.List;

/** Application-scoped, local-only tool laboratory. It has no Android app-control or network tools. */
public final class AutonomousToolRuntime {
    private static volatile AutonomousToolRuntime instance;
    private final File stateFile;
    private final Context appContext;
    private final File inductiveFile;
    private final File proposedSkillFile;
    private final File suggestionCooldownFile;
    private volatile InductiveSkillLab inductive;
    private final AtomicBoolean paused = new AtomicBoolean();
    private final AtomicLong cancellationEpoch = new AtomicLong();
    private volatile AutonomousToolLab laboratory;

    private AutonomousToolRuntime(Context context) {
        this(new File(context.getApplicationContext().getNoBackupFilesDir(), "autonomy/tools.json"),
                context.getApplicationContext());
    }
    /** Test seam using the same real file store and laboratory, without a mocked Android service. */
    AutonomousToolRuntime(File stateFile) { this(stateFile, null); }
    private AutonomousToolRuntime(File stateFile, Context appContext) {
        if (stateFile == null) throw new IllegalArgumentException("Archivo ausente.");
        this.appContext = appContext;
        this.stateFile = stateFile;
        this.inductiveFile = new File(stateFile.getParentFile(), "inductive-skills.json");
        this.proposedSkillFile = new File(stateFile.getParentFile(), "proposed-inductive-skill.json");
        this.suggestionCooldownFile = new File(stateFile.getParentFile(), "auto-skill-suggestion-cooldown.json");
    }
    public static AutonomousToolRuntime get(Context context) {
        if (instance == null) {
            synchronized (AutonomousToolRuntime.class) {
                if (instance == null) instance = new AutonomousToolRuntime(context);
            }
        }
        return instance;
    }
    public static boolean handles(String input) {
        return cloudSkillCommand(input) != 0 || learningCommand(input) != 0 || inductiveCommand(input) != 0
                || proposalCommand(input) != 0 || experimentCommand(input)
                || AutonomousToolCommand.parse(input) != null;
    }
    /** Exact owner-channel commands; never inferred from model or cloud text. */
    private static int cloudSkillCommand(String input) {
        if (input == null || input.length() > 100) return 0;
        String value = java.text.Normalizer.normalize(
                input.toLowerCase(java.util.Locale.ROOT).trim(),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
        if ("estado habilidades pcloud".equals(value)) return 1;
        if ("restaura habilidades pcloud".equals(value)) return 2;
        return 0;
    }

    private File inbox(String name) {
        return new File(new File(stateFile.getParentFile(), "cloud-inbox"), name);
    }

    private String cloudSkillStatus() {
        int staged = (inbox("tools.json").isFile() ? 1 : 0)
                + (inbox("inductive-skills.json").isFile() ? 1 : 0);
        return staged == 0 ? "No hay copias de habilidades pendientes de restauración."
                : "Hay " + staged + " copia(s) de habilidades de pCloud en cuarentena. "
                + "Para restaurarlas, pausa el laboratorio y di exactamente: restaura habilidades pcloud. "
                + "Nunca se instalan automáticamente ni sustituyen los catálogos existentes.";
    }

    /**
     * Restore only with explicit verified-owner approval while all existing
     * tool computations are paused. Never downloads or executes cloud content.
     */
    synchronized String restoreStagedForVerifiedOwner(boolean authorized) {
        if (!authorized) return "Restauración denegada: falta verificar al dueño.";
        if (!paused.get()) return "Primero pausa el laboratorio para restaurar habilidades de pCloud.";
        int restored = 0;
        try {
            if (salve.data.sync.SkillCloudArchive.installIfApproved(stateFile, inbox("tools.json"),
                    salve.data.sync.SkillCloudArchive.TOOLS, true)) restored++;
            if (salve.data.sync.SkillCloudArchive.installIfApproved(inductiveFile, inbox("inductive-skills.json"),
                    salve.data.sync.SkillCloudArchive.INDUCTIVE, true)) restored++;
            if (restored > 0) {
                laboratory = null; inductive = null;
                return "Restauré " + restored + " catálogo(s) simbólicos verificados. "
                        + "Los catálogos existentes no se sobrescribieron. "
                        + "El laboratorio sigue pausado: revisa habilidades aprendidas y reanúdalo explícitamente.";
            }
            return "No restauré ningún catálogo: no hay copias válidas pendientes, "
                    + "o ya existen habilidades locales que debo conservar.";
        } catch (Exception rejected) {
            return "No pude restaurar todas las copias. Conservo los archivos y el laboratorio pausado.";
        }
    }

    /** Exact commands from the user channel, not inferred from quoted documents or memory. */
    private static int learningCommand(String input) {
        if (input == null || input.length() > 160) return 0;
        String text = java.text.Normalizer.normalize(input.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
        switch (text) {
            case "estado aprendizaje experimental": return 1;
            case "activa aprendizaje experimental": return 2;
            case "desactiva aprendizaje experimental": return 3;
            default: return 0;
        }
    }
    /** Commands must originate in the user turn, not retrieved pages or LLM output. */
    private static int inductiveCommand(String input) {
        if (input == null || input.length() > AutonomousToolCommand.MAX_INPUT_BYTES) return 0;
        String value = input.trim();
        if (value.equalsIgnoreCase("habilidades aprendidas")) return 1;
        if (value.toLowerCase(java.util.Locale.ROOT).startsWith("aprende habilidad:")) return 2;
        if (value.toLowerCase(java.util.Locale.ROOT).startsWith("usa habilidad:")) return 3;
        return 0;
    }

    /** Experimental challenge explicitly requested in the user's own turn. */
    private static boolean experimentCommand(String input) {
        return input != null && input.trim().equalsIgnoreCase("experimento de aprendizaje");
    }

    /** Explicit, user-channel approval; never parse proposals from LLM or retrieved pages. */
    private static int proposalCommand(String input) {
        if (input == null || input.length() > 180) return 0;
        String value = input.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.equals("propuestas de habilidades")) return 4;
        if (value.startsWith("propón habilidad:") || value.startsWith("propon habilidad:")) return 1;
        if (value.startsWith("aprueba habilidad ")) return 2;
        if (value.startsWith("descarta habilidad ")) return 3;
        return 0;
    }

    /**
     * Called only by the real plan worker, after a terminal result is journaled.
     * Never installs or promotes a skill. One pending proposal and a seven-day
     * cooldown prevent repeated prompts, including after user dismissal.
     */
    public synchronized boolean observeFailedPlanHistory(List<AgentRun> plans, BooleanSupplier stopped) {
        if (paused.get() || stopped != null && stopped.getAsBoolean()) return false;
        InductiveSkillProposal.Kind kind = AgentSkillGapDetector.detect(plans, System.currentTimeMillis());
        if (kind == null) return false;
        long epoch = cancellationEpoch.get();
        BooleanSupplier cancelled = () -> paused.get() || cancellationEpoch.get() != epoch
                || Thread.currentThread().isInterrupted()
                || stopped != null && stopped.getAsBoolean();
        try {
            if (cancelled.getAsBoolean() || skills().knows(InductiveSkillProposal.skillName(kind)))
                return false;
            JsonObject pending = pendingSkill();
            if (pending != null && pending.get("expires").getAsLong() >= System.currentTimeMillis())
                return false;
            String lastJson = new FileToolStateStore(suggestionCooldownFile).read();
            if (lastJson != null) {
                JsonObject last = AutonomousToolCommand.parseObject(lastJson);
                if (last.size() != 3 || !last.has("schema") || last.get("schema").getAsInt() != 1
                        || !last.has("kind") || !last.has("until")
                        || !last.get("kind").getAsString().matches("SORT_UNIQUE_ODDS|SORT_UNIQUE_EVENS"))
                    return false; // corrupt marker: do not overwrite automatically
                if (kind.name().equals(last.get("kind").getAsString())
                        && last.get("until").getAsLong() > System.currentTimeMillis())
                    return false;
            }
            InductiveSkillProposal.Evaluation evaluation = InductiveSkillProposal.evaluate(skills(), kind, cancelled);
            if (!evaluation.promotable() || cancelled.getAsBoolean()) return false;
            JsonObject proposal = new JsonObject();
            proposal.addProperty("schema", 1);
            proposal.addProperty("id", java.util.UUID.randomUUID().toString());
            proposal.addProperty("kind", kind.name());
            proposal.add("program", evaluation.program.deepCopy());
            proposal.addProperty("expires", System.currentTimeMillis() + 86_400_000L);
            if (cancelled.getAsBoolean()) return false;
            // Atomic proposal first: failed proposal storage must never start a
            // cooldown that hides the opportunity from the user.
            new FileToolStateStore(proposedSkillFile).write(proposal.toString());
            if (cancelled.getAsBoolean()) {
                proposedSkillFile.delete(); // no installation, even if deletion fails
                return false;
            }
            JsonObject cooldown = new JsonObject();
            cooldown.addProperty("schema", 1);
            cooldown.addProperty("kind", kind.name());
            cooldown.addProperty("until", System.currentTimeMillis() + 7L * 24 * 60 * 60_000L);
            try {
                new FileToolStateStore(suggestionCooldownFile).write(cooldown.toString());
            } catch (Exception denied) {
                proposedSkillFile.delete();
                return false;
            }
            return true;
        } catch (Exception failure) {
            // Never modify existing skills or grant permissions on an observer failure.
            return false;
        }
    }

    /** Read-only summary exposed when the user checks Salve's plan list/status. */
    public synchronized String pendingSkillSuggestion() {
        if (paused.get()) return "";
        try {
            JsonObject pending = pendingSkill();
            if (pending == null || pending.get("expires").getAsLong() <= System.currentTimeMillis())
                return "";
            String kind = pending.get("kind").getAsString();
            String description = kind.equals("SORT_UNIQUE_ODDS")
                    ? "ordenar impares sin repetir" : "ordenar pares sin repetir";
            return "Sugerencia de habilidad, todavía NO instalada: " + description
                    + ". Di 'propuestas de habilidades' para revisar; solo tú puedes aprobar o descartar.";
        } catch (Exception invalid) { return ""; }
    }

    /** Persistent pending metadata contains no examples, contacts, model text or secrets. */
    private JsonObject pendingSkill() throws Exception {
        String json = new FileToolStateStore(proposedSkillFile).read();
        if (json == null) return null;
        JsonObject data = AutonomousToolCommand.parseObject(json);
        if (data.size() != 5 || !data.has("schema") || data.get("schema").getAsInt() != 1
                || !data.has("id") || !data.get("id").getAsString()
                        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                || !data.has("kind") || !data.has("program") || !data.get("program").isJsonArray()
                || !data.has("expires") || !data.get("expires").getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Propuesta guardada inválida.");
        InductiveSkillProposal.Kind.valueOf(data.get("kind").getAsString());
        long expires = data.get("expires").getAsLong();
        if (expires <= 0 || expires > System.currentTimeMillis() + 86_400_000L)
            throw new IllegalArgumentException("Vigencia de propuesta inválida.");
        return data;
    }
    private String proposalReply(String input, BooleanSupplier stopped) {
        int action = proposalCommand(input);
        if (action == 0) return null;
        if (paused.get()) return "Laboratorio pausado: no creo ni promuevo habilidades.";
        long epoch = cancellationEpoch.get();
        BooleanSupplier cancelled = () -> paused.get() || cancellationEpoch.get() != epoch
                || Thread.currentThread().isInterrupted()
                || (stopped != null && stopped.getAsBoolean());
        if (cancelled.getAsBoolean()) return "Propuesta cancelada.";
        try {
            if (action == 4) {
                JsonObject pending = pendingSkill();
                return pending == null ? "No hay propuestas pendientes."
                        : "Pendiente: " + pending.get("kind").getAsString()
                        + "; ID " + pending.get("id").getAsString()
                        + ". Solo el usuario puede aprobarla o descartarla.";
            }
            String raw = input.trim();
            if (action == 1) {
                JsonObject existing = pendingSkill();
                if (existing != null && existing.get("expires").getAsLong() > System.currentTimeMillis())
                    return "Ya hay una propuesta pendiente; revísala o descártala antes de crear otra.";
                InductiveSkillProposal.Kind kind = InductiveSkillProposal.recognize(
                        raw.substring(raw.indexOf(':') + 1).trim());
                if (kind == null) return "Necesidad no reconocida. Por ahora: ordenar impares sin repetir, ordenar pares sin repetir, sumas acumuladas o diferencias consecutivas.";
                InductiveSkillProposal.Evaluation result = InductiveSkillProposal.evaluate(skills(), kind, cancelled);
                if (!result.promotable()) return "Propuesta rechazada: no superó la referencia independiente.";
                if (cancelled.getAsBoolean()) return "Propuesta cancelada.";
                JsonObject proposal = new JsonObject();
                String id = java.util.UUID.randomUUID().toString();
                proposal.addProperty("schema", 1);
                proposal.addProperty("id", id);
                proposal.addProperty("kind", kind.name());
                proposal.add("program", result.program.deepCopy());
                proposal.addProperty("expires", System.currentTimeMillis() + 86_400_000L);
                new FileToolStateStore(proposedSkillFile).write(proposal.toString());
                if (cancelled.getAsBoolean()) {
                    proposedSkillFile.delete();
                    return "Propuesta cancelada; no instalé ninguna habilidad.";
                }
                return "Propuesta NO instalada: " + result.skillName
                        + ". Candidatos probados: " + result.candidatesChecked
                        + "; casos nuevos: " + result.candidateSuccesses + "/" + result.total
                        + "; referencia sin aprender: " + result.baselineSuccesses + "/" + result.total
                        + ". Para guardarla, di exactamente: aprueba habilidad " + id
                        + ". Para descartarla: descarta habilidad " + id + ". Caduca en 24 horas.";
            }
            JsonObject pending = pendingSkill();
            if (pending == null) return "No existe ninguna propuesta pendiente.";
            // User approval must include the full, exact proposal UUID.
            String suppliedId = raw.substring(raw.lastIndexOf(' ') + 1);
            if (!pending.get("id").getAsString().equals(suppliedId))
                return "El identificador no corresponde a la propuesta pendiente; no cambié nada.";
            if (pending.get("expires").getAsLong() < System.currentTimeMillis())
                return "La propuesta caducó; solicita una nueva evaluación.";
            if (action == 3) {
                if (!proposedSkillFile.delete()) return "No pude descartar el archivo pendiente; no cambié habilidades.";
                return "Propuesta descartada; ninguna habilidad fue instalada.";
            }
            InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.valueOf(
                    pending.get("kind").getAsString());
            if (cancelled.getAsBoolean()) return "Aprobación cancelada.";
            JsonObject installed = InductiveSkillProposal.promote(skills(), kind,
                    pending.getAsJsonArray("program"), cancelled);
            if (!proposedSkillFile.delete())
                return "Habilidad certificada y guardada, pero no pude limpiar la propuesta pendiente. "
                        + installed.get("name").getAsString();
            return "Habilidad aprobada, verificada y guardada: " + installed.get("name").getAsString()
                    + ". Puedes consultarla con habilidades aprendidas o usarla con usa habilidad:.";
        } catch (IllegalArgumentException invalid) {
            return "No se aceptó la propuesta: " + invalid.getMessage();
        } catch (Exception unavailable) {
            return "No pude completar la propuesta o su almacenamiento; no afirmo haber promovido una habilidad.";
        }
    }

    private InductiveSkillLab skills() throws Exception {
        if (inductive == null) {
            synchronized (this) {
                if (inductive == null) {
                    final FileToolStateStore file = new FileToolStateStore(inductiveFile);
                    inductive = new InductiveSkillLab(new InductiveSkillLab.Store() {
                        @Override public String read() throws Exception { return file.read(); }
                        @Override public void write(String json) throws Exception { file.write(json); }
                    });
                }
            }
        }
        return inductive;
    }

    public static boolean isPauseCommand(String input) {
        AutonomousToolCommand command = AutonomousToolCommand.parse(input);
        return command != null && command.action == AutonomousToolCommand.Action.PAUSE;
    }
    /** Never queues behind a solve or touches disk; an old solve stays cancelled after resume. */
    public void pause() {
        paused.set(true);
        cancellationEpoch.incrementAndGet();
        AutonomousToolLab current = laboratory;
        if (current != null) current.pause();
    }
    public String respond(String input, BooleanSupplier stopped) {
        int cloudSkill = cloudSkillCommand(input);
        if (cloudSkill != 0) {
            if (appContext == null || !salve.core.identity.IdentityManager.get(appContext).privateAccess())
                return "Las habilidades de pCloud son privadas: debes verificar tu identidad.";
            if (cloudSkill == 1) return cloudSkillStatus();
            if (!salve.data.sync.CloudSyncManager.isEnabled(appContext)
                    || !salve.data.sync.CloudSyncManager.isPCloudConfigured(appContext))
                return "Conecta pCloud y habilita la sincronización antes de restaurar sus habilidades.";
            return restoreStagedForVerifiedOwner(stopped == null || !stopped.getAsBoolean());
        }
        if (experimentCommand(input)) {
            if (paused.get()) return "Experimento cancelado: laboratorio pausado.";
            long epoch = cancellationEpoch.get();
            BooleanSupplier cancelled = () -> paused.get() || cancellationEpoch.get() != epoch
                    || Thread.currentThread().isInterrupted()
                    || stopped != null && stopped.getAsBoolean();
            try {
                if (cancelled.getAsBoolean()) return "Experimento cancelado.";
                BlindSkillTransferExperiment.Result report =
                        BlindSkillTransferExperiment.runNewChallenge(cancelled);
                return "EXPERIMENTO DE APRENDIZAJE LOCAL (sin instalación): " + report.report()
                        + ". La prueba final es nueva y solo se consulta tras congelar el programa. "
                        + "Si falla, el examen se conserva como resultado negativo, "
                        + "no se usa para ajustar y aprobar el mismo candidato. "
                        + "Prueba limitada a listas numéricas; no demuestra AGI.";
            } catch (Exception failure) {
                return cancelled.getAsBoolean() ? "Experimento cancelado."
                        : "Experimento fallido sin instalar habilidad: "
                        + failure.getClass().getSimpleName();
            }
        }
        if (proposalCommand(input) != 0) return proposalReply(input, stopped);
        int inductiveAction = inductiveCommand(input);
        if (inductiveAction != 0) {
            if (paused.get()) return "Laboratorio pausado; reanúdalo para usar habilidades.";
            long epoch = cancellationEpoch.get();
            BooleanSupplier cancelled = () -> paused.get() || cancellationEpoch.get() != epoch
                    || Thread.currentThread().isInterrupted()
                    || (stopped != null && stopped.getAsBoolean());
            if (cancelled.getAsBoolean()) return "Solicitud cancelada.";
            try {
                if (inductiveAction == 1) return skills().catalog() + "\n" + cloudSkillStatus();
                String value = input.trim();
                JsonObject data = AutonomousToolCommand.parseObject(value.substring(value.indexOf(':') + 1).trim());
                if (inductiveAction == 2) {
                    return "Síntesis inductiva: " + skills().learn(data, cancelled)
                            + ". La certificación solo cubre los casos reservados proporcionados; "
                            + "no prueba generalización universal ni entrenamiento de pesos.";
                }
                if (data.size() != 2 || !data.has("name") || !data.has("input")
                        || !data.get("name").isJsonPrimitive() || !data.get("name").getAsJsonPrimitive().isString())
                    return "Uso inválido: requiere name e input.";
                if (cancelled.getAsBoolean()) return "Solicitud cancelada.";
                return "Resultado de habilidad guardada: " + skills().apply(data.get("name").getAsString(), data.get("input"));
            } catch (IllegalArgumentException invalid) {
                return "Habilidad rechazada: " + invalid.getMessage();
            } catch (Exception unavailable) {
                return "No pude certificar o leer la habilidad. No afirmo que haya aprendido.";
            }
        }
        int learning = learningCommand(input);
        if (learning != 0) {
            if (Thread.currentThread().isInterrupted() || (stopped != null && stopped.getAsBoolean()))
                return "Solicitud cancelada; no cambié la configuración experimental.";
            try {
                return learning == 1 ? lab().learningStatus() : lab().setExperimentalLearning(learning == 2);
            } catch (Exception unavailable) {
                return "No pude leer o guardar el aprendizaje experimental. No lo he dado por activado ni he sustituido el registro.";
            }
        }
        AutonomousToolCommand command = AutonomousToolCommand.parse(input);
        if (command == null) return null;
        if (command.action == AutonomousToolCommand.Action.PAUSE) {
            pause();
            return "Laboratorio pausado; he solicitado detener el cálculo activo. La pausa dura durante esta sesión de la aplicación.";
        }
        if (command.action == AutonomousToolCommand.Action.RESUME) {
            paused.set(false);
            AutonomousToolLab current = laboratory;
            if (current != null) current.resume();
            return "Laboratorio reanudado. Puedes plantear un nuevo reto; los cálculos cancelados no se reinician.";
        }
        try {
            switch (command.action) {
                case CATALOG: return salve.core.autonomy.ToolRegistry.summary();
                case STATUS: return lab().describe() + "\n" + cloudSkillStatus() + "\n" + help();
                case ROLLBACK:
                    if (!AutonomousToolCommand.isFamily(command.payload))
                        return "Indica una familia: revierte herramienta route, knapsack, schedule o dependencies.";
                    return lab().rollback(command.payload);
                case SOLVE: return solve(AutonomousToolCommand.parseObject(command.payload), stopped);
                default: return help();
            }
        } catch (IllegalArgumentException invalid) {
            return "No pude aceptar ese reto: comprueba el JSON y los límites. " + help();
        } catch (Exception unavailable) {
            return "El estado del laboratorio no está disponible. No lo he sustituido ni he dado por resuelto el reto.";
        }
    }
    /** Called exclusively on a model-generated tool reply, never on user text or retrieved memories. */
    public String respondToModel(String reply, BooleanSupplier stopped) {
        try {
            JsonObject call = AutonomousToolCommand.parseObject(reply);
            if (call.size() != 2 || !call.has("tool") || !call.get("tool").isJsonPrimitive()
                    || !call.get("tool").getAsJsonPrimitive().isString()
                    || !"SOLVE_CHALLENGE".equals(call.get("tool").getAsString())
                    || !call.has("challenge") || !call.get("challenge").isJsonObject())
                throw new IllegalArgumentException("Formato de herramienta inválido.");
            JsonObject challenge = call.getAsJsonObject("challenge");
            String interpreted = challenge.toString();
            if (interpreted.length() > 2048)
                return "El reto interpretado es demasiado extenso para mostrarlo en esta respuesta. Usa resuelve reto: seguido del JSON explícito, hasta 64 KiB.";
            return solve(challenge, stopped) + "\nDatos interpretados: " + interpreted
                    + "\nComprueba estos datos: el verificador comprueba el cálculo, no que el modelo haya interpretado correctamente tu petición.";
        } catch (IllegalArgumentException invalid) {
            return "El modelo propuso un reto inválido. No lo ejecuté. " + help();
        } catch (Exception unavailable) {
            return "No pude completar el reto del modelo ni verificar su resultado.";
        }
    }
    /** Juego propio: un reto inventado por Salve, resuelto y verificado por el laboratorio. */
    public JsonObject play(JsonObject challenge, BooleanSupplier stopped) throws Exception {
        long epoch = cancellationEpoch.get();
        if (paused.get()) return null;
        return lab().solve(challenge, () -> paused.get() || cancellationEpoch.get() != epoch
                || Thread.currentThread().isInterrupted() || (stopped != null && stopped.getAsBoolean()));
    }

    private String solve(JsonObject challenge, BooleanSupplier stopped) throws Exception {
        long epoch = cancellationEpoch.get();
        if (paused.get()) return "El laboratorio está pausado. Di reanuda el laboratorio para aceptar nuevos retos.";
        JsonObject report = lab().solve(challenge, () -> paused.get() || cancellationEpoch.get() != epoch
                || Thread.currentThread().isInterrupted() || (stopped != null && stopped.getAsBoolean()));
        return AutonomousToolReply.summarize(report);
    }
    private AutonomousToolLab lab() throws Exception {
        if (laboratory == null) {
            synchronized (this) {
                if (laboratory == null) {
                    AutonomousToolLab created = new AutonomousToolLab(new FileToolStateStore(stateFile));
                    if (paused.get()) created.pause();
                    laboratory = created;
                }
            }
        }
        return laboratory;
    }
    /** Relevant verified program metadata only; never challenge data or autobiographical memory. */
    /** Read-only capability awareness; no user examples, model text or private memories. */
    public String learnedSkillsSummary() {
        try { return skills().catalog(); }
        catch (Exception unavailable) { return "El catálogo inductivo no está disponible para leerlo."; }
    }

    public String proceduralContextFor(String input) {
        String family = AutonomousToolCommand.offeredFamilyFor(input);
        if (family.isEmpty()) return "";
        try { return lab().proceduralContext(family); }
        catch (Exception unavailable) { return ""; }
    }
    public static String instructionFor(String input) {
        String family = AutonomousToolCommand.offeredFamilyFor(input);
        if (!salve.core.autonomy.ToolRegistry.families().contains(family)) return "";
        String schema = salve.core.autonomy.ToolRegistry.inputTemplate(family);
        return "CÁLCULO LOCAL: sólo si todos los datos están explícitos, responde un JSON "
                + "{\"tool\":\"SOLVE_CHALLENGE\",\"challenge\":{\"schema\":1,\"id\":\"consulta\",\"family\":\""
                + family + "\",\"input\":" + schema + "}}. "
                + "Usa enteros; nodos o elementos<=14; índices desde 0; capacidad<=100. "
                + "No inventes datos ni cambies el problema: pregunta si falta algo. "
                + "Es cálculo puro sin confirmación externa, red ni código libre. "
                + "El verificador comprueba el cálculo, no tu interpretación.\n";
    }
    private static String help() {
        return "Prueba resuelve reto: {\"schema\":1,\"id\":\"consulta\",\"family\":\"dependencies\",\"input\":{\"nodes\":3,\"edges\":[[0,1],[1,2]]}}. "
                + "También puedes pausar, reanudar o revertir una herramienta. Se admiten cuatro familias acotadas; el laboratorio no cambia el código de la aplicación. "
                + "Comandos: estado aprendizaje experimental; activa aprendizaje experimental; desactiva aprendizaje experimental. " 
                + "Laboratorio inductivo: habilidades aprendidas; aprende habilidad: {nombre, train, holdout}; usa habilidad: {name, input}. "
                + "Propuestas locales: propón habilidad: ordenar impares sin repetir; aprueba habilidad ID; descarta habilidad ID. "
                + "Copias: estado habilidades pcloud; primero pausa el laboratorio y luego restaura habilidades pcloud (solo dueño verificado). "
                + "Experimento controlado: experimento de aprendizaje (reintentos solo en desarrollo y examen final no reutilizado). " 
                + "Cada ejemplo de train/holdout usa las claves in y out, con listas numéricas.";
    }
}
