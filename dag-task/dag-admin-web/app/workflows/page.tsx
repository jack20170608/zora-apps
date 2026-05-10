"use client"

import { useState } from "react"
import Link from "next/link"
import { motion } from "framer-motion"
import {
  GitBranch,
  Plus,
  Search,
  Filter,
  Grid3X3,
  List,
  MoreHorizontal,
  Play,
  Pencil,
  Trash2,
  Clock,
  CheckCircle2,
  XCircle,
} from "lucide-react"
import { PageTransition } from "@/components/ui/page-transition"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useWorkflows, useDeleteWorkflow, useExecuteWorkflow } from "@/hooks/use-api"
import type { Workflow } from "@/types"
import { formatRelativeTime } from "@/lib/utils"

type ViewMode = "grid" | "list"

const statusIcons = {
  success: CheckCircle2,
  failed: XCircle,
  running: Clock,
  pending: Clock,
}

const statusColors = {
  success: "text-emerald-400",
  failed: "text-red-400",
  running: "text-blue-400",
  pending: "text-gray-400",
}

function WorkflowCard({ workflow }: { workflow: Workflow }) {
  const executeMutation = useExecuteWorkflow()

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      whileHover={{ y: -2 }}
      transition={{ duration: 0.2 }}
    >
      <Card className="h-full hover:shadow-lg transition-shadow">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-lg bg-primary/10">
                <GitBranch className="h-4 w-4 text-primary" />
              </div>
              <div>
                <CardTitle className="text-base">{workflow.name}</CardTitle>
                <p className="text-xs text-muted-foreground font-mono">{workflow.key}</p>
              </div>
            </div>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => executeMutation.mutate({ id: workflow.key })}>
                  <Play className="mr-2 h-4 w-4" /> Execute
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href={`/studio?workflow=${workflow.key}`}>
                    <Pencil className="mr-2 h-4 w-4" /> Edit
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem className="text-destructive">
                  <Trash2 className="mr-2 h-4 w-4" /> Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground line-clamp-2">{workflow.description}</p>
          
          <div className="flex flex-wrap gap-1.5">
            {workflow.tags?.map((tag) => (
              <Badge key={tag} variant="secondary" className="text-xs">
                {tag}
              </Badge>
            ))}
          </div>

          <div className="flex items-center justify-between pt-2 border-t">
            <div className="flex items-center gap-4 text-xs text-muted-foreground">
              <span>v{workflow.version}</span>
              <span>{workflow.executionCount || 0} runs</span>
            </div>
            {workflow.lastExecution && (
              <div className="flex items-center gap-1.5">
                {(() => {
                  const Icon = statusIcons[workflow.lastExecution.status as keyof typeof statusIcons] || Clock
                  return <Icon className={`h-3.5 w-3.5 ${statusColors[workflow.lastExecution.status as keyof typeof statusColors] || 'text-gray-400'}`} />
                })()}
                <span className="text-xs text-muted-foreground">
                  {formatRelativeTime(workflow.lastExecution.startedAt)}
                </span>
              </div>
            )}
          </div>

          {workflow.successRate !== undefined && (
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground">Success Rate</span>
                <span className={workflow.successRate >= 90 ? 'text-emerald-400' : workflow.successRate >= 70 ? 'text-amber-400' : 'text-red-400'}>
                  {workflow.successRate}%
                </span>
              </div>
              <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                <div 
                  className={`h-full rounded-full transition-all ${
                    workflow.successRate >= 90 ? 'bg-emerald-500' : 
                    workflow.successRate >= 70 ? 'bg-amber-500' : 'bg-red-500'
                  }`}
                  style={{ width: `${workflow.successRate}%` }}
                />
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </motion.div>
  )
}

function WorkflowCardSkeleton() {
  return (
    <Card className="h-full">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <Skeleton className="h-8 w-8 rounded-lg" />
            <div className="space-y-1.5">
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-3 w-20" />
            </div>
          </div>
          <Skeleton className="h-8 w-8" />
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
        <div className="flex gap-1.5">
          <Skeleton className="h-4 w-12" />
          <Skeleton className="h-4 w-12" />
        </div>
        <Skeleton className="h-3 w-full" />
        <Skeleton className="h-3 w-full" />
      </CardContent>
    </Card>
  )
}

export default function WorkflowsPage() {
  const [viewMode, setViewMode] = useState<ViewMode>("grid")
  const [searchQuery, setSearchQuery] = useState("")
  
  const { data: workflowsData, isLoading } = useWorkflows({ page: 1, pageSize: 50 })
  
  const workflows: Workflow[] = workflowsData?.items || []

  const filteredWorkflows = workflows.filter(
    (w) =>
      w.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      w.key.toLowerCase().includes(searchQuery.toLowerCase()) ||
      w.tags?.some((t) => t.toLowerCase().includes(searchQuery.toLowerCase()))
  )

  return (
    <PageTransition>
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Workflows</h1>
          <p className="text-muted-foreground">
            Manage and monitor your workflow templates.
          </p>
        </div>
        <Link href="/studio">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            New Workflow
          </Button>
        </Link>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search workflows..."
            className="pl-9"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <Button variant="outline" size="icon">
          <Filter className="h-4 w-4" />
        </Button>
        <div className="flex items-center border rounded-md p-1">
          <Button
            variant={viewMode === "grid" ? "secondary" : "ghost"}
            size="icon"
            className="h-8 w-8"
            onClick={() => setViewMode("grid")}
          >
            <Grid3X3 className="h-4 w-4" />
          </Button>
          <Button
            variant={viewMode === "list" ? "secondary" : "ghost"}
            size="icon"
            className="h-8 w-8"
            onClick={() => setViewMode("list")}
          >
            <List className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          <WorkflowCardSkeleton />
          <WorkflowCardSkeleton />
          <WorkflowCardSkeleton />
        </div>
      ) : viewMode === "grid" ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredWorkflows.map((workflow) => (
            <WorkflowCard key={workflow.id} workflow={workflow} />
          ))}
        </div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <div className="divide-y">
              {filteredWorkflows.map((workflow) => (
                <div key={workflow.id} className="flex items-center justify-between p-4 hover:bg-muted/50 transition-colors">
                  <div className="flex items-center gap-4">
                    <GitBranch className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <p className="font-medium">{workflow.name}</p>
                      <p className="text-xs text-muted-foreground">{workflow.key} • v{workflow.version}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="flex gap-1">
                      {workflow.tags?.map((tag) => (
                        <Badge key={tag} variant="secondary" className="text-xs">{tag}</Badge>
                      ))}
                    </div>
                    <span className="text-sm text-muted-foreground">{workflow.executionCount || 0} runs</span>
                    <div className="flex items-center gap-2">
                      <Button variant="ghost" size="icon" className="h-8 w-8">
                        <Play className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" className="h-8 w-8">
                        <Pencil className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
    </PageTransition>
  )
}
