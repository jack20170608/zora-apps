#!/bin/bash
#
# Helper script: exits with a non-zero status code.
# Used to test error handling and exit code propagation.
#
# Arguments:
#   $1 - exit code to return (default: 42)

EXIT_CODE="${1:-42}"

echo "This script will exit with code $EXIT_CODE"
echo "Simulating failure..."

exit "$EXIT_CODE"
