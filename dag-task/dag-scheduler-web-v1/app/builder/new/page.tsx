'use client';

import { useState } from "react";
import { useRouter } from "next/navigation";
import { DagCanvas } from "@/components/dag-builder/DagCanvas";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { templateApi } from "@/lib/api-client";
import type { DagDefinition } from "@/types/dag";
import type { TaskTemplate } from "@/types/template";

export default function NewDagBuilderPage() {
  const router = useRouter();
  const [templateKey, setTemplateKey] = useState("");
  const [templateName, setTemplateName] = useState("");
  const [description, setDescription] = useState("");
  const [version, setVersion] = useState("1.0.0");
  const [saving, setSaving] = useState(false);
  const [setActive, setSetActive] = useState(true);

  const handleSave = async (dagDefinition: DagDefinition) => {
    if (!templateKey.trim() || !templateName.trim() || !version.trim()) {
      alert("Please fill in all required fields: template key, name, version");
      return;
    }

    setSaving(true);
    try {
      const template: TaskTemplate = {
        templateKey: templateKey.trim(),
        templateName: templateName.trim(),
        description: description.trim(),
        version: version.trim(),
        active: false,
        dagDefinition: JSON.stringify(dagDefinition),
        parameterSchema: "{}",
      };

      const response = await templateApi.create(template, setActive);
      if (response.code === 200 || response.code === 0) {
        alert("Template created successfully!");
        router.push("/templates");
      } else {
        alert(`Error: ${response.message}`);
      }
    } catch (error) {
      console.error("Failed to save template:", error);
      alert("Error saving template. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4 h-[calc(100vh-100px)] flex flex-col">
      <div className="flex-none">
        <h1 className="text-3xl font-bold tracking-tight">DAG Builder</h1>
        <p className="text-muted-foreground">
          Create a new workflow template by dragging nodes onto the canvas.
        </p>
      </div>
      <div className="flex gap-4 flex-none">
        <Card className="flex-1">
          <CardHeader>
            <CardTitle>Template Information</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="templateKey">Template Key *</Label>
              <Input
                id="templateKey"
                placeholder="etl-data-processing"
                value={templateKey}
                onChange={(e) => setTemplateKey(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="templateName">Template Name *</Label>
              <Input
                id="templateName"
                placeholder="ETL Data Processing"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="version">Version *</Label>
              <Input
                id="version"
                placeholder="1.0.0"
                value={version}
                onChange={(e) => setVersion(e.target.value)}
              />
            </div>
            <div className="space-y-2 col-span-3">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Description of what this template does..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
              />
            </div>
            <div className="col-span-3 flex items-center gap-2">
              <input
                id="setActive"
                type="checkbox"
                checked={setActive}
                onChange={(e) => setSetActive(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300"
              />
              <Label htmlFor="setActive" className="font-normal cursor-pointer">
                Set as active version after creation
              </Label>
            </div>
          </CardContent>
        </Card>
      </div>
      <div className="flex-1 min-h-0">
        <DagCanvas onSave={handleSave} saving={saving} />
      </div>
    </div>
  );
}
