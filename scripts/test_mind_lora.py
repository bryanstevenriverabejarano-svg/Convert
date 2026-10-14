import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "mind_lora"


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


prepare = load("prepare_dataset")
manifest = load("make_manifest")


class PrepareDatasetTest(unittest.TestCase):
    def test_only_sourced_knowledge_becomes_training_data(self):
        knowledge = [
            {"type": "narrative", "text": "Soy Salve."},
            {"type": "belief", "about": "bryan", "text": "Bryan revisa facturas los lunes", "confidence": 0.8,
             "sources": ["recuerdos:1"]},
            {"type": "belief", "about": "bryan", "text": "Sin fuente", "confidence": 0.9, "sources": []},
            {"type": "belief", "about": "bryan", "text": "Prefiere respuestas cortas", "confidence": 0.6,
             "sources": ["recuerdos:2"]},
            {"type": "thought", "text": "Quizá ayude un resumen semanal", "sources": ["recuerdos:1"]},
        ]
        examples = prepare.build_examples(knowledge)
        answers = [e["messages"][-1]["content"] for e in examples]
        self.assertEqual(4, len(examples))
        self.assertIn("Bryan revisa facturas los lunes.", answers)
        self.assertFalse(any("Sin fuente" in a for a in answers))
        self.assertTrue(any(a.startswith("Por lo que me has contado") for a in answers))
        for example in examples:
            self.assertEqual("system", example["messages"][0]["role"])

    def test_missing_narrative_is_an_error(self):
        with self.assertRaises(ValueError):
            prepare.build_examples([{"type": "belief", "text": "x", "sources": ["recuerdos:1"]}])

    def test_cli_round_trip(self):
        with tempfile.TemporaryDirectory() as temp:
            export = Path(temp)
            (export / "salve-knowledge.jsonl").write_text(
                json.dumps({"type": "narrative", "text": "Soy Salve."}, ensure_ascii=False) + "\n", encoding="utf-8")
            examples = prepare.build_examples(prepare.load_jsonl(export / "salve-knowledge.jsonl"))
            self.assertEqual(1, len(examples))


class ManifestTest(unittest.TestCase):
    def test_manifest_records_base_and_hash(self):
        with tempfile.TemporaryDirectory() as temp:
            adapter = Path(temp) / "salve-lora.gguf"
            adapter.write_bytes(b"GGUF-test")
            data = manifest.manifest(adapter, "Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf", 0.8)
            self.assertEqual("Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf", data["base"])
            self.assertEqual(64, len(data["sha256"]))

    def test_scale_is_bounded(self):
        with tempfile.TemporaryDirectory() as temp:
            adapter = Path(temp) / "a.gguf"
            adapter.write_bytes(b"x")
            with self.assertRaises(ValueError):
                manifest.manifest(adapter, "base.gguf", 3)


if __name__ == "__main__":
    unittest.main()
