#!/usr/bin/env bash
# Test UTF-8 output handling

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    104 \
    "ShellUtf8Test" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":"echo 你好，世界！UTF-8 test: 中文"}'
