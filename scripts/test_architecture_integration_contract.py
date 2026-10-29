"""Architecture regression checks complement JVM tests: no private prompt reaches node routing."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


class IntegratedArchitectureContracts(unittest.TestCase):
    def test_private_self_model_generation_uses_existing_phone_only_route(self):
        mind = source("app/src/main/java/salve/core/MindRuntime.java")
        local = mind.split("private MindGenerator local(SalveLLM.Role role)", 1)[1].split(
            "    /**", 1)[0]
        self.assertIn("generateLocalResult(prompt, role, null)", local)
        self.assertNotIn(".generateResult(", local)
        goals = source("app/src/main/java/salve/core/GoalAutonomyRuntime.java")
        self.assertIn(
            "generateLocalResult(prompt, SalveLLM.Role.EVALUADOR, null)", goals)
        self.assertNotIn("generateResult(prompt, SalveLLM.Role.EVALUADOR)", goals)

    def test_periodic_sleep_dream_and_play_are_not_independent_when_focused(self):
        for name in ("DreamWorker", "PlayWorker"):
            worker = source(f"app/src/main/java/salve/work/{name}.java")
            self.assertIn("SleepFocusPolicy.allowStandalone(SleepFocusWorker.active(", worker)
            self.assertIn("getInputData().getBoolean(SleepFocusPolicy.EXPLICIT_REQUEST, false)", worker)
            self.assertIn("putBoolean(SleepFocusPolicy.EXPLICIT_REQUEST, true)", worker)

    def test_tool_discovery_expands_only_the_existing_authorized_catalog(self):
        prompt = source("app/src/main/java/salve/core/cognition/CognitivePrompt.java")
        turn = source("app/src/main/java/salve/core/MotorConversacional.java")
        self.assertIn("return limitTools(available, 0);", prompt)
        self.assertIn("promptBudgetChars >= 9000 ? 1100", prompt)
        self.assertIn("CognitivePrompt.prioritize(cognitiveTools(", turn)
        self.assertIn("catalogPromptBudget);", turn)


if __name__ == "__main__":
    unittest.main()
