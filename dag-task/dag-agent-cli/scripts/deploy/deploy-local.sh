#!/usr/bin/env bash
#
# Local deployment script for dag-agent-cli.
#
# This script builds the project via Maven and copies the fat jar
# to a local directory (e.g. for testing or local staging).
#
# Usage:
#   ./deploy-local.sh [target_dir]
#
# Arguments:
#   target_dir - local destination directory (default: ./local-deploy)
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"

LOCAL_DEPLOY_DIR="${1:-$SCRIPT_DIR/local-deploy}"
JAR_NAME="dag-agent-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar"
JAR_PATH="$TARGET_DIR/$JAR_NAME"

echo "========================================"
echo "Local Deploy"
echo "========================================"
echo ""

# Build
echo "[STEP] Building project..."
cd "$PROJECT_ROOT"
mvn clean package -pl dag-agent-cli -am -DskipTests

if [[ ! -f "$JAR_PATH" ]]; then
    echo "[ERROR] Fat jar not found: $JAR_PATH"
    exit 1
fi

# Copy
echo ""
echo "[STEP] Copying to $LOCAL_DEPLOY_DIR..."
mkdir -p "$LOCAL_DEPLOY_DIR"
cp "$JAR_PATH" "$LOCAL_DEPLOY_DIR/"

# Verify
echo ""
echo "[STEP] Verifying..."
ls -lh "$LOCAL_DEPLOY_DIR/$JAR_NAME"

echo ""
echo "========================================"
echo "Local deploy completed."
echo "========================================"
