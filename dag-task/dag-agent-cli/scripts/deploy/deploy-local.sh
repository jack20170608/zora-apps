#!/usr/bin/env bash
#
# Local deployment script for dag-agent-cli.
#
# This script builds the project via Maven (using the distribution archive)
# and extracts it to a local directory (e.g. for testing or local staging).
#
# Usage:
#   ./deploy-local.sh [target_dir]
#
# Arguments:
#   target_dir - local destination directory (default: ./local-deploy)
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DAG_CLI_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PROJECT_ROOT="$(cd "$DAG_CLI_ROOT/../" && pwd)"
TARGET_DIR="$DAG_CLI_ROOT/target"

LOCAL_DEPLOY_DIR="${1:-$SCRIPT_DIR/local-deploy}"

echo "========================================"
echo "Local Deploy"
echo "========================================"
echo ""

# Build
echo "[STEP] Building distribution package..."
cd "$PROJECT_ROOT"
mvn clean package -pl dag-agent-cli -am -DskipTests

# Discover distribution archive dynamically AFTER build (avoids hardcoding version)
DIST_PATH=$(ls "$TARGET_DIR"/dag-agent-cli-*-distribution.tar.gz 2>/dev/null | head -1)
DIST_NAME=$(basename "$DIST_PATH" 2>/dev/null || true)

if [[ ! -f "$DIST_PATH" ]]; then
    echo "[ERROR] Distribution archive not found: $DIST_PATH"
    exit 1
fi

# Extract
echo ""
echo "[STEP] Extracting to $LOCAL_DEPLOY_DIR..."
mkdir -p "$LOCAL_DEPLOY_DIR"
tar -xzf "$DIST_PATH" -C "$LOCAL_DEPLOY_DIR" --overwrite

# Verify
echo ""
echo "[STEP] Verifying deployed contents..."
find "$LOCAL_DEPLOY_DIR" -maxdepth 3 -type f | sort

echo ""
echo "========================================"
echo "Local deploy completed."
echo "========================================"
