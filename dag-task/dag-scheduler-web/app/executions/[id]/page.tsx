"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { useParams } from "next/navigation"
import Link from "next/link"
import {
  ArrowLeft,
  RotateCcw,
  X,
  Activity,
  Terminal,
  Download,
  Pause,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import { useRealTimeLogs } from "@/components/executions/useRealTimeLogs"
import { GanttChart } from "@/components/executions/GanttChart"
import type { ExecutionStatus } from "@/types"
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

export default function ExecutionDetailPage() {
  const params = useParams()
  const executionId = params.id as string
  const [activeTab, setActiveTab] = useState("overview")
  const [isRunning, setIsRunning] = useState(true)

  const logs = useRealTimeLogs(executionId, isRunning)

  const tasks = [
    { id: "task-001", nodeName: "Extract Data", status: "success" as ExecutionStatus, startedAt: new Date(Date.now() - 300000).toISOString(), duration: 120000 },
    { id: "task-002", nodeName: "Transform Data", status: "success" as ExecutionStatus, startedAt: new Date(Date.now() - 180000).toISOString(), duration: 180000 },
    { id: "task-003", nodeName: "Load Data", status: isRunning ? "running" : "success" as ExecutionStatus, startedAt: new Date(Date.now() - 60000).toISOString(), duration: isRunning ? 60000 : 120000 },
    { id: "task-004", nodeName: "Verify Data", status: isRunning ? "pending" : "success" as ExecutionStatus, startedAt: undefined, duration: 0 },
  ]

  const config = statusConfig[isRunning ? "running" : "success"]
  const totalDuration = 300000

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
              Daily ETL Pipeline v2.1.0
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {isRunning && (
            <>
              <Button variant="outline" size="sm" onClick={() => setIsRunning(false)}>
                <Pause className="mr-2 h-4 w-4" />
                Pause
              </Button>
              <Button variant="destructive" size="sm">
                <X className="mr-2 h-4 w-4" />
                Cancel
              </Button>
            </>
          )}
          {!isRunning && (
            <Button size="sm">
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
            <p className="text-xs text-muted-foreground">Status</p>
            <div className="flex items-center gap-2 mt-1">
              <div className="w-2 h-2 rounded-full animate-pulse" style={{ backgroundColor: config.color }} />
              <p className="text-sm font-medium">{config.label}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Started</p>
            <p className="text-sm font-medium">{formatDate(new Date(Date.now() - 300000).toISOString())}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Duration</p>
            <p className="text-sm font-medium">{formatDuration(totalDuration)}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Progress</p>
            <div className="flex items-center gap-2 mt-1">
              <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                <motion.div
                  className="h-full rounded-full"
                  style={{ backgroundColor: config.color }}
                  initial={{ width: 0 }}
                  animate={{ width: `${isRunning ? 65 : 100}%` }}
                  transition={{ duration: 1 }}
                />
              </div>
              <span className="text-sm font-medium">{isRunning ? 65 : 100}%</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="timeline">Timeline</TabsTrigger>
          <TabsTrigger value="logs">
            Logs
            {isRunning && <span className="ml-2 w-2 h-2 rounded-full bg-blue-500 animate-pulse" />}
          </TabsTrigger>
          <TabsTrigger value="parameters">Parameters</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <div className="grid gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Task Execution</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {tasks.map((task, index) => {
                    const taskConfig = statusConfig[task.status]
                    return (
                      <div key={task.id} className="flex items-start gap-4">
                        <div className="flex flex-col items-center">
                          <div className="w-3 h-3 rounded-full" style={{ backgroundColor: taskConfig.color }} />
                          {index < tasks.length - 1 && (
                            <div className="w-0.5 h-12 bg-muted mt-1" />
                          )}
                        </div>
                        <div className="flex-1 pb-6">
                          <div className="flex items-center justify-between">
                            <p className="font-medium">{task.nodeName}</p>
                            <Badge variant={taskConfig.variant} className="text-xs">{taskConfig.label}</Badge>
                          </div>
                          {task.duration > 0 && (
                            <p className="text-xs text-muted-foreground mt-1">
                              Duration: {formatDuration(task.duration)}
                            </p>
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Gantt Chart</CardTitle>
              </CardHeader>
              <CardContent>
                <GanttChart tasks={tasks} totalDuration={totalDuration} />
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="timeline" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Execution Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {tasks.map((task) => {
                  const taskConfig = statusConfig[task.status]
                  return (
                    <div key={task.id} className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
                      <div className="flex items-center gap-3">
                        <div className="p-2 rounded-md" style={{ backgroundColor: `${taskConfig.color}15` }}>
                          <Activity className="h-4 w-4" style={{ color: taskConfig.color }} />
                        </div>
                        <div>
                          <p className="font-medium">{task.nodeName}</p>
                          <p className="text-xs text-muted-foreground">ID: {task.id}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <Badge variant={taskConfig.variant}>{taskConfig.label}</Badge>
                        <p className="text-xs text-muted-foreground mt-1">
                          {task.duration > 0 ? formatDuration(task.duration) : "Pending"}
                        </p>
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
                <Download className="mr-2 h-4 w-4" />
                Download
              </Button>
            </CardHeader>
            <CardContent>
              <ScrollArea className="h-[500px]">
                <div className="space-y-0.5 font-mono text-sm">
                  {logs.length === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                      <Terminal className="h-8 w-8 mx-auto mb-2 opacity-50" />
                      <p>Waiting for logs...</p>
                    </div>
                  ) : (
                    logs.map((log, index) => (
                      <motion.div
                        key={index}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        className="flex gap-3 p-1.5 hover:bg-muted/50 rounded"
                      >
                        <span className="text-muted-foreground text-xs shrink-0 w-[160px]">
                          {formatDate(log.timestamp)}
                        </span>
                        <span className={`text-xs shrink-0 w-12 font-bold ${
                          log.level === "ERROR" ? "text-red-400" :
                          log.level === "WARN" ? "text-amber-400" :
                          "text-emerald-400"
                        }`}>
                          {log.level}
                        </span>
                        <span className="text-foreground">{log.message}</span>
                      </motion.div>
                    ))
                  )}
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
                {JSON.stringify({
                  source: "production_db",
                  target: "warehouse",
                  date: "2024-01-07",
                  batch_size: 1000,
                  parallel: true
                }, null, 2)}
              </pre>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
