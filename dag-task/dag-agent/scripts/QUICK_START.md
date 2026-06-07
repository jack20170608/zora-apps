# DAG Agent Benchmark Testing - Quick Start Guide

## 🚀 快速开始（30秒）

```bash
# 1. 进入脚本目录
cd dag-task/dag-agent/scripts

# 2. 使脚本可执行
chmod +x benchmark-*.sh *.py

# 3. 运行快速基准测试
./benchmark-quick.sh
```

## 📋 可用工具概览

| 工具 | 用途 | 用时 | 适用场景 |
|------|------|------|---------|
| **benchmark-quick.sh** | 快速吞吐量测试 | 1-2分钟 | 日常使用、快速反��� |
| **benchmark-performance-test.sh** | 全面性能分析 | 5-15分钟 | 详细分析、性能报告 |
| **benchmark-suite.sh** | 交互式菜单 | 可变 | 综合测试、交互执行 |
| **benchmark-analyzer.py** | 结果分析工具 | <1分钟 | 数据分析、生成报告 |
| **benchmark.conf** | 配置文件 | - | 参数定制 |

## 🎯 常用场景

### 场景1：日常快速性能验证

```bash
./benchmark-quick.sh
```

**输出内容：**
- 顺序提交吞吐量
- 并发提交吞吐量
- Agent健康状态

### 场景2：详细的性能分析报告

```bash
# 运行全面基准测试
./benchmark-performance-test.sh --tasks 500 --threads 8

# 自动分析结果
python3 benchmark-analyzer.py benchmark_results_*/metrics.csv
```

**生成文件：**
- `benchmark.log` - 详细日志
- `metrics.csv` - 原始数据
- `benchmark_report.html` - 交互式HTML报告
- `benchmark_analysis.json` - 结构化数据
- `benchmark_analysis.txt` - 文本报告

### 场景3：压力测试（找出系统极限）

```bash
./benchmark-performance-test.sh \
  --tasks 2000 \
  --threads 16 \
  --batch-size 50
```

### 场景4：特定Task类型的性能评估

```bash
# Shell脚本任务
./benchmark-performance-test.sh --task-type shell --tasks 100

# IO密集任务
./benchmark-performance-test.sh --task-type io --tasks 100

# CPU密集任务
./benchmark-performance-test.sh --task-type cpu --tasks 50
```

### 场景5：交互式菜单进行综合测试

```bash
./benchmark-suite.sh
```

选择各种选项进行交互式测试。

## 📊 理解性能指标

### 吞吐量 (Throughput)
```
顺序提交吞吐量: 40.16 tasks/sec
并发提交吞吐量: 95.61 tasks/sec
```
- **高吞吐量** = 系统能处理更多任务
- **目标** > 1000 tasks/sec

### 延迟 (Latency)
```
P50:  2.5ms   # 50%的请求在2.5ms内完成
P95:  8.3ms   # 95%的请求在8.3ms内完成
P99: 45.2ms   # 99%的请求在45.2ms内完成
```
- **低延迟** = 用户体验更好
- **P95** 是关键指标（实际用户体验）
- **目标** P95 < 10ms, P99 < 50ms

### 成功率 (Success Rate)
```
Success Rate: 99.95%
```
- **高成功率** = 系统稳定可靠
- **目标** > 99.5%

## 🔧 自定义测试

### 使用命令行参数

```bash
./benchmark-performance-test.sh \
  --url http://your-agent:20000 \
  --tasks 500 \          # 总任务数
  --threads 8 \          # 并发线程数
  --batch-size 20 \      # 批处理大小
  --task-type echo \     # 任务类型
  --duration 300         # 最大测试时长（秒）
```

### 编辑配置文件

```bash
# 编辑 benchmark.conf
nano benchmark.conf

# 修改以下重要参数：
FULL_BENCHMARK_TASKS=500          # 任务数
FULL_BENCHMARK_THREADS=8          # 线程数
THROUGHPUT_TARGET=1000            # 吞吐量目标
LATENCY_P99_TARGET=50             # P99延迟目标
```

## 📈 分析和报告

### 生成HTML报告（推荐）

```bash
# 找到最新的metrics.csv
ls -lt benchmark_results_*/metrics.csv | head -1

# 生成分析报告
python3 benchmark-analyzer.py benchmark_results_YYYYMMDD_HHMMSS/metrics.csv

# 用浏览器打开 benchmark_analysis.html
open benchmark_results_YYYYMMDD_HHMMSS/benchmark_analysis.html
```

### 查看文本报告

```bash
cat benchmark_results_YYYYMMDD_HHMMSS/benchmark_analysis.txt
```

### 转换为JSON格式

```bash
cat benchmark_results_YYYYMMDD_HHMMSS/benchmark_analysis.json | jq .
```

## 🐛 故障排查

