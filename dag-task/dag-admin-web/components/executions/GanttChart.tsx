"use client"

import { formatDuration } from "@/lib/utils"
import type { ExecutionStatus } from "@/types"

const statusColors: Record<ExecutionStatus, string> = {
  success: "#10B981",
  failed: "#EF4444",
  running: "#3B82F6",
  pending: "#6B7280",
  queued: "#F59E0B",
  cancelled: "#F59E0B",
  retrying: "#F59E0B",
  skipped: "#8B5CF6",
}

interface GanttTask {
  id: string
  nodeName: string
  status: ExecutionStatus
  startedAt?: string
  duration?: number
}

export function GanttChart({ tasks, totalDuration }: { tasks: GanttTask[]; totalDuration: number }) {
  const startTime = tasks[0]?.startedAt 
    ? new Date(tasks[0].startedAt).getTime() 
    : Date.now()

  return (
    <div className="space-y-2">
      {tasks.map((task) => {
        const taskStart = task.startedAt 
          ? new Date(task.startedAt).getTime() - startTime 
          : 0
        const taskDuration = task.duration || 0
        const leftPercent = totalDuration > 0 ? (taskStart / totalDuration) * 100 : 0
        const widthPercent = totalDuration > 0 ? (taskDuration / totalDuration) * 100 : 0
        const color = statusColors[task.status]

        return (
          <div key={task.id} className="flex items-center gap-4">
            <div className="w-32 shrink-0 text-sm truncate" title={task.nodeName}>
              {task.nodeName}
            </div>
            <div className="flex-1 h-8 bg-muted rounded-md relative overflow-hidden">
              {taskDuration > 0 && (
                <div
                  className="absolute top-1 bottom-1 rounded-sm transition-all"
                  style={{
                    left: `${leftPercent}%`,
                    width: `${Math.max(widthPercent, 2)}%`,
                    backgroundColor: color,
                  }}
                />
              )}
            </div>
            <div className="w-20 shrink-0 text-xs text-muted-foreground text-right">
              {task.duration && task.duration > 0 ? formatDuration(task.duration) : "Pending"}
            </div>
          </div>
        )
      })}
    </div>
  )
}
