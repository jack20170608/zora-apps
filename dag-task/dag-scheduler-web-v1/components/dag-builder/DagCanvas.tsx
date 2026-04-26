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
  type ReactFlowInstance,
} from "reactflow";
import "reactflow/dist/style.css";

import { NodePanel } from "./NodePanel";
import { NodeConfigPanel } from "./NodeConfigPanel";
import { Toolbar } from "./Toolbar";
import type { DagNode, DagEdge, DagDefinition, DagValidationResult, DagNodeData } from "@/types/dag";
import { NODE_TYPES } from "@/types/dag";

interface DagCanvasProps {
  initialNodes?: DagNode[];
  initialEdges?: DagEdge[];
  onSave: (definition: DagDefinition) => void;
  saving: boolean;
}

/** Generate a unique node ID using timestamp + counter */
function createNodeIdGenerator() {
  let counter = 0;
  return () => `node_${Date.now()}_${++counter}`;
}

function validateDag(nodes: DagNode[], edges: DagEdge[]): DagValidationResult {
  const errors: string[] = [];

  // Check for empty graph
  if (nodes.length === 0) {
    errors.push("DAG is empty (no nodes)");
    return { valid: false, errors };
  }

  // Build adjacency list
  const adjacency = new Map<string, string[]>();
  nodes.forEach(node => adjacency.set(node.id, []));
  edges.forEach(edge => {
    adjacency.get(edge.source)?.push(edge.target);
  });

  // Check for cycles using DFS
  const visited = new Set<string>();
  const recursionStack = new Set<string>();

  function hasCycle(nodeId: string): boolean {
    visited.add(nodeId);
    recursionStack.add(nodeId);

    const neighbors = adjacency.get(nodeId) ?? [];
    for (const neighbor of neighbors) {
      if (!visited.has(neighbor)) {
        if (hasCycle(neighbor)) return true;
      } else if (recursionStack.has(neighbor)) {
        return true;
      }
    }

    recursionStack.delete(nodeId);
    return false;
  }

  for (const node of nodes) {
    if (!visited.has(node.id) && hasCycle(node.id)) {
      errors.push("DAG contains a cycle (not a valid Directed Acyclic Graph)");
      break;
    }
  }

  // Check for disconnected nodes (warn only, not error)
  // Single node without edges is valid; multiple disconnected components may be intentional

  return {
    valid: errors.length === 0,
    errors,
  };
}

function DagCanvasContent({ initialNodes = [], initialEdges = [], onSave, saving }: DagCanvasProps) {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance | null>(null);
  const [selectedNode, setSelectedNode] = useState<DagNode | null>(null);
  const [validationResult, setValidationResult] = useState<DagValidationResult | null>(null);
  const [isValid, setIsValid] = useState(false);
  const getNextNodeId = useMemo(() => createNodeIdGenerator(), []);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge({ ...params, animated: true }, eds)),
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
      if (!type || !reactFlowInstance) return;

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
          config: { ...(nodeTypeInfo?.defaultConfig ?? {}) },
        },
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [reactFlowInstance, setNodes, getNextNodeId]
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
    if (typeof window !== "undefined" && window.confirm("Clear all nodes and edges from canvas?")) {
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
      if (typeof window !== "undefined") {
        window.alert(`Validation failed:\n${result.errors.join("\n")}`);
      }
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
    default: ({ data, selected }: { data: DagNodeData; selected?: boolean }) => (
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
          deleteKeyCode={["Backspace", "Delete"]}
        >
          <Background />
          <Controls />
          <MiniMap />
        </ReactFlow>
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
