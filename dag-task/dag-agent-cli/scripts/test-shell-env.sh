#!/usr/bin/env bash
# Test environment variables passed to shell

source "$(dirname "$0")/run-local.sh"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    # Windows
    CMD='{"command":"echo MY_VALUE=%MY_VAR%","env":{"MY_VAR":"hello-windows"}}'
else
    # Unix
    CMD='{"command":"echo MY_VALUE=$MY_VAR","env":{"MY_VAR":"hello-unix"}}'
fi

run_cli \
    103 \
    "ShellEnvTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD"
