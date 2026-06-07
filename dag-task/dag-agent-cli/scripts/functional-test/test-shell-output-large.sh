#!/usr/bin/env bash
# Test large stdout output handling

source "$(dirname "$0")/../common/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/../common/output_large.sh"
CMD="{\"command\":\"bash $SCRIPT_PATH 500\"}"

run_cli \
    430 \
    "ShellOutputLargeTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000
