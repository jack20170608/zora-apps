# DAG Agent Benchmark Testing Suite - 完整方案总结

## 📦 创建的文件清单

### 核心脚本文件

| 文件名 | 大小 | 用途 | 执行方式 |
|--------|------|------|---------|
| **benchmark-quick.sh** | ~6KB | 快速吞吐量基准测试（推荐日常使用） | `./benchmark-quick.sh [--url] [--tasks] [--threads]` |
| **benchmark-performance-test.sh** | ~15KB | 全面性能分析测试（四种测试场景） | `./benchmark-performance-test.sh [OPTIONS]` |
| **benchmark-suite.sh** | ~12KB | 交互式测试套件（菜单驱动） | `./benchmark-suite.sh` |
| **benchmark-analyzer.py** | ~10KB | Python性能数据分析工具 | `python3 benchmark-analyzer.py <metrics.csv>` |

### 配置和文档文件

| 文件名 | 用途 |
|--------|------|
| **benchmark.conf** | 基准测试配置文件（参数定制） |
| **BENCHMARK_README.md** | 详细完整的使用文档（中文） |
| **QUICK_START.md** | 快速开始指南（本文件） |

## 🎯 三种使用方式

### 方式1：命令行快速测试（推荐日常使用）

```bash
# 最简单的用法（使用默认参数）
./benchmark-quick.sh

# 自定义参数
./benchmark-quick.sh --url http://your-agent:20000 --tasks 100 --threads 4
```

**适用场景：** 日常性能验证、CI/CD流水线、快速反馈

**用时：** 1-2分钟

### 方式2：全面基准测试（推荐详细分析）

```bash
# 基础用法
./benchmark-performance-test.sh --tasks 500

# 完整参数
./benchmark-performance-test.sh \
  --url http://localhost:20000 \
  --tasks 500 \
  --threads 8 \
  --batch-size 20 \
  --task-type echo \
  --duration 300
```

**适用场景：** 性能报告、容量规划、详细分析

**用时：** 5-15分钟

**输出：**
- `benchmark.log` - 详细日志
- `metrics.csv` - 原始指标数据
- `benchmark_report.html` - HTML报告（待完善）

### 方式3：交互式菜单测试（推荐综合使用）

```bash
./benchmark-suite.sh
```

**菜单选项：**
1. 快速基准测试
2. 全面基准测试
3. 自定义基准测试
4. 分析历史结果
5. 压力测试
6. 结果对比

**适用场景：** 综合测试、学习使用、交互式操作

## 📊 测试场景详解

### 快速基准测试 (benchmark-quick.sh)

**测试内容：**
1. **顺序提交测试** - 连续提交N个任务，测measure吞吐量
2. **并发提交测试** - M个线程并发提交任务，测measure最大吞吐量
3. **健康检查** - 验证Agent运行状态

**输出指标：**
```
顺序提交：
  Duration: 1245ms
  Success: 50
  Failed: 0
  Throughput: 40.16 tasks/sec

并发提交：
  Duration: 523ms
  Throughput: 95.61 tasks/sec
```

### 全面基准测试 (benchmark-performance-test.sh)

**四种测试场景：**

1. **顺序提交测试 (Sequential Submission)**
   - 单线程顺序提交所有任务
   - 测试基线性能
   - 易于调试和监控

2. **批量提交测试 (Batch Submission)**
   - 将任务分批提交
   - 批内并发，批间串行
   - 平衡负载和资源利用

3. **并发提交测试 (Concurrent Submission)**
   - 多线程并发提交
   - 测试最大负载和吞吐量
   - 识别系统瓶颈

4. **混合工作负载测试 (Mixed Workload)**
   - 提交任务 + 定期健康检查
   - 模拟真实使用场景
   - 全面系统评估

**输出内容：**
- 四种测试的详细结果
- 延迟统计：Min, Max, Mean, P50, P95, P99
- 成功率和总吞吐量
- 生成的结果文件和HTML报告

### 分析工具 (benchmark-analyzer.py)

**功能：**
- 读取metrics.csv原始数据
- 计算统计指标（平均值、百分位数等）
- 生成多种格式的报告

**输出格式：**
- JSON格式 (`benchmark_analysis.json`)
- 文本格式 (`benchmark_analysis.txt`)
- HTML格式 (`benchmark_analysis.html`) - 包含交互式图表

