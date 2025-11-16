package salve.core;

/**
 * Todos los tipos de intención que maneja Salve.
 */
public enum IntentType {
    GUARDAR_RECUERDO,
    BUSCAR_RECUERDO_TEXT,
    BUSCAR_RECUERDO_EMO,
    AGREGAR_MISION,
    CICLO_SUENO,
    REFLEXION,
    RECORDAR_POR_TEXTO,
    RECORDAR_POR_EMOCION,
    OBTENER_REFLEXION,
    NINGUNO,          // antes era DESCONOCIDO en tu switch
    DESCONOCIDO       // si lo necesitas como alias
}