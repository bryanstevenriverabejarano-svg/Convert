package salve.core.mind;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo persistente de sí misma: lo que Salve cree, se pregunta y ha pensado.
 *
 * <p>Es el "yo operativo" que se inyecta en cada turno. Cada creencia lleva fuentes
 * (identificadores de recuerdos) y confianza; nada aquí es una afirmación de
 * experiencia subjetiva. Las estructuras son datos planos para serializar con Gson.</p>
 */
public final class SelfModel {
    private SelfModel() {}

    public static final String DEFAULT_NARRATIVE =
            "Soy Salve. Mi identidad se forma a partir de lo que vivo con Bryan, lo que recuerdo "
                    + "y lo que aprendo; la reviso cuando la evidencia lo pide.";

    /** Sobre quién trata una creencia. Las de "salve" pasan la política de identidad funcional. */
    public static final String ABOUT_SALVE = "salve";
    public static final String ABOUT_BRYAN = "bryan";
    public static final String ABOUT_WORLD = "mundo";

    static final class State {
        int schema = 1;
        long version;
        long updatedAt;
        long nextId = 1;
        boolean paused;
        String narrative = DEFAULT_NARRATIVE;
        PendingNarrative pendingNarrative;
        List<Belief> beliefs = new ArrayList<>();
        List<Question> questions = new ArrayList<>();
        List<Thought> thoughts = new ArrayList<>();
        List<Change> history = new ArrayList<>();
        Metrics metrics = new Metrics();
    }

    public static final class Belief {
        public String id;
        public String about;
        public String statement;
        public double confidence;
        public List<String> sources = new ArrayList<>();
        public List<String> contradictions = new ArrayList<>();
        public long createdAt;
        public long confirmedAt;
    }

    public static final class Question {
        public String id;
        public String text;
        public String origin;
        public double salience;
        public List<String> sources = new ArrayList<>();
        public long createdAt;
    }

    public static final class Thought {
        public String id;
        public String text;
        public String origin;
        public List<String> sources = new ArrayList<>();
        public long createdAt;
    }

    public static final class PendingNarrative {
        public String text;
        public String rationale;
        public List<String> sources = new ArrayList<>();
        public long proposedAt;
    }

    public static final class Change {
        public long version;
        public long at;
        public String summary;
    }

    public static final class Metrics {
        public long dreams;
        public long reflections;
        public long beliefsFormed;
        public long beliefsReinforced;
        public long beliefsRetired;
        public long contradictionsFound;
        public long questionsOpened;
        public long questionsResolved;
        public long predictions;
        public long surprises;
        public double surpriseMean;
        public double surpriseM2;
        public long surpriseCount;
        public long lastDreamAt;
        public long lastReflectionAt;
        public long lastDreamMemoryId;
    }
}