### 问题：连接超时

```
ERROR: dag-agent server is not reachable at http://localhost:20000
```

**解决方法：**
```bash
# 1. 验证Agent是否运行
curl http://localhost:20000/agent/api/v1/agent/ping

# 2. 如果未运行，启动Agent
cd dag-task/dag-agent-muserver
mvn exec:java -Denv=local
```

### 问题：高失败率

```
Total Failed: 50
Success Rate: 50%
```

**可能原因：**
- Agent队列已满
- Agent过载
- 网络问题

**解决方案：**
```bash
# 减少并发
./benchmark-quick.sh --threads 2

# 减少任务数
./benchmark-quick.sh --tasks 30

# 增加超时时间
export DAG_AGENT_TIMEOUT=30
./benchmark-quick.sh
```

### 问题：延迟异常高

```
P99: 5000ms (期望 < 50ms)
```

**优化方法：**
```bash
# 选择轻量级任务
./benchmark-performance-test.sh --task-type echo

# 减少并发
./benchmark-performance-test.sh --threads 2

# 增加Agent资源（修改JVM配置后重启）
```

## 📚 文件说明

```
dag-task/dag-agent/scripts/
├── benchmark-quick.sh              # 快速基准测试
├── benchmark-performance-test.sh    # 全面基准测试
├── benchmark-suite.sh               # 交互式测试套件
├── benchmark-analyzer.py            # 结果分析工具
├── benchmark.conf                   # 配置文件
├── BENCHMARK_README.md              # 详细文档（本文件）
├── QUICK_START.md                   # 本快速开始指南
├── run-api.sh                       # 通用API函数库（依赖）
└── benchmark_results_*/             # 生成的结果目录
    ├── benchmark.log
    ├── metrics.csv
    ├── benchmark_report.html
    ├── benchmark_analysis.json
    └── benchmark_analysis.txt
```

## 🎓 进阶使用

### 编写自定义测试脚本

```bash
#!/bin/bash
source "$(dirname "$0")/run-api.sh"

# 使用API函数
api_post "submit" '{
  "taskId": 1,
  "name": "CustomTask",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{}",
  "dealer": "custom"
}'
```

### 集成到CI/CD

```yaml
# .github/workflows/benchmark.yml
name: Benchmark
on: [push]
jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - run: cd dag-task/dag-agent/scripts && chmod +x benchmark-*.sh
      - run: ./benchmark-quick.sh
      - uses: actions/upload-artifact@v2
        with:
          name: benchmark-results
          path: benchmark_results_*/
```

### 长期监控

```bash
#!/bin/bash
# benchmark-monitor.sh - 持续监控性能

while true; do
    echo "=== Benchmark Run at $(date) ==="
    ./benchmark-quick.sh >> benchmark_monitor.log 2>&1
    
    # 每小时运行一次
    sleep 3600
done
```

## 📞 获取帮助

### 查看更多文档

```bash
# 全面使用指南
cat BENCHMARK_README.md

# 查看脚本帮助
./benchmark-performance-test.sh --help
```

### 查看脚本源码

```bash
# 查看快速基准测试的���现
less benchmark-quick.sh

# 查看分析工具的源码
less benchmark-analyzer.py
```

## 💡 最佳实践

1. **定期进行基准测试** - 建立性能基线
2. **记录环境信息** - CPU、内存、JVM配置等
3. **使用一致的测试条件** - 便于对比结果
4. **生成HTML报告** - 便于分享和展示
5. **监控趋势变化** - 发现性能退化
6. **从小到大测试** - 逐步增加负载
7. **保留历史数据** - 便于长期分析

## 🚦 性能目标概览

| 指标 | 目标 | 优秀 | 警告 | 临界 |
|------|------|------|------|------|
| **吞吐量** (tasks/s) | >1000 | >1500 | 500-1000 | <500 |
| **P95延迟** (ms) | <10 | <5 | 10-20 | >20 |
| **P99延迟** (ms) | <50 | <20 | 50-100 | >100 |
| **成功率** (%) | >99.5 | >99.9 | 99-99.5 | <99 |

## 📝 示例工作流

```bash
# 1. 启动Agent
cd dag-task/dag-agent-muserver
mvn exec:java -Denv=local &

# 2. 等待Agent启动
sleep 10

# 3. 进入脚本目录
cd ../dag-agent/scripts

# 4. 运行快速基准测试获取基线
./benchmark-quick.sh

# 5. 运行全面测试获取详细数据
./benchmark-performance-test.sh --tasks 500 --threads 8

# 6. 分析结果
python3 benchmark-analyzer.py benchmark_results_*/metrics.csv

# 7. 在浏览器中查看报告
open benchmark_results_*/benchmark_analysis.html
```

---

**版本**: 1.0  
**最后更新**: 2024-05-26  
**维护**: DAG Project Team

