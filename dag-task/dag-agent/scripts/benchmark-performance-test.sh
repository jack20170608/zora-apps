#!/usr/bin/env bash
#
# Comprehensive benchmark test for dag-agent performance evaluation
#
# Usage:
#   ./benchmark-performance-test.sh [OPTIONS]
#
# Options:
#   --url <url>              Agent base URL (default: http://localhost:20000)
#   --threads <n>            Number of concurrent threads (default: 4)
#   --tasks <n>              Target number of tasks (default: 100)
#   --batch-size <n>         Batch size for parallel submission (default: 10)
#   --task-type <type>       Task type: echo|shell|io|cpu (default: echo)
#   --duration <seconds>     Benchmark duration limit (default: 300)
#   --help                   Show this help message
#
# Example:
#   ./benchmark-performance-test.sh --url http://localhost:20000 --threads 4 --tasks 100
#

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Parse command line arguments
BASE_URL="${DAG_AGENT_URL:-http://localhost:20000}"
THREAD_COUNT=4
TARGET_TASKS=100
BATCH_SIZE=10
TASK_TYPE="echo"
MAX_DURATION=300
RESULTS_DIR=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            BASE_URL="$2"
            shift 2
            ;;
        --threads)
            THREAD_COUNT="$2"
            shift 2
            ;;
        --tasks)
            TARGET_TASKS="$2"
            shift 2
            ;;
        --batch-size)
            BATCH_SIZE="$2"
            shift 2
            ;;
        --task-type)
            TASK_TYPE="$2"
            shift 2
            ;;
        --duration)
            MAX_DURATION="$2"
            shift 2
            ;;
        --help)
            grep "^#" "$0" | tail -n +2
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Source the common API functions
source "$SCRIPT_DIR/run-api.sh"

# Colors for terminal output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Benchmark state
BENCHMARK_START_TIME=$(date +%s%N)
TOTAL_SUBMITTED=0
TOTAL_SUCCESS=0
TOTAL_FAILED=0
TOTAL_LATENCY=0
MIN_LATENCY=999999
MAX_LATENCY=0
DECLARE -a LATENCIES=()

# Ensure results directory exists
RESULTS_DIR="${RESULTS_DIR:-benchmark_results_$(date +%Y%m%d_%H%M%S)}"
mkdir -p "$RESULTS_DIR"

# Log file
LOG_FILE="$RESULTS_DIR/benchmark.log"
METRICS_FILE="$RESULTS_DIR/metrics.csv"

# Initialize metrics file with CSV header
echo "task_id,submission_time_ms,status,latency_ms" > "$METRICS_FILE"

###############################################################################
# Utility Functions
###############################################################################

log_info() {
    local msg="$1"
    echo -e "${BLUE}[INFO]${NC} $msg" | tee -a "$LOG_FILE"
}

log_success() {
    local msg="$1"
    echo -e "${GREEN}[OK]${NC} $msg" | tee -a "$LOG_FILE"
}

log_error() {
    local msg="$1"
    echo -e "${RED}[ERROR]${NC} $msg" | tee -a "$LOG_FILE"
}

log_warn() {
    local msg="$1"
    echo -e "${YELLOW}[WARN]${NC} $msg" | tee -a "$LOG_FILE"
}

# Record latency measurement
record_latency() {
    local latency_ms="$1"
    LATENCIES+=("$latency_ms")
    TOTAL_LATENCY=$((TOTAL_LATENCY + latency_ms))

    if [[ $latency_ms -lt $MIN_LATENCY ]]; then
        MIN_LATENCY=$latency_ms
    fi
    if [[ $latency_ms -gt $MAX_LATENCY ]]; then
        MAX_LATENCY=$latency_ms
    fi
}

