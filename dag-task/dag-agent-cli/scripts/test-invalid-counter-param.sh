#!/usr/bin/env bash
# Test SimpleCounter with invalid param (from > to, expected to fail)

source "$(dirname "$0")/run-local.sh"

run_cli \
    407 \
    "InvalidCounterParamTest" \
    "top.ilovemyhome.dagtask.agent.execution.SimpleCounterExecution" \
    '{"from":10,"to":5,"intervalMillisecond":100}'

# Expected to fail
exit 0
