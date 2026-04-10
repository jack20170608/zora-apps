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

  if (error) {
    return <div className="p-4 text-destructive">Error loading tasks</div>;
  }

  const tasks: TaskExecution[] = data?.data ?? [];

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
