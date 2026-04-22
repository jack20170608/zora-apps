'use client';

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHead,
  TableRow,
} from "@/components/ui/table";
import { taskApi } from "@/lib/api-client";
import type { TaskExecution, TaskStatus } from "@/types/task";
import { AlertTriangle } from "lucide-react";

// Mock data for when backend API doesn't exist yet
const MOCK_TASKS: TaskExecution[] = [
  {
    id: 1,
    orderKey: "etl-production-daily",
    orderName: "Production ETL Daily Run",
    templateKey: "etl-data-processing",
    templateVersion: "1.1.0",
    status: "SUCCESS",
    startTime: new Date(Date.now() - 3600000).toISOString(),
    durationMs: 12450,
    agentId: "default-agent-001",
  },
  {
    id: 2,
    orderKey: "backup-users-db-daily",
    orderName: "Daily Backup - Users Database",
    templateKey: "daily-database-backup",
    templateVersion: "1.0.0",
    status: "SUCCESS",
    startTime: new Date(Date.now() - 7200000).toISOString(),
    durationMs: 8320,
    agentId: "python-worker-002",
  },
  {
    id: 3,
    orderKey: "train-model-weekly",
    orderName: "Weekly ML Model Retrain",
    templateKey: "ml-model-training",
    templateVersion: "1.0.0",
    status: "RUNNING",
    startTime: new Date(Date.now() - 1800000).toISOString(),
    agentId: "docker-agent-003",
  },
  {
    id: 4,
    orderKey: "cleanup-old-backups",
    orderName: "Cleanup Old Backup Files",
    templateKey: "daily-database-backup",
    templateVersion: "1.0.0",
    status: "ERROR",
    startTime: new Date(Date.now() - 10800000).toISOString(),
    endTime: new Date(Date.now() - 10740000).toISOString(),
    durationMs: 60000,
    agentId: "default-agent-001",
  },
];

const statusConfig: Record<TaskStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning' | 'danger' }> = {
  INIT: { label: "Init", variant: "outline" },
  DISPATCHED: { label: "Dispatched", variant: "secondary" },
  RUNNING: { label: "Running", variant: "warning" },
  SUCCESS: { label: "Success", variant: "success" },
  ERROR: { label: "Error", variant: "danger" },
  TIMEOUT: { label: "Timeout", variant: "danger" },
  SKIPPED: { label: "Skipped", variant: "outline" },
  HOLD: { label: "Hold", variant: "secondary" },
  CANCELLED: { label: "Cancelled", variant: "outline" },
};

function formatDuration(ms: number | undefined): string {
  if (ms === undefined || ms === null) return "-";
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function formatDateTime(isoString: string | undefined): string {
  if (!isoString) return "-";
  try {
    return new Date(isoString).toLocaleString();
  } catch {
    return isoString;
  }
}

export function TaskList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["tasks"],
    queryFn: () => taskApi.listExecutions(),
    retry: false,
  });

  const isUsingMock = !!error;
  const tasks: TaskExecution[] = isUsingMock ? MOCK_TASKS : (data?.data ?? []);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Recent Executions</CardTitle>
        {isUsingMock && (
          <div className="flex items-center gap-1.5 text-xs text-amber-600">
            <AlertTriangle className="h-3.5 w-3.5" />
            <span>Mock data</span>
          </div>
        )}
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="p-4 text-muted-foreground text-center">Loading tasks...</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Template</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Started</TableHead>
                <TableHead>Duration</TableHead>
                <TableHead>Agent</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tasks.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground">
                    No task executions found.
                  </TableCell>
                </TableRow>
              ) : (
                tasks.map((task) => {
                  const statusInfo = statusConfig[task.status] ?? { label: task.status, variant: "outline" as const };
                  return (
                    <TableRow key={task.id}>
                      <TableCell className="font-mono text-xs">{task.id}</TableCell>
                      <TableCell className="font-medium">{task.orderName}</TableCell>
                      <TableCell>
                        <span className="text-xs text-muted-foreground">
                          {task.templateKey} v{task.templateVersion}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={statusInfo.variant}>
                          {statusInfo.label}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-xs">
                        {formatDateTime(task.startTime)}
                      </TableCell>
                      <TableCell className="text-xs">
                        {formatDuration(task.durationMs)}
                      </TableCell>
                      <TableCell className="text-xs">{task.agentId ?? "-"}</TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
