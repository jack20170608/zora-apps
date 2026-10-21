#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

for command in java mvn curl openssl; do
    command -v "$command" >/dev/null || { echo "Missing command: $command" >&2; exit 1; }
done
for port in 9100 9101 9102; do
    if curl --silent --output /dev/null --connect-timeout 1 --max-time 1 "http://localhost:$port/health"; then
        echo "Port $port is already in use; stop the existing service first." >&2
        exit 1
    fi
done

if [[ -z "${SSO_DEMO_PASSWORD:-}" ]]; then
    read -r -s -p "Password for researcher (at least 16 characters): " SSO_DEMO_PASSWORD
    printf '\n'
fi
if [[ ${#SSO_DEMO_PASSWORD} -lt 16 ]]; then
    echo "The research password must contain at least 16 characters." >&2
    exit 1
fi
export SSO_DEMO_PASSWORD
export SSO_CLIENT_A_SECRET="${SSO_CLIENT_A_SECRET:-$(openssl rand -hex 24)}"
export SSO_CLIENT_B_SECRET="${SSO_CLIENT_B_SECRET:-$(openssl rand -hex 24)}"

mvn -B package
mkdir -p .run
pids=()
cleanup() {
    for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
    for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

wait_for_service() {
    local port="$1" pid="$2"
    for ((attempt = 0; attempt < 90; attempt++)); do
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "Service on $port exited. Check .run/*.log." >&2
            return 1
        fi
        if curl --fail --silent --output /dev/null --max-time 2 "http://localhost:$port/health"; then return 0; fi
        sleep 1
    done
    echo "Service on $port did not become ready. Check .run/*.log." >&2
    return 1
}

java -jar authorization-server/target/authorization-server-0.1.0-SNAPSHOT.jar > .run/authorization-server.log 2>&1 &
pids+=("$!")
wait_for_service 9100 "${pids[0]}"
java -jar oidc-client/target/oidc-client-0.1.0-SNAPSHOT.jar --spring.profiles.active=app-a > .run/app-a.log 2>&1 &
pids+=("$!")
java -jar oidc-client/target/oidc-client-0.1.0-SNAPSHOT.jar --spring.profiles.active=app-b > .run/app-b.log 2>&1 &
pids+=("$!")
wait_for_service 9101 "${pids[1]}"
wait_for_service 9102 "${pids[2]}"
printf 'SSO lab is ready.\nApp A: http://localhost:9101/\nApp B: http://localhost:9102/\nUser: researcher\nPress Ctrl+C to stop all lab services.\n'
wait -n "${pids[@]}"
