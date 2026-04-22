import axios, { AxiosError } from 'axios';
import type { TaskTemplate } from '@/types/template';
import type { DashboardStats, TaskExecution, AgentInfo } from '@/types/task';
import type { ApiResponse } from './types';

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 30000,
});

// Request interceptor - could add auth tokens here
apiClient.interceptors.request.use(
  (config) => {
    // Add auth header if needed
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for unified error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response) {
      console.error(
        `API Error [${error.response.status}]:`,
        error.response.data?.message || error.message
      );
    } else if (error.request) {
      console.error('API No Response:', error.message);
    } else {
      console.error('API Request Error:', error.message);
    }
    return Promise.reject(error);
  }
);

// Template API methods
export const templateApi = {
  listAllActive: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template').then(res => res.data),

  listAll: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template/all').then(res => res.data),

  listVersions: (templateKey: string): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get(`/v1/template/${encodeURIComponent(templateKey)}/versions`).then(res => res.data),

  getActive: (templateKey: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${encodeURIComponent(templateKey)}/active`).then(res => res.data),

  getByVersion: (templateKey: string, version: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${encodeURIComponent(templateKey)}/v/${encodeURIComponent(version)}`).then(res => res.data),

  create: (template: TaskTemplate, setActive: boolean): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.post(`/v1/template?setActive=${setActive}`, template).then(res => res.data),

  update: (templateKey: string, version: string, template: TaskTemplate): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.put(`/v1/template/${encodeURIComponent(templateKey)}/v/${encodeURIComponent(version)}`, template).then(res => res.data),

  deactivate: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.post(`/v1/template/${encodeURIComponent(templateKey)}/v/${encodeURIComponent(version)}/deactivate`).then(res => res.data),

  delete: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.delete(`/v1/template/${encodeURIComponent(templateKey)}/v/${encodeURIComponent(version)}`).then(res => res.data),

  instantiate: (
    templateKey: string,
    orderKey: string,
    orderName: string,
    params: Record<string, string>
  ): Promise<ApiResponse<unknown>> =>
    apiClient.post(
      `/v1/template/${encodeURIComponent(templateKey)}/instantiate?orderKey=${encodeURIComponent(orderKey)}&orderName=${encodeURIComponent(orderName)}`,
      params
    ).then(res => res.data),
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