# Suppress verbose API logging during benchmark
quiet_api_post() {
    local endpoint="$1"
    local body="$2"
    local url="$API_BASE/$endpoint"

    local start_time=$(date +%s%N)

    local resp
    resp=$(curl -s -w "\n%{http_code}" \
        -H "Content-Type: application/json" \
        -X POST -d "$body" \
        "$url" 2>/dev/null)

    local end_time=$(date +%s%N)
    local latency_ms=$(((end_time - start_time) / 1000000))

    local http_code
    http_code=$(echo "$resp" | tail -n 1)
    local body_out
    body_out=$(echo "$resp" | sed '$d')

    echo "$http_code|$latency_ms|$body_out"
}

###############################################################################
# Task Creation Functions
###############################################################################

# Create an Echo task
create_echo_task() {
    local task_id="$1"
    local timestamp=$(iso_now)

    cat <<EOF
{
  "taskId": $task_id,
  "name": "BenchmarkEcho-$task_id",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"greeting\\":\\"benchmark\\",\\"idx\\":$task_id}",
  "dealer": "benchmark-test",
  "requestDt": "$timestamp",
  "reportResult": false
}
EOF
}

# Create a Shell task
create_shell_task() {
    local task_id="$1"
    local timestamp=$(iso_now)

    cat <<EOF
{
  "taskId": $task_id,
  "name": "BenchmarkShell-$task_id",
  "taskType": "SHELL_SCRIPT",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution",
  "input": "{\\"command\\":\\"echo 'Task $task_id'\\"}",
  "dealer": "benchmark-test",
  "requestDt": "$timestamp",
  "reportResult": false
}
EOF
}

# Create an IO task
create_io_task() {
    local task_id="$1"
    local timestamp=$(iso_now)

    cat <<EOF
{
  "taskId": $task_id,
  "name": "BenchmarkIO-$task_id",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"message\\":\\"IO task $task_id\\"}",
  "dealer": "benchmark-test",
  "requestDt": "$timestamp",
  "reportResult": false
}
EOF
}

# Create a CPU task
create_cpu_task() {
    local task_id="$1"
    local timestamp=$(iso_now)

    cat <<EOF
{
  "taskId": $task_id,
  "name": "BenchmarkCPU-$task_id",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"greeting\\":\\"cpu-bound-$task_id\\"}",
  "dealer": "benchmark-test",
  "requestDt": "$timestamp",
  "reportResult": false
}
EOF
}

# Create task based on type
create_task() {
    local task_id="$1"
    case "$TASK_TYPE" in
        echo)
            create_echo_task "$task_id"
            ;;
        shell)
            create_shell_task "$task_id"
            ;;
        io)
            create_io_task "$task_id"
            ;;
        cpu)
            create_cpu_task "$task_id"
            ;;
        *)
            create_echo_task "$task_id"
            ;;
    esac
}

###############################################################################
# Benchmark Test Functions
###############################################################################

# Test 1: Sequential submission
benchmark_sequential() {
    log_info "Test 1: Sequential Task Submission (${TARGET_TASKS} tasks)"

    local base_task_id=100000

    for i in $(seq 1 "$TARGET_TASKS"); do
        local task_id=$((base_task_id + i))
        local body=$(create_task "$task_id")

        local response=$(quiet_api_post "submit" "$body")
        local http_code=$(echo "$response" | cut -d'|' -f1)
        local latency_ms=$(echo "$response" | cut -d'|' -f2)

        if [[ "$http_code" == "202" ]] || [[ "$http_code" == "200" ]]; then
            ((TOTAL_SUCCESS++))
            record_latency "$latency_ms"
            echo "$task_id,$(date +%s%N),success,$latency_ms" >> "$METRICS_FILE"
        else
            ((TOTAL_FAILED++))
            echo "$task_id,$(date +%s%N),failed,$latency_ms" >> "$METRICS_FILE"
        fi

        ((TOTAL_SUBMITTED++))

        # Progress indicator
        if [[ $((i % 10)) -eq 0 ]]; then
            printf "."
        fi
    done
    echo ""

    log_success "Sequential submission completed: Success=$TOTAL_SUCCESS, Failed=$TOTAL_FAILED"
}

