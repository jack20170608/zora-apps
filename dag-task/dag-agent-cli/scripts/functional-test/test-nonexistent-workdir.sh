#!/usr/bin/env bash
# Test non-existent working directory (behavior depends on OS; may succeed or fail)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    406 \
    "NonExistentWorkdirTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":"pwd","workingDirectory":"/this_path_does_not_exist_12345"}'

# Result varies by platform and shell, so don't force exit code
exit 0
