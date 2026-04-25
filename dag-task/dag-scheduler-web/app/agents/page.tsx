"use client"

import { motion } from "framer-motion"
import {
  Server,
  Cpu,
  HardDrive,
  Activity,
  CheckCircle2,
  XCircle,
  Clock,
  Tag,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import type { Agent, AgentStatus } from "@/types"
import { formatRelativeTime } from "@/lib/utils"

const mockAgents: Agent[] = [
  {
    id: "agent-001",
    name: "worker-01",
    host: "192.168.1.101",
    status: "online",
    version: "2.1.0",
    capabilities: ["shell", "python", "docker"],
    currentTasks: 3,
    maxTasks: 10,
    cpuUsage: 45,
    memoryUsage: 62,
    diskUsage: 30,
    lastHeartbeat: "2024-01-07T10:55:00Z",
    tags: ["production", "gpu"],
  },
  {
    id: "agent-002",
    name: "worker-02",
    host: "192.168.1.102",
    status: "busy",
    version: "2.1.0",
    capabilities: ["shell", "python", "java"],
    currentTasks: 10,
    maxTasks: 10,
    cpuUsage: 89,
    memoryUsage: 78,
    diskUsage: 45,
    lastHeartbeat: "2024-01-07T10:54:00Z",
    tags: ["production"],
  },
  {
    id: "agent-003",
    name: "worker-03",
    host: "192.168.1.103",
    status: "online",
    version: "2.0.1",
    capabilities: ["shell", "database"],
    currentTasks: 1,
    maxTasks: 5,
    cpuUsage: 23,
    memoryUsage: 34,
    diskUsage: 60,
    lastHeartbeat: "2024-01-07T10:56:00Z",
    tags: ["staging"],
  },
  {
    id: "agent-004",
    name: "gpu-01",
    host: "192.168.1.201",
    status: "offline",
    version: "2.1.0",
    capabilities: ["python", "ml"],
    currentTasks: 0,
    maxTasks: 4,
    cpuUsage: 0,
    memoryUsage: 0,
    diskUsage: 25,
    lastHeartbeat: "2024-01-07T08:00:00Z",
    tags: ["production", "gpu", "ml"],
  },
]

const statusConfig: Record<AgentStatus, { label: string; variant: any; color: string; icon: any }> = {
  online: { label: "Online", variant: "success", color: "#10B981", icon: CheckCircle2 },
  offline: { label: "Offline", variant: "failure", color: "#EF4444", icon: XCircle },
  busy: { label: "Busy", variant: "warning", color: "#F59E0B", icon: Activity },
}

function AgentCard({ agent }: { agent: Agent }) {
  const config = statusConfig[agent.status]
  const StatusIcon = config.icon
  const loadPercent = (agent.currentTasks / agent.maxTasks) * 100

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <Card className="hover:shadow-lg transition-shadow">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className={`p-2.5 rounded-lg`} style={{ backgroundColor: `${config.color}15` }}>
                <Server className="h-5 w-5" style={{ color: config.color }} />
              </div>
              <div>
                <CardTitle className="text-base">{agent.name}</CardTitle>
                <p className="text-xs text-muted-foreground font-mono">{agent.host}</p>
              </div>
            </div>
            <Badge variant={config.variant} className="gap-1">
              <StatusIcon className="h-3 w-3" />
              {config.label}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-1.5">
            {agent.tags.map((tag) => (
              <Badge key={tag} variant="secondary" className="text-xs">
                {tag}
              </Badge>
            ))}
          </div>

          <div className="space-y-3">
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground flex items-center gap-1">
                  <Cpu className="h-3 w-3" /> CPU
                </span>
                <span className={agent.cpuUsage > 80 ? 'text-red-400' : agent.cpuUsage > 60 ? 'text-amber-400' : 'text-emerald-400'}>
                  {agent.cpuUsage}%
                </span>
              </div>
              <Progress value={agent.cpuUsage} className="h-1.5"
                style={{ backgroundColor: 'hsl(var(--muted))' }}
              />
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground flex items-center gap-1">
                  <HardDrive className="h-3 w-3" /> Memory
                </span>
                <span className={agent.memoryUsage > 80 ? 'text-red-400' : agent.memoryUsage > 60 ? 'text-amber-400' : 'text-emerald-400'}>
                  {agent.memoryUsage}%
                </span>
              </div>
              <Progress value={agent.memoryUsage} className="h-1.5"
                style={{ backgroundColor: 'hsl(var(--muted))' }}
              />
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground flex items-center gap-1">
                  <Activity className="h-3 w-3" /> Tasks
                </span>
                <span>{agent.currentTasks}/{agent.maxTasks}</span>
              </div>
              <Progress value={loadPercent} className="h-1.5"
                style={{ backgroundColor: 'hsl(var(--muted))' }}
              />
            </div>
          </div>

          <div className="flex items-center justify-between pt-2 border-t text-xs text-muted-foreground">
            <span>v{agent.version}</span>
            <span className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {formatRelativeTime(agent.lastHeartbeat)}
            </span>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  )
}

export default function AgentsPage() {
  const onlineCount = mockAgents.filter(a => a.status === 'online').length
  const busyCount = mockAgents.filter(a => a.status === 'busy').length
  const offlineCount = mockAgents.filter(a => a.status === 'offline').length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Agents</h1>
        <p className="text-muted-foreground">
          Monitor and manage execution agents in your cluster.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-lg bg-emerald-500/10">
              <CheckCircle2 className="h-6 w-6 text-emerald-400" />
            </div>
            <div>
              <p className="text-2xl font-bold">{onlineCount}</p>
              <p className="text-xs text-muted-foreground">Online</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-lg bg-amber-500/10">
              <Activity className="h-6 w-6 text-amber-400" />
            </div>
            <div>
              <p className="text-2xl font-bold">{busyCount}</p>
              <p className="text-xs text-muted-foreground">Busy</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-lg bg-red-500/10">
              <XCircle className="h-6 w-6 text-red-400" />
            </div>
            <div>
              <p className="text-2xl font-bold">{offlineCount}</p>
              <p className="text-xs text-muted-foreground">Offline</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {mockAgents.map((agent) => (
          <AgentCard key={agent.id} agent={agent} />
        ))}
      </div>
    </div>
  )
}
