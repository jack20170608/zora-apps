# DAG Agent Benchmark Testing Guide

## Overview

DAG Agent benchmark testing是一套用于评估dag-agent性能的工具和脚本。它包括：

- **benchmark-quick.sh** - 快速基准测试（推荐日常使用）
- **benchmark-performance-test.sh** - 全面基准测试（详细分析）
- **benchmark.conf** - 配置文件（参数设置）

## Quick Start

### 1. 启动DAG Agent

```bash
cd dag-task/dag-agent-muserver
mvn exec:java -Denv=local
```

Agent会在 `http://localhost:20000` 启动

### 2. 运行快速基准测试

```bash
cd dag-task/dag-agent/scripts
chmod +x benchmark-*.sh
./benchmark-quick.sh
```

输出示例：
```
========================================
DAG Agent Quick Benchmark Test
========================================
Agent URL:     http://localhost:20000
Tasks Count:   50
Thread Count:  4

[1] Testing Sequential Submission Throughput...
Duration:   1245ms
Success:    50
Failed:     0
Throughput: 40.16 tasks/sec

[2] Testing Concurrent Submission...
Duration:   523ms
Throughput: 95.61 tasks/sec

[3] Checking Agent Health...
========================================
Quick Benchmark Test Completed!
========================================
```

## Scripts Usage

### 快速基准测试 (benchmark-quick.sh)

用途：快速验证agent性能，获得基本的吞吐量和并发metrics。

用时：1-2分钟

```bash
# 基础用法（使用默认参数）
./benchmark-quick.sh

# 指定agent URL
./benchmark-quick.sh --url http://your-agent:20000

# 指定任务数和线程数
./benchmark-quick.sh --tasks 100 --threads 8

# 完整示例
./benchmark-quick.sh --url http://localhost:20000 --tasks 100 --threads 4
```

**输出内容：**
- 顺序提交吞吐量（tasks/sec）
- 并发提交吞吐量
- Agent健康状态

### 全面基准测试 (benchmark-performance-test.sh)

用途：深度性能分析，包含多种测试场景和详细的延迟分析。

用时：5-15分钟（取决于任务数）

```bash
./benchmark-performance-test.sh [OPTIONS]

Options:
  --url <url>              Agent base URL (default: http://localhost:20000)
  --threads <n>            Number of concurrent threads (default: 4)
  --tasks <n>              Target number of tasks (default: 100)
  --batch-size <n>         Batch size for parallel submission (default: 10)
  --task-type <type>       Task type: echo|shell|io|cpu (default: echo)
  --duration <seconds>     Benchmark duration limit (default: 300)
  --help                   Show help message
```

**示例：**

```bash
# 快速测试（100个任务）
./benchmark-performance-test.sh --url http://localhost:20000 --tasks 100

# 压力测试（1000个任务，8个线程）
./benchmark-performance-test.sh --tasks 1000 --threads 8

# 特定任务类型测试
./benchmark-performance-test.sh --task-type shell --tasks 500

# 完整的并发测试
./benchmark-performance-test.sh --url http://localhost:20000 \
    --tasks 500 --threads 16 --batch-size 20
```

**输出内容：**
- 测试配置信息
- 四种测试场景的结果：
  1. 顺序提交（Sequential Submission）
  2. 批量提交（Batch Submission）
  3. 并发提交（Concurrent Submission）
  4. 混合工作负载（Mixed Workload）
- 详细的延迟统计：Min, Max, Average, P50, P95, P99
- 成功率和总吞吐量
- 生成的结果文件和HTML报告

**结果文件：**

所有结果保存在 `benchmark_results_YYYYMMDD_HHMMSS/` 目录：
- `benchmark.log` - 详细日志
- `metrics.csv` - 原始metricsCSV格式，包含每个任务的提交时间和延迟
- `benchmark_report.html` - HTML格式的性能报告（可在浏览器中查看）

## Configuration File (benchmark.conf)

配置文件位于 `dag-task/dag-agent/scripts/benchmark.conf`，定义了：

