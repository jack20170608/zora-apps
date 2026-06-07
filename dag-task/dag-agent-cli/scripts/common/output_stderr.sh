#!/bin/bash
#
# Helper script: outputs interleaved stdout and stderr messages.
# Used to test stderr capture and stdout/stderr ordering.
#

echo "[STDOUT] Starting process..."
echo "[STDERR] Initializing error stream..." >&2

for ((i=1; i<=10; i++))
do
    echo "[STDOUT] Progress step $i"
    if (( i % 3 == 0 )); then
        echo "[STDERR] Warning at step $i" >&2
    fi
    sleep 0.1
done

echo "[STDERR] Final error message" >&2
echo "[STDOUT] Process completed successfully"
