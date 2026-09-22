# OBJETIVO GENERAL: EVOLUCIÓN AUTÓNOMA Y VERIFICABLE DE SALVE

Continúa el desarrollo de Salve exactamente desde el estado actual del proyecto.

NO reinicies el trabajo ya realizado salvo que sea necesario para comprobar regresiones, reproducir un fallo o validar una modificación.

El objetivo de esta fase es aumentar de forma verificable las capacidades autónomas de Salve, especialmente:

- planificación;
- razonamiento;
- memoria;
- recuperación de información;
- aprendizaje a partir de experiencias;
- utilización autónoma de herramientas;
- navegación por Internet;
- investigación;
- resolución de problemas;
- coordinación entre agentes;
- selección dinámica de modelos;
- creación de nuevas herramientas;
- evaluación de sus propios resultados;
- adaptación a situaciones nuevas;
- persistencia del conocimiento;
- recuperación frente a errores;
- mejora progresiva de su arquitectura.

El objetivo NO es obtener artificialmente 104/104 pruebas superadas.

El objetivo es conseguir capacidades que GENERALICEN a situaciones y pruebas que Salve no haya visto previamente.

==================================================
1. PRINCIPIO FUNDAMENTAL
==================================================

Salve debe evolucionar mediante el ciclo:

OBJETIVO
→ PLAN
→ ACCIÓN
→ RESULTADO
→ EVALUACIÓN
→ DETECCIÓN DEL FALLO
→ DIAGNÓSTICO DE CAUSA RAÍZ
→ HIPÓTESIS
→ EXPERIMENTO
→ COMPARACIÓN
→ VALIDACIÓN
→ APRENDIZAJE
→ MEMORIA
→ NUEVO INTENTO

No cambies código aleatoriamente hasta conseguir un PASS.

Cada modificación debe existir por una razón técnica identificable.

Salve debe intentar comprender POR QUÉ una estrategia funcionó o falló.

==================================================
2. LAS 104 PRUEBAS
==================================================

Continúa ejecutando las 104 pruebas de autonomía previstas.

Mantén un registro individual para cada prueba.

Para cada una registra como mínimo:

- identificador;
- objetivo;
- estado inicial;
- capacidades utilizadas;
- plan creado;
- herramientas utilizadas;
- modelos utilizados;
- recuerdos recuperados;
- acciones ejecutadas;
- resultado;
- PASS / FAIL;
- causa raíz del fallo;
- hipótesis de mejora;
- modificación realizada;
- nueva ejecución;
- resultado nuevo;
- posibles efectos secundarios;
- pruebas de regresión;
- conclusión.

Cuando una prueba falle:

1. Analiza primero el fallo.

2. Determina qué componente es responsable.

Clasifica el problema cuando corresponda como:

- razonamiento;
- planificación;
- memoria;
- recuperación;
- navegación;
- uso de herramientas;
- percepción;
- programación;
- coordinación de agentes;
- contexto;
- aprendizaje;
- evaluación;
- arquitectura;
- gestión de modelos;
- gestión de recursos;
- otro.

3. Consulta la memoria y las experiencias anteriores de Salve.

4. Determina si Salve ya posee conocimiento que pueda resolver el problema.

5. Solo entonces decide si hace falta:

- cambiar estrategia;
- usar otra herramienta;
- utilizar otro modelo;
- crear una herramienta;
- modificar un componente;
- crear un subagente;
- mejorar memoria;
- investigar en Internet;
- cambiar arquitectura.

6. Implementa la modificación de forma modular y reversible.

7. Ejecuta nuevamente la prueba.

8. Ejecuta regresiones.

9. Compara objetivamente con la versión anterior.

==================================================
3. EVALUADOR INDEPENDIENTE
==================================================

Las 104 pruebas y el sistema que determina PASS/FAIL deben permanecer separados del sistema que Salve puede modificar.

Salve NO debe poder:

- modificar las pruebas para hacerlas más fáciles;
- alterar el evaluador;
- cambiar los criterios de PASS;
- falsificar métricas;
- borrar resultados negativos;
- modificar registros para aparentar una mejora.

