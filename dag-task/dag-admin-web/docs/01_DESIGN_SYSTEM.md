# DAG Scheduler Web - Design Document

## 1. Design Philosophy

### References
- **Apache Airflow 2.x** - Industry standard for workflow management
- **Temporal Web UI** - Modern execution tracing and visualization
- **n8n** - Best-in-class node editor with intuitive UX
- **Dagster** - Data-aware workflow design with rich metadata
- **GitHub Actions** - Clean CI/CD pipeline visualization
- **Grafana** - Professional monitoring dashboards

### Principles
1. **Immediate Feedback** - Every action provides instant visual response
2. **Context Preservation** - Users always know where they are in the system
3. **Progressive Disclosure** - Complex features unfold on demand
4. **Visualization First** - Graphs over tables, animations over static
5. **Keyboard Friendly** - Full keyboard navigation and shortcuts
6. **Dark Mode First** - Professional tools look best in dark mode

## 2. Information Architecture

```
Overview (全局概览)
├── Real-time Execution Waterfall
├── System Health Radar
├── Performance Metrics
├── Quick Actions Dock
│
Workflows (工作流管理)
├── Gallery View (卡片网格)
├── List View (数据表格)
├── Workflow Detail
│   ├── DAG Preview (静态)
│   ├── Version Timeline
│   ├── Parameter Schema
│   └── Execution History
│
Studio (DAG 设计器) - Core Experience
├── Canvas (Infinite Canvas)
├── Node Palette (左侧)
├── Properties Panel (右侧)
├── Console Panel (底部)
├── Toolbar (顶部)
└── Breadcrumb Navigation
│
Executions (执行管理)
├── Execution List
│   ├── Filter & Search
│   ├── List/Timeline/Gantt Views
│   └── Bulk Operations
├── Execution Detail
│   ├── Runtime DAG (Animated)
│   ├── Gantt Chart
│   ├── Structured Logs
│   ├── Variables Inspector
│   └── Task Actions
│
Agents (执行器集群)
├── Cluster Topology
├── Agent List
├── Resource Monitor
└── Task Assignment Visualizer
│
Settings (系统设置)
├── Global Parameters
├── Notification Channels
├── User Management
└── System Configuration
```

## 3. Color System

### Semantic Colors
```
Success:   #10B981 (emerald-500)  - 完成、成功、在线
Failure:   #EF4444 (red-500)      - 失败、错误、离线  
Running:   #3B82F6 (blue-500)     - 运行中、处理中
Warning:   #F59E0B (amber-500)    - 警告、重试、待定
Pending:   #6B7280 (gray-500)     - 等待、排队、未开始
Skipped:   #8B5CF6 (violet-500)   - 跳过、手动忽略
```

### Node Type Colors
```
Task:       #3B82F6 (blue)    - 通用任务
Shell:      #06B6D4 (cyan)    - Shell 脚本
Python:     #FBBF24 (amber)   - Python 脚本
Java:       #F97316 (orange)  - Java 程序
Docker:     #0EA5E9 (sky)     - Docker 容器
Database:   #10B981 (emerald) - 数据库操作
Decision:   #EC4899 (pink)    - 条件分支
Fork:       #8B5CF6 (violet)  - 并行分支
Join:       #6366F1 (indigo)  - 汇聚节点
```

## 4. Typography

```
Display:    Inter, system-ui, sans-serif
Mono:       JetBrains Mono, Fira Code, monospace

Scale:
- Page Title:     24px / font-semibold / tracking-tight
- Section Title:  18px / font-medium
- Card Title:     16px / font-medium
- Body:           14px / font-normal / leading-relaxed
- Caption:        12px / font-medium / text-muted
- Code:           13px / font-mono / leading-normal
```

## 5. Spacing & Layout

```
Sidebar:        256px fixed
Header:         64px fixed
Content Padding: 24px
Card Gap:       16px
Border Radius:  
  - Cards:      12px
  - Buttons:    8px
  - Inputs:     6px
  - Badges:     9999px
```

