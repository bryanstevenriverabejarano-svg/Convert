#!/usr/bin/env python3
"""Summarize Salve's software voice timings without retaining log lines or content."""

import argparse
from collections import Counter
import json
import math
import sys


METRICS = (
    "asr_ready_ms", "asr_finalize_ms", "response_wait_ms",
    "reply_to_tts_start_ms", "submit_to_tts_start_ms",
    "tts_duration_ms", "turn_total_ms",
)
OUTCOMES = {"COMPLETED", "INTERRUPTED", "RECOGNITION_ERROR", "STOPPED"}
FIELDS = {"token", "result", "stop_reason", "asr_error", *METRICS}


def parse_attempt(line):
    marker = "voice_turn "
    start = line.find(marker)
    if start < 0 or len(line) > 4096:
        return None
    fields = {}
    for item in line[start + len(marker):].split():
        key, separator, value = item.partition("=")
        if not separator or key not in FIELDS or key in fields:
            return None
        fields[key] = value
    if fields.keys() != FIELDS or fields["result"] not in OUTCOMES:
        return None
    if not fields["token"].isascii() or not fields["token"].isdigit() or int(fields["token"]) <= 0:
        return None
    durations = {}
    for key in METRICS:
        value = fields[key]
        if value == "missing":
            durations[key] = None
        elif value.isascii() and value.isdigit() and int(value) <= 2**63 - 1:
            durations[key] = int(value)
        else:
            return None
    # Only this allowlisted result and numeric durations leave the parser.
    return fields["result"], durations


def percentile(values, fraction):
    """Nearest-rank percentile; no synthetic sample for missing observations."""
    if not values:
        return None
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * fraction) - 1)]


def summarize(lines):
    outcomes = Counter()
    samples = {key: [] for key in METRICS}
    for line in lines:
        attempt = parse_attempt(line)
        if attempt is None:
            continue
        outcome, durations = attempt
        outcomes[outcome] += 1
        # Interrupted/failed attempts have incomplete timings: count them separately.
        if outcome == "COMPLETED":
            for key, value in durations.items():
                if value is not None:
                    samples[key].append(value)
    return {
        "attempts": sum(outcomes.values()),
        "outcomes": {key: outcomes[key] for key in sorted(OUTCOMES)},
        "percentile_method": "nearest_rank",
        "timings_for": "COMPLETED",
        "metrics": {
            key: {"samples": len(values), "p50_ms": percentile(values, .50),
                  "p95_ms": percentile(values, .95)}
            for key, values in samples.items()
        },
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", nargs="?", default="-", help="Logcat file, or - for standard input")
    args = parser.parse_args()
    if args.log == "-":
        result = summarize(sys.stdin)
    else:
        with open(args.log, encoding="utf-8", errors="replace") as source:
            result = summarize(source)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
