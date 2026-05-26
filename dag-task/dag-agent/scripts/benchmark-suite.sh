#!/usr/bin/env bash
#
# DAG Agent Benchmark Suite - Integrated Quick Start
#
# This script provides an interactive menu for running various benchmark tests
#
# Usage: ./benchmark-suite.sh [--auto|--interactive]
#

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODE="${1:-interactive}"  # auto or interactive

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Source common API
source "$SCRIPT_DIR/run-api.sh"

# Configuration
DEFAULT_AGENT_URL="http://localhost:20000"
DEFAULT_THREADS=4
DEFAULT_TASKS=100

###############################################################################
# UI Functions
###############################################################################

print_header() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║        DAG Agent Benchmark Testing Suite v1.0              ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_menu() {
    echo -e "${CYAN}Select a benchmark test:${NC}"
    echo ""
    echo "  1) ${GREEN}Quick Benchmark${NC}        - Fast throughput test (recommended for daily use)"
    echo "  2) ${GREEN}Full Benchmark${NC}         - Comprehensive performance analysis"
    echo "  3) ${GREEN}Custom Benchmark${NC}       - Configure test parameters"
    echo "  4) ${GREEN}Analyze Results${NC}        - Analyze previous benchmark results"
    echo "  5) ${GREEN}Stress Test${NC}            - High-load stress testing"
    echo "  6) ${GREEN}Compare Results${NC}        - Compare multiple benchmark runs"
    echo ""
    echo "  0) ${RED}Exit${NC}"
    echo ""
    printf "${CYAN}Enter your choice (0-6): ${NC}"
}

print_status() {
    local status="$1"
    local message="$2"

    case $status in
        success)
            echo -e "${GREEN}✓${NC} $message"
            ;;
        error)
            echo -e "${RED}✗${NC} $message"
            ;;
        info)
            echo -e "${BLUE}ℹ${NC} $message"
            ;;
        warn)
            echo -e "${YELLOW}⚠${NC} $message"
            ;;
    esac
}

###############################################################################
# Benchmark Functions
###############################################################################

run_quick_benchmark() {
    echo ""
    echo -e "${CYAN}=== Quick Benchmark ===${NC}"
    echo ""
    printf "${CYAN}Agent URL (default: $DEFAULT_AGENT_URL): ${NC}"
    read agent_url
    agent_url="${agent_url:-$DEFAULT_AGENT_URL}"

    printf "${CYAN}Number of tasks (default: $DEFAULT_TASKS): ${NC}"
    read num_tasks
    num_tasks="${num_tasks:-$DEFAULT_TASKS}"

    printf "${CYAN}Number of threads (default: $DEFAULT_THREADS): ${NC}"
    read num_threads
    num_threads="${num_threads:-$DEFAULT_THREADS}"

    echo ""
    print_status info "Starting quick benchmark..."

    "$SCRIPT_DIR/benchmark-quick.sh" \
        --url "$agent_url" \
        --tasks "$num_tasks" \
        --threads "$num_threads"

    if [[ $? -eq 0 ]]; then
        print_status success "Quick benchmark completed!"
    else
        print_status error "Benchmark failed!"
        return 1
    fi
}

run_full_benchmark() {
    echo ""
    echo -e "${CYAN}=== Full Benchmark ===${NC}"
    echo ""
    printf "${CYAN}Agent URL (default: $DEFAULT_AGENT_URL): ${NC}"
    read agent_url
    agent_url="${agent_url:-$DEFAULT_AGENT_URL}"

    printf "${CYAN}Number of tasks (default: 500): ${NC}"
    read num_tasks
    num_tasks="${num_tasks:-500}"

    printf "${CYAN}Number of threads (default: 8): ${NC}"
    read num_threads
    num_threads="${num_threads:-8}"

    printf "${CYAN}Task type [echo|shell|io|cpu] (default: echo): ${NC}"
    read task_type
    task_type="${task_type:-echo}"

    echo ""
    print_status info "Starting full benchmark (this may take several minutes)..."

    "$SCRIPT_DIR/benchmark-performance-test.sh" \
        --url "$agent_url" \
        --tasks "$num_tasks" \
        --threads "$num_threads" \
        --task-type "$task_type"

    if [[ $? -eq 0 ]]; then
        print_status success "Full benchmark completed!"

        # Find latest metrics file and offer to analyze
        latest_metrics=$(find "$SCRIPT_DIR" -name "metrics.csv" -type f -printf '%T@ %p\n' | sort -rn | head -1 | cut -d' ' -f2-)
        if [[ -n "$latest_metrics" ]]; then
            echo ""
            printf "${CYAN}Analyze results? (y/n): ${NC}"
            read analyze
            if [[ "$analyze" == "y" ]]; then
                python3 "$SCRIPT_DIR/benchmark-analyzer.py" "$latest_metrics"
            fi
        fi
    else
        print_status error "Benchmark failed!"
        return 1
    fi
}

