#!/bin/bash
#
# Helper script: outputs a large volume of data to stdout.
# Used to test stdout buffer handling and log truncation behavior.
#
# Arguments:
#   $1 - number of lines to output (default: 10000)

LINE_COUNT="${1:-10000}"

echo "Generating $LINE_COUNT lines of output..."

for ((i=1; i<=LINE_COUNT; i++))
do
    echo "[LINE-$i] Lorem ipsum dolor sit amet, consectetur adipiscing elit. " \
         "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " \
         "UUID=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "no-uuid")"
done

echo "✅ Finished generating $LINE_COUNT lines."