# Test 2: Batch submission
benchmark_batch() {
    log_info "Test 2: Batch Task Submission (batch size=$BATCH_SIZE)"

    local base_task_id=200000
    local batch_num=1

    for batch in $(seq 0 $((TARGET_TASKS / BATCH_SIZE - 1))); do
        local batch_start=$((base_task_id + batch * BATCH_SIZE))
        local batch_success=0
        local batch_failed=0

        # Submit batch in parallel
        for j in $(seq 0 $((BATCH_SIZE - 1))); do
            local task_id=$((batch_start + j))
            local body=$(create_task "$task_id")

            (
                local response=$(quiet_api_post "submit" "$body")
                local http_code=$(echo "$response" | cut -d'|' -f1)
                local latency_ms=$(echo "$response" | cut -d'|' -f2)

                if [[ "$http_code" == "202" ]] || [[ "$http_code" == "200" ]]; then
                    echo "$task_id,$(date +%s%N),success,$latency_ms" >> "$METRICS_FILE"
                else
                    echo "$task_id,$(date +%s%N),failed,$latency_ms" >> "$METRICS_FILE"
                fi
            ) &
        done

        # Wait for batch to complete
        wait

        ((batch_num++))
        printf "."
    done
    echo ""

    log_success "Batch submission completed"
}

# Test 3: Concurrent submission
benchmark_concurrent() {
    log_info "Test 3: Concurrent Task Submission (${THREAD_COUNT} threads, ${TARGET_TASKS} tasks)"

    local base_task_id=300000
    local tasks_per_thread=$((TARGET_TASKS / THREAD_COUNT))

    for t in $(seq 1 "$THREAD_COUNT"); do
        (
            for j in $(seq 1 "$tasks_per_thread"); do
                local task_id=$((base_task_id + (t - 1) * tasks_per_thread + j))
                local body=$(create_task "$task_id")

                local response=$(quiet_api_post "submit" "$body")
                local http_code=$(echo "$response" | cut -d'|' -f1)
                local latency_ms=$(echo "$response" | cut -d'|' -f2)

                if [[ "$http_code" == "202" ]] || [[ "$http_code" == "200" ]]; then
                    echo "$task_id,$(date +%s%N),success,$latency_ms" >> "$METRICS_FILE"
                else
                    echo "$task_id,$(date +%s%N),failed,$latency_ms" >> "$METRICS_FILE"
                fi
            done
        ) &
    done

    # Wait for all threads
    wait

    log_success "Concurrent submission completed"
}

# Test 4: Mixed workload
benchmark_mixed() {
    log_info "Test 4: Mixed Workload (submit + health checks)"

    local base_task_id=400000
    local half_tasks=$((TARGET_TASKS / 2))

    for i in $(seq 1 "$half_tasks"); do
        local task_id=$((base_task_id + i))
        local body=$(create_task "$task_id")

        local response=$(quiet_api_post "submit" "$body")
        local http_code=$(echo "$response" | cut -d'|' -f1)
        local latency_ms=$(echo "$response" | cut -d'|' -f2)

        if [[ "$http_code" == "202" ]] || [[ "$http_code" == "200" ]]; then
            echo "$task_id,$(date +%s%N),success,$latency_ms" >> "$METRICS_FILE"
        else
            echo "$task_id,$(date +%s%N),failed,$latency_ms" >> "$METRICS_FILE"
        fi

        # Perform health check every 10 tasks
        if [[ $((i % 10)) -eq 0 ]]; then
            curl -s "$API_BASE/health" > /dev/null 2>&1
        fi

        printf "."
    done
    echo ""

    log_success "Mixed workload completed"
}

###############################################################################
# Latency Analysis Functions
###############################################################################