```bash
# 基准测试模式
BENCHMARK_MODE="quick"  # quick|full|custom

# 快速测试参数
QUICK_BENCHMARK_TASKS=50
QUICK_BENCHMARK_THREADS=4

# 全面测试参数
FULL_BENCHMARK_TASKS=500
FULL_BENCHMARK_THREADS=8

# Task参数
BENCHMARK_TASK_TYPE="echo"  # echo|shell|io|cpu
TASK_TIMEOUT_MS=5000

# 性能目标
THROUGHPUT_TARGET=1000           # tasks/second
LATENCY_P99_TARGET=50            # milliseconds
SUCCESS_RATE_TARGET=99.5         # percentage

# 输出配置
SAVE_METRICS=true
GENERATE_HTML_REPORT=true
OUTPUT_DIRECTORY="benchmark_results"
```

## Performance Metrics Interpretation

### Throughput (吞吐量)

**定义：** 单位时间内成功提交的任务数（tasks/second）

**目标值：**
- 优秀: > 1000 tasks/sec
- 良好: 800-1000 tasks/sec
- 需要优化: < 800 tasks/sec

**如何改进：**
- 增加并发线程数
- 检查网络延迟
- 调整Agent的`maxConcurrentTasks`配置
- 检查任务执行器的实现

### Latency (延迟)

**定义：** 从提交请求到收到响应所耗费的时间（毫秒）

**重要百分位数：**
- **P50 (中位数)** - 50%的请求在此时间内完成
- **P95** - 95%的请求在此时间内完成（关键用户体验指标）
- **P99** - 99%的请求在此时间内完成（最坏情况）

**目标值：**
- P50: < 2ms
- P95: < 10ms
- P99: < 50ms

**如何改进：**
- 网络优化（减少网络往返时间）
- 减少并发数量以减轻Agent负载
- 增加Agent的资源分配
- 优化数据库查询（如果适用）

### Success Rate (成功率)

**定义：** 成功提交的任务占总提交任务的百分比

**目标值：**
- 优秀: > 99.5%
- 良好: 99-99.5%
- 需要调查: < 99%

**失败原因：**
- 队列已满（429错误）
- Agent不可用（500错误）
- 网络超时
- 请求格式错误（400错误）

## Test Scenarios

### Scenario 1: Sequential Submission（顺序提交）

**目的：** 基础吞吐量测试，单线程顺序提交

**特点：**
- 简单、可预测
- 易于调试
- 充分利用网络连接

**适用场景：**
- 基线性能测试
- 故障排查
- 单个Agent实例验证

### Scenario 2: Batch Submission（批量提交）

**目的：** 并行批处理吞吐量测试

**特点：**
- 在批内并发提交
- 批间串行处理
- 平衡负载和资源利用

**适用场景：**
- 生产环境的典型场景
- 资源受限环境
- 稳定性测试

### Scenario 3: Concurrent Submission（并发提交）

**目的：** 高并发场景下的性能测试

**特点：**
- 多线程并发提交
- 最大负载测试
- 识别性能瓶颈

**适用场景：**
- 高并发环境
- 容量规划
- 服务器压力测试

### Scenario 4: Mixed Workload（混合工作负载）

**目的：** 真实场景模拟

**特点：**
- 任务提交 + 健康检查
- 接近生产环境
- 全面系统评估

**适用场景：**
- 系统集成测试
- 生产环保验证
- 长期稳定性测试

## Advanced Usage

### 使用特定Task类型进行测试

```bash
# Echo任务（轻量级，快速完成）
./benchmark-performance-test.sh --task-type echo --tasks 100

# Shell任务（模拟实际的命令执行）
./benchmark-performance-test.sh --task-type shell --tasks 50

# IO任务（测试I/O密集操作）
./benchmark-performance-test.sh --task-type io --tasks 100

# CPU任务（测试CPU密集操作）
./benchmark-performance-test.sh --task-type cpu --tasks 50
```

### 压力测试（找出系统极限）

```bash
# 逐步增加负载
for tasks in 100 500 1000 5000; do
    echo "Testing with $tasks tasks..."
    ./benchmark-performance-test.sh --tasks $tasks --threads 8
    sleep 10
done
```

### 长期稳定性测试

```bash
# 持续运行基准测试
while true; do
    ./benchmark-quick.sh
    echo "Round completed at $(date)"
    sleep 60
done
```

