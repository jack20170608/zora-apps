"use client"

import { useState } from "react"
import Link from "next/link"
import { motion } from "framer-motion"
import {
  Search,
  Filter,
  Clock,
  CheckCircle2,
  XCircle,
  Activity,
  RotateCcw,
  X,
  ChevronDown,
  Calendar,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import type { Execution, ExecutionStatus } from "@/types"
import { formatDate, formatDuration, formatRelativeTime } from "@/lib/utils"

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

const mockExecutions: Execution[] = [
  {
    id: "exec-001",
    workflowId: "wf-001",
    workflowKey: "etl-daily",
    workflowName: "Daily ETL Pipeline",
    version: "2.1.0",
    status: "running",
    triggerType: "scheduled",
    startedAt: "2024-01-07T10:30:00Z",
    progress: 65,
  },
  {
    id: "exec-002",
    workflowId: "wf-002",
    workflowKey: "backup-weekly",
    workflowName: "Weekly Database Backup",
    version: "1.5.0",
    status: "success",
    triggerType: "manual",
    startedAt: "2024-01-07T09:00:00Z",
    endedAt: "2024-01-07T09:15:00Z",
    duration: 900000,
  },
  {
    id: "exec-003",
    workflowId: "wf-003",
    workflowKey: "ml-training",
    workflowName: "ML Model Training",
    version: "3.0.0",
    status: "failed",
    triggerType: "api",
    startedAt: "2024-01-07T08:00:00Z",
    endedAt: "2024-01-07T08:45:00Z",
    duration: 2700000,
    errorMessage: "GPU memory insufficient",
  },
  {
    id: "exec-004",
    workflowId: "wf-001",
    workflowKey: "etl-daily",
    workflowName: "Daily ETL Pipeline",
    version: "2.1.0",
    status: "success",
    triggerType: "scheduled",
    startedAt: "2024-01-06T10:30:00Z",
    endedAt: "2024-01-06T10:45:00Z",
    duration: 900000,
  },
  {
    id: "exec-005",
    workflowId: "wf-004",
    workflowKey: "data-cleanup",
    workflowName: "Data Cleanup Task",
    version: "1.0.0",
    status: "pending",
    triggerType: "scheduled",
    startedAt: "2024-01-07T11:00:00Z",
  },
  {
    id: "exec-006",
    workflowId: "wf-002",
    workflowKey: "backup-weekly",
    workflowName: "Weekly Database Backup",
    version: "1.5.0",
    status: "cancelled",
    triggerType: "manual",
    startedAt: "2024-01-07T07:00:00Z",
    endedAt: "2024-01-07T07:05:00Z",
    duration: 300000,
  },
  {
    id: "exec-007",
    workflowId: "wf-003",
    workflowKey: "ml-training",
    workflowName: "ML Model Training",
    version: "3.0.0",
    status: "retrying",
    triggerType: "api",
    startedAt: "2024-01-07T06:00:00Z",
  },
]

const statusOptions: ExecutionStatus[] = ["success", "failed", "running", "pending", "queued", "cancelled", "retrying", "skipped"]

export default function ExecutionsPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedStatuses, setSelectedStatuses] = useState<ExecutionStatus[]>([])
  const [showFilters, setShowFilters] = useState(false)

  const filteredExecutions = mockExecutions.filter((execution) => {
    const matchesSearch =
      execution.workflowName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      execution.id.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesStatus = selectedStatuses.length === 0 || selectedStatuses.includes(execution.status)
    return matchesSearch && matchesStatus
  })

  const toggleStatus = (status: ExecutionStatus) => {
    setSelectedStatuses((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Executions</h1>
          <p className="text-muted-foreground">
            Monitor and manage workflow executions.
          </p>
        </div>
        <Button variant="outline">
          <RotateCcw className="mr-2 h-4 w-4" />
          Refresh
        </Button>
      </div>

      <div className="space-y-4">
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search executions..."
              className="pl-9"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Button
            variant={showFilters ? "secondary" : "outline"}
            onClick={() => setShowFilters(!showFilters)}
          >
            <Filter className="mr-2 h-4 w-4" />
            Filters
            {selectedStatuses.length > 0 && (
              <Badge variant="secondary" className="ml-2">
                {selectedStatuses.length}
              </Badge>
            )}
          </Button>
          <Button variant="outline">
            <Calendar className="mr-2 h-4 w-4" />
            Last 7 Days
            <ChevronDown className="ml-2 h-4 w-4" />
          </Button>
        </div>

        {showFilters && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="flex flex-wrap gap-2 p-4 rounded-lg bg-muted/50"
          >
            {statusOptions.map((status) => {
              const config = statusConfig[status]
              const isSelected = selectedStatuses.includes(status)
              return (
                <button
                  key={status}
                  onClick={() => toggleStatus(status)}
                  className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                    isSelected
                      ? "ring-2 ring-offset-2 ring-offset-background"
                      : "opacity-60 hover:opacity-100"
                  }`}
                  style={{
                    backgroundColor: `${config.color}15`,
                    color: config.color,
                    boxShadow: isSelected ? `0 0 0 2px ${config.color}` : undefined,
                  }}
                >
                  {isSelected && <CheckCircle2 className="h-3 w-3" />}
                  {config.label}
                </button>
              )
            })}
            {selectedStatuses.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSelectedStatuses([])}
              >
                <X className="mr-1 h-3 w-3" />
                Clear
              </Button>
            )}
          </motion.div>
        )}
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-4 text-xs font-medium text-muted-foreground">Execution</th>
                  <th className="text-left p-4 text-xs font-medium text-muted-foreground">Workflow</th>
                  <th className="text-left p-4 text-xs font-medium text-muted-foreground">Status</th>
                  <th className="text-left p-4 text-xs font-medium text-muted-foreground">Trigger</th>
                  <th className="text-left p-4 text-xs font-medium text-muted-foreground">Started</th>
                  <th className="text-left p-4 text-xs font-medium text-muted-foreground">Duration</th>
                  <th className="text-right p-4 text-xs font-medium text-muted-foreground">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {filteredExecutions.map((execution) => {
                  const config = statusConfig[execution.status]
                  return (
                    <motion.tr
                      key={execution.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="hover:bg-muted/50 transition-colors"
                    >
                      <td className="p-4">
                        <Link href={`/executions/${execution.id}`} className="block">
                          <p className="text-sm font-mono font-medium">{execution.id}</p>
                          <p className="text-xs text-muted-foreground">v{execution.version}</p>
                        </Link>
                      </td>
                      <td className="p-4">
                        <p className="text-sm font-medium">{execution.workflowName}</p>
                        <p className="text-xs text-muted-foreground font-mono">{execution.workflowKey}</p>
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          {execution.status === 'running' && execution.progress && (
                            <div className="w-12 h-1 bg-muted rounded-full overflow-hidden">
                              <div
                                className="h-full rounded-full transition-all"
                                style={{ width: `${execution.progress}%`, backgroundColor: config.color }}
                              />
                            </div>
                          )}
                          <Badge variant={config.variant}>{config.label}</Badge>
                        </div>
                      </td>
                      <td className="p-4">
                        <span className="text-sm capitalize">{execution.triggerType}</span>
                      </td>
                      <td className="p-4">
                        <span className="text-sm text-muted-foreground">
                          {execution.startedAt ? formatRelativeTime(execution.startedAt) : '-'}
                        </span>
                      </td>
                      <td className="p-4">
                        <span className="text-sm text-muted-foreground">
                          {execution.duration ? formatDuration(execution.duration) : '-'}
                        </span>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex justify-end gap-2">
                          {execution.status === 'running' && (
                            <Button variant="ghost" size="sm">
                              <X className="h-4 w-4" />
                            </Button>
                          )}
                          {(execution.status === 'failed' || execution.status === 'cancelled') && (
                            <Button variant="ghost" size="sm">
                              <RotateCcw className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </td>
                    </motion.tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
