import type { Node, Edge } from 'reactflow';

export interface DagNodeData {
  label: string;
  type: string;
  config: Record<string, string>;
}

export type DagNode = Node<DagNodeData>;
export type DagEdge = Edge;

export interface DagDefinition {
  nodes: DagNode[];
  edges: DagEdge[];
}

export interface DagValidationResult {
  valid: boolean;
  errors: string[];
}

/** Standard node types available in the palette */
export interface NodeTypeInfo {
  type: string;
  label: string;
  description: string;
  defaultConfig: Record<string, string>;
}

export const NODE_TYPES: NodeTypeInfo[] = [
  { type: 'task', label: 'Task', description: 'Generic task node', defaultConfig: {} },
  { type: 'shell', label: 'Shell Command', description: 'Execute shell command', defaultConfig: { command: '' } },
  { type: 'python', label: 'Python Script', description: 'Run Python script', defaultConfig: { scriptPath: '' } },
  { type: 'java', label: 'Java Job', description: 'Run Java job', defaultConfig: { mainClass: '', args: '' } },
  { type: 'docker', label: 'Docker Container', description: 'Run Docker container', defaultConfig: { image: '', command: '' } },
];
