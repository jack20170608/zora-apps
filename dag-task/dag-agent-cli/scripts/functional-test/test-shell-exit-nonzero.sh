#!/usr/bin/env bash
# Test non-zero exit code from called script (expected to fail)

source "$(dirname "$0")/../common/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/../common/exit_nonzero.sh"
CMD="{\"command\":\"bash $SCRIPT_PATH 7\"}"

run_cli \
    435 \
    "ShellExitNonzeroTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000 \
    true
