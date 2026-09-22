"""Execute the actual Room DAO query strings on SQLite; no Android runtime needed."""
import json
from pathlib import Path
import re
import sqlite3
import unittest


DAO_ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/salve/data/db"


def query(dao, method):
    source = (DAO_ROOT / (dao + ".java")).read_text(encoding="utf-8")
    expression = r'@Query\("((?:\\.|[^"\\])*)"\)\s+[\w<>]+\s+' + re.escape(method) + r'\('
    match = re.search(expression, source)
    if not match:
        raise AssertionError("Missing actual DAO query: " + dao + "." + method)
    return json.loads('"' + match.group(1) + '"')


class MemoryQuerySqlTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.executescript("""
            CREATE TABLE recuerdos(id INTEGER PRIMARY KEY, frase TEXT, etiquetas TEXT, timestamp INTEGER);
            CREATE TABLE knowledge_nodes(id INTEGER PRIMARY KEY, etiqueta TEXT, resumen TEXT,
                etiquetasSerializadas TEXT, relevanciaCreativa INTEGER, creadoEn INTEGER);
            CREATE TABLE knowledge_relations(id INTEGER PRIMARY KEY, origenId INTEGER, destinoId INTEGER,
                peso REAL, creadoEn INTEGER);
        """)

    def tearDown(self):
        self.db.close()

    def records(self, *rows):
        self.db.executemany("INSERT INTO recuerdos VALUES (?, ?, ?, ?)", rows)

    def test_chronology_and_recent_search_use_timestamp_then_id(self):
        self.records((9, "proyecto último", "[]", 3000), (2, "proyecto primero", "[]", 1000),
                     (1, "proyecto intermedio", "[]", 2000), (3, "proyecto empate", "[]", 1000))
        self.assertEqual(2, self.db.execute(query("RecuerdoDao", "primerRecuerdo")).fetchone()[0])
        self.assertEqual(9, self.db.execute(query("RecuerdoDao", "ultimoRecuerdo")).fetchone()[0])
        rows = self.db.execute(query("RecuerdoDao", "buscarRecientes"),
                               {"palabraClave": "proyecto", "limite": 3}).fetchall()
        self.assertEqual([9, 1, 3], [row[0] for row in rows])

    def test_profile_lookup_has_exact_literal_tag_and_latest_version(self):
        self.records((1, "antes", '["profile:preference_cafe"]', 1000),
                     (2, "vigente", '["hecho_usuario","profile:preference_cafe"]', 2000),
                     (3, "prefijo", '["profile:preference_cafeteria"]', 4000),
                     (4, "comodín", '["profile:preferenceXcafe"]', 5000))
        row = self.db.execute(query("RecuerdoDao", "ultimoPorEtiqueta"),
                              {"etiqueta": "profile:preference_cafe"}).fetchone()
        self.assertEqual((2, "vigente"), row[:2])

    def test_delete_does_not_remove_prefix_or_underscore_neighbours(self):
        self.records((1, "café", '["profile:preference_cafe"]', 1000),
                     (2, "cafetería", '["profile:preference_cafeteria"]', 2000),
                     (3, "distinto", '["profile:preferenceXcafe"]', 3000))
        self.db.execute(query("RecuerdoDao", "eliminarPorEtiqueta"),
                        {"etiqueta": "profile:preference_cafe"})
        self.assertEqual([2, 3], [row[0] for row in self.db.execute("SELECT id FROM recuerdos ORDER BY id")])

    def test_graph_search_reads_label_summary_tags_and_applies_limit(self):
        self.db.executemany("INSERT INTO knowledge_nodes VALUES (?, ?, ?, ?, ?, ?)", [
            (1, "telescopio", "", "[]", 2, 1000),
            (2, "equipo", "telescopio óptico", "[]", 5, 1000),
            (3, "compra", "", '["telescopio"]', 3, 2000),
        ])
        rows = self.db.execute(query("KnowledgeNodeDao", "buscarPorTexto"),
                               {"texto": "telescopio", "limit": 2}).fetchall()
        self.assertEqual([2, 3], [row[0] for row in rows])
        self.assertEqual(1, self.db.execute(query("KnowledgeNodeDao", "findById"), {"id": 1}).fetchone()[0])

    def test_graph_edges_include_incoming_links_and_limit_by_weight(self):
        self.db.executemany("INSERT INTO knowledge_relations VALUES (?, ?, ?, ?, ?)", [
            (1, 2, 1, 0.9, 1000), (2, 1, 3, 0.8, 2000), (3, 4, 5, 1.0, 3000),
        ])
        rows = self.db.execute(query("KnowledgeRelationDao", "relacionesDeNodo"),
                               {"nodeId": 1, "limit": 1}).fetchall()
        self.assertEqual([1], [row[0] for row in rows])


if __name__ == "__main__":
    unittest.main()
