#!/usr/bin/env bash
#
# Common setup for dag-agent-cli test scripts.
# Works in both local development and distribution environments.
# Source this file at the top of each test script:
#   source "$(dirname "$0")/run-local.sh"
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Detect environment: distribution package has lib/ directory
if [[ -d "$SCRIPT_DIR/../lib" ]]; then
    # Distribution environment
    JAR_PATH=$(ls "$SCRIPT_DIR/../lib"/dag-agent-cli-*-jar-with-dependencies.jar 2>/dev/null | head -1)
    if [[ -z "$JAR_PATH" || ! -f "$JAR_PATH" ]]; then
        echo "ERROR: Fat jar not found in $SCRIPT_DIR/../lib"
        exit 1
    fi
else
    # Local development environment
    PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
    JAR_NAME="dag-agent-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar"
    JAR_PATH="$PROJECT_ROOT/target/$JAR_NAME"

    if [[ ! -f "$JAR_PATH" ]]; then
        echo "ERROR: Fat jar not found at $JAR_PATH"
        echo "Please build first: mvn clean package -pl dag-agent-cli -am -DskipTests"
        exit 1
    fi
fi

JAVA_CMD="${JAVA_CMD:-java}"
LOG_DIR="${LOG_DIR:-$SCRIPT_DIR/../logs}"

run_cli() {
    local id="$1"
    local name="$2"
    local execution_class="$3"
    local input_json="${4:-\{\}}"
    local timeout_ms="${5:-30000}"

    echo "========================================"
    echo "Test: $name (id=$id)"
    echo "========================================"

    "$JAVA_CMD" -jar "$JAR_PATH" \
        -I "$id" \
        -N "$name" \
        -e "$execution_class" \
        -i "$input_json" \
        -l "$LOG_DIR" \
        -t "$timeout_ms"

    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
        echo "[PASS] $name"
    else
        echo "[FAIL] $name (exit code: $exit_code)"
    fi
    echo ""
    return $exit_code
}
