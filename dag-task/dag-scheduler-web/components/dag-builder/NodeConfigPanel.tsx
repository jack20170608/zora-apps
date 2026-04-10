'use client';

import { useState, useEffect } from "react";
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
      setConfig(node.data.config);
    }
  }, [node]);

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

  const handleConfigChange = (key: string, value: string) => {
    const newConfig = { ...config, [key]: value };
    setConfig(newConfig);
    onChange(node.id, { config: newConfig });
  };

  const handleLabelChange = (value: string) => {
    setLabel(value);
    onChange(node.id, { label: value });
  };

  const handleDelete = () => {
    onDelete(node.id);
  };

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

        {Object.entries(config).map(([key, value]) => (
          <div key={key} className="space-y-2">
            <Label htmlFor={`config-${key}`}>{key}</Label>
            <Input
              id={`config-${key}`}
              value={value}
              onChange={(e) => handleConfigChange(key, e.target.value)}
              placeholder={`Enter ${key}`}
            />
          </div>
        ))}

        {Object.keys(config).length === 0 && (
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
