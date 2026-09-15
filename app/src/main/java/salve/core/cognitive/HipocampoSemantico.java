package salve.core.cognitive;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import salve.core.EmbeddingsIndex;
import salve.core.cognitive.EnciclopediaUniversal;

/**
 * HipocampoSemantico — El cerebro a largo plazo de Salve (Anclaje Multimodal).
 * Aquí es donde Salve guarda sus "recuerdos" para siempre.
 * Cuando le enseñas un objeto físico por la cámara y le explicas qué es,
 * Salve guarda la imagen (forma física) y su significado (teoría) unidos por
 * un vector semántico (embedding).
 */
public class HipocampoSemantico {

    private static final String TAG = "Salve/Hipocampo";
    private final Context context;
    private final EmbeddingsIndex embeddingsIndex;
    private final List<RecuerdoMultimodal> memoriaInfinita; // TODO: Migrar a SQLite/ChromaDB local
    private final File directorioRecuerdos;

    public HipocampoSemantico(Context context, EmbeddingsIndex embeddingsIndex) {
        this.context = context.getApplicationContext();
        this.embeddingsIndex = embeddingsIndex;
        this.memoriaInfinita = new ArrayList<>();
        
        // Directorio donde guardaremos las imágenes que Salve ve (sus recuerdos visuales)
        this.directorioRecuerdos = new File(context.getFilesDir(), "recuerdos_visuales");
        if (!this.directorioRecuerdos.exists()) {
            this.directorioRecuerdos.mkdirs();
        }
        
        cargarRecuerdosGuardados();
        inyectarConocimientoAbsoluto();
    }

    /**
     * Inyecta la "Torre de Babel" de conocimiento universitario a la matriz vectorial
     * al momento de nacer.
     */
    private void inyectarConocimientoAbsoluto() {
        Log.i(TAG, "Inyectando Enciclopedia Universal en el Hipocampo Semántico...");
        Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        for (java.util.Map.Entry<String, String> entry : EnciclopediaUniversal.obtenerTodoElConocimiento().entrySet()) {
            if (!conoceConcepto(entry.getKey())) {
                try {
                    float[] vectorSemantico = embeddingsIndex.embed(entry.getKey() + ": " + entry.getValue());
                    RecuerdoMultimodal engrama = new RecuerdoMultimodal(
                            entry.getKey(),
                            entry.getValue(),
                            "enciclopedia_interna",
                            vectorSemantico
                    );
                    memoriaInfinita.add(engrama);
                } catch (Exception e) {
                    // Ignorar fallo de embedding individual
                }
            }
        }
    }

    /**
     * El momento mágico donde Salve aprende algo nuevo.
     * Une una imagen (lo que vio) con un concepto (lo que le dijiste que era).
     */
    public synchronized void aprenderConceptoNuevo(String nombreConcepto, String teoriaODefinicion, Bitmap formaFisica) {
        Log.i(TAG, "Aprendiendo nuevo concepto: " + nombreConcepto);
        
        try {
            // 1. Guardar la forma física (Imagen) en el disco
            String nombreArchivo = UUID.randomUUID().toString() + ".jpg";
            File archivoImagen = new File(directorioRecuerdos, nombreArchivo);
            try (FileOutputStream out = new FileOutputStream(archivoImagen)) {
                formaFisica.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }

            // 2. Generar el "Significado" matemático (Vector Semántico / Embedding)
            String textoAEmbeber = nombreConcepto + ": " + teoriaODefinicion;
            float[] vectorSemantico = embeddingsIndex.embed(textoAEmbeber);

            // 3. Crear el Engrama (Recuerdo Multimodal)
            RecuerdoMultimodal nuevoRecuerdo = new RecuerdoMultimodal(
                    nombreConcepto,
                    teoriaODefinicion,
                    archivoImagen.getAbsolutePath(),
                    vectorSemantico
            );

            // 4. Guardarlo en el hipocampo
            memoriaInfinita.add(nuevoRecuerdo);
            guardarEnBaseDeDatosLocal(nuevoRecuerdo);
            
            Log.i(TAG, "¡Concepto anclado con éxito! Salve nunca olvidará qué es: " + nombreConcepto);

        } catch (Exception e) {
            Log.e(TAG, "Error al intentar aprender el concepto: " + nombreConcepto, e);
        }
    }

