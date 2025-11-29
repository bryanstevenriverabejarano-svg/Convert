package salve.core;

import java.io.*;
import java.util.*;

/**
 * WordPiece es un tokenizador sencillo para modelos de lenguaje
 * basados en WordPiece/BERT. Convierte cadenas de texto en
 * secuencias de IDs según un vocabulario. Esta implementación
 * incluye un preprocesado básico (lowercasing) y soporta la
 * convención de prefijo "##" para subtokens.
 */
public class WordPiece {
    private final Map<String, Integer> vocab;
    private final boolean doLowerCase = true;
    private final int unkId;

    /**
     * Carga el vocabulario desde un InputStream.
     * Cada línea del fichero debe contener un token en el orden correcto.
     *
     * @param vocabStream InputStream del vocabulario.
     * @throws IOException Si ocurre algún error de lectura.
     */
    public WordPiece(InputStream vocabStream) throws IOException {
        vocab = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(vocabStream))) {
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    vocab.put(line, idx++);
                }
            }
        }
        unkId = vocab.getOrDefault("[UNK]", 100);
    }

    /**
     * Tokeniza un texto en IDs de WordPiece con un máximo de longitud.
     * Agrega tokens [CLS] al inicio y [SEP] al final, y rellena
     * con [PAD] hasta alcanzar maxLen.
     *
     * @param text Texto de entrada.
     * @param maxLen Longitud máxima de tokens (incluyendo CLS/SEP y PAD).
     * @return Array de IDs de longitud maxLen.
     */
    public int[] tokenizeToIds(String text, int maxLen) {
        if (text == null) {
            int[] empty = new int[maxLen];
            Arrays.fill(empty, vocab.getOrDefault("[PAD]", 0));
            return empty;
        }
        if (doLowerCase) {
            text = text.toLowerCase(Locale.ROOT);
        }
        List<String> tokens = basicTokenize(text);
        List<Integer> ids = new ArrayList<>();
        // Token CLS
        ids.add(vocab.getOrDefault("[CLS]", 101));
        // Tokenizamos cada palabra
        for (String tok : tokens) {
            ids.addAll(wordpiece(tok));
            if (ids.size() >= maxLen - 1) {
                break;
            }
        }
        // Token SEP
        ids.add(vocab.getOrDefault("[SEP]", 102));
        // Relleno con PAD
        while (ids.size() < maxLen) {
            ids.add(vocab.getOrDefault("[PAD]", 0));
        }
        // Convertimos a array
        int[] result = new int[maxLen];
        for (int i = 0; i < maxLen; i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    // Tokenización básica: separa por espacios y puntuación.
    private List<String> basicTokenize(String t) {
        // Reemplazamos signos de puntuación por espacios + signo + espacios.
        // Luego dividimos por espacios.
        String spaced = t.replaceAll("[\\p{Punct}]", " $0 ");
        return Arrays.asList(spaced.split("\\s+"));
    }

    // Tokeniza una palabra en subtokens WordPiece.
    private List<Integer> wordpiece(String token) {
        List<Integer> out = new ArrayList<>();
        int start = 0;
        while (start < token.length()) {
            int end = token.length();
            Integer found = null;
            String sub;
            while (start < end) {
                sub = token.substring(start, end);
                if (start > 0) {
                    sub = "##" + sub;
                }
                Integer id = vocab.get(sub);
                if (id != null) {
                    found = id;
                    break;
                }
                end--;
            }
            if (found == null) {
                out.add(unkId);
                break;
            }
            out.add(found);
            start = end;
        }
        return out;
    }
}
