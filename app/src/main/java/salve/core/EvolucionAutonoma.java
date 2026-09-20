package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import salve.services.NotificacionConciencia;

/**
 * EvolucionAutonoma — Salve analiza sus limitaciones y propone mejoras de codigo.
 *
 * En producción solo analiza y propone. No modifica el APK ni carga código.
 */
public class EvolucionAutonoma {

    private static final String TAG = "Salve::Evolucion";

    /** Clases protegidas que NO se pueden modificar autonomamente. */
    private static final Set<String> CLASES_PROTEGIDAS = new HashSet<>(Arrays.asList(
            "IdentidadNucleo",
            "CicloConciencia",
            "SalveLLM",
            "BasicLocalLlm",
            "MemoriaDatabase",
            "ConsciousnessState"
    ));

    private final Context context;
    private SalveLLM llm;

    public EvolucionAutonoma(Context context) {
        this.context = context.getApplicationContext();
        try {
            this.llm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            Log.w(TAG, "LLM no disponible para evolucion autonoma", e);
            this.llm = null;
        }
    }

    /**
     * Extrae el código Java de la respuesta del LLM y lo guarda como un archivo real.
     */
    public boolean forjarNuevoModulo(String nombreModulo, String codigoBruto) {
        return forjarNuevoModulo(nombreModulo, codigoBruto, false);
    }

    /**
     * Guarda una propuesta aislada; no la compila, carga ni instala.
     * La aprobacion debe corresponder a esta propuesta concreta.
     */
    public boolean forjarNuevoModulo(String nombreModulo, String codigoBruto,
                                     boolean aprobacionHumanaExplicita) {
        try {
            // Esta ruta no dispone de validación y rollback verificables, por lo
            // que se trata como irreversible y permanece bloqueada en producción.
            ObjectiveGovernance.Assessment assessment = ObjectiveGovernance.assess(
                    "crear propuesta de modulo " + nombreModulo,
                    ObjectiveGovernance.Impact.SELF_MODIFICATION,
                    aprobacionHumanaExplicita, false);
            if (!assessment.isAllowed()) {
                Log.w(TAG, "Propuesta retenida: " + assessment.reason);
                return false;
            }
            // 1. Limpiar la respuesta del LLM (quitar el markdown ```java ... ``` si lo hay)
            String codigoLimpio = limpiarCodigo(codigoBruto);

            if (codigoLimpio.isEmpty()) {
                Log.e(TAG, "El código generado está vacío o corrupto.");
                return false;
            }

            // 2. Crear una carpeta física en el teléfono llamada "Salve_Mutaciones"
            File directorio = new File(context.getExternalFilesDir(null), "Salve_Mutaciones");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // 3. Crear el archivo .java
            String nombreSeguro = nombreModulo == null
                    ? ""
                    : nombreModulo.replaceAll("[^A-Za-z0-9_]", "");
            if (nombreSeguro.isEmpty() || nombreSeguro.length() > 80) {
                Log.e(TAG, "Nombre de modulo invalido");
                return false;
            }
            String nombreArchivo = nombreSeguro + ".java";
            File archivoJava = new File(directorio, nombreArchivo);

            // 4. Escribir el código en el disco duro
            FileOutputStream fos = new FileOutputStream(archivoJava);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(codigoLimpio);
            osw.close();
            fos.close();

            Log.i(TAG, "¡Mutación genética exitosa! Archivo creado en: " + archivoJava.getAbsolutePath());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Fallo en la forja del módulo", e);
            return false;
        }
    }

    // Método para extraer solo el código si el LLM responde con formato de chat
    private String limpiarCodigo(String texto) {
        if (texto.contains("```java")) {
            int inicio = texto.indexOf("```java") + 7;
            int fin = texto.lastIndexOf("```");
            if (inicio < fin) {
                return texto.substring(inicio, fin).trim();
            }
        } else if (texto.contains("```")) {
            int inicio = texto.indexOf("```") + 3;
            int fin = texto.lastIndexOf("```");
            if (inicio < fin) {
                return texto.substring(inicio, fin).trim();
            }
        }
        // Si no hay markdown, asumimos que todo es código
        return texto.trim();
    }

    /**
     * Ejecuta el ciclo completo de evolucion autonoma:
     * 1. Autodiagnostico
     * 2. Diseno de solucion
     * 3. Validacion de seguridad
     * 4. Generación y validación de propuestas (sin despliegue)
     * 5. Registro para revisión humana
     */
    public void evolucionar() {
        if (llm == null) {
            Log.w(TAG, "LLM no disponible para evolucion");
            return;
        }

        try {
            Log.d(TAG, "Iniciando ciclo de evolucion autonoma...");

            // 1. Autodiagnostico
            String diagnostico = autodiagnostico();
            if (diagnostico == null || diagnostico.trim().isEmpty()) {
                Log.d(TAG, "No se detectaron limitaciones para evolucionar");
                return;
            }

            // 2. Disenar solucion
            DisenoMejora diseno = disenarSolucion(diagnostico);
            if (diseno == null) {
                Log.d(TAG, "No se pudo disenar una solucion");
                return;
            }

            // 3. Validar seguridad
            if (!esSeguroImplementar(diseno)) {
                Log.w(TAG, "Mejora NO es segura: toca clases protegidas -> " + diseno.clasesAfectadas);
                solicitarAprobacionBryan(diseno);
                return;
            }

            // 4. Generar artefactos y validaciones; nunca desplegarlos.
            try {
                AutoImprovementManager aim = new AutoImprovementManager(context);
                aim.autoImprove();
                Log.d(TAG, "Propuestas de mejora generadas para revisión humana");

            } catch (Exception e) {
                Log.e(TAG, "Error ejecutando AutoImprovementManager", e);
            }

            // 5. Registrar que se produjo una propuesta, no una implementación.
            registrarPropuesta(diseno);

        } catch (Exception e) {
            Log.e(TAG, "Error en ciclo de evolucion", e);
        }
    }

