"use client"

import { motion } from "framer-motion"
import {
  GitBranch,
  Play,
  CheckCircle2,
  XCircle,
  Clock,
  Server,
  Activity,
  TrendingUp,
  AlertTriangle,
  Bell,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { useToast } from "@/components/ui/toast-provider"
import { PageTransition, StaggerContainer, StaggerItem } from "@/components/ui/page-transition"
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts"
import type { Execution, ExecutionStatus } from "@/types"
import { formatRelativeTime } from "@/lib/utils"
import { useDashboardStats, useTrends } from "@/hooks/use-api"
import { Skeleton } from "@/components/ui/skeleton"

const statusConfig: Record<ExecutionStatus, { label: string; variant: any; color: string; icon: any }> = {
  success: { label: "Success", variant: "success", color: "#10B981", icon: CheckCircle2 },
  failed: { label: "Failed", variant: "failure", color: "#EF4444", icon: XCircle },
  running: { label: "Running", variant: "running", color: "#3B82F6", icon: Activity },
  pending: { label: "Pending", variant: "pending", color: "#6B7280", icon: Clock },
  queued: { label: "Queued", variant: "warning", color: "#F59E0B", icon: Clock },
  cancelled: { label: "Cancelled", variant: "warning", color: "#F59E0B", icon: XCircle },
  retrying: { label: "Retrying", variant: "warning", color: "#F59E0B", icon: Activity },
  skipped: { label: "Skipped", variant: "skipped", color: "#8B5CF6", icon: CheckCircle2 },
}

const statusDistribution = [
  { name: "Success", value: 1425, color: "#10B981" },
  { name: "Failed", value: 45, color: "#EF4444" },
  { name: "Running", value: 7, color: "#3B82F6" },
  { name: "Pending", value: 46, color: "#6B7280" },
]

function getStatusDistribution(stats: any) {
  if (!stats) return statusDistribution
  return [
    { name: "Success", value: stats.totalExecutions - stats.failedExecutions24h, color: "#10B981" },
    { name: "Failed", value: stats.failedExecutions24h, color: "#EF4444" },
    { name: "Running", value: stats.runningExecutions, color: "#3B82F6" },
    { name: "Pending", value: Math.max(0, stats.totalExecutions - stats.runningExecutions - stats.failedExecutions24h), color: "#6B7280" },
  ]
}

function StatCard({ title, value, description, icon: Icon, trend }: {
  title: string
  value: string | number
  description?: string
  icon: any
  trend?: { value: number; positive: boolean }
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            {title}
          </CardTitle>
          <Icon className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{value}</div>
          {description && (
            <p className="text-xs text-muted-foreground mt-1">{description}</p>
          )}
          {trend && (
            <div className={`flex items-center gap-1 mt-2 text-xs ${trend.positive ? 'text-emerald-400' : 'text-red-400'}`}>
              <TrendingUp className="h-3 w-3" />
              <span>{trend.value}%</span>
            </div>
          )}
        </CardContent>
      </Card>
    </motion.div>
  )
}

export default function OverviewPage() {
  const { addToast } = useToast()
  const { data: stats, isLoading: statsLoading } = useDashboardStats()
  const { data: trendsData } = useTrends(7)

  const trendItems = trendsData || []
  const mockRecentExecutions: Execution[] = [
    { id: "exec-001", workflowId: "wf-001", workflowKey: "etl-daily", workflowName: "Daily ETL Pipeline", version: "2.1.0", status: "running", triggerType: "scheduled", startedAt: "2024-01-07T10:30:00Z", progress: 65 },
    { id: "exec-002", workflowId: "wf-002", workflowKey: "backup-weekly", workflowName: "Weekly Database Backup", version: "1.5.0", status: "success", triggerType: "manual", startedAt: "2024-01-07T09:00:00Z", endedAt: "2024-01-07T09:15:00Z", duration: 900000 },
    { id: "exec-003", workflowId: "wf-003", workflowKey: "ml-training", workflowName: "ML Model Training", version: "3.0.0", status: "failed", triggerType: "api", startedAt: "2024-01-07T08:00:00Z", endedAt: "2024-01-07T08:45:00Z", duration: 2700000, errorMessage: "GPU memory insufficient" },
    { id: "exec-008", workflowId: "wf-005", workflowKey: "email-notifications", workflowName: "Email Notification System", version: "1.2.0", status: "success", triggerType: "scheduled", startedAt: "2024-01-07T10:00:00Z", endedAt: "2024-01-07T10:02:00Z", duration: 120000 },
    { id: "exec-010", workflowId: "wf-007", workflowKey: "log-analysis", workflowName: "Log Analysis Pipeline", version: "1.8.0", status: "success", triggerType: "scheduled", startedAt: "2024-01-07T07:00:00Z", endedAt: "2024-01-07T07:30:00Z", duration: 1800000 },
    { id: "exec-011", workflowId: "wf-008", workflowKey: "api-health-check", workflowName: "API Health Check", version: "1.0.0", status: "running", triggerType: "scheduled", startedAt: "2024-01-07T10:45:00Z", progress: 30 },
  ]

  return (
    <PageTransition>
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Overview</h1>
          <p className="text-muted-foreground">
            Monitor system health and workflow performance at a glance.
          </p>
        </div>
        <div className="flex gap-2">
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => addToast({
              title: "System Status",
              description: "All systems operational",
              type: "success"
            })}
          >
            <Bell className="mr-2 h-4 w-4" />
            Test Toast
          </Button>
          <Button>
            <Play className="mr-2 h-4 w-4" />
            New Execution
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statsLoading ? (
          <>
            <Card><CardContent className="p-6"><Skeleton className="h-4 w-24 mb-2" /><Skeleton className="h-8 w-16" /></CardContent></Card>
            <Card><CardContent className="p-6"><Skeleton className="h-4 w-24 mb-2" /><Skeleton className="h-8 w-16" /></CardContent></Card>
            <Card><CardContent className="p-6"><Skeleton className="h-4 w-24 mb-2" /><Skeleton className="h-8 w-16" /></CardContent></Card>
            <Card><CardContent className="p-6"><Skeleton className="h-4 w-24 mb-2" /><Skeleton className="h-8 w-16" /></CardContent></Card>
          </>
        ) : (
          <>
            <StatCard title="Total Workflows" value={stats?.totalWorkflows || 0} description={`${stats?.activeWorkflows || 0} active`} icon={GitBranch} />
            <StatCard title="Running Executions" value={stats?.runningExecutions || 0} description="Currently processing" icon={Activity} />
            <StatCard title="Success Rate" value={`${stats?.successRate || 0}%`} description="Last 7 days" icon={CheckCircle2} />
            <StatCard title="Online Agents" value={`${stats?.onlineAgents || 0}/${stats?.totalAgents || 0}`} description="Available workers" icon={Server} />
          </>
        )}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Execution Trends</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={trendItems.length > 0 ? trendItems.map((t: any) => ({ date: t.date, successful: t.successes, failed: t.failures })) : []}>
                <defs>
                  <linearGradient id="colorSuccessful" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="colorFailed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#EF4444" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#EF4444" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                <XAxis dataKey="date" tickFormatter={(v) => new Date(v).toLocaleDateString("zh-CN", { month: "short", day: "numeric" })} stroke="#6B7280" />
                <YAxis stroke="#6B7280" />
                <Tooltip contentStyle={{ backgroundColor: "#1F2937", border: "1px solid #374151", borderRadius: "8px" }} />
                <Area type="monotone" dataKey="successful" stroke="#10B981" fill="url(#colorSuccessful)" />
                <Area type="monotone" dataKey="failed" stroke="#EF4444" fill="url(#colorFailed)" />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Status Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie data={getStatusDistribution(stats)} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                  {getStatusDistribution(stats).map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: "#1F2937", border: "1px solid #374151", borderRadius: "8px" }} />
              </PieChart>
            </ResponsiveContainer>
            <div className="flex flex-wrap gap-3 justify-center">
              {getStatusDistribution(stats).map((item) => (
                <div key={item.name} className="flex items-center gap-1.5">
                  <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: item.color }} />
                  <span className="text-xs text-muted-foreground">{item.name}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Recent Executions</CardTitle>
            <Button variant="ghost" size="sm">View All</Button>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-[320px]">
              <div className="space-y-3">
                {mockRecentExecutions.map((execution) => {
                  const config = statusConfig[execution.status]
                  const StatusIcon = config.icon
                  return (
                    <motion.div key={execution.id} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}
                      className="flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-full`} style={{ backgroundColor: `${config.color}15` }}>
                          <StatusIcon className="h-4 w-4" style={{ color: config.color }} />
                        </div>
                        <div>
                          <p className="text-sm font-medium">{execution.workflowName}</p>
                          <p className="text-xs text-muted-foreground">
                            {execution.triggerType} • {formatRelativeTime(execution.startedAt!)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {execution.status === 'running' && execution.progress && (
                          <div className="w-16 h-1.5 bg-muted rounded-full overflow-hidden">
                            <div className="h-full rounded-full transition-all" style={{ width: `${execution.progress}%`, backgroundColor: config.color }} />
                          </div>
                        )}
                        <Badge variant={config.variant}>{config.label}</Badge>
                      </div>
                    </motion.div>
                  )
                })}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>System Alerts</CardTitle>
            <Badge variant="warning">3 Active</Badge>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-[320px]">
              <div className="space-y-3">
                <div className="flex items-start gap-3 p-3 rounded-lg bg-red-500/5 border border-red-500/10">
                  <AlertTriangle className="h-5 w-5 text-red-400 mt-0.5 shrink-0" />
                  <div>
                    <p className="text-sm font-medium text-red-400">ML Model Training Failed</p>
                    <p className="text-xs text-muted-foreground mt-1">GPU memory insufficient - Agent gpu-01</p>
                    <p className="text-xs text-muted-foreground">2 hours ago</p>
                  </div>
                </div>
                <div className="flex items-start gap-3 p-3 rounded-lg bg-amber-500/5 border border-amber-500/10">
                  <AlertTriangle className="h-5 w-5 text-amber-400 mt-0.5 shrink-0" />
                  <div>
                    <p className="text-sm font-medium text-amber-400">High Agent Load</p>
                    <p className="text-xs text-muted-foreground mt-1">Agent worker-03 CPU usage 95%</p>
                    <p className="text-xs text-muted-foreground">5 hours ago</p>
                  </div>
                </div>
                <div className="flex items-start gap-3 p-3 rounded-lg bg-blue-500/5 border border-blue-500/10">
                  <AlertTriangle className="h-5 w-5 text-blue-400 mt-0.5 shrink-0" />
                  <div>
                    <p className="text-sm font-medium text-blue-400">Execution Delayed</p>
                    <p className="text-xs text-muted-foreground mt-1">ETL Pipeline queued for 15 minutes</p>
                    <p className="text-xs text-muted-foreground">1 hour ago</p>
                  </div>
                </div>
              </div>
            </ScrollArea>
          </CardContent>
        </Card>
      </div>
    </div>
    </PageTransition>
  )
}
