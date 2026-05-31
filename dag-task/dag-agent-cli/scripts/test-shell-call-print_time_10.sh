#!/usr/bin/env bash
# Test script: call print_time_10.sh via ShellTaskExecution
# This script executes print_time_10.sh which loops 100 times with 1s sleep each.

source "$(dirname "$0")/run-local.sh"

SCRIPT_PATH="$SCRIPT_DIR/print_time_10.sh"

# Build the command JSON with absolute path to the target script
CMD="{\"command\":\"bash $SCRIPT_PATH\"}"

# Note: timeout set to 120000ms (120s) because print_time_10.sh takes ~100s to complete
run_cli \
    420 \
    "ShellCallPrintTime100" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    "$CMD" \
    120000
