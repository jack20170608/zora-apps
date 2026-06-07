#!/bin/bash
#
# Helper script: echoes back all received arguments.
# Used to test argument passing from task input JSON.
#

echo "=== Argument Test ==="
echo "Script path: $0"
echo "Argument count: $#"
echo ""

if [[ $# -eq 0 ]]; then
    echo "No arguments received."
else
    echo "Arguments received:"
    i=1
    for arg in "$@"
    do
        echo "  [$i] = '$arg'"
        ((i++))
    done
fi

echo ""
echo "✅ Argument echo complete"
