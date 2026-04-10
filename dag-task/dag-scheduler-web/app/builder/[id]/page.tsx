'use client';

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { DagCanvas } from "@/components/dag-builder/DagCanvas";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { templateApi } from "@/lib/api-client";
import type { DagDefinition } from "@/types/dag";
import type { TaskTemplate } from "@/types/template";
import { useQuery } from "@tanstack/react-query";

export default function DagBuilderPage() {
  const params = useParams();
  const router = useRouter();
  const [templateKey, setTemplateKey] = useState("");
  const [templateName, setTemplateName] = useState("");
  const [description, setDescription] = useState("");
  const [version, setVersion] = useState("1.0.0");
  const [saving, setSaving] = useState(false);
  const [setActive, setSetActive] = useState(true);

  const id = params?.id as string;
  const isNew = id === "new";

  // If editing existing template by key/version, we need extra path segment
  // Handle the case where id is actually templateKey/version
  useEffect(() => {
    // If URL is /builder/etl/1.0.0, Next.js gives id as "etl/1.0.0"
    if (!isNew && id.includes('/')) {
      const [templateKeyFromUrl, versionFromUrl] = id.split('/');
      // Load existing template here
      templateApi.getByVersion(templateKeyFromUrl, versionFromUrl).then((res) => {
        const template = res.data;
        setTemplateKey(template.templateKey);
        setTemplateName(template.templateName);
        setDescription(template.description || "");
        setVersion(template.version);
      });
    }
  }, [id]);

  const handleSave = async (dagDefinition: DagDefinition) => {
    if (!templateKey || !templateName || !version) {
      alert("Please fill in all required fields: template key, name, version");
      return;
    }

    setSaving(true);
    try {
      const template: TaskTemplate = {
        templateKey,
        templateName,
        description,
        version,
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
      console.error(error);
      alert("Error saving template");
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
              <Label htmlFor="templateKey">Template Key</Label>
              <Input
                id="templateKey"
                placeholder="etl-data-processing"
                value={templateKey}
                onChange={(e) => setTemplateKey(e.target.value)}
                disabled={!isNew}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="templateName">Template Name</Label>
              <Input
                id="templateName"
                placeholder="ETL Data Processing"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="version">Version</Label>
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
          </CardContent>
        </Card>
      </div>
      <div className="flex-1 min-h-0">
        <DagCanvas onSave={handleSave} saving={saving} />
      </div>
    </div>
  );
}
