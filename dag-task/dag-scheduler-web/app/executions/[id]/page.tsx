"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { useParams } from "next/navigation"
import Link from "next/link"
import {
  ArrowLeft,
  RotateCcw,
  X,
  Clock,
  CheckCircle2,
  XCircle,
  Activity,
  Terminal,
  FileText,
  Settings,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import type { Execution, ExecutionStatus, TaskExecution } from "@/types"
import { formatDate, formatDuration } from "@/lib/utils"

const statusConfig: Record<ExecutionStatus, { label: string; variant: any; color: string }> = {
  success: { label: "Success", variant: "success", color: "#10B981" },
  failed: { label: "Failed", variant: "failure", color: "#EF4444" },
  running: { label: "Running", variant: "running", color: "#3B82F6" },
  pending: { label: "Pending", variant: "pending", color: "#6B7280" },
  queued: { label: "Queued", variant: "warning", color: "#F59E0B" },
  cancelled: { label: "Cancelled", variant: "warning", color: "#F59E0B" },
  retrying: { label: "Retrying", variant: "warning", color: "#F59E0B" },
  skipped: { label: "Skipped", variant: "skipped", color: "#8B5CF6" },
}

const mockExecution: Execution = {
  id: "exec-001",
  workflowId: "wf-001",
  workflowKey: "etl-daily",
  workflowName: "Daily ETL Pipeline",
  version: "2.1.0",
  status: "running",
  triggerType: "scheduled",
  triggeredBy: "system",
  startedAt: "2024-01-07T10:30:00Z",
  progress: 65,
  parameters: {
    source: "production_db",
    target: "warehouse",
    date: "2024-01-07",
  },
  tasks: [
    {
      id: "task-001",
      executionId: "exec-001",
      nodeId: "node-1",
      nodeName: "Extract Data",
      nodeType: "database",
      status: "success",
      agentId: "agent-001",
      agentName: "worker-01",
      startedAt: "2024-01-07T10:30:00Z",
      endedAt: "2024-01-07T10:32:00Z",
      duration: 120000,
      retries: 0,
      maxRetries: 3,
    },
    {
      id: "task-002",
      executionId: "exec-001",
      nodeId: "node-2",
      nodeName: "Transform Data",
      nodeType: "python",
      status: "success",
      agentId: "agent-001",
      agentName: "worker-01",
      startedAt: "2024-01-07T10:32:00Z",
      endedAt: "2024-01-07T10:35:00Z",
      duration: 180000,
      retries: 0,
      maxRetries: 3,
    },
    {
      id: "task-003",
      executionId: "exec-001",
      nodeId: "node-3",
      nodeName: "Load Data",
      nodeType: "database",
      status: "running",
      agentId: "agent-002",
      agentName: "worker-02",
      startedAt: "2024-01-07T10:35:00Z",
      retries: 0,
      maxRetries: 3,
    },
    {
      id: "task-004",
      executionId: "exec-001",
      nodeId: "node-4",
      nodeName: "Verify Data",
      nodeType: "task",
      status: "pending",
      retries: 0,
      maxRetries: 3,
    },
  ],
}

const mockLogs = [
  { timestamp: "2024-01-07T10:30:00Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Starting execution exec-001" },
  { timestamp: "2024-01-07T10:30:01Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-001 (Extract Data) started on agent worker-01" },
  { timestamp: "2024-01-07T10:32:00Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-001 completed successfully" },
  { timestamp: "2024-01-07T10:32:01Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-002 (Transform Data) started on agent worker-01" },
  { timestamp: "2024-01-07T10:35:00Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-002 completed successfully" },
  { timestamp: "2024-01-07T10:35:01Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-003 (Load Data) started on agent worker-02" },
  { timestamp: "2024-01-07T10:36:00Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-003 processing batch 1/5" },
  { timestamp: "2024-01-07T10:37:00Z", level: "warn" as "info" | "warn" | "error" | "debug", message: "Task task-003 slow query detected" },
  { timestamp: "2024-01-07T10:38:00Z", level: "info" as "info" | "warn" | "error" | "debug", message: "Task task-003 processing batch 3/5" },
]

export default function ExecutionDetailPage() {
  const params = useParams()
  const executionId = params.id as string
  const [activeTab, setActiveTab] = useState("overview")

  const execution = mockExecution
  const config = statusConfig[execution.status]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link href="/executions">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight">{executionId}</h1>
              <Badge variant={config.variant}>{config.label}</Badge>
            </div>
            <p className="text-muted-foreground">
              {execution.workflowName} • v{execution.version}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {execution.status === "running" && (
            <Button variant="outline">
              <X className="mr-2 h-4 w-4" />
              Cancel
            </Button>
          )}
          {(execution.status === "failed" || execution.status === "cancelled") && (
            <Button>
              <RotateCcw className="mr-2 h-4 w-4" />
              Retry
            </Button>
          )}
        </div>
      </div>

      {/* Info Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Trigger</p>
            <p className="text-sm font-medium capitalize">{execution.triggerType}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Started</p>
            <p className="text-sm font-medium">{execution.startedAt ? formatDate(execution.startedAt) : '-'}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Duration</p>
            <p className="text-sm font-medium">{execution.duration ? formatDuration(execution.duration) : '-'}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Progress</p>
            <div className="flex items-center gap-2">
              <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                <div className="h-full bg-blue-500 rounded-full" style={{ width: `${execution.progress || 0}%` }} />
              </div>
              <span className="text-sm font-medium">{execution.progress}%</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="tasks">Tasks</TabsTrigger>
          <TabsTrigger value="logs">Logs</TabsTrigger>
          <TabsTrigger value="parameters">Parameters</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Execution Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {execution.tasks?.map((task, index) => {
                  const taskConfig = statusConfig[task.status]
                  return (
                    <div key={task.id} className="flex items-start gap-4">
                      <div className="flex flex-col items-center">
                        <div className={`w-3 h-3 rounded-full`} style={{ backgroundColor: taskConfig.color }} />
                        {index < (execution.tasks?.length || 0) - 1 && (
                          <div className="w-0.5 h-12 bg-muted mt-1" />
                        )}
                      </div>
                      <div className="flex-1 pb-6">
                        <div className="flex items-center justify-between">
                          <p className="font-medium">{task.nodeName}</p>
                          <Badge variant={taskConfig.variant} className="text-xs">{taskConfig.label}</Badge>
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">
                          {task.agentName} • {task.duration ? formatDuration(task.duration) : 'Running'}
                        </p>
                      </div>
                    </div>
                  )
                })}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="tasks" className="space-y-4">
          <Card>
            <CardContent className="p-0">
              <div className="divide-y">
                {execution.tasks?.map((task) => {
                  const taskConfig = statusConfig[task.status]
                  return (
                    <div key={task.id} className="flex items-center justify-between p-4 hover:bg-muted/50 transition-colors">
                      <div>
                        <p className="font-medium">{task.nodeName}</p>
                        <p className="text-xs text-muted-foreground">{task.nodeType} • {task.agentName}</p>
                      </div>
                      <div className="flex items-center gap-4">
                        <span className="text-sm text-muted-foreground">
                          {task.duration ? formatDuration(task.duration) : '-'}
                        </span>
                        <Badge variant={taskConfig.variant}>{taskConfig.label}</Badge>
                      </div>
                    </div>
                  )
                })}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="logs" className="space-y-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Execution Logs</CardTitle>
              <Button variant="outline" size="sm">
                <Terminal className="mr-2 h-4 w-4" />
                Download
              </Button>
            </CardHeader>
            <CardContent>
              <ScrollArea className="h-[400px]">
                <div className="space-y-1 font-mono text-sm">
                  {mockLogs.map((log, index) => (
                    <div key={index} className="flex gap-3 p-1.5 hover:bg-muted/50 rounded">
                      <span className="text-muted-foreground text-xs shrink-0 w-[140px]">
                        {formatDate(log.timestamp)}
                      </span>
                      <span className={`text-xs shrink-0 w-12 ${
                        log.level === 'error' ? 'text-red-400' :
                        log.level === 'warn' ? 'text-amber-400' :
                        log.level === 'debug' ? 'text-blue-400' :
                        'text-emerald-400'
                      }`}>
                        {log.level.toUpperCase()}
                      </span>
                      <span className="text-foreground">{log.message}</span>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="parameters" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Execution Parameters</CardTitle>
            </CardHeader>
            <CardContent>
              <pre className="p-4 rounded-lg bg-muted font-mono text-sm overflow-auto">
                {JSON.stringify(execution.parameters, null, 2)}
              </pre>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
