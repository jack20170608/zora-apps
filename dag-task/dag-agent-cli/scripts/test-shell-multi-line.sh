#!/usr/bin/env bash
# Test multi-line shell command with complex output

source "$(dirname "$0")/run-local.sh"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    # Windows: multiple echo commands separated by &
    CMD='{"command":"echo line1 & echo line2 & echo line3"}'
else
    # Unix: multiple echo commands separated by ;
    CMD='{"command":"echo line1; echo line2; echo line3"}'
fi

run_cli \
    410 \
    "ShellMultiLineTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD"
