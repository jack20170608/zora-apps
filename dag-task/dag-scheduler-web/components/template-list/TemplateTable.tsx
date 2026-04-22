'use client';

import { useQuery } from "@tanstack/react-query";
import { Plus, Trash2, Eye, AlertTriangle } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHead,
  TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { templateApi } from "@/lib/api-client";
import type { TaskTemplate } from "@/types/template";

// Mock data for when backend API doesn't exist yet
const MOCK_TEMPLATES: TaskTemplate[] = [
  {
    templateKey: "etl-data-processing",
    templateName: "ETL Data Processing Workflow",
    description: "A general-purpose ETL workflow that extracts, transforms and loads data",
    version: "1.1.0",
    active: true,
    dagDefinition: "",
    parameterSchema: "",
  },
  {
    templateKey: "daily-database-backup",
    templateName: "Daily Database Backup",
    description: "Backup database to remote storage and verify backup integrity",
    version: "1.0.0",
    active: true,
    dagDefinition: "",
    parameterSchema: "",
  },
  {
    templateKey: "ml-model-training",
    templateName: "ML Model Training Pipeline",
    description: "End-to-end machine learning model training pipeline",
    version: "1.0.0",
    active: false,
    dagDefinition: "",
    parameterSchema: "",
  },
];

export function TemplateTable() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["templates"],
    queryFn: () => templateApi.listAll(),
    retry: false,
  });

  const isUsingMock = !!error;
  const templates: TaskTemplate[] = isUsingMock ? MOCK_TEMPLATES : (data?.data ?? []);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Templates</CardTitle>
        <div className="flex items-center gap-3">
          {isUsingMock && (
            <div className="flex items-center gap-1.5 text-xs text-amber-600">
              <AlertTriangle className="h-3.5 w-3.5" />
              <span>Mock data</span>
            </div>
          )}
          <Link href="/builder/new">
            <Button size="sm">
              <Plus className="mr-2 h-4 w-4" />
              New Template
            </Button>
          </Link>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="p-4 text-muted-foreground text-center">Loading templates...</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Key</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Description</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {templates.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    No templates found. Create your first template with the DAG builder.
                  </TableCell>
                </TableRow>
              ) : (
                templates.map((template) => (
                  <TableRow key={`${template.templateKey}-${template.version}`}>
                    <TableCell className="font-medium font-mono text-xs">{template.templateKey}</TableCell>
                    <TableCell>{template.templateName}</TableCell>
                    <TableCell>{template.version}</TableCell>
                    <TableCell>
                      <Badge variant={template.active ? "success" : "outline"}>
                        {template.active ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell className="max-w-md truncate text-xs text-muted-foreground">
                      {template.description}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Link href={`/builder/${template.templateKey}`}>
                          <Button size="sm" variant="outline" title="Edit">
                            <Eye className="h-4 w-4" />
                          </Button>
                        </Link>
                        <Button size="sm" variant="outline" className="text-destructive hover:bg-destructive hover:text-destructive-foreground" title="Delete">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
