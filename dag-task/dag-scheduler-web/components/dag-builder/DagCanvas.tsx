'use client';

import { useCallback, useRef, useState, useMemo } from "react";
import ReactFlow, {
  ReactFlowProvider,
  addEdge,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  MiniMap,
  Handle,
  Position,
  type Connection,
  type Edge,
  type Node,
  Panel,
} from "reactflow";
import "reactflow/dist/style.css";

import { NodePanel } from "./NodePanel";
import { NodeConfigPanel } from "./NodeConfigPanel";
import { Toolbar } from "./Toolbar";
import type { DagNode, DagEdge, DagDefinition, DagValidationResult, DagNodeData, NodeTypeInfo } from "@/types/dag";
import { NODE_TYPES } from "@/types/dag";
import { Button } from "@/components/ui/button";

let nodeIdCounter = 0;

interface DagCanvasProps {
  initialNodes?: DagNode[];
  initialEdges?: DagEdge[];
  onSave: (definition: DagDefinition) => void;
  saving: boolean;
}

function getNextNodeId() {
  return `node_${++nodeIdCounter}`;
}

function validateDag(nodes: DagNode[], edges: DagEdge[]): DagValidationResult {
  const errors: string[] = [];

  // Check for cycles using DFS
  const adjacency = new Map<string, string[]>();
  nodes.forEach(node => adjacency.set(node.id, []));
  edges.forEach(edge => {
    adjacency.get(edge.source)?.push(edge.target);
  });

  const visited = new Set<string>();
  const recursionStack = new Set<string>();

  function hasCycle(nodeId: string): boolean {
    if (!visited.has(nodeId)) {
      visited.add(nodeId);
      recursionStack.add(nodeId);

      const neighbors = adjacency.get(nodeId) || [];
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor) && hasCycle(neighbor)) {
          return true;
        } else if (recursionStack.has(neighbor)) {
          return true;
        }
      }
    }
    recursionStack.delete(nodeId);
    return false;
  }

  let hasCycleResult = false;
  for (const node of nodes) {
    if (!visited.has(node.id) && hasCycle(node.id)) {
      hasCycleResult = true;
      break;
    }
  }

  if (hasCycleResult) {
    errors.push("DAG contains a cycle (not a valid Directed Acyclic Graph)");
  }

  // Check for disconnected nodes
  if (nodes.length > 1) {
    const connectedNodes = new Set<string>();
    edges.forEach(edge => {
      connectedNodes.add(edge.source);
      connectedNodes.add(edge.target);
    });
    const disconnected = nodes.filter(n => !connectedNodes.has(n.id));
    if (disconnected.length > 0) {
      errors.push(`${disconnected.length} disconnected node(s) found`);
    }
  }

  // Check for empty graph
  if (nodes.length === 0) {
    errors.push("DAG is empty (no nodes)");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

function DagCanvasContent({ initialNodes = [], initialEdges = [], onSave, saving }: DagCanvasProps) {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [reactFlowInstance, setReactFlowInstance] = useState<any>(null);
  const [selectedNode, setSelectedNode] = useState<DagNode | null>(null);
  const [validationResult, setValidationResult] = useState<DagValidationResult | null>(null);
  const [isValid, setIsValid] = useState(false);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type = event.dataTransfer.getData("application/reactflow/type");
      if (!type) return;

      const position = reactFlowInstance.screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      const nodeTypeInfo = NODE_TYPES.find(t => t.type === type);
      const newNode: DagNode = {
        id: getNextNodeId(),
        type: "default",
        position,
        data: {
          label: nodeTypeInfo?.label || type,
          type,
          config: { ...(nodeTypeInfo?.defaultConfig || {}) },
        },
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [reactFlowInstance, setNodes]
  );

  const onNodeClick = useCallback((_: React.MouseEvent, node: DagNode) => {
    setSelectedNode(node);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
  }, []);

  const updateNodeData = useCallback(
    (nodeId: string, newData: Partial<DagNodeData>) => {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === nodeId) {
            return { ...node, data: { ...node.data, ...newData } };
          }
          return node;
        })
      );
      setSelectedNode((prev) =>
        prev?.id === nodeId ? { ...prev, data: { ...prev.data, ...newData } } : prev
      );
    },
    [setNodes]
  );

  const deleteSelectedNode = useCallback(
    (nodeId: string) => {
      setNodes((nds) => nds.filter((n) => n.id !== nodeId));
      setEdges((eds) => eds.filter((e) => e.source !== nodeId && e.target !== nodeId));
      setSelectedNode(null);
    },
    [setNodes, setEdges]
  );

  const clearCanvas = useCallback(() => {
    if (confirm("Clear all nodes and edges from canvas?")) {
      setNodes([]);
      setEdges([]);
      setSelectedNode(null);
      setValidationResult(null);
      setIsValid(false);
    }
  }, [setNodes, setEdges]);

  const validateCurrent = useCallback(() => {
    const result = validateDag(nodes, edges);
    setValidationResult(result);
    setIsValid(result.valid);
    return result;
  }, [nodes, edges]);

  const handleSave = useCallback(() => {
    const result = validateCurrent();
    if (!result.valid) {
      alert(`Validation failed:\n${result.errors.join("\n")}`);
      return;
    }
    onSave({ nodes, edges });
  }, [nodes, edges, validateCurrent, onSave]);

  const zoomIn = useCallback(() => {
    reactFlowInstance?.zoomIn();
  }, [reactFlowInstance]);

  const zoomOut = useCallback(() => {
    reactFlowInstance?.zoomOut();
  }, [reactFlowInstance]);

  const fitView = useCallback(() => {
    reactFlowInstance?.fitView({ padding: 0.2 });
  }, [reactFlowInstance]);

  const nodeTypes = useMemo(() => ({
    default: ({ data, selected }: { data: DagNodeData; selected: boolean }) => (
      <div className={`shadow-md rounded-md px-4 py-2 border-2 bg-background ${selected ? 'border-primary' : 'border-border'}`}>
        <Handle type="target" position={Position.Left} className="!bg-border !w-3 !h-3" />
        <div className="font-medium text-sm">{data.label}</div>
        <div className="text-xs text-muted-foreground">{data.type}</div>
        <Handle type="source" position={Position.Right} className="!bg-border !w-3 !h-3" />
      </div>
    ),
  }), []);

  return (
    <div className="flex h-full gap-4">
      <NodePanel />
      <div className="flex-1 relative border rounded-lg overflow-hidden" ref={reactFlowWrapper}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onInit={setReactFlowInstance}
          onDrop={onDrop}
          onDragOver={onDragOver}
          onNodeClick={onNodeClick}
          onPaneClick={onPaneClick}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap />
          <Toolbar
            isValid={isValid}
            validationResult={validationResult}
            onSave={handleSave}
            onClear={clearCanvas}
            onZoomIn={zoomIn}
            onZoomOut={zoomOut}
            onFitView={fitView}
            onValidate={validateCurrent}
            saving={saving}
          />
        </ReactFlow>
      </div>
      <NodeConfigPanel
        node={selectedNode}
        onChange={updateNodeData}
        onDelete={deleteSelectedNode}
      />
    </div>
  );
}

export function DagCanvas(props: DagCanvasProps) {
  return (
    <ReactFlowProvider>
      <DagCanvasContent {...props} />
    </ReactFlowProvider>
  );
}
