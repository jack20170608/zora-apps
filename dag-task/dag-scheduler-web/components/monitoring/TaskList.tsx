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
import type { TaskExecution } from "@/types/task";
import { cn } from "@/lib/utils";

// Mock data for when backend API doesn't exist yet
const MOCK_TASKS: TaskExecution[] = [
  {
    id: 1,
    orderKey: "etl-production-daily",
    orderName: "Production ETL Daily Run",
    templateKey: "etl-data-processing",
    templateVersion: "1.1.0",
    status: "COMPLETED",
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
    status: "COMPLETED",
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
    status: "FAILED",
    startTime: new Date(Date.now() - 10800000).toISOString(),
    endTime: new Date(Date.now() - 10740000).toISOString(),
    durationMs: 60000,
    agentId: "default-agent-001",
  },
];

const statusVariants: Record<TaskExecution["status"], any> = {
  PENDING: { label: "Pending", variant: "outline" },
  RUNNING: { label: "Running", variant: "warning" },
  COMPLETED: { label: "Completed", variant: "success" },
  FAILED: { label: "Failed", variant: "danger" },
  CANCELLED: { label: "Cancelled", variant: "outline" },
};

export function TaskList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["tasks"],
    queryFn: () => taskApi.listExecutions(),
    retry: false,
  });

  if (isLoading) {
    return <div className="p-4 text-muted-foreground">Loading tasks...</div>;
  }

  // Use mock data if API request fails (backend doesn't have endpoint yet)
  const tasks: TaskExecution[] = error ? MOCK_TASKS : (data?.data ?? []);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Executions</CardTitle>
      </CardHeader>
      <CardContent>
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
                const statusInfo = statusVariants[task.status];
                return (
                  <TableRow key={task.id}>
                    <TableCell>{task.id}</TableCell>
                    <TableCell className="font-medium">{task.orderName}</TableCell>
                    <TableCell>
                      {task.templateKey} v{task.templateVersion}
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusInfo.variant}>
                        {statusInfo.label}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {new Date(task.startTime).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      {task.durationMs ? `${(task.durationMs / 1000).toFixed(1)}s` : "-"}
                    </TableCell>
                    <TableCell>{task.agentId ?? "-"}</TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
