"""Pruebas pequeñas del inventario estático; no sustituyen la ejecución Android."""
import tempfile
from pathlib import Path
import unittest

from audit_cognitive_dependencies import inventory, target_for, subsystem


class DependencyInventoryTest(unittest.TestCase):
    def test_module_boundaries(self):
        self.assertEqual("core/conversation", subsystem("salve.core.conversation"))
        self.assertEqual("presentation", subsystem("salve.presentation.ui"))
        self.assertEqual("mlc4j", subsystem("ai.mlc.mlcllm"))

    def test_imports_and_fully_qualified_references_find_module_cycle(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = root / "app/src/main/java/salve/core"
            (folder / "conversation").mkdir(parents=True)
            (folder / "memory").mkdir(parents=True)
            (folder / "conversation/Turn.java").write_text(
                "package salve.core.conversation;\n"
                "import salve.core.memory.Store;\n"
                "class Turn { Store store; }\n", encoding="utf-8")
            (folder / "memory/Store.java").write_text(
                "package salve.core.memory;\n"
                "class Store { salve.core.conversation.Turn turn; }\n", encoding="utf-8")
            report = inventory(root)
            self.assertEqual(2, report["classes_indexed"])
            self.assertEqual(2, len(report["cross_module_edges"]))
            self.assertIn(["core/conversation", "core/memory"], report["module_cycles"])
            self.assertEqual(["Static Java/Kotlin imports and fully qualified references, main sources only"],
                             [report["scope"]])

    def test_static_import_resolves_the_owning_class(self):
        known = {"salve.core.memory.Store": object()}
        self.assertEqual("salve.core.memory.Store",
                         target_for("salve.core.memory.Store.read", known))
        self.assertIsNone(target_for("salve.core.missing.Nothing", known))


if __name__ == "__main__":
    unittest.main()
