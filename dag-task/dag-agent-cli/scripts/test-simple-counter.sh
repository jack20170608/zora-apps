#!/usr/bin/env bash
# Test SimpleCounterExecution

source "$(dirname "$0")/run-local.sh"

run_cli \
    201 \
    "SimpleCounterTest" \
    "top.ilovemyhome.dagtask.agent.execution.SimpleCounterExecution" \
    '{"from":1,"to":5,"intervalMillisecond":100}'
