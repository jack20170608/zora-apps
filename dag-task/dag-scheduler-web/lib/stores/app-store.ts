import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { StudioState, FilterState, ExecutionStatus } from '@/types';

interface AppState {
  // UI State
  sidebarCollapsed: boolean;
  theme: 'dark' | 'light' | 'system';
  
  // Actions
  toggleSidebar: () => void;
  setTheme: (theme: 'dark' | 'light' | 'system') => void;
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      theme: 'system',
      
      toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      setTheme: (theme) => set({ theme }),
    }),
    {
      name: 'dag-scheduler-settings',
    }
  )
);

interface StudioStore {
  state: StudioState;
  setSelectedNodes: (nodes: string[]) => void;
  setCanvasZoom: (zoom: number) => void;
  setCanvasPosition: (pos: { x: number; y: number }) => void;
  toggleGrid: () => void;
  toggleMinimap: () => void;
  toggleSnapToGrid: () => void;
  toggleConsole: () => void;
  addHistory: (state: { nodes: any[]; edges: any[] }) => void;
  undo: () => void;
  redo: () => void;
}

export const useStudioStore = create<StudioStore>((set) => ({
  state: {
    selectedNodes: [],
    canvasZoom: 1,
    canvasPosition: { x: 0, y: 0 },
    showGrid: true,
    showMinimap: true,
    snapToGrid: false,
    showConsole: false,
    history: [],
    historyIndex: -1,
  },
  
  setSelectedNodes: (nodes) =>
    set((s) => ({ state: { ...s.state, selectedNodes: nodes } })),
  setCanvasZoom: (zoom) =>
    set((s) => ({ state: { ...s.state, canvasZoom: zoom } })),
  setCanvasPosition: (pos) =>
    set((s) => ({ state: { ...s.state, canvasPosition: pos } })),
  toggleGrid: () =>
    set((s) => ({ state: { ...s.state, showGrid: !s.state.showGrid } })),
  toggleMinimap: () =>
    set((s) => ({ state: { ...s.state, showMinimap: !s.state.showMinimap } })),
  toggleSnapToGrid: () =>
    set((s) => ({ state: { ...s.state, snapToGrid: !s.state.snapToGrid } })),
  toggleConsole: () =>
    set((s) => ({ state: { ...s.state, showConsole: !s.state.showConsole } })),
  addHistory: (state) =>
    set((s) => {
      const newHistory = s.state.history.slice(0, s.state.historyIndex + 1);
      newHistory.push(state);
      return {
        state: {
          ...s.state,
          history: newHistory.slice(-50),
          historyIndex: newHistory.length - 1,
        },
      };
    }),
  undo: () =>
    set((s) => ({
      state: {
        ...s.state,
        historyIndex: Math.max(0, s.state.historyIndex - 1),
      },
    })),
  redo: () =>
    set((s) => ({
      state: {
        ...s.state,
        historyIndex: Math.min(s.state.history.length - 1, s.state.historyIndex + 1),
      },
    })),
}));

interface ExecutionFilterStore {
  filter: FilterState;
  setStatusFilter: (status: ExecutionStatus[]) => void;
  setDateRange: (range: { from?: string; to?: string }) => void;
  setWorkflowFilter: (workflowId?: string) => void;
  setSearchQuery: (query: string) => void;
  resetFilters: () => void;
}

export const useExecutionFilterStore = create<ExecutionFilterStore>((set) => ({
  filter: {
    status: [],
    dateRange: {},
    workflowId: undefined,
    searchQuery: '',
  },
  setStatusFilter: (status) =>
    set((s) => ({ filter: { ...s.filter, status } })),
  setDateRange: (range) =>
    set((s) => ({ filter: { ...s.filter, dateRange: range } })),
  setWorkflowFilter: (workflowId) =>
    set((s) => ({ filter: { ...s.filter, workflowId } })),
  setSearchQuery: (query) =>
    set((s) => ({ filter: { ...s.filter, searchQuery: query } })),
  resetFilters: () =>
    set({
      filter: {
        status: [],
        dateRange: {},
        workflowId: undefined,
        searchQuery: '',
      },
    }),
}));
