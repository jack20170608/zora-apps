// Core Types for DAG Scheduler Web

// ==================== Workflow Types ====================

export interface Workflow {
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
  executionCount?: number;
  successRate?: number;
}

export interface WorkflowVersion {
  version: string;
  createdAt: string;
  createdBy: string;
  active: boolean;
  dagDefinition: DagDefinition;
}

export interface DagDefinition {
  nodes: DagNode[];
  edges: DagEdge[];
}

export interface DagNode {
  id: string;
  type: string;
  position: { x: number; y: number };
  data: DagNodeData;
}

export interface DagNodeData {
  label: string;
  type: NodeType;
  config: Record<string, unknown>;
  description?: string;
}

export type NodeType = 
  | 'task' 
  | 'shell' 
  | 'python' 
  | 'java' 
  | 'docker' 
  | 'database' 
  | 'decision' 
  | 'fork' 
  | 'join';

export interface NodeTypeDefinition {
  type: NodeType;
  label: string;
  icon: string;
  color: string;
  description: string;
  defaultConfig: Record<string, unknown>;
  configSchema: ConfigField[];
}

export interface ConfigField {
  name: string;
  label: string;
  type: 'string' | 'number' | 'boolean' | 'textarea' | 'select' | 'json';
  required: boolean;
  default?: unknown;
  options?: { label: string; value: string }[];
  placeholder?: string;
  description?: string;
}

export interface DagEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  type?: 'default' | 'conditional';
  condition?: string;
}

export interface ParameterSchema {
  fields: ParameterField[];
}

export interface ParameterField {
  name: string;
  label: string;
  type: 'string' | 'number' | 'boolean' | 'date' | 'select' | 'multiselect';
  required: boolean;
  default?: unknown;
  description?: string;
}

// ==================== Execution Types ====================

export type ExecutionStatus = 
  | 'pending' 
  | 'queued' 
  | 'running' 
  | 'success' 
  | 'failed' 
  | 'cancelled' 
  | 'retrying' 
  | 'skipped';

export type TaskStatus = 
  | 'pending' 
  | 'queued' 
  | 'running' 
  | 'success' 
  | 'failed' 
  | 'cancelled' 
  | 'skipped';

export interface Execution {
  id: string;
  workflowId: string;
  workflowKey: string;
  workflowName: string;
  version: string;
  status: ExecutionStatus;
  triggerType: 'manual' | 'scheduled' | 'api' | 'webhook';
  triggeredBy?: string;
  startedAt?: string;
  endedAt?: string;
  duration?: number;
  parameters?: Record<string, unknown>;
  tasks?: TaskExecution[];
  progress?: number;
  errorMessage?: string;
}

export interface ExecutionSummary {
  id: string;
  status: ExecutionStatus;
  startedAt: string;
  endedAt?: string;
  duration?: number;
}

export interface TaskExecution {
  id: string;
  executionId: string;
  nodeId: string;
  nodeName: string;
  nodeType: string;
  status: TaskStatus;
  agentId?: string;
  agentName?: string;
  startedAt?: string;
  endedAt?: string;
  duration?: number;
  retries: number;
  maxRetries: number;
  output?: Record<string, unknown>;
  error?: TaskError;
  logs?: LogEntry[];
}

export interface TaskError {
  message: string;
  stackTrace?: string;
  exitCode?: number;
}

export interface LogEntry {
  timestamp: string;
  level: 'debug' | 'info' | 'warn' | 'error';
  message: string;
  taskId?: string;
}

// ==================== Agent Types ====================

export type AgentStatus = 'online' | 'offline' | 'busy';

export interface Agent {
  id: string;
  name: string;
  host: string;
  status: AgentStatus;
  version: string;
  capabilities: string[];
  currentTasks: number;
  maxTasks: number;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  lastHeartbeat: string;
  tags: string[];
}

// ==================== Dashboard Types ====================

export interface DashboardStats {
  totalWorkflows: number;
  activeWorkflows: number;
  totalExecutions: number;
  runningExecutions: number;
  successRate: number;
  avgDuration: number;
  onlineAgents: number;
  totalAgents: number;
  failedExecutions24h: number;
}

export interface TrendData {
  date: string;
  executions: number;
  successful: number;
  failed: number;
  avgDuration: number;
}

export interface ExecutionWaterfall {
  executionId: string;
  workflowName: string;
  status: ExecutionStatus;
  startTime: string;
  endTime?: string;
  duration: number;
}

// ==================== API Response Types ====================

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// ==================== UI State Types ====================

export type ViewMode = 'list' | 'grid' | 'timeline' | 'gantt';

export interface StudioState {
  selectedNodes: string[];
  canvasZoom: number;
  canvasPosition: { x: number; y: number };
  showGrid: boolean;
  showMinimap: boolean;
  snapToGrid: boolean;
  showConsole: boolean;
  history: { nodes: DagNode[]; edges: DagEdge[] }[];
  historyIndex: number;
}

export interface FilterState {
  status: ExecutionStatus[];
  dateRange: { from?: string; to?: string };
  workflowId?: string;
  searchQuery: string;
}