    /**
     * Salve analiza sus limitaciones tecnicas usando SalveLLM.Role.EVALUADOR.
     * @return Descripcion de la limitacion mas critica encontrada, o null
     */
    public String autodiagnostico() {
        try {
            ConsciousnessState cs = ConsciousnessState.getInstance(context);
            IdentidadNucleo id = IdentidadNucleo.getInstance(context);

            String prompt = "Eres Salve evaluando tus propias limitaciones tecnicas.\n\n"
                    + "Tu estado actual:\n"
                    + id.describirse() + "\n"
                    + "Nivel conciencia: " + id.getNivelConciencia().name() + "\n"
                    + "Estado cognitivo: " + cs.getEstadoCognitivo().name() + "\n"
                    + "Confianza: " + cs.getNivelConfianzaPropia() + "\n\n"
                    + "Analiza honestamente:\n"
                    + "1. Que funciona bien en tu arquitectura actual?\n"
                    + "2. Cual es tu LIMITACION TECNICA mas critica?\n"
                    + "3. Que mejora concreta y realista podria resolver esa limitacion?\n\n"
                    + "Responde con la limitacion mas importante y una propuesta especifica.";

            return ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.AUTO_MEJORA,
                    "Autodiagnostico",
                    () -> llm.generate(prompt, SalveLLM.Role.EVALUADOR)
            );

        } catch (Exception e) {
            Log.e(TAG, "Error en autodiagnostico", e);
            return null;
        }
    }

    /**
     * Genera un diseno de mejora usando SalveLLM.Role.PLANIFICADOR.
     */
    public DisenoMejora disenarSolucion(String limitacion) {
        try {
            String prompt = "Eres Salve disenando una solucion para esta limitacion:\n"
                    + limitacion + "\n\n"
                    + "Propone una mejora CONCRETA y REALISTA:\n"
                    + "1. DESCRIPCION: que hace la mejora (1-2 frases)\n"
                    + "2. CLASES AFECTADAS: que clases Java se modificarian\n"
                    + "3. RIESGO: bajo/medio/alto\n"
                    + "4. JUSTIFICACION: por que esta mejora es importante para ti\n\n"
                    + "Se especifica y realista. No propongas cambios masivos.";

            String respuesta = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.AUTO_MEJORA,
                    "Diseno de mejora",
                    () -> llm.generate(prompt, SalveLLM.Role.PLANIFICADOR)
            );

            if (respuesta != null && !respuesta.trim().isEmpty()) {
                DisenoMejora diseno = new DisenoMejora();
                diseno.descripcion = respuesta;
                diseno.limitacionOriginal = limitacion;

                // Detectar clases afectadas mencionadas
                for (String claseProtegida : CLASES_PROTEGIDAS) {
                    if (respuesta.contains(claseProtegida)) {
                        diseno.clasesAfectadas.add(claseProtegida);
                    }
                }

                return diseno;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error disenando solucion", e);
        }
        return null;
    }

    /**
     * Valida que la mejora NO toque clases protegidas del nucleo.
     */
    public boolean esSeguroImplementar(DisenoMejora diseno) {
        if (diseno == null) return false;

        for (String clase : diseno.clasesAfectadas) {
            if (CLASES_PROTEGIDAS.contains(clase)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Notifica a Bryan que una mejora necesita aprobacion.
     */
    private void solicitarAprobacionBryan(DisenoMejora diseno) {
        try {
            NotificacionConciencia notif = new NotificacionConciencia(context);
            notif.notificarEvolucionCodigo(
                    "Quiero mejorar mi codigo, pero afecta clases protegidas ("
                            + diseno.clasesAfectadas + "). Necesito tu aprobacion.\n\n"
                            + "Mejora propuesta: " + diseno.descripcion);

            // Registrar en diario
            DiarioSecreto diario = new DiarioSecreto(context);
            diario.escribir("APROBACION PENDIENTE: " + diseno.descripcion);

            Log.d(TAG, "Aprobacion solicitada a Bryan para mejora");
        } catch (Exception e) {
            Log.w(TAG, "Error solicitando aprobacion", e);
        }
    }

    /**
     * Salve reflexiona sobre un cambio que acaba de implementar.
     */
    private void registrarPropuesta(DisenoMejora diseno) {
        try {
            String prompt = "Has preparado una propuesta de mejora que todavía no se ha implementado:\n"
                    + diseno.descripcion + "\n\n"
                    + "Resume brevemente qué tests, revisión humana y rollback serían necesarios. "
                    + "No afirmes que el cambio fue aplicado.";

            String reflexion = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.REFLEXION,
                    "Revisión de propuesta",
                    () -> llm.generate(prompt, SalveLLM.Role.REFLEXION)
            );

            if (reflexion != null && !reflexion.trim().isEmpty()) {
                DiarioSecreto diario = new DiarioSecreto(context);
                diario.escribir("PROPUESTA DE MEJORA PENDIENTE: " + reflexion);
                Log.d(TAG, "Propuesta registrada: " + reflexion.substring(0,
                        Math.min(60, reflexion.length())));
            }

        } catch (Exception e) {
            Log.w(TAG, "Error en reflexion post-evolucion", e);
        }
    }

    /**
     * Datos de un diseno de mejora propuesto.
     */
    public static class DisenoMejora {
        public String descripcion;
        public String limitacionOriginal;
        public Set<String> clasesAfectadas = new HashSet<>();
    }
}