# Calculate percentile
calculate_percentile() {
    local percentile="$1"
    local array_size=${#LATENCIES[@]}

    if [[ $array_size -eq 0 ]]; then
        echo "0"
        return
    fi

    local index=$(awk "BEGIN {printf \"%.0f\", ($percentile / 100) * $array_size}")
    index=$((index - 1))

    if [[ $index -lt 0 ]]; then
        index=0
    fi
    if [[ $index -ge $array_size ]]; then
        index=$((array_size - 1))
    fi

    # Sort array and get value at index (simple approach)
    echo "${LATENCIES[$index]}"
}

# Analyze latency distributions
analyze_latencies() {
    log_info "Analyzing latency data..."

    if [[ ${#LATENCIES[@]} -eq 0 ]]; then
        log_warn "No latency data collected"
        return
    fi

    # Sort latencies
    IFS=$'\n' read -r -d '' -a SORTED_LATENCIES < <(printf '%s\0' "${LATENCIES[@]}" | sort -zn) || true

    local count=${#SORTED_LATENCIES[@]}

    if [[ $count -gt 0 ]]; then
        local avg=$((TOTAL_LATENCY / count))

        # Calculate percentiles (simple approach)
        local p50_idx=$((count / 2))
        local p95_idx=$((count * 95 / 100))
        local p99_idx=$((count * 99 / 100))

        [[ $p50_idx ->= $count ]] && p50_idx=$((count - 1))
        [[ $p95_idx -ge $count ]] && p95_idx=$((count - 1))
        [[ $p99_idx -ge $count ]] && p99_idx=$((count - 1))

        local p50=${SORTED_LATENCIES[$p50_idx]:-0}
        local p95=${SORTED_LATENCIES[$p95_idx]:-0}
        local p99=${SORTED_LATENCIES[$p99_idx]:-0}

        echo "  Count:     $count"
        echo "  Min:       ${MIN_LATENCY}ms"
        echo "  Max:       ${MAX_LATENCY}ms"
        echo "  Average:   ${avg}ms"
        echo "  P50:       ${p50}ms"
        echo "  P95:       ${p95}ms"
        echo "  P99:       ${p99}ms"

        return
    fi
}

###############################################################################
# Report Generation
###############################################################################

generate_html_report() {
    local html_file="$RESULTS_DIR/benchmark_report.html"
    local benchmark_duration=$(($(date +%s%N) - BENCHMARK_START_TIME))
    benchmark_duration=$((benchmark_duration / 1000000))

    local throughput=$(awk "BEGIN {printf \"%.2f\", ($TOTAL_SUBMITTED * 1000.0) / $benchmark_duration}")
    local success_rate=$(awk "BEGIN {printf \"%.2f\", ($TOTAL_SUCCESS * 100.0) / ($TOTAL_SUBMITTED > 0 ? $TOTAL_SUBMITTED : 1)}")

    cat > "$html_file" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>DAG Agent Benchmark Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .summary { background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .metric { margin: 10px 0; }
        .metric label { font-weight: bold; width: 150px; display: inline-block; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background: #4CAF50; color: white; }
        .good { color: green; }
        .warning { color: orange; }
        .bad { color: red; }
    </style>
</head>
<body>
    <h1>DAG Agent Benchmark Report</h1>
    <p>Generated: <script>document.write(new Date().toLocaleString());</script></p>

    <div class="summary">
        <h2>Test Configuration</h2>
        <div class="metric"><label>Agent URL:</label> AGENT_URL</div>
        <div class="metric"><label>Task Type:</label> TASK_TYPE</div>
        <div class="metric"><label>Threads:</label> THREAD_COUNT</div>
        <div class="metric"><label>Target Tasks:</label> TARGET_TASKS</div>
        <div class="metric"><label>Batch Size:</label> BATCH_SIZE</div>
    </div>

    <div class="summary">
        <h2>Overall Results</h2>
        <div class="metric"><label>Total Submitted:</label> <span class="good">TOTAL_SUBMITTED</span></div>
        <div class="metric"><label>Total Success:</label> <span class="good">TOTAL_SUCCESS</span></div>
        <div class="metric"><label>Total Failed:</label> <span>TOTAL_FAILED</span></div>
        <div class="metric"><label>Success Rate:</label> <span>SUCCESS_RATE%</span></div>
        <div class="metric"><label>Duration:</label> BENCHMARK_DURATION ms</div>
        <div class="metric"><label>Throughput:</label> <span class="good">THROUGHPUT tasks/sec</span></div>
    </div>

    <div class="summary">
        <h2>Latency Statistics (ms)</h2>
        <div class="metric"><label>Min:</label> MIN_LATENCY</div>
        <div class="metric"><label>Max:</label> MAX_LATENCY</div>
        <div class="metric"><label>Average:</label> AVG_LATENCY</div>
        <div class="metric"><label>P50:</label> P50_LATENCY</div>
        <div class="metric"><label>P95:</label> P95_LATENCY</div>
        <div class="metric"><label>P99:</label> P99_LATENCY</div>
    </div>
</body>
</html>
EOF

    log_success "HTML report generated: $html_file"
}

print_summary() {
    local benchmark_duration=$(($(date +%s%N) - BENCHMARK_START_TIME))
    benchmark_duration=$((benchmark_duration / 1000000))

    local throughput=$(awk "BEGIN {printf \"%.2f\", ($TOTAL_SUBMITTED * 1000.0) / ($benchmark_duration > 0 ? $benchmark_duration : 1)}")
    local success_rate=$(awk "BEGIN {printf \"%.2f\", ($TOTAL_SUCCESS * 100.0) / ($TOTAL_SUBMITTED > 0 ? $TOTAL_SUBMITTED : 1)}")
    local avg_latency=0

    if [[ ${#LATENCIES[@]} -gt 0 ]]; then
        avg_latency=$((TOTAL_LATENCY / ${#LATENCIES[@]}))
    fi

    echo ""
    echo "========================================"
    echo "Benchmark Summary Report"
    echo "========================================"
    echo ""
    echo "Test Configuration:"
    echo "  Agent URL:       $BASE_URL"
    echo "  Task Type:       $TASK_TYPE"
    echo "  Threads:         $THREAD_COUNT"
    echo "  Target Tasks:    $TARGET_TASKS"
    echo "  Batch Size:      $BATCH_SIZE"
    echo ""
    echo "Overall Results:"
    printf "  Total Submitted: %s\n" "$TOTAL_SUBMITTED"
    printf "  Total Success:   ${GREEN}%s${NC}\n" "$TOTAL_SUCCESS"
    printf "  Total Failed:    ${RED}%s${NC}\n" "$TOTAL_FAILED"
    printf "  Success Rate:    %s%%\n" "$success_rate"
    printf "  Duration:        %s ms\n" "$benchmark_duration"
    printf "  Throughput:      ${GREEN}%s tasks/sec${NC}\n" "$throughput"
    echo ""
    echo "Latency Statistics (ms):"
    analyze_latencies | sed 's/^/  /'
    echo ""
    echo "========================================"
    echo "Results saved to: $RESULTS_DIR"
    echo "========================================"
}

###############################################################################
# Main Execution
###############################################################################

main() {
    log_info "Starting DAG Agent Benchmark Test Suite"
    log_info "Configuration: URL=$BASE_URL, Threads=$THREAD_COUNT, Tasks=$TARGET_TASKS"

    # Verify server connectivity
    if ! ping_server; then
        log_error "Failed to connect to agent at $BASE_URL"
        exit 1
    fi

    log_success "Agent server is reachable"
    echo ""

    # Run benchmark tests
    benchmark_sequential
    echo ""

    benchmark_batch
    echo ""

    benchmark_concurrent
    echo ""

    benchmark_mixed
    echo ""

    # Generate reports
    print_summary | tee -a "$LOG_FILE"
    generate_html_report

    log_success "Benchmark test completed successfully"
}

# Run main function
main