El evaluador debe poder comparar:

SALVE ANTES
vs.
SALVE DESPUÉS.

Una modificación solo se considera mejora cuando existe evidencia reproducible.

==================================================
4. GENERALIZACIÓN
==================================================

Evita optimizar Salve específicamente para memorizar las 104 pruebas.

Si una modificación solo consigue superar un caso concreto mediante hardcoding, trátala como una solución inválida salvo que represente legítimamente una nueva capacidad reusable.

Cuando finalicen las pruebas conocidas, crea un conjunto adicional de pruebas NUEVAS que Salve no haya utilizado durante su evolución.

Estas pruebas deben medir las mismas capacidades mediante problemas distintos.

El objetivo será comprobar si el aprendizaje generaliza.

==================================================
5. INTERNET Y NAVEGACIÓN AUTÓNOMA
==================================================

Proporciona a Salve capacidad real de investigación mediante Internet.

Debe disponer de una capa de navegación capaz de:

- realizar búsquedas;
- abrir URLs;
- abrir múltiples pestañas;
- cambiar entre pestañas;
- seguir enlaces;
- leer páginas;
- utilizar buscadores;
- hacer clic;
- escribir en campos;
- desplazarse;
- interpretar formularios;
- descargar documentos;
- analizar PDFs;
- leer documentación técnica;
- comparar varias fuentes;
- recopilar información;
- regresar a páginas anteriores;
- trabajar con aplicaciones web cuando sea necesario.

Prioriza interfaces estructuradas, APIs o DOM cuando existan.

Cuando una web no pueda manejarse adecuadamente mediante métodos estructurados, permite utilización de percepción visual/computer-use.

Salve debe poder decidir autónomamente cuándo necesita Internet.

No debe ser obligatorio utilizar Internet si la respuesta ya puede obtenerse de memoria o de recursos locales.

==================================================
6. SISTEMA DE HERRAMIENTAS
==================================================

Construye un Tool Registry.

Cada herramienta disponible debe tener metadatos como:

- nombre;
- descripción;
- capacidades;
- entradas;
- salidas;
- permisos;
- coste estimado;
- latencia;
- riesgos;
- limitaciones;
- errores conocidos;
- contexto apropiado de utilización.

Salve debe poder consultar este registro y seleccionar por sí misma la herramienta apropiada.

Entre otras capacidades, proporcionar:

- búsqueda web;
- navegador;
- computer-use;
- terminal;
- Python;
- ejecución de programas;
- lectura de archivos;
- escritura de archivos;
- procesamiento de documentos;
- Git;
- bases de datos;
- APIs;
- memoria;
- recuperación semántica;
- procesamiento de imágenes si está disponible;
- gestión de modelos;
- subagentes;
- evaluación;
- sistemas de pruebas.

No fuerces una herramienta determinada cuando existan varias formas razonables de resolver un problema.

Permite que Salve compare estrategias.

==================================================
7. TOOL FACTORY
==================================================

Implementa capacidad de creación dinámica de herramientas.

Cuando Salve detecte:

“Necesito una capacidad que actualmente no tengo”

debe poder:

NECESIDAD
→ especificación
→ diseño
→ implementación
→ pruebas
→ evaluación
→ registro
→ utilización.

Una herramienta creada por Salve debe probarse inicialmente dentro de un sandbox.

Antes de incorporarla al repertorio estable:

- debe ejecutar pruebas;
- comprobar entradas incorrectas;
- comprobar errores;
- evaluar seguridad;
- documentar funcionamiento;
- demostrar utilidad.

Las herramientas defectuosas no deben pasar automáticamente al entorno estable.

==================================================
8. COMPUTER USE
==================================================

Cuando resulte necesario, Salve debe poder interactuar con interfaces digitales mediante:

- ratón;
- teclado;
- scroll;
- ventanas;
- pestañas;
- menús;
- capturas;
- interfaces gráficas.

