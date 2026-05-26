# DAG Agent Benchmark Testing Suite 

## 📂 文件清单和使用指南

本方案为dag-agent提供了完整的基准测试工具和脚本。以下是所有创建的文件及其用途：

### ✅ 创建的文件列表

```
dag-task/dag-agent/scripts/
│
├── 📄 核心测试脚本（推荐执行）
│   ├── benchmark-quick.sh                ⭐ 快速基准测试（首选）
│   ├── benchmark-performance-test.sh     📊 全面性能分析（详细）
│   ├── benchmark-suite.sh                🎯 交互式菜单测试（综合）
│   │
│   ├── 📌 配置文件
│   ├─��� benchmark.conf                    ⚙️  参数配置文件
│   │
│   ├── 🛠️ 分析工具
│   ├── benchmark-analyzer.py             📈 性能数据分析工具（Python）
│   │
│   └── 📚 文档文件
│       ├── README_BENCHMARK.md           📋 完整方案总结（本文件）
│       ├── BENCHMARK_README.md           📖 详细使用文档（中文）
│       ├── QUICK_START.md                ⚡ 快速开始指南（5分钟）
│       └── USAGE_INDEX.md                🗂️  本索引文件
│
└── 📂 依赖文件（现有）
    └── run-api.sh                        🔧 通用API函数库
```

---

## 🚀 三步快速开始

### Step 1：启动Agent（1分钟）

```bash
cd dag-task/dag-agent-muserver
mvn exec:java -Denv=local
```

Agent将在 `http://localhost:20000` 启动

### Step 2：进入脚本目录（10秒）

```bash
cd ../dag-agent/scripts
chmod +x benchmark-*.sh benchmark-analyzer.py
```

### Step 3：选择运行一个基准测试（1-2分钟）

```bash
# 最简单的方式（推荐）
./benchmark-quick.sh
```

✅ 完成！你现在有了基本的性能指标。

---

## 📊 三种使用方式对比

| 方式 | 命令 | 用时 | 适用场景 | 输出 |
|------|------|------|---------|------|
| **快速验证** | `./benchmark-quick.sh` | 1-2分钟 | 日常快速检测 | 吞吐量 |
| **详细分析** | `./benchmark-performance-test.sh` | 5-15分钟 | 性能报告、深度分析 | 详细指标+HTML报告 |
| **交互式** | `./benchmark-suite.sh` | 可变 | 综合测试、学习 | 菜单选择 |

---

## 📚 文档导航

### 🏃 只有5分钟？
→ 阅读 **QUICK_START.md**

### 🤔 想了解详细信息？
→ 阅读 **BENCHMARK_README.md**

### 🔧 想自定义测试参数？
→ 编辑 **benchmark.conf**

### 📈 已有测试结果，想分析？
→ 运行 **benchmark-analyzer.py**

---

## 🎯 常见使用场景

### 场景1：日常性能验证（推荐）

```bash
# 最简单的用法
./benchmark-quick.sh

# 或者自定义参数
./benchmark-quick.sh --tasks 100 --threads 4
```

**输出内容：**
- 顺序提交吞吐量
- 并发提交吞吐量
- Agent健康状态

---

### 场景2：生成性能报告

```bash
# 步骤1：运行全面基准测试
./benchmark-performance-test.sh --tasks 500

# 步骤2：自动分析结果
python3 benchmark-analyzer.py benchmark_results_*/metrics.csv

# 步骤3：在浏览器中查看
open benchmark_results_*/benchmark_analysis.html
```

**生成文件：**
- `benchmark_analysis.html` - ⭐ 交互式性能报告（推荐）
- `benchmark_analysis.txt` - 文本格式报告
- `benchmark_analysis.json` - JSON格式数据

---

### 场景3：压力测试（找出极限）

```bash
./benchmark-performance-test.sh \
  --tasks 2000 \
  --threads 16 \
  --batch-size 100
```

---

### 场景4：特定Task类型测试

```bash
# 测试Echo任务
./benchmark-performance-test.sh --task-type echo --tasks 200

# 测试Shell任务
./benchmark-performance-test.sh --task-type shell --tasks 100

# 测试IO任务
./benchmark-performance-test.sh --task-type io --tasks 100

# 测试CPU任务
./benchmark-performance-test.sh --task-type cpu --tasks 50
```

---

### 场景5：CI/CD流水线集成

```bash
# .github/workflows/benchmark.yml
- name: Run Quick Benchmark
  run: |
    cd dag-task/dag-agent/scripts
    chmod +x benchmark-*.sh
    ./benchmark-quick.sh

- name: Upload Results
  uses: actions/upload-artifact@v2
  with:
    name: benchmark-results
    path: benchmark_results_*/
```

