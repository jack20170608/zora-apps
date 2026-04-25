"use client"

import { useParams } from "next/navigation"
import Link from "next/link"
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
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import type { Workflow, WorkflowVersion, ExecutionStatus } from "@/types"
import { formatDate, formatRelativeTime } from "@/lib/utils"

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
          <Button variant="outline">
            <Play className="mr-2 h-4 w-4" />
            Execute
          </Button>
          <Link href="/studio">
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
          <TabsTrigger value="executions">Executions</TabsTrigger>
          <TabsTrigger value="parameters">Parameters</TabsTrigger>
        </TabsList>

        <TabsContent value="dag">
          <Card>
            <CardContent className="p-8">
              <div className="flex items-center justify-center h-[300px] text-muted-foreground">
                <div className="text-center">
                  <GitBranch className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>DAG visualization will be rendered here</p>
                </div>
              </div>
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
