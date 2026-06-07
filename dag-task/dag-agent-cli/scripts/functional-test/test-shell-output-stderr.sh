#!/usr/bin/env bash
# Test stderr capture and stdout/stderr interleaving

source "$(dirname "$0")/../common/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/../common/output_stderr.sh"
CMD="{\"command\":\"bash $SCRIPT_PATH\"}"

run_cli \
    431 \
    "ShellOutputStderrTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000