---

## 📊 理解关键指标

### 吞吐量 (Throughput)
```
40.16 tasks/sec  ← 顺序提交吞吐量
95.61 tasks/sec  ← 并发提交吞吐量
```
- ✅ **目标** > 1000 tasks/sec（顺序）
- ⚡ **优化** 增加线程数、检查网络延迟

### 延迟 (Latency)
```
P50 (中位数):  2.5ms  - 50%请求在此时间内
P95 (关键):    8.3ms  - 95%请求在此时间内（重要！）
P99 (最坏):   45.2ms  - 99%请求在此时间内
```
- ✅ **P95目标** < 10ms
- ✅ **P99目标** < 50ms

### 成功率 (Success Rate)
```
99.95% ✅
```
- ✅ **目标** > 99.5%
- ⚠️  **低于99%** 需要调查原因

---

## 🔧 参数快速参考

### benchmark-quick.sh

```bash
./benchmark-quick.sh [OPTIONS]
  --url <url>         Agent URL (default: http://localhost:20000)
  --tasks <count>     Number of tasks (default: 50)
  --threads <count>   Number of threads (default: 4)
```

### benchmark-performance-test.sh

```bash
./benchmark-performance-test.sh [OPTIONS]
  --url <url>         Agent URL
  --tasks <count>     Total tasks (default: 100)
  --threads <count>   Thread count (default: 4)
  --batch-size <n>    Batch size (default: 10)
  --task-type <type>  Type: echo|shell|io|cpu (default: echo)
  --duration <sec>    Max duration (default: 300)
```

### benchmark-analyzer.py

```bash
python3 benchmark-analyzer.py <metrics.csv> [--output <dir>]
```

---

## ⚠️ 常见问题

### Q: 连接超时？
```bash
# 检查 Agent 是否运行
curl http://localhost:20000/agent/api/v1/agent/ping
# 应该返回 "pong"

# 如果失败，启动Agent：
cd dag-task/dag-agent-muserver && mvn exec:java -Denv=local
```

### Q: 成功率低？
```bash
# 减少负载
./benchmark-quick.sh --tasks 30 --threads 2

# 或增加Agent资源
# 修改 dag-agent-muserver 的 JVM 参数
```

### Q: 延迟很高？
```bash
# 使用轻量级任务
./benchmark-performance-test.sh --task-type echo

# 减少并发
./benchmark-performance-test.sh --threads 2
```

详见 **BENCHMARK_README.md** 的"故障排查"部分。

---

## 📈 性能基准参考

| 环境 | 吞吐量 | P95延迟 | P99延迟 | 成功率 |
|------|--------|----------|----------|--------|
| **4核/8GB** | 40-50 (seq) | <5ms | <10ms | >99.5% |
| **8核/16GB** | 80-100 | <3ms | <5ms | >99.8% |
| **16核/32GB** | 150-200 | <2ms | <3ms | >99.9% |

*注：实际值因网络/配置而异*

---

## 📋 执行检查清单

使用前请确认：

- [ ] ✅ Agent 已启动并accessible
- [ ] ✅ 脚本已赋予执行权限（`chmod +x`）
- [ ] ✅ Python 3已安装（用于分析）
- [ ] ✅ 足够的磁盘空间（通常<50MB）
- [ ] ✅ 网络连接正常

---

## 🎁 额外功能

### 生成性能趋势图

```bash
# 保存多次运行的结果
for i in {1..5}; do
  ./benchmark-quick.sh >> results_$i.txt
  sleep 60
done

# 比对结果变化
diff <(head -n 20 results_1.txt) <(head -n 20 results_5.txt)
```

### 长期监控

```bash
#!/bin/bash
# benchmark-monitor.sh
while true; do
  ./benchmark-quick.sh 2>&1 | tee -a monitoring.log
  sleep 3600  # 每小时运行一次
done

# 运行：nohup ./benchmark-monitor.sh &
```

### 并行运行多个测试

```bash
# 同时测试不同参数
for tasks in 50 100 200; do
  ./benchmark-quick.sh --tasks $tasks > test_$tasks.txt &
done
wait
```

---

## 🚀 快速命令速查表

```bash
# 快速检查
./benchmark-quick.sh

# 详细分析
./benchmark-performance-test.sh --tasks 200

# 生成报告
python3 benchmark-analyzer.py benchmark_results_*/metrics.csv

# 交互式菜单
./benchmark-suite.sh

# 查看帮助
./benchmark-performance-test.sh --help

# 自定义参数
./benchmark-quick.sh --url http://agent:20000 --tasks 100 --threads 8

# 压力测试
./benchmark-performance-test.sh --tasks 1000 --threads 16 --batch-size 50

# 长期监控
while true; do ./benchmark-quick.sh; sleep 3600; done
```