run_custom_benchmark() {
    echo ""
    echo -e "${CYAN}=== Custom Benchmark ===${NC}"
    echo ""

    printf "${CYAN}Agent URL (default: $DEFAULT_AGENT_URL): ${NC}"
    read agent_url
    agent_url="${agent_url:-$DEFAULT_AGENT_URL}"

    printf "${CYAN}Number of tasks (default: $DEFAULT_TASKS): ${NC}"
    read num_tasks
    num_tasks="${num_tasks:-$DEFAULT_TASKS}"

    printf "${CYAN}Number of threads (default: $DEFAULT_THREADS): ${NC}"
    read num_threads
    num_threads="${num_threads:-$DEFAULT_THREADS}"

    printf "${CYAN}Batch size (default: 10): ${NC}"
    read batch_size
    batch_size="${batch_size:-10}"

    printf "${CYAN}Task type [echo|shell|io|cpu] (default: echo): ${NC}"
    read task_type
    task_type="${task_type:-echo}"

    printf "${CYAN}Max duration in seconds (default: 300): ${NC}"
    read duration
    duration="${duration:-300}"

    echo ""
    print_status info "Starting custom benchmark..."

    "$SCRIPT_DIR/benchmark-performance-test.sh" \
        --url "$agent_url" \
        --tasks "$num_tasks" \
        --threads "$num_threads" \
        --batch-size "$batch_size" \
        --task-type "$task_type" \
        --duration "$duration"

    if [[ $? -eq 0 ]]; then
        print_status success "Custom benchmark completed!"
    else
        print_status error "Benchmark failed!"
        return 1
    fi
}

analyze_results() {
    echo ""
    echo -e "${CYAN}=== Analyze Results ===${NC}"
    echo ""

    # Find all metrics files
    metrics_files=()
    while IFS= read -r file; do
        metrics_files+=("$file")
    done < <(find "$SCRIPT_DIR" -name "metrics.csv" -type f 2>/dev/null | sort -r)

    if [[ ${#metrics_files[@]} -eq 0 ]]; then
        print_status error "No benchmark results found!"
        return 1
    fi

    echo "Found ${#metrics_files[@]} benchmark result(s):"
    echo ""

    for i in "${!metrics_files[@]}"; do
        local file="${metrics_files[$i]}"
        local dir=$(dirname "$file")
        local mtime=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$dir" 2>/dev/null || stat -c "%y" "$dir" 2>/dev/null | cut -d' ' -f1-2)
        echo "  $((i + 1))) $dir ($mtime)"
    done
    echo ""
    printf "${CYAN}Select result to analyze (1-${#metrics_files[@]}): ${NC}"
    read selection

    if [[ "$selection" -lt 1 ]] || [[ "$selection" -gt ${#metrics_files[@]} ]]; then
        print_status error "Invalid selection!"
        return 1
    fi

    local selected_file="${metrics_files[$((selection - 1))]}"
    local output_dir=$(dirname "$selected_file")

    print_status info "Analyzing $selected_file..."

    if command -v python3 &> /dev/null; then
        python3 "$SCRIPT_DIR/benchmark-analyzer.py" "$selected_file" --output "$output_dir"
    else
        print_status error "Python3 not found. Cannot analyze results."
        return 1
    fi
}

stress_test() {
    echo ""
    echo -e "${CYAN}=== Stress Test ===${NC}"
    echo ""
    printf "${CYAN}Agent URL (default: $DEFAULT_AGENT_URL): ${NC}"
    read agent_url
    agent_url="${agent_url:-$DEFAULT_AGENT_URL}"

    printf "${CYAN}Number of concurrent threads (default: 16): ${NC}"
    read num_threads
    num_threads="${num_threads:-16}"

    printf "${CYAN}Number of tasks per thread (default: 100): ${NC}"
    read tasks_per_thread
    tasks_per_thread="${tasks_per_thread:-100}"

    echo ""
    print_status info "Starting stress test with $num_threads threads..."

    "$SCRIPT_DIR/benchmark-performance-test.sh" \
        --url "$agent_url" \
        --threads "$num_threads" \
        --tasks $((num_threads * tasks_per_thread)) \
        --batch-size $((tasks_per_thread / 2 + 1))

    if [[ $? -eq 0 ]]; then
        print_status success "Stress test completed!"
    else
        print_status error "Stress test failed!"
        return 1
    fi
}

compare_results() {
    echo ""
    echo -e "${CYAN}=== Compare Results ===${NC}"
    echo ""
    print_status info "This feature requires analysis of multiple benchmark runs."
    print_status info "Run multiple benchmarks first, then compare their results."
    echo ""
    echo "Example:"
    echo "  1. Run benchmark on configuration A"
    echo "  2. Run benchmark on configuration B"
    echo "  3. Analyze both results"
    echo "  4. Compare the generated reports"
    echo ""
    printf "${CYAN}Enter results directory path: ${NC}"
    read result_dir

    if [[ ! -d "$result_dir" ]]; then
        print_status error "Directory not found: $result_dir"
        return 1
    fi

    print_status info "Found the following results:"
    find "$result_dir" -name "metrics.csv" -type f | nl
}

###############################################################################
# Main Loop
###############################################################################

main() {
    print_header

    # Check server connectivity
    echo -e "${CYAN}Checking server connectivity...${NC}"
    if ! ping_server 2>/dev/null; then
        echo ""
        print_status warn "Agent server may not be reachable."
        printf "${CYAN}Continue anyway? (y/n): ${NC}"
        read continue_anyway
        if [[ "$continue_anyway" != "y" ]]; then
            print_status error "Exiting."
            exit 1
        fi
    else
        print_status success "Agent server is reachable."
    fi

    echo ""

    while true; do
        print_menu
        read choice

        case "$choice" in
            1)
                run_quick_benchmark
                ;;
            2)
                run_full_benchmark
                ;;
            3)
                run_custom_benchmark
                ;;
            4)
                analyze_results
                ;;
            5)
                stress_test
                ;;
            6)
                compare_results
                ;;
            0)
                echo ""
                print_status info "Exiting benchmark suite."
                exit 0
                ;;
            *)
                print_status error "Invalid choice!"
                ;;
        esac

        echo ""
        printf "${CYAN}Press Enter to continue...${NC}"
        read
    done
}

# Run main function
main "$@"

