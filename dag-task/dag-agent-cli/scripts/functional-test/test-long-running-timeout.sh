#!/usr/bin/env bash
# Test LongRunningExecution with a shorter CLI timeout than task duration
# Task sleeps 10s but CLI timeout is 2s (expected to fail with timeout)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    408 \
    "LongRunningTimeoutTest" \
    "top.ilovemyhome.dagtask.agent.execution.LongRunningExecution" \
    '{"durationSeconds":10,"description":"Should timeout"}' \
    2000 \
    true