## 6. Animation Specifications

```
Page Transition:     200ms ease-out fade + 8px translateY
Card Hover:          150ms ease-out shadow + 2px translateY
Node Selection:      100ms spring scale(1.02) + glow
Status Change:       300ms ease-in-out color transition
Toast Notification:  300ms slideIn + 200ms fadeOut
Modal:               200ms ease-out scale(0.95→1) + fade
```

## 7. Keyboard Shortcuts

```
Global:
  Cmd/Ctrl + K    - Command Palette (全局搜索)
  Cmd/Ctrl + /    - Keyboard Shortcuts Help

Studio:
  Cmd/Ctrl + S    - Save Workflow
  Cmd/Ctrl + Z    - Undo
  Cmd/Ctrl + Shift + Z  - Redo
  Cmd/Ctrl + D    - Duplicate Node
  Delete          - Delete Selected Node
  Space + Drag    - Pan Canvas
  Scroll          - Zoom Canvas
  1-9             - Select Node Type (Palette)
  Esc             - Deselect / Close Panel

Executions:
  R               - Refresh List
  F               - Focus Search
```

## 8. Responsive Breakpoints

```
Mobile:     < 768px   (隐藏 Sidebar, 汉堡菜单)
Tablet:     768px+    (折叠 Sidebar)
Desktop:    1024px+   (完整布局)
Wide:       1440px+   (扩展画布)
```

## 9. State Management

```typescript
// Zustand Store Structure
interface AppState {
  // UI State
  sidebarCollapsed: boolean;
  theme: 'dark' | 'light' | 'system';
  
  // Studio State
  studio: {
    selectedNodes: string[];
    canvasZoom: number;
    canvasPosition: { x: number; y: number };
    showGrid: boolean;
    showMinimap: boolean;
    history: { nodes: Node[]; edges: Edge[] }[];
    historyIndex: number;
  };
  
  // Execution State
  executions: {
    selectedExecutionId: string | null;
    filterStatus: ExecutionStatus[];
    viewMode: 'list' | 'timeline' | 'gantt';
  };
}
```

## 10. API Design

```typescript
// RESTful API Endpoints
GET    /api/v1/workflows              - List workflows
POST   /api/v1/workflows              - Create workflow
GET    /api/v1/workflows/:id          - Get workflow detail
PUT    /api/v1/workflows/:id          - Update workflow
DELETE /api/v1/workflows/:id          - Delete workflow
GET    /api/v1/workflows/:id/versions - List versions
POST   /api/v1/workflows/:id/execute  - Trigger execution

GET    /api/v1/executions             - List executions
GET    /api/v1/executions/:id         - Get execution detail
GET    /api/v1/executions/:id/logs    - Stream logs
POST   /api/v1/executions/:id/retry   - Retry execution
POST   /api/v1/executions/:id/cancel  - Cancel execution

GET    /api/v1/agents                 - List agents
GET    /api/v1/agents/:id/metrics     - Agent metrics

GET    /api/v1/stats/overview         - Dashboard statistics
GET    /api/v1/stats/trends           - Historical trends

// WebSocket Events
ws://.../executions/:id/stream       - Real-time execution updates
ws://.../agents/stream               - Agent heartbeat stream
ws://.../logs/:executionId/:taskId   - Real-time log streaming
```

## 11. Data Models