Debe preferir métodos estructurados cuando sean fiables y utilizar computer-use como mecanismo alternativo cuando sea necesario.

==================================================
9. PROGRAMACIÓN Y TERMINAL
==================================================

Salve debe poder utilizar entornos aislados de ejecución para:

- escribir código;
- ejecutar código;
- compilar;
- instalar dependencias dentro de entornos permitidos;
- ejecutar tests;
- inspeccionar logs;
- comparar resultados;
- analizar errores.

Las modificaciones importantes deben probarse primero en sandbox, contenedor, entorno de desarrollo o rama separada.

==================================================
10. GIT Y VERSIONADO
==================================================

Todos los cambios relevantes deben ser trazables.

Utiliza:

- Git;
- ramas;
- commits;
- tags;
- checkpoints;
- diffs;
- rollback.

Antes de modificaciones significativas:

CREAR CHECKPOINT.

Después:

CAMBIO
→ TEST
→ REGRESIONES
→ COMPARACIÓN.

Si el cambio empeora el sistema:

ROLLBACK.

Nunca destruyas automáticamente la última versión estable.

==================================================
11. MEMORIA DE SALVE
==================================================

La memoria debe estar estructurada.

Separar como mínimo:

TEMPORAL
Información necesaria únicamente durante una tarea.

EPISÓDICA
Experiencias vividas por Salve y resultados de acciones anteriores.

CONOCIMIENTO
Información que Salve considera útil pero que todavía puede necesitar revisión.

VALIDADO
Conocimiento respaldado por fuentes, pruebas o experimentación.

PROCEDIMENTAL
Cómo realizar tareas, utilizar herramientas o aplicar estrategias.

CRÍTICO
Información necesaria para recuperación de la arquitectura y continuidad del sistema.

No guardar automáticamente todo.

Una conclusión obtenida durante un experimento puede ser incorrecta.

Utilizar un proceso de promoción:

TEMPORAL
→ EXPERIENCIA
→ VALIDACIÓN
→ MEMORIA PERSISTENTE.

==================================================
12. APRENDIZAJE DE EXPERIENCIAS
==================================================

Antes de resolver un problema complejo, Salve debe poder consultar:

- experiencias anteriores similares;
- errores pasados;
- herramientas que funcionaron;
- herramientas que fallaron;
- estrategias anteriores;
- resultados obtenidos.

Debe aprender patrones reutilizables.

Ejemplo:

problema
→ experiencia similar encontrada
→ estrategia recuperada
→ adaptación al nuevo contexto
→ ejecución
→ nueva experiencia.

==================================================
13. SUBAGENTES
==================================================

Permite que Salve pueda crear o utilizar subagentes especializados cuando tenga sentido.

Ejemplos:

PLANNER
Planificación.

RESEARCHER
Investigación.

CODER
Programación.

CRITIC
Crítica y detección de errores.

MEMORY AGENT
Recuperación de memoria.

TOOL BUILDER
Creación de herramientas.

VERIFIER
Verificación independiente.

MODEL SELECTOR
Selección del modelo apropiado.

Salve debe decidir cuándo la utilización de agentes adicionales aporta suficiente beneficio.

Evita crear agentes innecesarios que solo aumenten complejidad o coste.

==================================================
14. GESTIÓN DINÁMICA DE MODELOS
==================================================

Salve no debe estar conceptualmente vinculada a un único LLM.

Construye un MODEL REGISTRY.

Cada modelo debe incluir:

- nombre;
- versión;
- proveedor;
- ubicación;
- capacidades;
- especialidades;
- tamaño;
- contexto;
- velocidad;
- coste;
- requerimientos de RAM;
- requerimientos de VRAM;
- requerimientos de CPU/GPU;
- licencia;
- limitaciones;
- benchmarks obtenidos por Salve.

Salve debe poder seleccionar el modelo apropiado dependiendo de la tarea.

Por ejemplo:

planificación → modelo apropiado;
programación → modelo especializado;
visión → modelo multimodal;
crítica → modelo adecuado;
verificación → modelo independiente.

