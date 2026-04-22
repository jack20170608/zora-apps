import type { LucideIcon } from 'lucide-react';

/** Task execution status - aligned with backend TaskStatus enum */
export type TaskStatus = 'INIT' | 'DISPATCHED' | 'RUNNING' | 'SUCCESS' | 'ERROR' | 'TIMEOUT' | 'SKIPPED' | 'HOLD' | 'CANCELLED';

export interface TaskExecution {
  id: number;
  orderKey: string;
  orderName: string;
  templateKey: string;
  templateVersion: string;
  status: TaskStatus;
  startTime: string;
  endTime?: string;
  durationMs?: number;
  agentId?: string;
}

export interface AgentInfo {
  agentId: string;
  agentUrl: string;
  maxConcurrentTasks: number;
  maxPendingTasks: number;
  supportedExecutionKeys: string[];
  running: boolean;
  pendingTasks: number;
  runningTasks: number;
  finishedTasks: number;
}

export interface DashboardStats {
  totalTemplates: number;
  activeTemplates: number;
  totalTasksToday: number;
  completedTasks: number;
  failedTasks: number;
  successRate: number;
  onlineAgents: number;
  totalAgents: number;
}

export interface StatCardItem {
  title: string;
  value: string | number;
  icon: LucideIcon;
  description?: string;
}