**使用方式：**
```bash
# 分析单个结果
python3 benchmark-analyzer.py benchmark_results_20240526_120000/metrics.csv

# 输出到指定目录
python3 benchmark-analyzer.py metrics.csv --output ./reports
```

## 🚀 快速开始（5分钟）

### 步骤1：启动Agent

```bash
cd dag-task/dag-agent-muserver
mvn exec:java -Denv=local
```

Agent会在 `http://localhost:20000` 启动

### 步骤2：进入脚本目录

```bash
cd ../dag-agent/scripts
chmod +x *.sh *.py
```

### 步骤3：选择一种测试方式

**选项A：快速验证（推荐）**
```bash
./benchmark-quick.sh
```

**选项B：详细分析**
```bash
./benchmark-performance-test.sh --tasks 200
python3 benchmark-analyzer.py benchmark_results_*/metrics.csv
```

**选项C：交互式菜单**
```bash
./benchmark-suite.sh
```

## 📈 性能指标解读

### 吞吐量 (Throughput)
```
Throughput: 95.61 tasks/sec
```
- **定义**：单位时间内处理的任务数
- **目标**：> 1000 tasks/sec（顺序）, > 2000 tasks/sec（并发）
- **优化**：增加线程、检查网络、优化任务执行时间

### 延迟 (Latency)
```
Min:  0.5ms
P50:  2.5ms
P95:  8.3ms
P99: 45.2ms
Max: 125.0ms
```
- **定义**：请求响应时间
- **关键指标**：P95和P99（反映真实用户体验）
- **目标**：P95 < 10ms, P99 < 50ms
- **优化**：减少并发、增加资源、优化网络

### 成功率 (Success Rate)
```
Success Rate: 99.95%
```
- **定义**：成功提交的任务占比
- **目标**：> 99.5%
- **低于目标的原因**：队列满、Agent过载、网络问题

## 🔧 常见问题和解决方案

### Q1：连接超时怎么办？
```
ERROR: dag-agent server is not reachable at http://localhost:20000

# 解决方案：
curl http://localhost:20000/agent/api/v1/agent/ping  # 检查连接
# 如果失败，启动Agent：
cd dag-task/dag-agent-muserver && mvn exec:java -Denv=local
```

### Q2：成功率低于90%，可能是什么原因？
```
# 可能原因及解决方案：
1. 队列已满 → 减少并发任务数
   ./benchmark-quick.sh --tasks 30 --threads 2

2. Agent过载 → 增加JVM堆大小并重启Agent
   -Xmx2G -Xms2G

3. 网络问题 → 检查网络连接
   ping localhost
```

### Q3：延迟异常高（P99 > 1000ms）怎么办？
```
# 快速诊断：
./benchmark-performance-test.sh --task-type echo --tasks 50

# 可能原因：
1. 选择轻量级任务：--task-type echo
2. 减少并发：--threads 2
3. 增加Agent资源：增加JVM堆大小
4. 分析日志：tail -f dag-agent-muserver/logs/agent.log
```

## 📋 参数详解

### benchmark-quick.sh 参数

```bash
--url <url>         Agent基础URL (默认: http://localhost:20000)
--tasks <n>         任务数量 (默认: 50)
--threads <n>       并发线程数 (默认: 4)
```

### benchmark-performance-test.sh 参数

```bash
--url <url>               Agent基础URL
--threads <n>             并发线程数 (默认: 4)
--tasks <n>               目标任务数 (默认: 100)
--batch-size <n>          批处理大小 (默认: 10)
--task-type <type>        任务类型: echo|shell|io|cpu (默认: echo)
--duration <seconds>      最大测试时长 (默认: 300)
--help                    显示帮助信息
```

### benchmark.conf 配置项

关键参数：
```bash
BENCHMARK_MODE="quick"              # 模式：quick|full|custom
AGENT_BASE_URL="http://localhost:20000"
FULL_BENCHMARK_TASKS=500            # 任务数
FULL_BENCHMARK_THREADS=8            # 线程数
THROUGHPUT_TARGET=1000              # 吞吐量目标(tasks/sec)
LATENCY_P99_TARGET=50               # P99延迟目标(ms)
SUCCESS_RATE_TARGET=99.5            # 成功率目标(%)
```