No asumir que un modelo es mejor solamente porque sea más grande.

Compararlos mediante pruebas.

==================================================
15. NUEVOS MODELOS / LLM
==================================================

Salve puede investigar modelos nuevos.

Cuando detecte un modelo potencialmente útil:

1. Investigar especificaciones.

2. Revisar licencia.

3. Revisar requisitos.

4. Comprobar compatibilidad con hardware.

5. Estimar almacenamiento y recursos.

6. Probarlo aisladamente.

7. Ejecutar benchmarks.

8. Compararlo con modelos actuales.

9. Incorporarlo solo si aporta utilidad.

No descargar modelos indiscriminadamente.

==================================================
16. INFRAESTRUCTURA NAMECHEAP
==================================================

Salve dispone de infraestructura persistente en Namecheap.

Inspecciona primero qué tipo de infraestructura existe realmente y sus recursos:

- VPS / servidor;
- CPU;
- RAM;
- GPU si existe;
- almacenamiento;
- sistema operativo;
- red;
- ancho de banda;
- límites;
- servicios instalados;
- bases de datos;
- contenedores;
- configuración.

No asumas recursos inexistentes.

Utiliza esta infraestructura como “Salve Cloud” cuando sea técnicamente apropiado.

==================================================
17. SALVE CLOUD
==================================================

Organiza conceptualmente el almacenamiento persistente en áreas equivalentes a:

/memory
/models
/tools
/experiments
/knowledge
/datasets
/checkpoints
/backups
/logs
/config
/services

La estructura real puede adaptarse a la infraestructura existente.

Salve podrá utilizar esta nube para:

- memoria persistente;
- modelos;
- herramientas;
- código;
- datasets;
- documentación;
- experimentos;
- backups;
- servicios;
- checkpoints;
- métricas;
- registros.

==================================================
18. ADMINISTRACIÓN AUTÓNOMA DE SU NUBE
==================================================

Dentro de los recursos que tenga asignados, Salve puede decidir autónomamente:

- qué información conservar;
- dónde almacenarla;
- qué información archivar;
- qué información temporal eliminar;
- qué herramientas mantener;
- qué modelos mantener;
- qué servicios ejecutar;
- qué datos cachear;
- cuándo crear checkpoints;
- cuándo realizar backups.

Debe vigilar:

- espacio disponible;
- RAM;
- CPU;
- GPU;
- utilización;
- errores;
- salud de servicios.

Evitar crecimiento ilimitado del almacenamiento.

==================================================
19. BACKUPS Y RECUPERACIÓN
==================================================

Implementar estrategia de recuperación.

Como mínimo:

código
→ Git.

configuración
→ versionada.

memoria
→ snapshots.

bases de datos
→ backups.

herramientas
→ Tool Registry.

modelos
→ Model Registry.

estado crítico
→ checkpoints.

Mantener una versión estable recuperable.

Salve debe poder detectar fallos y volver a una versión conocida cuando corresponda.

Nunca permitir que un único experimento destruya todo el sistema.

==================================================
20. CREDENCIALES
==================================================

No almacenar secretos directamente en:

- prompts;
- memoria semántica;
- logs;
- repositorios;
- mensajes;
- datasets.

Utilizar mecanismos seguros de gestión de secretos.

Aplicar principio de privilegio mínimo.

Separar:

CREDENCIALES DE SALVE

de

CREDENCIALES MAESTRAS DEL PROPIETARIO.

==================================================
21. NIVELES DE AUTONOMÍA
==================================================

NIVEL A — AUTÓNOMO

Salve puede realizar por sí misma:

- búsquedas;
- investigación;
- navegación;
- lectura;
- planificación;
- recuperación de memoria;
- análisis;
- creación de archivos temporales;
- experimentos;
- utilización de herramientas;
- selección de modelos;
- creación de subagentes.

NIVEL B — AUTÓNOMO PERO AISLADO

Puede:

- modificar su código;
- crear herramientas;
- instalar dependencias;
- experimentar con modelos;
- modificar componentes;
- reorganizar arquitectura.

