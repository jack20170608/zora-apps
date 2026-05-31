#!/usr/bin/env bash
# Test working directory file creation

source "$(dirname "$0")/../common/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/../common/write_files.sh"
CMD="{\"command\":\"bash $SCRIPT_PATH 3 result\"}"

run_cli \
    433 \
    "ShellWriteFilesTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000
