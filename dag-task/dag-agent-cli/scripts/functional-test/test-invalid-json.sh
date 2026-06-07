#!/usr/bin/env bash
# Test malformed JSON input (expected to fail)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    402 \
    "InvalidJsonTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command": "echo hello"' \
    true
