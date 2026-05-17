#!/usr/bin/env bash
# Test LongRunningExecution with 2-second duration

source "$(dirname "$0")/run-local.sh"

run_cli \
    202 \
    "LongRunningTest" \
    "top.ilovemyhome.dagtask.agent.execution.LongRunningExecution" \
    '{"durationSeconds":2,"description":"CLI test run"}'
