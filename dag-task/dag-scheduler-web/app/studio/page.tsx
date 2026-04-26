"use client"

import { useState, useCallback, useRef, useMemo } from "react"
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
} from "reactflow"
import "reactflow/dist/style.css"
import {
  Save, Trash2, ZoomIn, ZoomOut, Maximize,
  Undo, Redo, CheckCircle2, AlertCircle,
  Search, ChevronRight, Settings, Play, Plus, X,
  FileText, GitBranch, Type, Terminal, FileCode,
  Container, Database, Diamond, Split, Merge,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import type { DagNode, NodeType, DagNodeData } from "@/types"

const nodeTypesList = [
  { type: "task" as NodeType, label: "Task", color: "#3B82F6", description: "General purpose task" },
  { type: "shell" as NodeType, label: "Shell", color: "#06B6D4", description: "Shell command" },
  { type: "python" as NodeType, label: "Python", color: "#FBBF24", description: "Python script" },
  { type: "java" as NodeType, label: "Java", color: "#F97316", description: "Java application" },
  { type: "docker" as NodeType, label: "Docker", color: "#0EA5E9", description: "Docker container" },
  { type: "database" as NodeType, label: "Database", color: "#10B981", description: "DB operation" },
  { type: "decision" as NodeType, label: "Decision", color: "#EC4899", description: "Conditional branch" },
  { type: "fork" as NodeType, label: "Fork", color: "#8B5CF6", description: "Parallel execution" },
  { type: "join" as NodeType, label: "Join", color: "#6366F1", description: "Synchronization" },
]

function validateDag(nodes: Node[], edges: Edge[]) {
  const errors: string[] = []
  if (nodes.length === 0) {
    errors.push("DAG is empty")
    return { valid: false, errors }
  }
  const adjacency = new Map<string, string[]>()
  nodes.forEach((node) => adjacency.set(node.id, []))
  edges.forEach((edge) => adjacency.get(edge.source)?.push(edge.target))
  const visited = new Set<string>()
  const stack = new Set<string>()
  function hasCycle(id: string): boolean {
    visited.add(id)
    stack.add(id)
    for (const n of adjacency.get(id) ?? []) {
      if (!visited.has(n) && hasCycle(n)) return true
      if (stack.has(n)) return true
    }
    stack.delete(id)
    return false
  }
  for (const node of nodes) {
    if (!visited.has(node.id) && hasCycle(node.id)) {
      errors.push("Cycle detected")
      break
    }
  }
  return { valid: errors.length === 0, errors }
}

function StudioContent() {
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [rfi, setRfi] = useState<ReactFlowInstance | null>(null)
  const [selected, setSelected] = useState<DagNode | null>(null)
  const [valid, setValid] = useState(true)
  const [history, setHistory] = useState([{ nodes: [] as Node[], edges: [] as Edge[] }])
  const [hIndex, setHIndex] = useState(0)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const counterRef = useRef(0)

  const getId = () => `node_${Date.now()}_${++counterRef.current}`

  const addHistory = useCallback((ns: Node[], es: Edge[]) => {
    setHistory(prev => {
      const nh = prev.slice(0, hIndex + 1)
      nh.push({ nodes: ns, edges: es })
      return nh.slice(-50)
    })
    setHIndex(prev => Math.min(prev + 1, 49))
  }, [hIndex])

  const onConnect = useCallback((params: Connection) => {
    const ne = addEdge({ ...params, animated: true }, edges)
    setEdges(ne)
    addHistory(nodes as Node[], ne)
  }, [nodes, edges, addHistory])

  const onDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = "move"
  }, [])

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    const type = e.dataTransfer.getData("application/reactflow/type") as NodeType
    if (!type || !rfi) return
    const pos = rfi.screenToFlowPosition({ x: e.clientX, y: e.clientY })
    const info = nodeTypesList.find(n => n.type === type)
    const nn: DagNode = {
      id: getId(), type: "default", position: pos,
      data: { label: info?.label || type, type, config: {} }
    }
    const ns = nodes.concat(nn)
    setNodes(ns)
    addHistory(ns, edges)
  }, [rfi, nodes, edges, addHistory])

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => setSelected(node as unknown as DagNode), [])
  const onPaneClick = useCallback(() => setSelected(null), [])

  const updateNode = useCallback((id: string, data: Partial<DagNodeData>) => {
    setNodes(ns => ns.map(n => n.id === id ? { ...n, data: { ...n.data, ...data } } : n))
    setSelected(prev => prev?.id === id ? { ...prev, data: { ...prev.data, ...data } } : prev)
  }, [setNodes])

  const deleteNode = useCallback((id: string) => {
    setNodes(ns => ns.filter(n => n.id !== id))
    setEdges(es => es.filter(e => e.source !== id && e.target !== id))
    setSelected(null)
  }, [setNodes, setEdges])

  const validate = useCallback(() => {
    const r = validateDag(nodes, edges)
    setValid(r.valid)
    return r
  }, [nodes, edges])

  const undo = useCallback(() => {
    if (hIndex > 0) {
      const hi = hIndex - 1
      setHIndex(hi)
      setNodes(history[hi].nodes)
      setEdges(history[hi].edges)
    }
  }, [hIndex, history])

  const redo = useCallback(() => {
    if (hIndex < history.length - 1) {
      const hi = hIndex + 1
      setHIndex(hi)
      setNodes(history[hi].nodes)
      setEdges(history[hi].edges)
    }
  }, [hIndex, history])

  const clear = useCallback(() => {
    if (window.confirm("Clear all nodes and edges?")) {
      setNodes([])
      setEdges([])
      setSelected(null)
      setValid(true)
      addHistory([], [])
    }
  }, [addHistory])

  const save = useCallback(() => {
    const r = validate()
    if (!r.valid) {
      alert("Validation failed: " + r.errors.join(", "))
      return
    }
    alert("Workflow saved!")
  }, [validate])

  const nodeTypes = useMemo(() => ({
    default: ({ data, selected: sel }: { data: DagNodeData; selected?: boolean }) => {
      const info = nodeTypesList.find(n => n.type === data.type)
      return (
        <div className={`shadow-lg rounded-lg px-4 py-2.5 border-2 min-w-[120px] ${sel ? 'border-primary ring-2 ring-primary/20' : 'border-border'}`}
          style={{ backgroundColor: 'hsl(var(--card))' }}>
          <Handle type="target" position={Position.Left} className="!bg-border !w-3 !h-3" />
          <div className="flex items-center gap-2">
            {info && <div className="w-2 h-2 rounded-full" style={{ backgroundColor: info.color }} />}
            <div>
              <div className="font-medium text-sm">{data.label}</div>
              <div className="text-xs text-muted-foreground">{data.type}</div>
            </div>
          </div>
          <Handle type="source" position={Position.Right} className="!bg-border !w-3 !h-3" />
        </div>
      )
    },
  }), [])

  const [paletteSearch, setPaletteSearch] = useState("")
  const [rightPanel, setRightPanel] = useState<'info' | 'properties'>('info')
  const [workflowInfo, setWorkflowInfo] = useState({
    key: '',
    name: '',
    version: '1.0.0',
    description: '',
    tags: [] as string[]
  })
  const [newTag, setNewTag] = useState("")
  
  const filteredNodes = nodeTypesList.filter(n =>
    n.label.toLowerCase().includes(paletteSearch.toLowerCase())
  )

  return (
    <div className="flex h-[calc(100vh-4rem)]">
      {/* Node Palette */}
      <div className="w-64 border-r bg-card flex flex-col">
        <div className="p-4 border-b">
          <h2 className="font-semibold mb-3">Node Palette</h2>
          <div className="relative">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input placeholder="Search..." className="pl-9" value={paletteSearch}
              onChange={e => setPaletteSearch(e.target.value)} />
          </div>
        </div>
        <ScrollArea className="flex-1">
          <div className="p-3 space-y-2">
            {filteredNodes.map(node => (
              <div key={node.type} draggable
                onDragStart={e => {
                  e.dataTransfer.setData("application/reactflow/type", node.type)
                  e.dataTransfer.effectAllowed = "move"
                }}
                className="flex items-center gap-3 p-3 rounded-lg bg-muted/50 hover:bg-muted cursor-move transition-colors">
                <div className="w-3 h-3 rounded-full shrink-0" style={{ backgroundColor: node.color }} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium">{node.label}</p>
                  <p className="text-xs text-muted-foreground truncate">{node.description}</p>
                </div>
              </div>
            ))}
          </div>
        </ScrollArea>
      </div>

      {/* Canvas */}
      <div className="flex-1 relative" ref={wrapperRef}>
        <ReactFlow
          nodes={nodes} edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onInit={setRfi}
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

        {/* Toolbar */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 bg-background/95 backdrop-blur-sm border rounded-lg shadow-lg px-4 py-2 flex items-center gap-2">
          <Badge variant={valid ? "success" : "failure"} className="gap-1">
            {valid ? <CheckCircle2 className="h-3 w-3" /> : <AlertCircle className="h-3 w-3" />}
            {valid ? "Valid" : "Invalid"}
          </Badge>
          <Separator orientation="vertical" className="h-5" />
          <Button size="sm" variant="outline" onClick={validate}><CheckCircle2 className="h-4 w-4" /></Button>
          <Button size="sm" variant="outline" onClick={undo} disabled={hIndex <= 0}><Undo className="h-4 w-4" /></Button>
          <Button size="sm" variant="outline" onClick={redo} disabled={hIndex >= history.length - 1}><Redo className="h-4 w-4" /></Button>
          <Button size="sm" variant="outline" onClick={() => rfi?.zoomIn()}><ZoomIn className="h-4 w-4" /></Button>
          <Button size="sm" variant="outline" onClick={() => rfi?.zoomOut()}><ZoomOut className="h-4 w-4" /></Button>
          <Button size="sm" variant="outline" onClick={() => rfi?.fitView()}><Maximize className="h-4 w-4" /></Button>
          <Separator orientation="vertical" className="h-5" />
          <Button size="sm" variant="outline" onClick={clear}><Trash2 className="h-4 w-4 mr-1" />Clear</Button>
          <Button size="sm" onClick={save} disabled={!valid}><Save className="h-4 w-4 mr-1" />Save</Button>
        </div>
      </div>

      {/* Right Panel */}
      <div className="w-80 border-l bg-card flex flex-col">
        <div className="p-2 border-b flex gap-1">
          <Button 
            variant={rightPanel === 'info' ? 'secondary' : 'ghost'} 
            size="sm" 
            className="flex-1 text-xs"
            onClick={() => setRightPanel('info')}
          >
            <FileText className="mr-1.5 h-3.5 w-3.5" />
            Workflow
          </Button>
          <Button 
            variant={rightPanel === 'properties' ? 'secondary' : 'ghost'} 
            size="sm" 
            className="flex-1 text-xs"
            onClick={() => selected && setRightPanel('properties')}
            disabled={!selected}
          >
            <Settings className="mr-1.5 h-3.5 w-3.5" />
            Node
          </Button>
        </div>
        
        {rightPanel === 'info' ? (
          <ScrollArea className="flex-1">
            <div className="p-4 space-y-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground">Workflow Key *</label>
                <Input 
                  value={workflowInfo.key} 
                  onChange={e => setWorkflowInfo({...workflowInfo, key: e.target.value})}
                  className="mt-1.5" 
                  placeholder="e.g., etl-daily"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Name *</label>
                <Input 
                  value={workflowInfo.name} 
                  onChange={e => setWorkflowInfo({...workflowInfo, name: e.target.value})}
                  className="mt-1.5" 
                  placeholder="Workflow display name"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Version *</label>
                <Input 
                  value={workflowInfo.version} 
                  onChange={e => setWorkflowInfo({...workflowInfo, version: e.target.value})}
                  className="mt-1.5" 
                  placeholder="1.0.0"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Description</label>
                <Input 
                  value={workflowInfo.description} 
                  onChange={e => setWorkflowInfo({...workflowInfo, description: e.target.value})}
                  className="mt-1.5" 
                  placeholder="Describe this workflow..."
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Tags</label>
                <div className="flex gap-2 mt-1.5">
                  <Input 
                    value={newTag}
                    onChange={e => setNewTag(e.target.value)}
                    className="flex-1" 
                    placeholder="Add tag..."
                    onKeyDown={e => {
                      if (e.key === 'Enter' && newTag.trim()) {
                        setWorkflowInfo({...workflowInfo, tags: [...workflowInfo.tags, newTag.trim()]})
                        setNewTag('')
                      }
                    }}
                  />
                  <Button 
                    size="sm" 
                    variant="outline"
                    onClick={() => {
                      if (newTag.trim()) {
                        setWorkflowInfo({...workflowInfo, tags: [...workflowInfo.tags, newTag.trim()]})
                        setNewTag('')
                      }
                    }}
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                <div className="flex flex-wrap gap-1.5 mt-2">
                  {workflowInfo.tags.map((tag, i) => (
                    <Badge key={i} variant="secondary" className="gap-1 text-xs">
                      {tag}
                      <button 
                        onClick={() => setWorkflowInfo({
                          ...workflowInfo, 
                          tags: workflowInfo.tags.filter((_, idx) => idx !== i)
                        })}
                        className="hover:text-destructive"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              </div>
              
              <Separator />
              
              <div className="space-y-2">
                <Button 
                  className="w-full" 
                  onClick={() => {
                    if (!workflowInfo.key || !workflowInfo.name) {
                      alert('Please fill in required fields')
                      return
                    }
                    save()
                  }} 
                  disabled={!valid}
                >
                  <Save className="mr-2 h-4 w-4" />
                  Save Workflow
                </Button>
                <Button 
                  variant="outline" 
                  className="w-full" 
                  onClick={() => alert('Execute workflow: ' + workflowInfo.name)}
                  disabled={!valid || nodes.length === 0}
                >
                  <Play className="mr-2 h-4 w-4" />
                  Execute Now
                </Button>
              </div>
            </div>
          </ScrollArea>
        ) : selected ? (
          <ScrollArea className="flex-1">
            <div className="p-4 space-y-4">
              <div className="flex items-center gap-2">
                {(() => {
                  const info = nodeTypesList.find(n => n.type === selected.data.type)
                  return info ? (
                    <>
                      <div className="w-3 h-3 rounded-full" style={{ backgroundColor: info.color }} />
                      <span className="text-sm font-medium">{info.label} Node</span>
                    </>
                  ) : null
                })()}
              </div>
              
              <Tabs defaultValue="basic">
                <TabsList className="w-full">
                  <TabsTrigger value="basic" className="flex-1 text-xs">Basic</TabsTrigger>
                  <TabsTrigger value="config" className="flex-1 text-xs">Config</TabsTrigger>
                </TabsList>
                
                <TabsContent value="basic" className="space-y-3 mt-3">
                  <div>
                    <label className="text-xs font-medium text-muted-foreground">Label</label>
                    <Input 
                      value={selected.data.label}
                      onChange={e => updateNode(selected.id, { label: e.target.value })}
                      className="mt-1.5"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-muted-foreground">Description</label>
                    <Input 
                      value={selected.data.description || ""}
                      onChange={e => updateNode(selected.id, { description: e.target.value })}
                      className="mt-1.5" 
                      placeholder="Optional description..."
                    />
                  </div>
                </TabsContent>
                
                <TabsContent value="config" className="space-y-3 mt-3">
                  {selected.data.type === 'shell' && (
                    <div>
                      <label className="text-xs font-medium text-muted-foreground">Command</label>
                      <Input 
                        value={(selected.data.config?.command as string) || ""}
                        onChange={e => updateNode(selected.id, { 
                          config: { ...selected.data.config, command: e.target.value }
                        })}
                        className="mt-1.5 font-mono text-xs" 
                        placeholder="echo 'Hello World'"
                      />
                    </div>
                  )}
                  {selected.data.type === 'python' && (
                    <div>
                      <label className="text-xs font-medium text-muted-foreground">Script</label>
                      <Input 
                        value={(selected.data.config?.script as string) || ""}
                        onChange={e => updateNode(selected.id, { 
                          config: { ...selected.data.config, script: e.target.value }
                        })}
                        className="mt-1.5 font-mono text-xs" 
                        placeholder="script.py"
                      />
                    </div>
                  )}
                  {selected.data.type === 'docker' && (
                    <>
                      <div>
                        <label className="text-xs font-medium text-muted-foreground">Image</label>
                        <Input 
                          value={(selected.data.config?.image as string) || ""}
                          onChange={e => updateNode(selected.id, { 
                            config: { ...selected.data.config, image: e.target.value }
                          })}
                          className="mt-1.5 text-xs" 
                          placeholder="nginx:latest"
                        />
                      </div>
                      <div>
                        <label className="text-xs font-medium text-muted-foreground">Command</label>
                        <Input 
                          value={(selected.data.config?.command as string) || ""}
                          onChange={e => updateNode(selected.id, { 
                            config: { ...selected.data.config, command: e.target.value }
                          })}
                          className="mt-1.5 font-mono text-xs" 
                          placeholder="Optional..."
                        />
                      </div>
                    </>
                  )}
                  {selected.data.type === 'database' && (
                    <div>
                      <label className="text-xs font-medium text-muted-foreground">SQL Query</label>
                      <Input 
                        value={(selected.data.config?.query as string) || ""}
                        onChange={e => updateNode(selected.id, { 
                          config: { ...selected.data.config, query: e.target.value }
                        })}
                        className="mt-1.5 font-mono text-xs" 
                        placeholder="SELECT * FROM table"
                      />
                    </div>
                  )}
                  {!['shell', 'python', 'docker', 'database'].includes(selected.data.type) && (
                    <div className="text-center py-4 text-muted-foreground text-sm">
                      No specific configuration for this node type
                    </div>
                  )}
                </TabsContent>
              </Tabs>
              
              <Separator />
              
              <Button 
                variant="destructive" 
                size="sm" 
                className="w-full" 
                onClick={() => deleteNode(selected.id)}
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete Node
              </Button>
            </div>
          </ScrollArea>
        ) : (
          <div className="flex-1 flex items-center justify-center p-8 text-center text-muted-foreground">
            <div>
              <Settings className="h-8 w-8 mx-auto mb-3 opacity-50" />
              <p className="text-sm">Select a node to configure its properties</p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default function StudioPage() {
  return (
    <div className="h-[calc(100vh-4rem)] -m-6 -mt-8">
      <ReactFlowProvider>
        <StudioContent />
      </ReactFlowProvider>
    </div>
  )
}
