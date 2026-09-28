-- Historical v4 entity schema, from initial repository revision b479065.
-- Entity fields/indexes are unchanged in v5; v5 adds only memory_sync_state.
CREATE TABLE recuerdos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, frase TEXT, emocion TEXT, intensidad INTEGER NOT NULL, etiquetas TEXT, binario TEXT, timestamp INTEGER NOT NULL);
CREATE TABLE reflexiones (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tipo TEXT, contenido TEXT, profundidad REAL NOT NULL, emocion TEXT, origen TEXT, certeza REAL NOT NULL, estado TEXT, timestamp INTEGER NOT NULL);
CREATE TABLE misiones (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, texto TEXT);
CREATE TABLE plugins (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, version TEXT, file_path TEXT, score REAL NOT NULL, timestamp INTEGER NOT NULL);
CREATE TABLE sync_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, payload TEXT, createdAt INTEGER NOT NULL, tries INTEGER NOT NULL);
CREATE TABLE knowledge_nodes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, etiqueta TEXT NOT NULL, tipo TEXT NOT NULL, emocionDominante TEXT NOT NULL, resumen TEXT NOT NULL, etiquetasSerializadas TEXT NOT NULL, relevanciaCreativa INTEGER NOT NULL, creadoEn INTEGER NOT NULL);
CREATE UNIQUE INDEX index_knowledge_nodes_etiqueta ON knowledge_nodes(etiqueta);
CREATE TABLE knowledge_relations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, origenId INTEGER NOT NULL, destinoId INTEGER NOT NULL, tipoRelacion TEXT NOT NULL, peso REAL NOT NULL, narrativa TEXT NOT NULL, creadoEn INTEGER NOT NULL, FOREIGN KEY(origenId) REFERENCES knowledge_nodes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(destinoId) REFERENCES knowledge_nodes(id) ON UPDATE NO ACTION ON DELETE CASCADE);
CREATE INDEX index_knowledge_relations_origenId ON knowledge_relations(origenId);
CREATE INDEX index_knowledge_relations_destinoId ON knowledge_relations(destinoId);
CREATE UNIQUE INDEX index_knowledge_relations_origenId_destinoId_tipoRelacion ON knowledge_relations(origenId, destinoId, tipoRelacion);
