#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
MODE="${1:-all}"
[[ "$MODE" == all || "$MODE" == --clients-only ]] || { echo "Usage: bash run.sh [--clients-only]" >&2; exit 1; }
for command in java mvn curl openssl; do
    command -v "$command" >/dev/null || { echo "Missing command: $command" >&2; exit 1; }
done
for port in 9201 9202; do
    if curl --silent --output /dev/null --connect-timeout 1 --max-time 1 "http://localhost:$port/health"; then
        echo "Port $port is already in use." >&2; exit 1
    fi
done

if [[ "$MODE" == all ]]; then
    command -v docker >/dev/null || { echo "Docker is required, or start Keycloak manually and use --clients-only. See README.md." >&2; exit 1; }
    docker info >/dev/null 2>&1 || { echo "Docker daemon is not available." >&2; exit 1; }
    if curl --silent --output /dev/null --connect-timeout 1 --max-time 1 http://localhost:9200/; then
        echo "Port 9200 is already in use." >&2; exit 1
    fi
    if [[ -z "${MICRONAUT_SSO_PASSWORD:-}" ]]; then
        read -r -s -p "Password for researcher (at least 16 characters): " MICRONAUT_SSO_PASSWORD
        printf '\n'
    fi
    [[ ${#MICRONAUT_SSO_PASSWORD} -ge 16 ]] || { echo "Password is too short." >&2; exit 1; }
    export MICRONAUT_SSO_PASSWORD
    export MICRONAUT_SSO_CLIENT_A_SECRET="${MICRONAUT_SSO_CLIENT_A_SECRET:-$(openssl rand -hex 24)}"
    export MICRONAUT_SSO_CLIENT_B_SECRET="${MICRONAUT_SSO_CLIENT_B_SECRET:-$(openssl rand -hex 24)}"
else
    : "${MICRONAUT_SSO_CLIENT_A_SECRET:?Set the same client A secret used when importing the realm}"
    : "${MICRONAUT_SSO_CLIENT_B_SECRET:?Set the same client B secret used when importing the realm}"
    export MICRONAUT_SSO_CLIENT_A_SECRET MICRONAUT_SSO_CLIENT_B_SECRET
fi
[[ ${#MICRONAUT_SSO_CLIENT_A_SECRET} -ge 16 && ${#MICRONAUT_SSO_CLIENT_B_SECRET} -ge 16 ]] || {
    echo "Client secrets must contain at least 16 characters." >&2; exit 1;
}

mvn -B package
mkdir -p .run
pids=()
container=""
cleanup() {
    for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
    for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
    if [[ -n "$container" ]]; then
        docker logs "$container" > .run/keycloak.log 2>&1 || true
        docker stop "$container" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [[ "$MODE" == all ]]; then
    import_file="$ROOT/provider/realm-micronaut-sso.json"
    if command -v cygpath >/dev/null; then import_file="$(cygpath -w "$import_file")"; fi
    container="micronaut-sso-lab-$$-$RANDOM"
    MSYS_NO_PATHCONV=1 docker run --detach --rm --name "$container" \
        --publish 127.0.0.1:9200:8080 \
        --mount "type=bind,source=$import_file,target=/opt/keycloak/data/import/realm-micronaut-sso.json,readonly" \
        --env MICRONAUT_SSO_PASSWORD --env MICRONAUT_SSO_CLIENT_A_SECRET --env MICRONAUT_SSO_CLIENT_B_SECRET \
        quay.io/keycloak/keycloak:26.8.1 start-dev --hostname=http://localhost:9200 --import-realm >/dev/null
fi

ready=false
for ((attempt = 0; attempt < 150; attempt++)); do
    if curl --fail --silent --output /dev/null --max-time 2 http://localhost:9200/realms/micronaut-sso/.well-known/openid-configuration; then
        ready=true; break
    fi
    sleep 1
done
[[ "$ready" == true ]] || { echo "Keycloak realm is not ready. Check the provider logs." >&2; exit 1; }

java -jar target/micronaut-security-sso-0.1.0-SNAPSHOT.jar > .run/app-a.log 2>&1 &
pids+=("$!")
java -Dmicronaut.environments=app-b -jar target/micronaut-security-sso-0.1.0-SNAPSHOT.jar > .run/app-b.log 2>&1 &
pids+=("$!")
for index in 0 1; do
    port=$((9201 + index))
    ready=false
    for ((attempt = 0; attempt < 60; attempt++)); do
        kill -0 "${pids[$index]}" 2>/dev/null || { echo "Application exited; check .run/*.log." >&2; exit 1; }
        if curl --fail --silent --output /dev/null --max-time 2 "http://localhost:$port/health"; then ready=true; break; fi
        sleep 1
    done
    [[ "$ready" == true ]] || { echo "Application on $port is not ready." >&2; exit 1; }
done
printf 'Micronaut SSO is ready.\nApp A: http://localhost:9201/\nApp B: http://localhost:9202/\nUser: researcher\nPress Ctrl+C to stop this script\047s services.\n'
wait -n "${pids[@]}"