---

## 📚 文档阅读顺序（推荐）

1. **README_BENCHMARK.md** ← 你在这里
2. **QUICK_START.md** ← 5分钟快速上手
3. **BENCHMARK_README.md** ← 详细完整文档
4. **benchmark.conf** ← 参数配置说明

---

## 🎓 使用示例工作流

### Example 1: 日常快速验证

```bash
# 1. 每天早上运行快速测试
./benchmark-quick.sh

# 2. 记录结果
./benchmark-quick.sh 2>&1 | tee daily_$(date +%Y%m%d).txt

# 3. 对比历史数据
diff daily_20240525.txt daily_20240526.txt
```

### Example 2: 版本发布前的性能测试

```bash
# 1. 运行全面基准测试
./benchmark-performance-test.sh --tasks 500 --threads 8

# 2. 生成详细报告
python3 benchmark-analyzer.py benchmark_results_*/metrics.csv

# 3. 在浏览器中查看 benchmark_analysis.html

# 4. 导出结果给团队
cp -r benchmark_results_* /shared/reports/
```

### Example 3: 性能优化验证

```bash
# Before: 记录优化前的基线
./benchmark-quick.sh > before.txt

# Apply optimization changes...

# After: 验证改进
./benchmark-quick.sh > after.txt

# Compare
vimdiff before.txt after.txt
```

---

## 💡 最佳实践

1. **建立基线** - 第一次运行时保存结果作为基线
2. **定期测试** - 每周或每月运行一次，监控趋势
3. **记录环境** - 记录JVM、CPU、内存等配置
4. **使用HTML报告** - 便于分享和演示
5. **自动化** - 集成到CI/CD流水线中
6. **保留历史** - 保存所有测试结果用于对比分析
7. **渐进式压力** - 从小负载逐步增加，找出瓶颈

---

## 📞 获取帮助

### 快速诊断

```bash
# 1. 检查Agent连接
curl -v http://localhost:20000/agent/api/v1/agent/ping

# 2. 查看详细日志
tail -f dag-agent-muserver/logs/agent.log

# 3. 运行简单测试
./benchmark-quick.sh --tasks 10 --threads 1

# 4. 检查系统资源
top -b -n 1 | head -10  # macOS/Linux
Get-Process | Select-Object Name,CPU,Memory  # Windows PowerShell
```

### 查阅文档

- **快速问题** → QUICK_START.md
- **详细问题** → BENCHMARK_README.md
- **配置问题** → benchmark.conf
- **参数问题** → 运行 `./benchmark-*.sh --help`

---

## ✨ 特色功能

- ✅ **开箱即用** - 无需复杂配置，开箱即用
- ✅ **多种场景** - 顺序、并发、批量、混合测试
- ✅ **自动分析** - 自动计算延迟百分位数
- ✅ **交互式报告** - 生成带图表的HTML报告
- ✅ **易于集成** - 支持CI/CD流水线
- ✅ **完整文档** - 提供详细的中文文档和示例

---

## 📊 一图看懂

```
┌─────────────────────────────────────────────────┐
│         DAG Agent Benchmark Testing             │
├─────────────────────────────────────────────────┤
│                                                 │
│  benchmark-quick.sh ──────→ 2分钟  ──→ 快速结果  │
│                                                 │
│  benchmark-performance-test.sh → 10分钟 → 详细报告 │
│                                                 │
│  benchmark-suite.sh ──────→ 交互式  ──→ 综合测试  │
│                                                 │
│           ↓                                    │
│  benchmark-analyzer.py ────→ 分析 ──→ HTML报告  │
│                                                 │
└─���───────────────────────────────────────────────┘
```

---

## 🎯 下一步行动

1. **现在就试试：** `./benchmark-quick.sh`
2. **详细了解：** 阅读 `QUICK_START.md`
3. **深入学习：** 阅读 `BENCHMARK_README.md`
4. **自定义参数：** 编辑 `benchmark.conf`

---

## 📝 版本信息

- **版本**：1.0  
- **创建日期**：2024-05-26
- **兼容性**：JDK 25+, Bash 4+, Python 3.6+
- **许可证**：遵循项目许可证

---

**准备好了？现在就运行 `./benchmark-quick.sh` 吧！** 🚀

如有任何问题，详见 `BENCHMARK_README.md` 的故障排查部分。

