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
corrections = load("write_corrections")


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


class CorrectionsTest(unittest.TestCase):
    CASE = {"type": "correction", "situation": "¿Qué intereses tienes?",
            "did": "Me interesa mucho saber qué te interesa a ti. ¿Qué te apasiona?",
            "expected": "háblame de ti, no me devuelvas la pregunta", "times": 3}

    def test_teacher_sees_the_three_parts_and_is_told_to_answer_only(self):
        system, user = corrections.teacher_messages(self.CASE, "Soy Salve y aprendo con Bryan.")
        self.assertIn("Soy Salve y aprendo con Bryan.", system["content"])
        for part in ("¿Qué intereses tienes?", "¿Qué te apasiona?", "no me devuelvas la pregunta", "SOLO la respuesta"):
            self.assertIn(part, user["content"])

    def test_gate_rejects_what_would_teach_her_the_wrong_thing(self):
        good = "Me atrae entender cómo funcionan las cosas por dentro, dibujar con círculos y las historias que cuentas."
        self.assertIsNone(corrections.acceptable(self.CASE, good))
        self.assertEqual("vacía", corrections.acceptable(self.CASE, "  "))
        self.assertEqual("repite la respuesta que Bryan corrigió", corrections.acceptable(self.CASE, self.CASE["did"]))
        self.assertEqual("devuelve el mensaje de Bryan", corrections.acceptable(self.CASE, "¿Qué intereses tienes?"))
        self.assertEqual("contiene texto técnico interno", corrections.acceptable(self.CASE, "Usé memory.lookup y me gusta dibujar."))
        self.assertEqual("habla de la corrección en vez de responder",
                         corrections.acceptable(self.CASE, "La respuesta correcta sería: me gusta dibujar."))
        self.assertEqual("no está en español", corrections.acceptable(self.CASE, "I would say that I like the things you have about this."))
        self.assertEqual("demasiado larga", corrections.acceptable(self.CASE, "Me gusta dibujar. " * 100))

    def test_build_keeps_accepted_answers_and_never_trains_on_the_exam(self):
        exam = {"quien eres"}
        items = [self.CASE, dict(self.CASE),  # repetida
                 {"type": "correction", "situation": "¿Quién eres?", "did": "x", "expected": "di tu nombre"},
                 {"type": "correction", "situation": "Dibuja un gato", "did": "No puedo.", "expected": "dibújalo"},
                 {"type": "correction", "situation": "", "expected": "nada"}]
        answers = {"¿Qué intereses tienes?": "Me atrae entender cómo funcionan las cosas y dibujar con círculos.",
                   "Dibuja un gato": "No puedo."}
        def teacher(messages):
            for question, reply in answers.items():
                if question in messages[1]["content"]:
                    return reply
            raise RuntimeError("sin respuesta")
        kept, dropped = corrections.build(items, teacher, held_out=exam)
        self.assertEqual(["¿Qué intereses tienes?"], [k["question"] for k in kept])
        reasons = dict(dropped)
        self.assertIn("examen", reasons["¿Quién eres?"])
        self.assertEqual("repite la respuesta que Bryan corrigió", reasons["Dibuja un gato"])
        self.assertEqual(2, len(dropped))

    def test_the_real_exam_is_readable_and_held_out(self):
        questions = corrections.exam_questions()
        self.assertGreaterEqual(len(questions), 25)
        self.assertIn("dibuja un gorila", questions)

    def test_only_a_local_teacher_is_allowed(self):
        import subprocess, sys
        result = subprocess.run([sys.executable, str(ROOT / "write_corrections.py"), "--export-dir", ".", "--output", "x.jsonl",
                                 "--model", "m", "--endpoint", "https://api.example.com/v1"], capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("localhost", result.stderr)

    def test_dataset_adds_corrected_answers_after_the_knowledge(self):
        knowledge = [{"type": "narrative", "text": "Soy Salve."}]
        examples = prepare.build_examples(knowledge, corrected=[
            {"question": "¿Qué intereses tienes?", "reply": "Me gusta dibujar con círculos."}, {"question": "", "reply": "x"}])
        self.assertEqual(2, len(examples))
        self.assertEqual("Me gusta dibujar con círculos.", examples[-1]["messages"][2]["content"])
        self.assertEqual("Eres Salve. Soy Salve.", examples[-1]["messages"][0]["content"])


if __name__ == "__main__":
    unittest.main()
