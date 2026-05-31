#!/usr/bin/env bash
# Test environment variable passing to shell task

source "$(dirname "$0")/../common/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/../common/use_env_vars.sh"
CMD="{\"command\":\"MY_VAR=hello_value TASK_NAME=env_test bash $SCRIPT_PATH\"}"

run_cli \
    432 \
    "ShellUseEnvVarsTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    30000
