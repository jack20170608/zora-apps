import axios from 'axios';
import type { 
  ApiResponse, 
  PaginatedResponse, 
  Workflow, 
  WorkflowVersion,
  Execution, 
  Agent, 
  DashboardStats, 
  TrendData,
  DagDefinition,
  ExecutionStatus
} from '@/types';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ==================== Workflow API ====================

export const workflowApi = {
  list: (params?: { page?: number; pageSize?: number; search?: string }) =>
    apiClient.get<ApiResponse<PaginatedResponse<Workflow>>>('/workflows', { params }),
  
  get: (id: string) =>
    apiClient.get<ApiResponse<Workflow>>(`/workflows/${id}`),
  
  create: (data: Partial<Workflow>) =>
    apiClient.post<ApiResponse<Workflow>>('/workflows', data),
  
  update: (id: string, data: Partial<Workflow>) =>
    apiClient.put<ApiResponse<Workflow>>(`/workflows/${id}`, data),
  
  delete: (id: string) =>
    apiClient.delete<ApiResponse<void>>(`/workflows/${id}`),
  
  getVersions: (id: string) =>
    apiClient.get<ApiResponse<WorkflowVersion[]>>(`/workflows/${id}/versions`),
  
  activateVersion: (id: string, version: string) =>
    apiClient.post<ApiResponse<void>>(`/workflows/${id}/versions/${version}/activate`),
  
  execute: (id: string, parameters?: Record<string, unknown>) =>
    apiClient.post<ApiResponse<Execution>>(`/workflows/${id}/execute`, { parameters }),
};

// ==================== Execution API ====================

export const executionApi = {
  list: (params?: { 
    page?: number; 
    pageSize?: number; 
    status?: ExecutionStatus[];
    workflowId?: string;
    from?: string;
    to?: string;
  }) =>
    apiClient.get<ApiResponse<PaginatedResponse<Execution>>>('/executions', { params }),
  
  get: (id: string) =>
    apiClient.get<ApiResponse<Execution>>(`/executions/${id}`),
  
  getLogs: (executionId: string, taskId?: string, params?: { from?: string; limit?: number }) =>
    apiClient.get<ApiResponse<string[]>>(`/executions/${executionId}/logs`, { 
      params: { ...params, taskId } 
    }),
  
  retry: (id: string, fromTask?: string) =>
    apiClient.post<ApiResponse<Execution>>(`/executions/${id}/retry`, { fromTask }),
  
  cancel: (id: string) =>
    apiClient.post<ApiResponse<void>>(`/executions/${id}/cancel`),
  
  getTasks: (id: string) =>
    apiClient.get<ApiResponse<Execution['tasks']>>(`/executions/${id}/tasks`),
};

// ==================== Agent API ====================

export const agentApi = {
  list: () =>
    apiClient.get<ApiResponse<Agent[]>>('/agents'),
  
  get: (id: string) =>
    apiClient.get<ApiResponse<Agent>>(`/agents/${id}`),
  
  getMetrics: (id: string) =>
    apiClient.get<ApiResponse<{ cpu: number[]; memory: number[]; tasks: number[] }>>(`/agents/${id}/metrics`),
};

// ==================== Stats API ====================

export const statsApi = {
  getOverview: () =>
    apiClient.get<ApiResponse<DashboardStats>>('/stats/overview'),
  
  getTrends: (params?: { days?: number }) =>
    apiClient.get<ApiResponse<TrendData[]>>(`/stats/trends`, { params }),
};

// ==================== WebSocket ====================

export function createExecutionWebSocket(executionId: string): WebSocket {
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8081/ws';
  return new WebSocket(`${wsUrl}/executions/${executionId}`);
}

export function createAgentWebSocket(): WebSocket {
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8081/ws';
  return new WebSocket(`${wsUrl}/agents`);
}

export default apiClient;
