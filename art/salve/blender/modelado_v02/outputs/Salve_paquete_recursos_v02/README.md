Este paquete contiene los 20 PNG nuevos con los nombres del catálogo real de Salve, su manifiesto, anclajes por imagen y un rig frontal 2D. Es un candidato preparado para revisar; sus recursos conservan `approved: false` y no han sustituido los recursos del producto.

Los 20 PNG pasan dimensiones, RGBA, transparencia, margen y SHA-256. Las cuatro vistas utilizadas por el escenario — frontal, sentada, cuclillas y rodillas — tienen revisión técnica de los centros de iris/boca y del punto de suelo. Esa revisión no certifica identidad artística, máscaras del runtime ni comportamiento en dispositivo.

El [patch Java candidato](../Salve_integracion_v02/salve_render_integration_candidate.patch) cambia la carga frontal y las coordenadas fijas de las poses. Copiar estos PNG por sí solo deja incompleta la integración. El [informe de integración](../Salve_integracion_v02/Informe_integracion_Salve.html) explica el contrato, las diferencias del runtime y la propuesta de cambio.

El módulo Android compiló en una copia aislada. Se repitieron 21 tests con el rig frontal nuevo; los tres tests de CoreRigTest incluyen 54 combinaciones de inclinación, brazos, paso y parpadeo sin invertir triángulos. La calidad visual de la deformación y el comportamiento de habitación/ventana flotante en distintos dispositivos siguen pendientes.

Los visemas y PNG de expresiones nuevos no son consumidos automáticamente por el runtime 2D. El guiño, la mano en barbilla y otras expresiones requieren un consumidor explícito o una ampliación posterior del producto. Las vistas inclinadas y las variantes adicionales pertenecen al catálogo, pero no son estados seleccionados actualmente por AvatarBodyCue.

Los [resultados de validación](validation.json) mantienen `productionReady: false` y `visualIdentityVerified: false`. La publicación, sustitución de recursos aprobados o incorporación definitiva requiere una instrucción expresa.
