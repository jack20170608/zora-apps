'use client';

import { NODE_TYPES } from "@/types/dag";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function NodePanel() {
  const onDragStart = (event: React.DragEvent, nodeType: string) => {
    event.dataTransfer.setData("application/reactflow/type", nodeType);
    event.dataTransfer.effectAllowed = "move";
  };

  return (
    <Card className="w-64 h-full overflow-y-auto">
      <CardHeader>
        <CardTitle>Node Types</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {NODE_TYPES.map((nodeType) => (
          <div
            key={nodeType.type}
            className={cn(
              "border rounded-md p-3 cursor-grab bg-background hover:bg-accent transition-colors",
              "active:cursor-grabbing"
            )}
            onDragStart={(e) => onDragStart(e, nodeType.type)}
            draggable
          >
            <div className="font-medium text-sm">{nodeType.label}</div>
            <div className="text-xs text-muted-foreground mt-1">
              {nodeType.description}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
