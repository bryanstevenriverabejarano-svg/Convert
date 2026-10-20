# Investigación guiada de Salve

El chat interpreta la petición antes de abrir una búsqueda web. `ResearchIntent` contiene un objetivo,
el sujeto, hasta cinco datos solicitados y hasta tres consultas breves. No almacena ni muestra
razonamiento privado del modelo. Si el plan JSON es inválido, se deriva una consulta conservadora
localmente; no se envía automáticamente todo el párrafo del usuario al buscador.

`WebResearchClient` entrega cada página al coordinador antes de pedir el siguiente enlace. El tiempo
que consume el análisis local no se descuenta del presupuesto de red. Los adaptadores antiguos y
Wikipedia pueden entregar un lote; sus páginas se analizan individualmente después de recibirlo.
Cada turno admite tres consultas, seis análisis de páginas distintas y once llamadas al modelo como
máximo, incluyendo las reparaciones. Se conserva la cancelación entre todas las etapas.

Se descartan portadas de redes sociales, pantallas de acceso y contenido irrelevante. El modelo debe
extraer citas literales de cada página: el coordinador comprueba que existan en el texto leído antes
de permitir que lleguen a la síntesis. Esa comprobación establece procedencia, **no verdad factual**.
La síntesis compara las fuentes, diferencia dominios, pide explicitar contradicciones y datos no
confirmados, y sólo admite referencias recibidas dentro del presupuesto de contexto. Hay un intento
de reparación de análisis por investigación y otro de respuesta final; no hay bucles ilimitados.

Para personas se exige distinguir coincidencias de identidad. Un nombre compartido no basta; ante
ambigüedad se pide el @usuario o perfil concreto y no se combinan sus atributos. Los objetivos se
limitan a información publicada. No se infiere personalidad ni atributos sensibles de fotografías.
Este lector obtiene extractos de texto (hasta 2.500 caracteres por página), no una copia integral del
sitio ni un análisis visual de sus imágenes. Un sitio que requiera sesión o JavaScript puede continuar
siendo inaccesible y debe indicarse; no se inventan los datos ausentes.

## Ventana y permiso

`SearchLive.prepare` abre la vista de estado antes de planificar, sin cargar la petición bruta en Google.
Las consultas del plan actualizan después el navegador. Con permiso concedido, la ventana se abre
sola si está activada y hay memoria suficiente. En la primera investigación con la conversación visible,
si falta permiso, abre el flujo de Ajustes una sola vez. `DeviceControlActivity` comprueba de nuevo
`Settings.canDrawOverlays` al volver y continúa con la consulta planificada más reciente. Rechazar el
permiso o desactivar el panel no bloquea la investigación. No se simulan clics de consentimiento ni se
conceden permisos con accesibilidad, root, ADB o cambios en AppOps.

## Validación

Las pruebas de protocolo usan modelos y transporte HTTP simulados: orden plan → búsqueda →
lectura → análisis, homónimos, portadas, citas inventadas, reparación, deduplicación, presupuestos,
cancelación y preservación de objetivos junto a URLs. Las pruebas Robolectric comprueban el permiso
real, la ausencia de petición bruta, el rechazo y la reanudación. No sustituyen la prueba de calidad con
Gemma en el dispositivo ni una búsqueda real en un proveedor que puede imponer CAPTCHA.

Prueba manual en Android: pedir una búsqueda con varios objetivos y errores tipográficos; observar
primero el objetivo, después consultas breves y páginas relevantes; cancelar durante una lectura;
negar el permiso y comprobar que el chat funciona; concederlo y volver para ver la ventana. Para
personas usar perfiles de prueba propios, contrastar dos fuentes y comprobar que un homónimo o un
campo sin evidencia se indica como no confirmado.
