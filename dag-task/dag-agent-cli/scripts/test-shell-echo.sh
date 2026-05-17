#!/usr/bin/env bash
# Test basic shell echo command

source "$(dirname "$0")/run-local.sh"

run_cli \
    101 \
    "ShellEchoTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":"echo hello from dag-agent-cli"}'
