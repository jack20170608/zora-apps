import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { workflowApi, executionApi, agentApi, statsApi } from "@/lib/api/client"
import type { Workflow, Execution, Agent, DashboardStats, TrendData } from "@/types"
import {
  mockWorkflows,
  mockExecutions,
  mockAgents,
  mockDashboardStats,
  mockTrends,
} from "@/lib/api/mock-data"

// ==================== Workflow Hooks ====================

export function useWorkflows(params?: { page?: number; pageSize?: number; search?: string }) {
  return useQuery({
    queryKey: ["workflows", params],
    queryFn: async () => {
      try {
        const res = await workflowApi.list(params)
        return res.data.data
      } catch {
        return {
          items: mockWorkflows,
          total: mockWorkflows.length,
          page: params?.page || 1,
          pageSize: params?.pageSize || 50,
          totalPages: 1,
        }
      }
    },
  })
}

export function useWorkflow(key: string) {
  return useQuery({
    queryKey: ["workflow", key],
    queryFn: async () => {
      const res = await workflowApi.get(key)
      return res.data.data
    },
    enabled: !!key,
  })
}

export function useWorkflowVersions(key: string) {
  return useQuery({
    queryKey: ["workflow-versions", key],
    queryFn: async () => {
      const res = await workflowApi.getVersions(key)
      return res.data.data
    },
    enabled: !!key,
  })
}

export function useCreateWorkflow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: workflowApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["workflows"] }),
  })
}

export function useUpdateWorkflow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Workflow> }) =>
      workflowApi.update(id, data),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] })
      queryClient.invalidateQueries({ queryKey: ["workflow", vars.id] })
    },
  })
}

export function useDeleteWorkflow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: workflowApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["workflows"] }),
  })
}

export function useExecuteWorkflow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, parameters }: { id: string; parameters?: Record<string, unknown> }) =>
      workflowApi.execute(id, parameters),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["executions"] }),
  })
}

// ==================== Execution Hooks ====================

export function useExecutions(params?: { page?: number; pageSize?: number; status?: any[]; workflowId?: string }) {
  return useQuery({
    queryKey: ["executions", params],
    queryFn: async () => {
      const res = await executionApi.list(params as any)
      return res.data.data
    },
  })
}

export function useExecution(id: string) {
  return useQuery({
    queryKey: ["execution", id],
    queryFn: async () => {
      const res = await executionApi.get(id)
      return res.data.data
    },
    enabled: !!id,
  })
}

export function useExecutionTasks(id: string) {
  return useQuery({
    queryKey: ["execution-tasks", id],
    queryFn: async () => {
      const res = await executionApi.getTasks(id)
      return res.data.data
    },
    enabled: !!id,
  })
}

export function useRetryExecution() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, fromTask }: { id: string; fromTask?: string }) =>
      executionApi.retry(id, fromTask),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["executions"] }),
  })
}

export function useCancelExecution() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: executionApi.cancel,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["executions"] }),
  })
}

// ==================== Agent Hooks ====================

export function useAgents() {
  return useQuery({
    queryKey: ["agents"],
    queryFn: async () => {
      try {
        const res = await agentApi.list()
        return res.data.data
      } catch {
        return mockAgents
      }
    },
  })
}

export function useAgent(id: string) {
  return useQuery({
    queryKey: ["agent", id],
    queryFn: async () => {
      const res = await agentApi.get(id)
      return res.data.data
    },
    enabled: !!id,
  })
}

export function useAgentMetrics(id: string) {
  return useQuery({
    queryKey: ["agent-metrics", id],
    queryFn: async () => {
      const res = await agentApi.getMetrics(id)
      return res.data.data
    },
    enabled: !!id,
  })
}

// ==================== Stats Hooks ====================

export function useDashboardStats() {
  return useQuery({
    queryKey: ["stats", "overview"],
    queryFn: async () => {
      try {
        const res = await statsApi.getOverview()
        return res.data.data
      } catch {
        return mockDashboardStats
      }
    },
  })
}

export function useTrends(days?: number) {
  return useQuery({
    queryKey: ["stats", "trends", days],
    queryFn: async () => {
      try {
        const res = await statsApi.getTrends({ days })
        return res.data.data
      } catch {
        return mockTrends
      }
    },
  })
}
