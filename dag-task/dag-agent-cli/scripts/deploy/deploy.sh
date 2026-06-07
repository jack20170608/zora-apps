#!/usr/bin/env bash
#
# Deployment script for dag-agent-cli.
#
# This script builds the project via Maven, creates the distribution archive
# (via maven-assembly-plugin using distribution.xml), and deploys it to a
# remote server via SCP.  The archive is then extracted in place on the target.
#
# Usage:
#   ./deploy.sh [profile]
#
# Environment variables (or edit the CONFIG section below):
#   DEPLOY_HOST      - target remote host (default: 192.168.1.100)
#   DEPLOY_USER      - SSH user (default: deploy)
#   DEPLOY_KEY       - SSH private key path (optional)
#   DEPLOY_DIR       - remote deployment directory (default: /opt/dag-agent-cli)
#   MVN_PROFILE      - Maven build profile (optional)
#

set -euo pipefail

# ==============================================================================
# CONFIG (override via environment variables or edit in place)
# ==============================================================================
DEPLOY_HOST="${DEPLOY_HOST:-10.10.10.20}"
DEPLOY_USER="${DEPLOY_USER:-jack}"
DEPLOY_KEY="${DEPLOY_KEY:-~/.ssh/id_rsa}"              # e.g. ~/.ssh/id_rsa_deploy
DEPLOY_DIR="${DEPLOY_DIR:-/appvol/ilovemyhome/apps/dag-task-cli}"
MVN_PROFILE="${MVN_PROFILE:-}"

# ==============================================================================
# DERIVED PATHS
# ==============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DAG_CLI_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PROJECT_ROOT="$(cd "$DAG_CLI_ROOT/../" && pwd)"
TARGET_DIR="$DAG_CLI_ROOT/target"

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
log_step "Building distribution package with Maven"

cd "$PROJECT_ROOT"

MVN_ARGS="-pl dag-agent-cli -am -DskipTests"
if [[ -n "$MVN_PROFILE" ]]; then
    MVN_ARGS="$MVN_ARGS -P$MVN_PROFILE"
fi

log_info "Running: mvn clean package $MVN_ARGS"
mvn clean package $MVN_ARGS

# Discover distribution archive dynamically AFTER build (avoids hardcoding version)
DIST_PATH=$(ls "$TARGET_DIR"/dag-agent-cli-*-distribution.tar.gz 2>/dev/null | head -1)
DIST_NAME=$(basename "$DIST_PATH" 2>/dev/null || true)

if [[ ! -f "$DIST_PATH" ]]; then
    log_error "Distribution archive not found: $DIST_PATH"
    exit 1
fi

log_info "Build completed.  Distribution: $DIST_NAME"

# ==============================================================================
# DEPLOY
# ==============================================================================
log_step "Deploying to $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR"

log_info "Ensuring remote directory exists..."
ssh_cmd "mkdir -p $DEPLOY_DIR"

log_info "Uploading distribution archive..."
scp_cmd "$DIST_PATH" "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/$DIST_NAME"

log_info "Extracting archive on remote host..."
ssh_cmd "cd $DEPLOY_DIR && tar -xzf $DIST_NAME --overwrite"

log_info "Removing remote archive file..."
ssh_cmd "rm -f $DEPLOY_DIR/$DIST_NAME"

# ==============================================================================
# POST-DEPLOY
# ==============================================================================
log_step "Post-deployment verification"

log_info "Listing deployed files on remote host:"
ssh_cmd "ls -lhR $DEPLOY_DIR/"

# Uncomment to automatically restart a remote service after deployment
# log_info "Restarting remote service..."
# ssh_cmd "sudo systemctl restart dag-agent-cli || true"

log_info "Deployment completed successfully."
