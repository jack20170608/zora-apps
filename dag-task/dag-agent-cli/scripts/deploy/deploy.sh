#!/usr/bin/env bash
#
# Deployment script for dag-agent-cli.
#
# This script builds the project via Maven and deploys the fat jar
# (and optionally the distribution archive) to a remote server via SCP.
#
# Usage:
#   ./deploy.sh [profile]
#
# Environment variables (or edit the CONFIG section below):
#   DEPLOY_HOST      - target remote host (default: 192.168.1.100)
#   DEPLOY_USER      - SSH user (default: deploy)
#   DEPLOY_KEY       - SSH private key path (optional)
#   DEPLOY_DIR       - remote deployment directory (default: /opt/dag-agent-cli)
#   DEPLOY_JAR       - whether to deploy fat jar (default: true)
#   DEPLOY_DIST      - whether to deploy distribution tarball (default: false)
#   MVN_PROFILE      - Maven build profile (optional)
#

set -euo pipefail

# ==============================================================================
# CONFIG (override via environment variables or edit in place)
# ==============================================================================
DEPLOY_HOST="${DEPLOY_HOST:-192.168.1.100}"
DEPLOY_USER="${DEPLOY_USER:-deploy}"
DEPLOY_KEY="${DEPLOY_KEY:-}"              # e.g. ~/.ssh/id_rsa_deploy
DEPLOY_DIR="${DEPLOY_DIR:-/opt/dag-agent-cli}"
DEPLOY_JAR="${DEPLOY_JAR:-true}"
DEPLOY_DIST="${DEPLOY_DIST:-false}"
MVN_PROFILE="${MVN_PROFILE:-}"

# ==============================================================================
# DERIVED PATHS
# ==============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"

JAR_NAME="dag-agent-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar"
DIST_NAME="dag-agent-cli-1.0.1-SNAPSHOT-distribution.tar.gz"

JAR_PATH="$TARGET_DIR/$JAR_NAME"
DIST_PATH="$TARGET_DIR/$DIST_NAME"

# ==============================================================================
# HELPERS
# ==============================================================================
log_info()  { echo "[INFO]  $*"; }
log_warn()  { echo "[WARN]  $*" >&2; }
log_error() { echo "[ERROR] $*" >&2; }
log_step()  { echo ""; echo "========================================"; echo "[STEP]  $*"; echo "========================================"; }

scp_cmd() {
    local src="$1"
    local dst="$2"
    if [[ -n "$DEPLOY_KEY" ]]; then
        scp -i "$DEPLOY_KEY" -o StrictHostKeyChecking=no "$src" "$dst"
    else
        scp -o StrictHostKeyChecking=no "$src" "$dst"
    fi
}

ssh_cmd() {
    local remote_cmd="$1"
    if [[ -n "$DEPLOY_KEY" ]]; then
        ssh -i "$DEPLOY_KEY" -o StrictHostKeyChecking=no "$DEPLOY_USER@$DEPLOY_HOST" "$remote_cmd"
    else
        ssh -o StrictHostKeyChecking=no "$DEPLOY_USER@$DEPLOY_HOST" "$remote_cmd"
    fi
}

# ==============================================================================
# BUILD
# ==============================================================================
log_step "Building project with Maven"

cd "$PROJECT_ROOT"

MVN_ARGS="-pl dag-agent-cli -am -DskipTests"
if [[ -n "$MVN_PROFILE" ]]; then
    MVN_ARGS="$MVN_ARGS -P$MVN_PROFILE"
fi

log_info "Running: mvn clean package $MVN_ARGS"
mvn clean package $MVN_ARGS

if [[ "$DEPLOY_JAR" == "true" && ! -f "$JAR_PATH" ]]; then
    log_error "Fat jar not found: $JAR_PATH"
    exit 1
fi

if [[ "$DEPLOY_DIST" == "true" && ! -f "$DIST_PATH" ]]; then
    log_error "Distribution archive not found: $DIST_PATH"
    exit 1
fi

log_info "Build completed successfully."

# ==============================================================================
# DEPLOY
# ==============================================================================
log_step "Deploying to $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR"

log_info "Ensuring remote directory exists..."
ssh_cmd "mkdir -p $DEPLOY_DIR"

if [[ "$DEPLOY_JAR" == "true" ]]; then
    log_info "Uploading fat jar..."
    scp_cmd "$JAR_PATH" "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/$JAR_NAME"
    log_info "Fat jar uploaded: $DEPLOY_DIR/$JAR_NAME"
fi

if [[ "$DEPLOY_DIST" == "true" ]]; then
    log_info "Uploading distribution archive..."
    scp_cmd "$DIST_PATH" "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/$DIST_NAME"
    log_info "Distribution archive uploaded: $DEPLOY_DIR/$DIST_NAME"
fi

# ==============================================================================
# POST-DEPLOY (optional)
# ==============================================================================
log_step "Post-deployment verification"

log_info "Listing deployed files on remote host:"
ssh_cmd "ls -lh $DEPLOY_DIR/"

# Uncomment the following block to automatically restart a remote service after deployment
# log_info "Restarting remote service..."
# ssh_cmd "sudo systemctl restart dag-agent-cli || true"

log_info "Deployment completed successfully."
