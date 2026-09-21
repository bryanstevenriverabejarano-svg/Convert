import json
import unittest

from summarize_voice_metrics import METRICS, parse_attempt, summarize


def attempt(value=10, outcome="COMPLETED", **overrides):
    fields = {"token": "1", "result": outcome, "stop_reason": "none", "asr_error": "none"}
    fields.update({key: str(value) for key in METRICS})
    fields.update(overrides)
    return "I/Salve/VoiceMetrics: voice_turn " + " ".join(f"{key}={value}" for key, value in fields.items())


class VoiceMetricsSummaryTest(unittest.TestCase):
    def test_percentiles_count_only_completed_turns(self):
        lines = [attempt(value) for value in range(1, 21)]
        lines += [attempt(9999, "INTERRUPTED"), attempt(9999, "RECOGNITION_ERROR")]
        summary = summarize(lines)
        self.assertEqual(22, summary["attempts"])
        self.assertEqual(20, summary["outcomes"]["COMPLETED"])
        self.assertEqual({"samples": 20, "p50_ms": 10, "p95_ms": 19},
                         summary["metrics"]["response_wait_ms"])

    def test_missing_timings_are_not_zero_samples(self):
        summary = summarize([attempt(reply_to_tts_start_ms="missing"), attempt(30)])
        self.assertEqual({"samples": 1, "p50_ms": 30, "p95_ms": 30},
                         summary["metrics"]["reply_to_tts_start_ms"])

    def test_empty_or_unrelated_log_produces_no_invented_measurement(self):
        summary = summarize(["Unrelated app output", "voice_turn malformed"])
        self.assertEqual(0, summary["attempts"])
        self.assertEqual({"samples": 0, "p50_ms": None, "p95_ms": None},
                         summary["metrics"]["tts_duration_ms"])

    def test_unknown_fields_duplicates_and_invalid_numbers_are_ignored(self):
        invalid = [attempt() + " text=PRIVATE", attempt() + " token=2",
                   attempt(response_wait_ms="NaN"), attempt(response_wait_ms="-5"),
                   attempt(response_wait_ms=str(2**63)), attempt(outcome="UNKNOWN"),
                   attempt(token="0")]
        for line in invalid:
            self.assertIsNone(parse_attempt(line))
        self.assertEqual(0, summarize(invalid)["attempts"])

    def test_report_does_not_echo_source_or_metadata(self):
        report = json.dumps(summarize(["My secret: PRIVATE", attempt(stop_reason="PRIVATE")]))
        self.assertNotIn("PRIVATE", report)
        self.assertNotIn("stop_reason", report)
        self.assertNotIn("token", report)


if __name__ == "__main__":
    unittest.main()