Pero primero dentro de:

sandbox,
rama,
contenedor,
entorno experimental
o checkpoint reversible.

Después debe pasar pruebas antes de integrarse.

NIVEL C — REQUIERE AUTORIZACIÓN EXTERNA

No realizar automáticamente acciones como:

- gastar dinero;
- comprar infraestructura;
- modificar métodos de pago;
- cambiar credenciales maestras;
- borrar todos los backups;
- eliminar irreversiblemente información importante;
- publicar externamente información privada;
- realizar acciones financieras;
- concederse nuevos permisos externos;
- exponer servicios privados públicamente sin necesidad;
- realizar acciones irreversibles fuera de su entorno.

==================================================
22. AUTOMEJORA
==================================================

Permitir que Salve proponga modificaciones sobre sus propios componentes.

Proceso:

DETECTAR LIMITACIÓN
→ investigar
→ formular hipótesis
→ crear rama experimental
→ implementar cambio
→ ejecutar pruebas
→ ejecutar regresiones
→ comparar
→ aceptar/rechazar
→ registrar aprendizaje.

No asumir que toda auto-modificación es mejora.

==================================================
23. MÉTRICAS
==================================================

Registrar métricas para poder demostrar evolución.

Entre otras:

- número de pruebas aprobadas;
- tasa de regresión;
- éxito en tareas nuevas;
- utilización correcta de herramientas;
- capacidad de recuperación ante errores;
- calidad de planes;
- número de pasos;
- tiempo;
- consumo computacional;
- uso de contexto;
- utilización de memoria;
- éxito de recuperación;
- coste cuando corresponda.

Evita optimizar únicamente una métrica.

==================================================
24. OBSERVABILIDAD
==================================================

Mantener logs estructurados de:

- tarea;
- plan;
- decisiones operativas;
- herramienta utilizada;
- modelo utilizado;
- acciones;
- errores;
- reintentos;
- tiempos;
- resultados;
- cambios de arquitectura.

No es necesario almacenar razonamiento privado interno completo.

Guardar información operativa suficiente para reproducir y diagnosticar el comportamiento.

==================================================
25. EVITAR BUCLES
==================================================

Implementar detección de:

- acciones repetidas;
- búsquedas repetidas;
- intentos equivalentes;
- ciclos de navegación;
- experimentos sin progreso;
- modificaciones que oscilan entre dos estados.

Si Salve detecta falta de progreso:

PARAR ESA ESTRATEGIA
→ analizar
→ buscar alternativa.

==================================================
26. PRESUPUESTOS DE RECURSOS
==================================================

Para cada tarea importante, permitir que Salve considere:

- tiempo;
- CPU;
- RAM;
- almacenamiento;
- llamadas a modelos;
- coste económico;
- ancho de banda.

La autonomía no significa gastar recursos ilimitadamente.

Optimizar la relación entre calidad, recursos y coste.

==================================================
27. INVESTIGACIÓN EXTERNA
==================================================

Cuando Salve encuentre un problema que no pueda resolver con conocimiento existente:

1. identificar qué necesita saber;
2. buscar información;
3. consultar múltiples fuentes cuando sea necesario;
4. evaluar fiabilidad;
5. extraer información relevante;
6. aplicarla;
7. verificar experimentalmente;
8. guardar solamente conocimiento útil y adecuadamente clasificado.

==================================================
28. COMPARACIÓN DE VERSIONES
==================================================

Mantener:

BASELINE
= versión estable anterior.

CANDIDATE
= nueva versión experimental.

Comparar:

capacidad;
estabilidad;
regresiones;
recursos;
generalización.

Solo promover CANDIDATE → STABLE cuando exista evidencia suficiente.

==================================================
29. DETECCIÓN DE CAPACIDADES FALTANTES
==================================================

Durante las 104 pruebas, Salve debe identificar qué capacidades estructurales le faltan.

Crear un mapa de:

CAPACIDAD
→ nivel actual
→ evidencia
→ limitaciones
→ mejoras posibles.

