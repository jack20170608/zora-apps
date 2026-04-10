import axios from 'axios';
import type { TaskTemplate } from '@/types/template';
import type { DashboardStats, TaskExecution, AgentInfo } from '@/types/task';
import type { ApiResponse } from './types';

const apiClient = axios.create({
  baseURL: typeof window === 'undefined' ? 'http://localhost:8080/api' : '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Template API methods
export const templateApi = {
  listAllActive: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template').then(res => res.data),

  listAll: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template/all').then(res => res.data),

  listVersions: (templateKey: string): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get(`/v1/template/${templateKey}/versions`).then(res => res.data),

  getActive: (templateKey: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${templateKey}/active`).then(res => res.data),

  getByVersion: (templateKey: string, version: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${templateKey}/v/${version}`).then(res => res.data),

  create: (template: TaskTemplate, setActive: boolean): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.post(`/v1/template?setActive=${setActive}`, template).then(res => res.data),

  update: (templateKey: string, version: string, template: TaskTemplate): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.put(`/v1/template/${templateKey}/v/${version}`, template).then(res => res.data),

  deactivate: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.post(`/v1/template/${templateKey}/v/${version}/deactivate`).then(res => res.data),

  delete: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.delete(`/v1/template/${templateKey}/v/${version}`).then(res => res.data),

  instantiate: (
    templateKey: string,
    orderKey: string,
    orderName: string,
    params: Record<string, string>
  ): Promise<ApiResponse<any>> =>
    apiClient.post(`/v1/template/${templateKey}/instantiate?orderKey=${orderKey}&orderName=${orderName}`, params)
      .then(res => res.data),
};

// Stats API methods
export const statsApi = {
  getDashboardStats: (): Promise<ApiResponse<DashboardStats>> =>
    apiClient.get('/v1/stats/dashboard').then(res => res.data),
};

// Task API methods
export const taskApi = {
  listExecutions: (): Promise<ApiResponse<TaskExecution[]>> =>
    apiClient.get('/v1/tasks').then(res => res.data),
};

// Agent API methods
export const agentApi = {
  listAgents: (): Promise<ApiResponse<AgentInfo[]>> =>
    apiClient.get('/v1/agent/all').then(res => res.data),
};

export default apiClient;
