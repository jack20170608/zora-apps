"use client"

import { useParams } from "next/navigation"
import Link from "next/link"
import { useState, useMemo } from "react"
import ReactFlow, {
  Background,
  Controls,
  Handle,
  Position,
  type Node,
  type Edge,
} from "reactflow"
import "reactflow/dist/style.css"
import {
  ArrowLeft,
  GitBranch,
  Play,
  Pencil,
  Trash2,
  Clock,
  CheckCircle2,
  XCircle,
  History,
  GitCompare,
  ArrowRight,
  Plus,
  Minus,
  Download,
  Upload,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import type { Workflow, WorkflowVersion, ExecutionStatus, DagNodeData } from "@/types"
import { formatDate, formatRelativeTime } from "@/lib/utils"

const nodeTypeColors: Record<string, string> = {
  task: "#3B82F6",
  shell: "#06B6D4",
  python: "#FBBF24",
  java: "#F97316",
  docker: "#0EA5E9",
  database: "#10B981",
  decision: "#EC4899",
  fork: "#8B5CF6",
  join: "#6366F1",
}

const mockWorkflow: Workflow = {
  id: "wf-001",
  key: "etl-daily",
  name: "Daily ETL Pipeline",
  description: "Extract, transform and load daily data from multiple sources",
  version: "2.1.0",
  active: true,
  dagDefinition: { nodes: [], edges: [] },
  parameterSchema: { fields: [] },
  tags: ["etl", "daily", "production"],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-07T00:00:00Z",
  executionCount: 156,
  successRate: 98.1,
}

const mockVersions: WorkflowVersion[] = [
  { version: "2.1.0", createdAt: "2024-01-07T00:00:00Z", createdBy: "admin", active: true, dagDefinition: { nodes: [], edges: [] } },
  { version: "2.0.0", createdAt: "2024-01-01T00:00:00Z", createdBy: "admin", active: false, dagDefinition: { nodes: [], edges: [] } },
  { version: "1.5.0", createdAt: "2023-12-15T00:00:00Z", createdBy: "admin", active: false, dagDefinition: { nodes: [], edges: [] } },
]

const mockExecutions = [
  { id: "exec-001", status: "running" as ExecutionStatus, startedAt: "2024-01-07T10:30:00Z" },
  { id: "exec-002", status: "success" as ExecutionStatus, startedAt: "2024-01-07T09:00:00Z", endedAt: "2024-01-07T09:15:00Z" },
  { id: "exec-003", status: "success" as ExecutionStatus, startedAt: "2024-01-06T10:30:00Z", endedAt: "2024-01-06T10:45:00Z" },
  { id: "exec-004", status: "failed" as ExecutionStatus, startedAt: "2024-01-05T10:30:00Z", endedAt: "2024-01-05T10:50:00Z" },
]

export default function WorkflowDetailPage() {
  const params = useParams()
  const workflowId = params.id as string
  const workflow = mockWorkflow
  const [compareVersions, setCompareVersions] = useState<{left: string; right: string}>({
    left: "2.0.0",
    right: "2.1.0"
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link href="/workflows">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight">{workflow.name}</h1>
              <Badge variant={workflow.active ? "success" : "outline"}>
                {workflow.active ? "Active" : "Inactive"}
              </Badge>
            </div>
            <p className="text-muted-foreground font-mono text-sm">{workflow.key}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button 
            variant="outline" 
            onClick={() => {
              const dataStr = JSON.stringify(workflow, null, 2)
              const dataBlob = new Blob([dataStr], { type: 'application/json' })
              const url = URL.createObjectURL(dataBlob)
              const link = document.createElement('a')
              link.href = url
              link.download = `${workflow.key}.json`
              document.body.appendChild(link)
              link.click()
              document.body.removeChild(link)
              URL.revokeObjectURL(url)
            }}
          >
            <Download className="mr-2 h-4 w-4" />
            Export
          </Button>
          <Button variant="outline">
            <Play className="mr-2 h-4 w-4" />
            Execute
          </Button>
          <Link href={`/studio?workflow=${workflow.key}`}>
            <Button variant="outline">
              <Pencil className="mr-2 h-4 w-4" />
              Edit
            </Button>
          </Link>
          <Button variant="destructive" size="icon">
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Version</p>
            <p className="text-lg font-bold">v{workflow.version}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Total Runs</p>
            <p className="text-lg font-bold">{workflow.executionCount}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Success Rate</p>
            <p className="text-lg font-bold text-emerald-400">{workflow.successRate}%</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Last Updated</p>
            <p className="text-sm font-medium">{formatRelativeTime(workflow.updatedAt)}</p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="dag">
        <TabsList>
          <TabsTrigger value="dag">DAG Preview</TabsTrigger>
          <TabsTrigger value="versions">Versions</TabsTrigger>
          <TabsTrigger value="compare">Compare</TabsTrigger>
          <TabsTrigger value="executions">Executions</TabsTrigger>
          <TabsTrigger value="parameters">Parameters</TabsTrigger>
        </TabsList>

        <TabsContent value="dag">
          <Card>
            <CardContent className="p-0">
              <DagPreview dagDefinition={workflow.dagDefinition} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="versions">
          <Card>
            <CardContent className="p-0">
              <div className="divide-y">
                {mockVersions.map((version) => (
                  <div key={version.version} className="flex items-center justify-between p-4 hover:bg-muted/50">
                    <div className="flex items-center gap-4">
                      <History className="h-5 w-5 text-muted-foreground" />
                      <div>
                        <p className="font-medium">v{version.version}</p>
                        <p className="text-xs text-muted-foreground">
                          by {version.createdBy} • {formatDate(version.createdAt)}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {version.active && <Badge variant="success">Active</Badge>}
                      <Button variant="ghost" size="sm">Activate</Button>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="executions">
          <Card>
            <CardContent className="p-0">
              <div className="divide-y">
                {mockExecutions.map((exec) => (
                  <div key={exec.id} className="flex items-center justify-between p-4 hover:bg-muted/50">
                    <div>
                      <p className="font-mono text-sm">{exec.id}</p>
                      <p className="text-xs text-muted-foreground">
                        {formatRelativeTime(exec.startedAt)}
                      </p>
                    </div>
                    <Badge variant={
                      exec.status === 'success' ? 'success' :
                      exec.status === 'failed' ? 'failure' :
                      exec.status === 'running' ? 'running' : 'pending'
                    }>
                      {exec.status}
                    </Badge>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="compare">
          <Card>
            <CardHeader>
              <CardTitle>Version Comparison</CardTitle>
              <CardDescription>Compare changes between versions</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4 mb-6">
                <div className="flex-1">
                  <label className="text-xs font-medium text-muted-foreground mb-1.5 block">Base Version</label>
                  <select 
                    className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                    value={compareVersions.left}
                    onChange={e => setCompareVersions({...compareVersions, left: e.target.value})}
                  >
                    {mockVersions.map(v => (
                      <option key={v.version} value={v.version}>v{v.version}</option>
                    ))}
                  </select>
                </div>
                <div className="pt-5">
                  <ArrowRight className="h-5 w-5 text-muted-foreground" />
                </div>
                <div className="flex-1">
                  <label className="text-xs font-medium text-muted-foreground mb-1.5 block">Compare Version</label>
                  <select 
                    className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                    value={compareVersions.right}
                    onChange={e => setCompareVersions({...compareVersions, right: e.target.value})}
                  >
                    {mockVersions.map(v => (
                      <option key={v.version} value={v.version}>v{v.version}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="space-y-4">
                <div className="flex items-center gap-3 p-3 rounded-lg bg-emerald-500/5 border border-emerald-500/10">
                  <Plus className="h-4 w-4 text-emerald-400" />
                  <div>
                    <p className="text-sm font-medium text-emerald-400">Added: Data Verification Node</p>
                    <p className="text-xs text-muted-foreground">New validation step at end of pipeline</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-3 rounded-lg bg-amber-500/5 border border-amber-500/10">
                  <GitCompare className="h-4 w-4 text-amber-400" />
                  <div>
                    <p className="text-sm font-medium text-amber-400">Modified: Extract Data Node</p>
                    <p className="text-xs text-muted-foreground">Changed timeout from 300s to 600s</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-3 rounded-lg bg-blue-500/5 border border-blue-500/10">
                  <History className="h-4 w-4 text-blue-400" />
                  <div>
                    <p className="text-sm font-medium text-blue-400">Unchanged: Transform Data Node</p>
                    <p className="text-xs text-muted-foreground">No modifications in this version</p>
                  </div>
                </div>
              </div>

              <div className="mt-6 p-4 rounded-lg bg-muted">
                <p className="text-sm font-medium mb-2">Summary</p>
                <div className="flex gap-4 text-xs">
                  <span className="text-emerald-400">1 addition</span>
                  <span className="text-amber-400">1 modification</span>
                  <span className="text-blue-400">1 unchanged</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="parameters">
          <Card>
            <CardHeader>
              <CardTitle>Parameter Schema</CardTitle>
              <CardDescription>Define input parameters for this workflow</CardDescription>
            </CardHeader>
            <CardContent>
              <pre className="p-4 rounded-lg bg-muted font-mono text-sm">
                {JSON.stringify(workflow.parameterSchema, null, 2)}
              </pre>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

function DagPreview({ dagDefinition }: { dagDefinition: { nodes: any[]; edges: any[] } }) {
  const nodes = useMemo(() => {
    return dagDefinition.nodes.map((n: any) => ({
      id: n.id,
      type: "preview",
      position: n.position,
      data: n.data,
    })) as Node[]
  }, [dagDefinition])

  const edges = useMemo(() => {
    return dagDefinition.edges.map((e: any) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      animated: true,
    })) as Edge[]
  }, [dagDefinition])

  const nodeTypes = useMemo(() => ({
    preview: ({ data }: { data: DagNodeData }) => {
      const color = nodeTypeColors[data.type] || "#6B7280"
      return (
        <div className="shadow-md rounded-lg px-3 py-2 border border-border bg-card min-w-[100px]">
          <Handle type="target" position={Position.Left} className="!bg-border !w-2.5 !h-2.5" />
          <div className="flex items-center gap-1.5">
            <div className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: color }} />
            <div>
              <div className="text-xs font-medium">{data.label}</div>
            </div>
          </div>
          <Handle type="source" position={Position.Right} className="!bg-border !w-2.5 !h-2.5" />
        </div>
      )
    },
  }), [])

  return (
    <div className="h-[400px]">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        fitView
        minZoom={0.2}
        maxZoom={1.5}
        nodesDraggable={false}
        nodesConnectable={false}
        elementsSelectable={false}
      >
        <Background />
        <Controls />
      </ReactFlow>
    </div>
  )
}
