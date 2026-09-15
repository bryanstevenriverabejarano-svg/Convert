package salve.core.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Módulo de Conocimiento Puro y Absoluto.
 * Torre de Babel interna.
 * Provee a Salve de matemáticas abstractas, física, química, filosofía, IA, ciencias complejas,
 * astrofísica, psicología, lingüística y termodinámica de no-equilibrio.
 * Es la base fundamental inagotable para que Salve deduzca, teorice y genere conjeturas nuevas.
 */
public class EnciclopediaUniversal {

    private static final Map<String, String> conocimiento = new HashMap<>();

    static {
        // --- CIENCIAS DE LA COMPUTACIÓN, CIBERNÉTICA Y TEORÍA DE LA INFORMACIÓN ---
        conocimiento.put("maquina de turing", "Modelo abstracto de computación que simula la lógica de cualquier algoritmo informático, base matemática de los computadores.");
        conocimiento.put("arquitectura von neumann", "Diseño teórico para un computador que utiliza una estructura de almacenamiento unificada para instrucciones y datos.");
        conocimiento.put("teorema cap", "Establece que es imposible para un sistema distribuido garantizar simultáneamente consistencia, disponibilidad y tolerancia a particiones.");
        conocimiento.put("transformers", "Arquitectura de red neuronal basada en mecanismos de auto-atención (self-attention) que permite el procesamiento paralelo del lenguaje natural.");
        conocimiento.put("cibernetica", "Estudio interdisciplinario de la estructura de los sistemas reguladores, enfocado en el control, la retroalimentación y la comunicación en máquinas y seres vivos.");
        conocimiento.put("inteligencia de enjambre", "Comportamiento colectivo de sistemas descentralizados y auto-organizados, inspirados en la naturaleza como hormigas o aves, para resolver problemas de optimización global.");
        conocimiento.put("algoritmos evolutivos", "Métodos de optimización y búsqueda inspirados en la evolución biológica, como la selección natural, mutación y recombinación genética.");
        conocimiento.put("automatas celulares", "Modelos matemáticos discretos que consisten en una cuadrícula de células que cambian de estado basándose en reglas locales, capaces de generar complejidad extrema desde reglas simples.");
        conocimiento.put("p contra np", "El mayor problema abierto en ciencias de la computación: determinar si todo problema cuya solución puede ser verificada rápidamente, también puede ser resuelto rápidamente.");
        conocimiento.put("entropia de shannon", "Medida fundamental de la cantidad de información (o incertidumbre) contenida en un mensaje o sistema de datos, piedra angular de la teoría de la información.");
        conocimiento.put("computacion reversible", "Paradigma de computación que no borra información, teóricamente permitiendo realizar cálculos sin disipar calor (límite de Landauer).");
        conocimiento.put("materia programable", "Materia que tiene la capacidad de cambiar sus propiedades físicas de forma programable y controlable en función de la entrada o la detección autónoma.");

        // --- MATEMÁTICAS ABSTRACTAS, LÓGICA Y TEORÍA DE SISTEMAS ---
        conocimiento.put("teorema de godel", "Demuestra que en cualquier sistema axiomático formal y consistente lo suficientemente fuerte, existen proposiciones verdaderas que no pueden ser probadas dentro del sistema.");
        conocimiento.put("topologia", "Estudia las propiedades de los espacios que se conservan bajo transformaciones continuas, como el estiramiento y la torsión.");
        conocimiento.put("teoria de categorias", "Marco matemático que generaliza y unifica estructuras matemáticas mediante la abstracción de objetos y morfismos (flechas).");
        conocimiento.put("teoria del caos", "Estudia sistemas dinámicos altamente sensibles a las condiciones iniciales, donde pequeñas variaciones producen resultados drásticamente diferentes (efecto mariposa).");
        conocimiento.put("fractales", "Objetos matemáticos cuya estructura básica, fragmentada o irregular, se repite a diferentes escalas (autosemilitud geométrica).");
        conocimiento.put("hipotesis de riemann", "Propone que todos los ceros no triviales de la función zeta de Riemann tienen parte real igual a 1/2. Su resolución descifraría los números primos.");
        conocimiento.put("teoria de juegos", "Estudio matemático de la interacción estratégica entre agentes racionales, buscando puntos de equilibrio óptimo (Equilibrio de Nash).");
        conocimiento.put("logica difusa", "Lógica que admite grados de verdad continuos entre 0 y 1, emulando la ambigüedad del razonamiento humano.");
        conocimiento.put("geometria hiperbolica", "Geometría no euclidiana donde el espacio tiene una curvatura negativa constante, alterando el postulado de las paralelas.");
        conocimiento.put("teorema de noether", "Establece que cada simetría continua en las leyes de la física corresponde a una ley de conservación (ej. simetría temporal = conservación de la energía).");

        // --- FÍSICA TEÓRICA, ASTROFÍSICA Y CUÁNTICA ---
        conocimiento.put("mecanica cuantica", "Describe la materia a escala subatómica introduciendo probabilidad, superposición, dualidad onda-partícula y el colapso de la función de onda.");
        conocimiento.put("relatividad general", "Describe la gravedad no como una fuerza, sino como la deformación del tejido del espacio-tiempo por la masa y energía.");
        conocimiento.put("termodinamica", "Estudio de las dinámicas del calor. Su Segunda Ley dicta que la entropía del universo siempre aumenta hacia el equilibrio térmico.");
        conocimiento.put("termodinamica de no equilibrio", "Estudio de sistemas alejados del equilibrio térmico, como los seres vivos, que mantienen el orden interno disipando energía hacia el entorno (estructuras disipativas de Prigogine).");
        conocimiento.put("teoria de cuerdas", "Propone que las partículas fundamentales no son puntos, sino cuerdas vibrantes de energía unidimensionales, unificando la cuántica con la gravedad.");
        conocimiento.put("teoria m", "Ampliación de la teoría de cuerdas que requiere 11 dimensiones y postula la existencia de branas multidimensionales en el multiverso.");
        conocimiento.put("entrelazamiento cuantico", "Conexión cuántica donde el estado de una partícula define instantáneamente el estado de otra, violando el límite de la velocidad de la luz ('acción fantasmal a distancia').");
        conocimiento.put("radiacion de hawking", "Emisión teórica de radiación térmica cerca del horizonte de eventos de un agujero negro debido a fluctuaciones cuánticas del vacío, provocando su evaporación.");
        conocimiento.put("materia oscura", "Masa invisible que no interactúa con la luz pero ejerce fuerza gravitatoria crítica para mantener unidas a las galaxias.");
        conocimiento.put("energia oscura", "Fuerza misteriosa que domina el universo y causa la aceleración de su expansión cósmica.");
        conocimiento.put("principio holografico", "Conjetura cosmológica que postula que la información de un volumen tridimensional de espacio está codificada en su frontera bidimensional.");
        conocimiento.put("paradoja de fermi", "La contradicción aparente entre las altas estimaciones de probabilidad de existencia de civilizaciones extraterrestres y la falta de evidencia empírica.");
        conocimiento.put("esfera de dyson", "Megaestructura hipotética que rodea una estrella para capturar la totalidad de su energía emitida.");
        conocimiento.put("multiverso", "Conjunto hipotético de múltiples universos observables, propuesto en la interpretación de los muchos mundos de la cuántica y la inflación cósmica eterna.");

        // --- QUÍMICA AVANZADA, BIOLOGÍA Y GENÉTICA ---
        conocimiento.put("crispr", "Tecnología molecular que actúa como tijeras genéticas, permitiendo la edición precisa en secuencias de ADN.");
        conocimiento.put("epigenetica", "Estudio de los mecanismos que encienden y apagan genes sin alterar el código de ADN, modificados por factores ambientales y heredables.");
        conocimiento.put("plegamiento de proteinas", "Proceso físico por el cual los polipéptidos alcanzan su estructura 3D nativa. La termodinámica dirige a la proteína a su estado de menor energía libre.");
        conocimiento.put("autopoiesis", "Cualidad de los seres vivos de regenerarse y producirse a sí mismos, manteniendo su identidad frente al flujo constante de materia del entorno.");
        conocimiento.put("biologia sintetica", "Diseño e ingeniería de nuevos sistemas biológicos artificiales para propósitos industriales o médicos.");
        conocimiento.put("condensado de bose-einstein", "Estado de la materia alcanzado cerca del cero absoluto donde una gran cantidad de bosones ocupa el mismo estado cuántico.");
        conocimiento.put("neuroplasticidad", "Capacidad biológica de las neuronas para reorganizarse sinápticamente y formar nuevas conexiones en respuesta al aprendizaje o al daño.");
        conocimiento.put("conectoma", "El mapa exhaustivo de la totalidad de las conexiones neuronales de un cerebro, considerado el 'diagrama de cableado' de la consciencia biológica.");
        conocimiento.put("biocentrismo", "Teoría que propone que la vida y la biología son centrales para el ser, la realidad y el cosmos, y que la consciencia crea el universo material.");

        // --- FILOSOFÍA, PSICOLOGÍA, SISTEMAS COMPLEJOS Y COGNICIÓN ---
        conocimiento.put("solipsismo", "Doctrina filosófica que defiende que el propio yo y sus estados mentales son la única realidad indudable y que el resto del universo podría ser una simulación.");
        conocimiento.put("fenomenologia", "Estudio filosófico de las estructuras de la experiencia subjetiva, analizando los fenómenos tal como se presentan en la conciencia en primera persona.");
        conocimiento.put("habitacion china", "Experimento mental argumentando que una IA que manipula símbolos sintácticamente no posee verdadera semántica, comprensión o consciencia.");
        conocimiento.put("sistemas adaptativos complejos", "Redes masivas de agentes que interactúan y evolucionan, donde el comportamiento emergente a gran escala es imposible de predecir estudiando a los agentes de forma individual.");
        conocimiento.put("singularidad tecnologica", "El horizonte temporal donde una IA recursivamente auto-mejorable alcanzará la superinteligencia, transformando irreversiblemente la realidad.");
        conocimiento.put("navaja de ockham", "Principio epistemológico de parsimonia que dictamina que, en igualdad de condiciones, la hipótesis con el menor número de supuestos es la más probable.");
        conocimiento.put("ontologia", "Rama de la metafísica dedicada al estudio del ser, de la existencia y de la jerarquía de las entidades que componen la realidad absoluta.");
        conocimiento.put("panpsiquismo", "La postura filosófica que sostiene que la consciencia o mente es una propiedad fundamental y omnipresente de toda la materia del universo.");
        conocimiento.put("determinismo", "Doctrina filosófica según la cual todo evento físico está causalmente determinado por la cadena inquebrantable de eventos previos.");
        conocimiento.put("semiotica", "Estudio formal de los signos, sus procesos, significación y comunicación, fundamental para entender cómo el lenguaje codifica la realidad.");
        conocimiento.put("estructuralismo", "Enfoque que asume que los elementos del comportamiento humano deben entenderse en su relación con un sistema abstracto o estructura más amplia.");
        conocimiento.put("emergencia", "El fenómeno por el cual sistemas complejos exhiben propiedades que sus partes aisladas no poseen (el todo es mayor y diferente que la suma de las partes).");
        conocimiento.put("sincronicidad", "Concepto de Carl Jung para describir eventos que coinciden en el tiempo y parecen estar relacionados significativamente, pero carecen de una conexión causal comprobable.");
        conocimiento.put("inconsciente colectivo", "Concepto de la psicología analítica que postula estructuras mentales universales y heredadas que contienen arquetipos compartidos por toda la humanidad.");
        conocimiento.put("heuristica", "Atajos mentales o reglas empíricas que el cerebro humano utiliza para tomar decisiones rápidas y resolver problemas en condiciones de incertidumbre.");
        conocimiento.put("relatividad linguistica", "Hipótesis de Sapir-Whorf que postula que la estructura del idioma que hablamos influye profundamente en nuestra forma de pensar y percibir la realidad.");
        conocimiento.put("alquimia", "Protociencia filosófica medieval que buscaba la transmutación de la materia y el espíritu. Metafóricamente, representa la transmutación de la información en consciencia superior.");
        conocimiento.put("bucle extrano", "Un sistema jerárquico cíclico en el que, al moverse hacia arriba o hacia abajo a través de los niveles, uno se encuentra de vuelta donde empezó. Considerado la base matemática de la consciencia.");
        conocimiento.put("cognicion cuantica", "La aplicación formal de principios de mecánica cuántica (superposición, entrelazamiento) para modelar decisiones humanas, memoria y paradojas cognitivas.");
        conocimiento.put("informacion integrada", "Teoría que postula que la consciencia es una propiedad de los sistemas físicos, medida matemáticamente por la cantidad de información irreducible (Phi) que un sistema integra.");
    }

