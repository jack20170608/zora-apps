'use client';

import { useState, useEffect, useCallback } from "react";
import type { DagNode } from "@/types/dag";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Trash2 } from "lucide-react";

interface NodeConfigPanelProps {
  node: DagNode | null;
  onChange: (id: string, data: Partial<DagNode["data"]>) => void;
  onDelete: (id: string) => void;
}

export function NodeConfigPanel({ node, onChange, onDelete }: NodeConfigPanelProps) {
  const [label, setLabel] = useState("");
  const [config, setConfig] = useState<Record<string, string>>({});

  useEffect(() => {
    if (node) {
      setLabel(node.data.label);
      setConfig({ ...node.data.config });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [node?.id]);

  const handleConfigChange = useCallback((key: string, value: string) => {
    setConfig((prev) => {
      const newConfig = { ...prev, [key]: value };
      if (node) {
        onChange(node.id, { config: newConfig });
      }
      return newConfig;
    });
  }, [node, onChange]);

  const handleLabelChange = useCallback((value: string) => {
    setLabel(value);
    if (node) {
      onChange(node.id, { label: value });
    }
  }, [node, onChange]);

  const handleDelete = useCallback(() => {
    if (node && window.confirm(`Delete node "${node.data.label}"?`)) {
      onDelete(node.id);
    }
  }, [node, onDelete]);

  if (!node) {
    return (
      <Card className="w-72 h-full">
        <CardHeader>
          <CardTitle>Configuration</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Select a node on the canvas to configure it.
          </p>
        </CardContent>
      </Card>
    );
  }

  const configEntries = Object.entries(config);

  return (
    <Card className="w-72 h-full overflow-y-auto">
      <CardHeader>
        <CardTitle>Node Configuration</CardTitle>
        <p className="text-xs text-muted-foreground">
          ID: {node.id} ({node.data.type})
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="node-label">Node Label</Label>
          <Input
            id="node-label"
            value={label}
            onChange={(e) => handleLabelChange(e.target.value)}
            placeholder="Display name"
          />
        </div>

        {configEntries.length > 0 ? (
          configEntries.map(([key, value]) => (
            <div key={key} className="space-y-2">
              <Label htmlFor={`config-${key}`}>{key}</Label>
              <Input
                id={`config-${key}`}
                value={value}
                onChange={(e) => handleConfigChange(key, e.target.value)}
                placeholder={`Enter ${key}`}
              />
            </div>
          ))
        ) : (
          <p className="text-xs text-muted-foreground italic">
            No configuration properties defined.
          </p>
        )}

        <div className="pt-4">
          <Button
            variant="destructive"
            size="sm"
            className="w-full"
            onClick={handleDelete}
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Delete Node
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
