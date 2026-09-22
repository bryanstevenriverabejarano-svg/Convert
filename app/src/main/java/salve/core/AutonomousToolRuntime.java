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

/** Application-scoped, local-only tool laboratory. It has no Android app-control or network tools. */
public final class AutonomousToolRuntime {
    private static volatile AutonomousToolRuntime instance;
    private final File stateFile;
    private final AtomicBoolean paused = new AtomicBoolean();
    private final AtomicLong cancellationEpoch = new AtomicLong();
    private volatile AutonomousToolLab laboratory;

    private AutonomousToolRuntime(Context context) {
        this(new File(context.getApplicationContext().getNoBackupFilesDir(), "autonomy/tools.json"));
    }

    /** Test seam using the same real file store and laboratory, without a mocked Android service. */
    AutonomousToolRuntime(File stateFile) {
        if (stateFile == null) throw new IllegalArgumentException("Archivo ausente.");
        this.stateFile = stateFile;
    }

    public static AutonomousToolRuntime get(Context context) {
        if (instance == null) {
            synchronized (AutonomousToolRuntime.class) {
                if (instance == null) instance = new AutonomousToolRuntime(context);
            }
        }
        return instance;
    }

    public static boolean handles(String input) { return AutonomousToolCommand.parse(input) != null; }

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
                case CATALOG:
                    return salve.core.autonomy.ToolRegistry.summary();
                case STATUS:
                    return lab().describe() + "\n" + help();
                case ROLLBACK:
                    if (!AutonomousToolCommand.isFamily(command.payload))
                        return "Indica una familia: revierte herramienta route, knapsack, schedule o dependencies.";
                    return lab().rollback(command.payload);
                case SOLVE:
                    return solve(AutonomousToolCommand.parseObject(command.payload), stopped);
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
    public String proceduralContextFor(String input) {
        String family = AutonomousToolCommand.offeredFamilyFor(input);
        if (family.isEmpty()) return "";
        try {
            return lab().proceduralContext(family);
        } catch (Exception unavailable) {
            // Absent context makes no claim that the journal is empty or that a tool was learned.
            return "";
        }
    }

    public static String instructionFor(String input) {
        String family = AutonomousToolCommand.offeredFamilyFor(input);
        if (!salve.core.autonomy.ToolRegistry.families().contains(family)) return "";
        String schema = salve.core.autonomy.ToolRegistry.inputTemplate(family);
        // One complete bounded instruction; prompt budgeting cannot leave a half contract behind.
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
                + "También puedes pausar, reanudar o revertir una herramienta. Se admiten cuatro familias acotadas; el laboratorio no cambia el código de la aplicación.";
    }
}