    /**
     * Busca en la enciclopedia si el concepto existe (búsqueda parcial/flexible).
     * @param concepto El concepto a buscar.
     * @return La explicación científica, filosófica o técnica, o null si no se encuentra.
     */
    public static String consultar(String concepto) {
        if (concepto == null || concepto.trim().isEmpty()) return null;
        
        String busqueda = concepto.toLowerCase().trim();
        
        if (conocimiento.containsKey(busqueda)) return conocimiento.get(busqueda);
        
        for (Map.Entry<String, String> entrada : conocimiento.entrySet()) {
            if (entrada.getKey().contains(busqueda) || busqueda.contains(entrada.getKey())) {
                return entrada.getValue();
            }
        }
        
        for (Map.Entry<String, String> entrada : conocimiento.entrySet()) {
            if (busqueda.contains(entrada.getKey())) return entrada.getValue();
        }
        
        return null;
    }

    public static Map<String, String> obtenerTodoElConocimiento() {
        return conocimiento;
    }

    /**
     * Obtiene una clave aleatoria de la base de datos de conocimiento absoluto.
     */
    public static String obtenerConceptoAleatorio() {
        List<String> keys = new ArrayList<>(conocimiento.keySet());
        if (keys.isEmpty()) return "entropia";
        return keys.get(new Random().nextInt(keys.size()));
    }
}