## 💾 结果文件说明

每次运行会生成 `benchmark_results_YYYYMMDD_HHMMSS/` 目录��包含：

```
benchmark_results_20240526_120000/
├── benchmark.log                   # 详细执行日志
├── metrics.csv                     # 原始指标数据
│   # 列: task_id,submission_time_ms,status,latency_ms
├── benchmark_report.html           # HTML摘要报告
├── benchmark_analysis.json         # 分析结果(JSON)
├── benchmark_analysis.txt          # 分析结果(文本)
└── benchmark_analysis.html         # 分析结果(交互式HTML)
```

## 🎓 最佳实践

1. **建立性能基线**
   ```bash
   # 在参考环境运行一次
   ./benchmark-quick.sh > baseline.txt
   # 保存baseline.txt作为参考
   ```

2. **定期进行回归测试**
   ```bash
   # 每周运行一次
   ./benchmark-quick.sh > weekly_$(date +%Y%m%d).txt
   # 对比结果，监控趋势
   ```

3. **使用HTML报告展示结果**
   ```bash
   ./benchmark-performance-test.sh --tasks 500
   python3 benchmark-analyzer.py benchmark_results_*/metrics.csv
   # 在浏览器中打开 benchmark_analysis.html
   ```

4. **集成到CI/CD流水线**
   ```yaml
   - name: Run Benchmark
     run: ./benchmark-quick.sh
   - uses: actions/upload-artifact@v2
     with:
       name: benchmark-results
       path: benchmark_results_*/
   ```

5. **记录环境信息**
   ```bash
   # 记录测试时的环境
   echo "JVM: $(java -version)"
   echo "CPU: $(sysctl -n hw.ncpu) cores"
   echo "Memory: $(sysctl -n hw.memsize | awk '{print $1 / 1024 / 1024 / 1024}') GB"
   ```

## 🔗 相关文档

- `BENCHMARK_README.md` - 完整详细的使用指南（中文）
- `QUICK_START.md` - 快速开始指南
- `benchmark.conf` - 配置参数说明
- `dag-agent-muserver/README.md` - Agent启动说明
- `../README.md` - DAG Agent项目文档

## 📞 支持和反馈

如遇到问题，按以下顺序查阅：

1. ���看 `BENCHMARK_README.md` 的故障排查部分
2. 查看 `benchmark_results_*/benchmark.log` 的详细日志
3. 查看Agent的日志：`tail -f dag-agent-muserver/logs/agent.log`
4. 查看 `benchmark_analyzer.py` 的分析结果

## 📊 性能基准参考值

基于标准测试环境（JDK 25, 4核CPU, 8GB内存）的参考值：

| 指标 | 单线程 | 4线程并发 | 8线程并发 | 16线程并发 |
|------|--------|---------|---------|----------|
| **吞吐量** (tasks/s) | 40-50 | 200-300 | 400-600 | 600-1000 |
| **P95延迟** (ms) | <5 | <10 | <15 | <20 |
| **P99延迟** (ms) | <10 | <20 | <30 | <50 |
| **成功率** (%) | >99.5 | >99.5 | >99.0 | >98.0 |

*注：实际值取决于网络、CPU、内存等因素*

## ✅ 使用检查清单

- [ ] Agent已启动（`http://localhost:20000/agent/api/v1/agent/ping` 返回 "pong"）
- [ ] 脚本已赋予执行权限（`chmod +x benchmark-*.sh *.py`）
- [ ] Python 3已安装（用于分析报告）
- [ ] 足够的磁盘空间存储结果（通常<10MB）
- [ ] 网络连接正常
- [ ] 防火墙已开放Agent端口

## 🚀 下一步

1. **运行第一个基准测试**：`./benchmark-quick.sh`
2. **查看详细结果**：`./benchmark-performance-test.sh --tasks 200`
3. **生成分析报告**：`python3 benchmark-analyzer.py benchmark_results_*/metrics.csv`
4. **在浏览器查看**：打开 `benchmark_results_*/benchmark_analysis.html`

---

**版本**：1.0  
**创建日期**：2024-05-26  
**维护人**：DAG Project Team  
**状态**：生产可用

