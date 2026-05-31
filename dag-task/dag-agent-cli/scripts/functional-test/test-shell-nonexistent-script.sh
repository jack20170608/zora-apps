#!/usr/bin/env bash
# Test calling a non-existent script (expected to fail)

source "$(dirname "$0")/../common/run-local.sh"

CMD='{"command":"bash /this_script_does_not_exist_12345.sh"}'

run_cli \
    436 \
    "ShellNonexistentScriptTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000 \
    true
