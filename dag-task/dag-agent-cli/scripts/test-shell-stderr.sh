#!/usr/bin/env bash
# Test shell command that writes to stderr but succeeds (exit 0)
# Verifies stderr lines are logged in real time

source "$(dirname "$0")/run-local.sh"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    # Windows: echo to stderr via cmd
    CMD='{"command":"echo error-msg >&2 & exit 0"}'
else
    # Unix: echo to stderr
    CMD='{"command":"echo error-msg >&2; exit 0"}'
fi

run_cli \
    409 \
    "ShellStderrTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD"