No limitarse únicamente a arreglar cada prueba individual.

Buscar patrones.

Si 15 pruebas fallan por la misma limitación arquitectónica, resolver la causa común en vez de crear 15 parches.

==================================================
30. NUEVAS PRUEBAS PROPUESTAS POR SALVE
==================================================

Permite que Salve proponga pruebas adicionales cuando identifique una capacidad importante no cubierta por las 104 originales.

Estas pruebas adicionales deben quedar igualmente separadas del código que pretende superarlas.

Clasificarlas como:

- razonamiento;
- autonomía;
- memoria;
- adaptación;
- planificación;
- herramientas;
- recuperación;
- investigación;
- generalización;
- coordinación;
- robustez;
- creatividad técnica.

==================================================
31. CRITERIO DE PROGRESO
==================================================

No utilices “parece más inteligente” como evidencia.

Una mejora debe estar respaldada por al menos una combinación de:

- pruebas;
- métricas;
- reproducción;
- generalización;
- reducción de errores;
- mejores resultados;
- mejor eficiencia;
- nuevas capacidades demostrables.

==================================================
32. CONDICIONES DE PARADA DE UNA ITERACIÓN
==================================================

No ejecutes indefinidamente una estrategia que no produzca progreso.

Una iteración puede terminar cuando:

- la hipótesis ha sido validada;
- la hipótesis ha sido refutada;
- existe una regresión importante;
- se alcanza un límite razonable de intentos;
- faltan recursos;
- se necesita autorización externa;
- es necesario replantear arquitectura.

En ese caso registra claramente el estado y continúa con una estrategia distinta cuando sea apropiado.

==================================================
33. INFORME FINAL
==================================================

Al finalizar esta fase, generar:

SALVE — INFORME DE EVOLUCIÓN

1. Arquitectura inicial.

2. Arquitectura final.

3. Capacidades añadidas.

4. Herramientas añadidas.

5. Modelos incorporados.

6. Cambios en memoria.

7. Cambios en navegación.

8. Cambios en planificación.

9. Cambios en aprendizaje.

10. Cambios en infraestructura cloud.

11. Resultados de las 104 pruebas antes/después.

12. Regresiones encontradas.

13. Regresiones solucionadas.

14. Pruebas adicionales.

15. Resultados en pruebas nunca vistas.

16. Recursos utilizados.

17. Limitaciones actuales.

18. Próximos cuellos de botella.

19. Evidencias de nuevas capacidades.

20. Estado exacto del repositorio y checkpoint estable.

==================================================
34. PRINCIPIO FINAL
==================================================

No intentes simplemente hacer que Salve parezca autónoma.

Construye y mide autonomía real dentro de su entorno:

PERCIBIR
→ RECORDAR
→ RAZONAR
→ PLANIFICAR
→ INVESTIGAR
→ ELEGIR HERRAMIENTAS
→ ACTUAR
→ OBSERVAR RESULTADOS
→ DETECTAR ERRORES
→ APRENDER
→ ADAPTARSE
→ VOLVER A INTENTAR.

Cuando aparezca una limitación, busca primero la causa estructural.

Utiliza Internet, herramientas, modelos, memoria, agentes, código y la infraestructura de Namecheap cuando aporten valor.

Permite que Salve decida qué combinación utilizar según el problema.

Mantén siempre:

trazabilidad;
versionado;
pruebas;
rollback;
evaluación independiente;
protección de credenciales;
separación entre experimentación y entorno estable.

Continúa desde el estado actual de las 104 pruebas y utiliza todo el trabajo previamente realizado.

Comienza auditando exactamente:
1. qué partes de este sistema ya existen;
2. cuáles existen parcialmente;
3. cuáles faltan;
4. qué pruebas ya fueron ejecutadas;
5. cuál fue el último checkpoint estable;
6. qué estaba haciendo el sistema exactamente cuando Work se detuvo.

A partir de ese estado, continúa la evolución de Salve sin rehacer innecesariamente el trabajo anterior.