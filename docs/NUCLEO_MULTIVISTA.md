# Nuevo núcleo multivista de Salve

La apariencia predeterminada utiliza `frontal.png` completa, con sus proporciones, cabeza, cuerpo y transparencia. Kuro, Shiro, el vestido original y las prendas personalizadas siguen en el armario. Son ilustraciones seleccionables independientes; esta entrega no reajusta esas prendas al nuevo cuerpo.

## Implementación

- Las 20 imágenes aportadas se conservan sin modificar en `app/src/main/assets/avatar/core`. `manifest.json` registra origen, dimensiones y SHA-256 de cada una. La pose horizontal de rodillas conserva sus 1374×1145 píxeles; las demás son 1024×1536.
- `CoreViewCatalog` permite resolver únicamente identificadores incluidos. El taller carga una vista a la vez fuera del hilo de interfaz y descarta resultados de selecciones antiguas. Mantiene relación de aspecto y transparencia.
- Nueva malla frontal continua de 64×96 celdas, con anclajes propios para ojos, boca, cabeza, hombros, codos, manos, caderas y rodillas. Se utiliza en conversación y en el taller. Se adaptan los pesos de cabeza, brazos y piernas al nuevo cuerpo.
- Habitación → **Núcleo · vistas y movimiento** permite inspeccionar las 20 vistas, mostrar articulaciones frontales y probar inclinación de cabeza, brazos, paso y parpadeo. Los controles de movimiento sólo afectan a la frontal; no se aplican coordenadas frontales a una espalda o pose sentada.
- El armario usa versión 3. Instalaciones nuevas y antiguos valores base migran al núcleo; los diseños personalizados y selecciones de otras prendas permanecen. Una selección explícita de Kuro en la nueva versión sigue siendo Kuro después de reiniciar.

## Alcance geométrico

Esto es una malla deformable **2D frontal** más referencias multivista; no un modelo 3D reconstruido. Las imágenes no aportan geometría oculta, topología, pesos tridimensionales ni secuencias temporales. Las poses sentada, de rodillas y agachada se pueden inspeccionar, pero no hay transición corporal continua entre ellas. El taller anterior conserva las capas antiguas y está rotulado como tal.

Pendientes para un modelo 3D fiel: reconciliar diferencias de proporción y cabello entre vistas, crear malla con topología articulable, superficies y materiales, esqueleto 3D, pesos y prendas adaptadas, y validar deformaciones y rendimiento en el S24 Ultra. No se atribuye esa capacidad al catálogo de imágenes.

## Verificación

`python scripts/verify_core_artwork.py` comprueba los 20 PNG RGBA, sus dimensiones, hashes, catálogo Java, identidad de la imagen base y anclajes. Se comprobó también la transparencia mediante decodificación completa de los PNG.

Se compilaron localmente las clases de catálogo, especificación y materiales usando Java 17 y se ejecutaron comprobaciones de resolución única de vistas, identificador incluido y conservación del material original. Las pruebas JVM de migración y geometría están preparadas para la compilación Android/CI; la revisión visual en el teléfono sigue pendiente.

Los originales suman 26.960.485 bytes (aproximadamente 25,7 MiB), sin contar la imagen base ni el resto de la aplicación. Sólo se decodifica la referencia seleccionada; no se mantienen las veinte vistas simultáneamente en memoria.
