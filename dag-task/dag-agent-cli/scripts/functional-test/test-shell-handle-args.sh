#!/usr/bin/env bash
# Test argument passing to shell script

source "$(dirname "$0")/../common/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/../common/handle_args.sh"
CMD="{\"command\":\"bash $SCRIPT_PATH 'hello world' 42 true\"}"

run_cli \
    434 \
    "ShellHandleArgsTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000