### 比较不同配置

```bash
# 并发线程对性能的影响
for threads in 1 2 4 8 16; do
    echo "Testing with $threads threads..."
    ./benchmark-performance-test.sh --threads $threads --tasks 200
done
```

## Troubleshooting

### 问题：连接超时

```
ERROR: dag-agent server is not reachable at http://localhost:20000
```

**解决：**
1. 验证Agent是否在运行：`curl http://localhost:20000/agent/api/v1/agent/ping`
2. 检查防火墙/网络配置
3. 确认Agent监听地址和端口

### 问题：高失败率

```
Total Failed: 50 (50% failure rate)
```

**可能原因：**
1. 队列已满 - 减少并发或增加Agent的`maxPendingTasks`
2. Agent过载 - 减少Task数量或增加Agent资源
3. 网络问题 - 检查网络连接质量

**解决方法：**
```bash
# 减少并发
./benchmark-quick.sh --threads 2

# 减少任务数
./benchmark-quick.sh --tasks 30

# 增加Agent配置（在agent.conf中）
maxConcurrentTasks = 8
maxPendingTasks = 200
```

### 问题：延迟异常高

```
P99:       5000ms (期望 < 50ms)
```

**原因分析：**
1. Agent资源不足（CPU/内存）
2. 网络延迟
3. Task执行时间过长

**优化方法：**
- 增加JVM堆大小
- 减少并发任务数
- 优化Task实现
- 选择轻量级Task类型（echo）

### 问题：内存不足

```
java.lang.OutOfMemoryError: Java heap space
```

**解决：**
```bash
# 或在Agent启动时增加堆大小
java -Xmx2G -Xms2G ...

# 减少Queue大小
./benchmark-quick.sh --tasks 30
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Performance Benchmark
on: [push, pull_request]

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Setup JDK
        uses: actions/setup-java@v2
        with:
          java-version: '25'
      
      - name: Start Agent
        run: |
          cd dag-task/dag-agent-muserver
          mvn exec:java -Denv=local &
          sleep 10
      
      - name: Run Benchmark
        run: |
          cd dag-task/dag-agent/scripts
          chmod +x benchmark-quick.sh
          ./benchmark-quick.sh
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    stages {
        stage('Start Agent') {
            steps {
                sh '''
                    cd dag-task/dag-agent-muserver
                    mvn exec:java -Denv=local &
                    sleep 10
                '''
            }
        }
        stage('Benchmark') {
            steps {
                sh '''
                    cd dag-task/dag-agent/scripts
                    chmod +x benchmark-quick.sh
                    ./benchmark-quick.sh
                '''
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'benchmark_results/**'
        }
    }
}
```

## Performance Goals and Reference Values

基于标准测试环境（JDK 25, 4核CPU, 8GB内存）：

| 指标 | 期望值 | 优秀 | 警告 | 临界 |
|------|--------|------|------|------|
| 顺序提交吞吐量(tasks/s) | 1000+ | >1500 | 500-1000 | <500 |
| 并发提交吞吐量(tasks/s) | 2000+ | >3000 | 1000-2000 | <1000 |
| P50延迟(ms) | <2 | <1 | 2-5 | >5 |
| P95延迟(ms) | <10 | <5 | 10-20 | >20 |
| P99延迟(ms) | <50 | <20 | 50-100 | >100 |
| 成功率(%) | >99.5 | >99.9 | 99-99.5 | <99 |
| 内存使用(MB) | <500 | <300 | 500-750 | >750 |

## Related Documentation

- [DAG Agent Architecture](../../../docs/01-ARCHITECTURE-initial-setup.md)
- [DAG Agent HTTP API](../README.md)
- [Agent Configuration Guide](../src/main/resources/config/agent.conf)
- [Task Execution Guide](../docs/task-execution.md)

## Support and Issues

如遇到问题，请：
1. 查看 `benchmark.log` 中的详细日志
2. 运行 `./benchmark-quick.sh` 进行快速诊断
3. 检查Agent日志：`tail -f dag-agent-muserver/logs/agent.log`
4. 提交issue时包含benchmark结果和Agent日志

