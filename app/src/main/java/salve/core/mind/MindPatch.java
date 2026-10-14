package salve.core.mind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Conjunto de cambios propuestos al {@link SelfModel}. Se aplica entero o no se aplica.
 * Las fuentes deben venir ya validadas por quien construye el parche.
 */
public final class MindPatch {
    final String origin;
    final List<NewBelief> beliefs = new ArrayList<>();
    final List<Contradiction> contradictions = new ArrayList<>();
    final List<NewQuestion> questions = new ArrayList<>();
    final List<String> resolvedQuestions = new ArrayList<>();
    final List<NewThought> thoughts = new ArrayList<>();
    NewNarrative narrative;
    boolean dream;
    long dreamMemoryId = -1;
    boolean reflection;

    public MindPatch(String origin) {
        this.origin = origin == null || origin.trim().isEmpty() ? "desconocido" : origin.trim();
    }

    public MindPatch belief(String about, String statement, double confidence, List<String> sources) {
        beliefs.add(new NewBelief(about, statement, confidence, copy(sources)));
        return this;
    }

    public MindPatch contradiction(String beliefId, List<String> sources, String note) {
        contradictions.add(new Contradiction(beliefId, copy(sources), note));
        return this;
    }

    public MindPatch question(String text, double salience, List<String> sources) {
        questions.add(new NewQuestion(text, salience, copy(sources)));
        return this;
    }

    public MindPatch resolve(String questionId) {
        if (questionId != null) resolvedQuestions.add(questionId);
        return this;
    }

    public MindPatch thought(String text, List<String> sources) {
        thoughts.add(new NewThought(text, copy(sources)));
        return this;
    }

    public MindPatch narrative(String text, String rationale, List<String> sources) {
        narrative = new NewNarrative(text, rationale, copy(sources));
        return this;
    }

    /** Marca el parche como resultado de un sueño que revisó recuerdos hasta {@code lastMemoryId}. */
    public MindPatch asDream(long lastMemoryId) {
        dream = true;
        dreamMemoryId = lastMemoryId;
        return this;
    }

    public MindPatch asReflection() {
        reflection = true;
        return this;
    }

    public boolean isEmpty() {
        return beliefs.isEmpty() && contradictions.isEmpty() && questions.isEmpty()
                && resolvedQuestions.isEmpty() && thoughts.isEmpty() && narrative == null;
    }

    private static List<String> copy(List<String> values) {
        return values == null ? Collections.emptyList() : new ArrayList<>(values);
    }

    static final class NewBelief {
        final String about, statement; final double confidence; final List<String> sources;
        NewBelief(String about, String statement, double confidence, List<String> sources) {
            this.about = about; this.statement = statement; this.confidence = confidence; this.sources = sources;
        }
    }

    static final class Contradiction {
        final String beliefId, note; final List<String> sources;
        Contradiction(String beliefId, List<String> sources, String note) {
            this.beliefId = beliefId; this.sources = sources; this.note = note;
        }
    }

    static final class NewQuestion {
        final String text; final double salience; final List<String> sources;
        NewQuestion(String text, double salience, List<String> sources) {
            this.text = text; this.salience = salience; this.sources = sources;
        }
    }

    static final class NewThought {
        final String text; final List<String> sources;
        NewThought(String text, List<String> sources) { this.text = text; this.sources = sources; }
    }

    static final class NewNarrative {
        final String text, rationale; final List<String> sources;
        NewNarrative(String text, String rationale, List<String> sources) {
            this.text = text; this.rationale = rationale; this.sources = sources;
        }
    }
}
