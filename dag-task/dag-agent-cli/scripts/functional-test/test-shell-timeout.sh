#!/usr/bin/env bash
# Test shell command timeout handling

source "$(dirname "$0")/../common/run-local.sh"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    # Windows - use ping to simulate long-running command
    CMD='{"command":"ping -n 5 127.0.0.1","timeoutSeconds":1}'
else
    # Unix - use sleep
    CMD='{"command":"sleep 10","timeoutSeconds":1}'
fi

run_cli \
    102 \
    "ShellTimeoutTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    15000 \
    true