```typescript
// Core Models
interface Workflow {
  id: string;
  key: string;
  name: string;
  description: string;
  version: string;
  active: boolean;
  dagDefinition: DagDefinition;
  parameterSchema: ParameterSchema;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  lastExecution?: ExecutionSummary;
}

interface Execution {
  id: string;
  workflowId: string;
  workflowKey: string;
  workflowName: string;
  status: 'pending' | 'running' | 'success' | 'failed' | 'cancelled' | 'retrying';
  triggerType: 'manual' | 'scheduled' | 'api' | 'webhook';
  startedAt: string;
  endedAt?: string;
  duration?: number;
  parameters: Record<string, unknown>;
  tasks: TaskExecution[];
  logs?: LogEntry[];
}

interface TaskExecution {
  id: string;
  executionId: string;
  nodeId: string;
  nodeName: string;
  nodeType: string;
  status: TaskStatus;
  agentId?: string;
  startedAt?: string;
  endedAt?: string;
  duration?: number;
  retries: number;
  maxRetries: number;
  output?: Record<string, unknown>;
  error?: TaskError;
}

interface Agent {
  id: string;
  name: string;
  host: string;
  status: 'online' | 'offline' | 'busy';
  version: string;
  capabilities: string[];
  currentTasks: number;
  maxTasks: number;
  cpuUsage: number;
  memoryUsage: number;
  lastHeartbeat: string;
}
```

## 12. Component Hierarchy

```
App
├── Providers (Theme, Query, Toast)
├── Layout
│   ├── Header
│   │   ├── Logo
│   │   ├── Global Search (Cmd+K)
│   │   ├── Theme Toggle
│   │   └── User Menu
│   ├── Sidebar
│   │   ├── Navigation (NavItem[])
│   │   └── Collapse Button
│   └── Main Content
│       ├── Page Header
│       │   ├── Title
│       │   ├── Breadcrumb
│       │   └── Actions
│       └── Page Content
│
Studio (Special Layout)
├── Studio Header
├── Left Panel (Node Palette)
├── Center (Canvas)
├── Right Panel (Properties)
└── Bottom Panel (Console)
```

## 13. Feature Specifications

### 13.1 Studio - DAG Builder
- **Infinite Canvas**: Pan with Space+Drag, Zoom with Scroll
- **Node Palette**: Categorized, searchable, draggable
- **Smart Connections**: Auto-routing, avoid overlap
- **Context Menu**: Right-click for quick actions
- **Multi-select**: Box select + Ctrl/Cmd click
- **Grid Snap**: Optional snap-to-grid
- **Mini-map**: Overview with viewport rectangle
- **Validation**: Real-time DAG validation (no cycles, connected)
- **Undo/Redo**: Full history stack
- **Templates**: Save node configurations as reusable snippets

### 13.2 Execution Detail
- **Runtime DAG**: Live-updating with color-coded status
- **Gantt Chart**: Task execution timeline
- **Log Viewer**: 
  - Syntax highlighting
  - Search & filter
  - Auto-scroll toggle
  - Log level filtering
  - Download logs
- **Task Inspector**:
  - Input/Output variables
  - Environment variables
  - Resource usage
  - Retry history
- **Actions**:
  - Retry single task
  - Retry from failed task
  - Mark as success/failed
  - Cancel execution

### 13.3 Overview Dashboard
- **Waterfall Chart**: Recent executions visualized as colored bars
- **Health Radar**: System metrics in radar chart
- **Trend Lines**: Success rate, execution count over time
- **Alert Feed**: Recent failures and warnings
- **Quick Stats**: Total workflows, active executions, agent health

## 14. Implementation Phases

### Phase 1: Foundation (Week 1)
- Project setup with Next.js 15 + React 19
- Design system (colors, typography, spacing)
- Layout components (Header, Sidebar, Page Shell)
- Routing structure
- API client setup

### Phase 2: Core Pages (Week 2)
- Overview dashboard
- Workflows list + detail
- Studio DAG builder (basic)
- Executions list

### Phase 3: Advanced Features (Week 3)
- Execution detail with live updates
- Studio advanced features (undo/redo, templates)
- Agents management
- Settings pages

### Phase 4: Polish (Week 4)
- Animations and transitions
- Keyboard shortcuts
- Command palette
- Performance optimization
- Testing

## 15. Performance Targets

- First Contentful Paint: < 1.5s
- Time to Interactive: < 3s
- Canvas FPS: > 30fps with 100 nodes
- Log Stream Latency: < 500ms
- API Response: < 200ms (cached)
