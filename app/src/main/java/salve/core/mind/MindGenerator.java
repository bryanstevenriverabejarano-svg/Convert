package salve.core.mind;

/** Inferencia de texto. En Android se conecta solo al modelo local: estos datos son personales. */
public interface MindGenerator {
    String generate(String prompt) throws Exception;
}
