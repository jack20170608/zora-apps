#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
readonly VERSION_FILE="${PROJECT_ROOT}/VERSION"
readonly ANSIBLE_ROOT="${PROJECT_ROOT}/host-helper-dist/deploy/ansible"
readonly PLAYBOOK="${ANSIBLE_ROOT}/playbooks/deploy.yml"

usage() {
  cat <<'EOF'
Usage: release.sh [-e environment]

Build and deploy host-helper with the version declared in VERSION.

Options:
  -e environment  Deployment environment and inventory name (default: sit)
  -h              Show this help message
EOF
}

fail() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

environment='sit'

while getopts ':e:h' option; do
  case "${option}" in
    e)
      environment="${OPTARG}"
      ;;
    h)
      usage
      exit 0
      ;;
    :)
      fail "Option -${OPTARG} requires a value"
      ;;
    \?)
      fail "Unknown option: -${OPTARG}"
      ;;
  esac
done
shift "$((OPTIND - 1))"

[[ $# -eq 0 ]] || fail "Unexpected argument: $1"
[[ "${environment}" =~ ^[a-zA-Z0-9][a-zA-Z0-9_-]*$ ]] || fail "Invalid environment: ${environment}"

[[ -r "${VERSION_FILE}" ]] || fail "Version file is not readable: ${VERSION_FILE}"
version="$(<"${VERSION_FILE}")"
version="${version%$'\r'}"
[[ "${version}" =~ ^[a-zA-Z0-9][a-zA-Z0-9._-]*$ ]] || fail "Invalid version in ${VERSION_FILE}: ${version}"
[[ "${version}" != *'..'* ]] || fail "Version must not contain '..': ${version}"

readonly inventory="${ANSIBLE_ROOT}/inventories/${environment}/hosts.ini"
readonly service_archive="${PROJECT_ROOT}/host-helper-dist/target/host-helper-dist-${version}-service.tar.gz"

[[ -f "${inventory}" ]] || fail "Inventory not found for environment '${environment}': ${inventory}"
[[ -f "${PLAYBOOK}" ]] || fail "Ansible playbook not found: ${PLAYBOOK}"

require_command mvn
require_command ansible-playbook

printf 'Building host-helper version %s\n' "${version}"
mvn -B -f "${PROJECT_ROOT}/pom.xml" clean package -Ppackage-service "-Drevision=${version}"

[[ -f "${service_archive}" ]] || fail "Expected service archive was not created: ${service_archive}"

printf 'Deploying host-helper version %s to %s\n' "${version}" "${environment}"
(
  cd -- "${PROJECT_ROOT}"
  ANSIBLE_ROLES_PATH="${ANSIBLE_ROOT}/roles" ansible-playbook \
    --inventory "${inventory}" \
    "${PLAYBOOK}" \
    --limit "${environment}" \
    --extra-vars "host_helper_env=${environment}" \
    --extra-vars "service_archive=${service_archive}"
)

printf 'Deployment completed: host-helper %s (%s)\n' "${version}" "${environment}"