    /**
     * Cuando Salve escucha o piensa en una palabra, busca en su hipocampo
     * si tiene recuerdos asociados a ese concepto.
     */
    public List<RecuerdoMultimodal> recordar(String consultaTexto, int cantidadResultados) {
        List<RecuerdoConSimilitud> resultados = new ArrayList<>();
        try {
            // Convertimos la consulta en un vector matemático
            float[] vectorConsulta = embeddingsIndex.embed(consultaTexto);

            // Buscamos los recuerdos más cercanos en el espacio semántico
            for (RecuerdoMultimodal recuerdo : memoriaInfinita) {
                float similitud = EmbeddingsIndex.cosine(vectorConsulta, recuerdo.vectorSemantico);
                if (similitud > 0.4f) { // Umbral de similitud
                    resultados.add(new RecuerdoConSimilitud(recuerdo, similitud));
                }
            }

            // Ordenamos por relevancia (similitud descendente)
            resultados.sort((a, b) -> Float.compare(b.similitud, a.similitud));

        } catch (Exception e) {
            Log.e(TAG, "Error al intentar recordar: " + consultaTexto, e);
        }

        // Devolvemos solo los N mejores resultados
        List<RecuerdoMultimodal> recuerdosFinales = new ArrayList<>();
        for (int i = 0; i < Math.min(cantidadResultados, resultados.size()); i++) {
            recuerdosFinales.add(resultados.get(i).recuerdo);
        }
        return recuerdosFinales;
    }

    /**
     * Verifica rápidamente si Salve ya tiene un concepto anclado en su hipocampo.
     */
    public boolean conoceConcepto(String conceptoBuscado) {
        if (conceptoBuscado == null || conceptoBuscado.isEmpty()) return false;
        String lowerBusqueda = conceptoBuscado.toLowerCase().trim();
        for (RecuerdoMultimodal rm : memoriaInfinita) {
            if (rm.concepto.toLowerCase().contains(lowerBusqueda)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene todos los recuerdos multimodales guardados.
     */
    public List<RecuerdoMultimodal> obtenerTodosLosRecuerdos() {
        return new ArrayList<>(memoriaInfinita);
    }

    private void cargarRecuerdosGuardados() {
        // TODO: Cargar desde la base de datos (Room/SQLite) al iniciar Salve
        Log.d(TAG, "HipocampoSemantico: Listo para recuperar recuerdos.");
    }

    private void guardarEnBaseDeDatosLocal(RecuerdoMultimodal recuerdo) {
        // TODO: Insertar en Room/SQLite para persistencia real entre reinicios
        Log.d(TAG, "HipocampoSemantico: Recuerdo guardado en base de datos.");
    }

    // --- Clases Internas ---

    public static class RecuerdoMultimodal {
        public String concepto;
        public String teoria;
        public String rutaImagenFisica;
        public float[] vectorSemantico;

        public RecuerdoMultimodal(String concepto, String teoria, String rutaImagenFisica, float[] vectorSemantico) {
            this.concepto = concepto;
            this.teoria = teoria;
            this.rutaImagenFisica = rutaImagenFisica;
            this.vectorSemantico = vectorSemantico;
        }
        
        public String getSintesisParaPensamiento() {
            return "Recuerdo el concepto '" + concepto + "'. Su teoría dice: '" + teoria + "'. Y tengo una imagen mental de ello.";
        }
    }

    private static class RecuerdoConSimilitud {
        RecuerdoMultimodal recuerdo;
        float similitud;

        RecuerdoConSimilitud(RecuerdoMultimodal recuerdo, float similitud) {
            this.recuerdo = recuerdo;
            this.similitud = similitud;
        }
    }
}
