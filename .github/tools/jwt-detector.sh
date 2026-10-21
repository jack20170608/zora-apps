#!/bin/bash
#
# JWT Detector - Analyze and validate JWT tokens
# Usage: ./jwt-detector.sh [options] [token or file]
#
# Options:
#   -f, --file <path>   Read JWT from file
#   -e, --extract       Extract JWT from Authorization header
#   -v, --validate      Validate token signature (requires secret)
#   -s, --secret <key>  Secret key for signature validation
#   -h, --help          Show this help message
#
# Examples:
#   ./jwt-detector.sh "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
#   ./jwt-detector.sh -f token.txt
#   ./jwt-detector.sh -e "Bearer eyJhbGciOiJIUzI1NiIs..."
#   ./jwt-detector.sh -v -s "my-secret-key" "eyJhbGciOiJIUzI1NiIs..."
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Usage function
usage() {
    head -30 "$0" | tail -20
    exit 0
}

# Base64URL decode
base64url_decode() {
    local input="$1"
    # Replace URL-safe characters with standard base64
    local padded=$(echo "$input" | tr '-_' '+/')
    # Add padding if needed
    local mod=$(( ${#padded} % 4 ))
    if [ $mod -eq 2 ]; then
        padded="${padded}=="
    elif [ $mod -eq 3 ]; then
        padded="${padded}="
    fi
    echo "$padded" | base64 -d 2>/dev/null || echo "$padded" | openssl base64 -d 2>/dev/null
}

# Parse JWT and extract parts
parse_jwt() {
    local token="$1"

    # Check if token has 3 parts
    local parts=$(echo "$token" | tr '.' '\n' | wc -l)
    if [ "$parts" -ne 3 ]; then
        echo -e "${RED}Error: Invalid JWT format - expected 3 parts separated by dots${NC}" >&2
        return 1
    fi

    # Extract header, payload, signature
    local header=$(echo "$token" | cut -d'.' -f1)
    local payload=$(echo "$token" | cut -d'.' -f2)
    local signature=$(echo "$token" | cut -d'.' -f3)

    echo "header=$header"
    echo "payload=$payload"
    echo "signature=$signature"
}

# Decode and display JWT parts
decode_jwt() {
    local token="$1"
    local show_header="${2:-true}"
    local show_payload="${3:-true}"

    # Parse JWT
    eval $(parse_jwt "$token")

    echo -e "${BOLD}${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}${CYAN}                        JWT Token Analysis                     ${NC}"
    echo -e "${BOLD}${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo

    # Decode Header
    if [ "$show_header" = "true" ]; then
        echo -e "${BOLD}${BLUE}┌─ HEADER ─────────────────────────────────────────────────────${NC}"
        local decoded_header=$(base64url_decode "$header")
        if command -v jq &> /dev/null; then
            echo "$decoded_header" | jq . 2>/dev/null || echo "$decoded_header"
        else
            echo "$decoded_header"
        fi
        echo -e "${BLUE}└──────────────────────────────────────────────────────────────${NC}"
        echo
    fi

    # Decode Payload
    if [ "$show_payload" = "true" ]; then
        echo -e "${BOLD}${GREEN}┌─ PAYLOAD ─────────────────────────────────────────────────────${NC}"
        local decoded_payload=$(base64url_decode "$payload")
        if command -v jq &> /dev/null; then
            echo "$decoded_payload" | jq . 2>/dev/null || echo "$decoded_payload"
        else
            echo "$decoded_payload"
        fi
        echo -e "${GREEN}└──────────────────────────────────────────────────────────────${NC}"
        echo

        # Check token expiration
        check_expiration "$decoded_payload"
    fi

    # Display signature (base64 encoded, not verified)
    echo -e "${BOLD}${YELLOW}┌─ SIGNATURE ────────────────────────────────────────────────────${NC}"
    echo "$signature"
    echo -e "${YELLOW}└──────────────────────────────────────────────────────────────${NC}"
}

# Check token expiration
check_expiration() {
    local payload="$1"

    # Extract exp claim
    local exp=$(echo "$payload" | grep -o '"exp":[0-9]*' | grep -o '[0-9]*' || true)

    if [ -z "$exp" ]; then
        echo -e "${YELLOW}⚠ No 'exp' claim found - token does not expire${NC}"
        return
    fi

    # Convert exp to readable date
    local exp_date=$(date -d "@$exp" 2>/dev/null || date -r "$exp" 2>/dev/null || echo "Unable to parse")

    # Get current timestamp
    local now=$(date +%s)

    if [ "$exp" -lt "$now" ]; then
        local expired_ago=$((now - exp))
        local days=$((expired_ago / 86400))
        local hours=$(((expired_ago % 86400) / 3600))
        echo -e "${RED}✗ Token EXPIRED${NC}"
        echo -e "  Expired: $exp_date"
        echo -e "  Expired ${days}d ${hours}h ago"
    else
        local remaining=$((exp - now))
        local days=$((remaining / 86400))
        local hours=$(((remaining % 86400) / 3600))
        local minutes=$(((remaining % 3600) / 60))
        echo -e "${GREEN}✓ Token VALID${NC}"
        echo -e "  Expires: $exp_date"
        echo -e "  Remaining: ${days}d ${hours}h ${minutes}m"
    fi
}

# Extract JWT from Authorization header
extract_from_header() {
    local header="$1"

    # Handle "Bearer <token>" format
    if [[ "$header" =~ Bearer[[:space:]]+(.+) ]]; then
        echo "${BASH_REMATCH[1]}"
    # Handle "Bearer:" prefix without space
    elif [[ "$header" =~ Bearer:(.+) ]]; then
        echo "${BASH_REMATCH[1]}"
    else
        echo "$header"
    fi
}

# Validate JWT signature (HS256)
validate_signature() {
    local token="$1"
    local secret="$2"

    # Parse JWT
    eval $(parse_jwt "$token")

    # Create signature input (header.payload)
    local signing_input="${header}.${payload}"

    # Calculate HMAC-SHA256
    local computed_signature=$(echo -n "$signing_input" | openssl dgst -sha256 -hmac "$secret" 2>/dev/null | cut -d' ' -f2)

    # Compare signatures (base64url encoded)
    local expected_signature=$(echo "$signature" | tr '-_' '+/' | cut -d'=' -f1)
    local computed_signature_b64=$(echo "$computed_signature" | tr -d '[:space:]' | cut -d'=' -f1 | tr '+/' '-_')

    if [ "$expected_signature" = "$computed_signature_b64" ]; then
        echo -e "${GREEN}✓ Signature VALID${NC}"
        return 0
    else
        echo -e "${RED}✗ Signature INVALID${NC}"
        return 1
    fi
}

# Main function
main() {
    local token=""
    local token_file=""
    local extract_mode=false
    local validate_mode=false
    local secret=""

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                usage
                ;;
            -f|--file)
                token_file="$2"
                shift 2
                ;;
            -e|--extract)
                extract_mode=true
                shift
                ;;
            -v|--validate)
                validate_mode=true
                shift
                ;;
            -s|--secret)
                secret="$2"
                shift 2
                ;;
            -*)
                echo -e "${RED}Error: Unknown option: $1${NC}" >&2
                usage
                ;;
            *)
                token="$1"
                shift
                ;;
        esac
    done

    # Read token from file if specified
    if [ -n "$token_file" ]; then
        if [ ! -f "$token_file" ]; then
            echo -e "${RED}Error: File not found: $token_file${NC}" >&2
            exit 1
        fi
        token=$(cat "$token_file" | tr -d '[:space:]')
    fi

    # Extract token from header if in extract mode
    if [ "$extract_mode" = true ]; then
        token=$(extract_from_header "$token")
    fi

    # Validate token exists
    if [ -z "$token" ]; then
        echo -e "${RED}Error: No JWT token provided${NC}" >&2
        usage
    fi

    # Remove whitespace
    token=$(echo "$token" | tr -d '[:space:]')

    # Decode and display JWT
    decode_jwt "$token"

    # Validate signature if requested
    if [ "$validate_mode" = true ]; then
        echo
        if [ -z "$secret" ]; then
            echo -e "${RED}Error: Secret key required for validation (--secret/-s)${NC}" >&2
            exit 1
        fi
        validate_signature "$token" "$secret"
    fi
}

# Run main function
main "$@"