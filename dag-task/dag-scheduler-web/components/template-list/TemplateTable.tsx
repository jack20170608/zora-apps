'use client';

import { useQuery } from "@tanstack/react-query";
import { Plus, Trash2, Eye } from "lucide-react";
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
import { cn } from "@/lib/utils";

export function TemplateTable() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["templates"],
    queryFn: () => templateApi.listAll(),
  });

  if (isLoading) {
    return <div className="p-4 text-muted-foreground">Loading templates...</div>;
  }

  if (error) {
    return <div className="p-4 text-destructive">Error loading templates</div>;
  }

  const templates: TaskTemplate[] = data?.data ?? [];

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Templates</CardTitle>
        <Link href="/builder/new">
          <Button size="sm">
            <Plus className="mr-2 h-4 w-4" />
            New Template
          </Button>
        </Link>
      </CardHeader>
      <CardContent>
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
                  <TableCell className="font-medium">{template.templateKey}</TableCell>
                  <TableCell>{template.templateName}</TableCell>
                  <TableCell>{template.version}</TableCell>
                  <TableCell>
                    <Badge variant={template.active ? "success" : "outline"}
                      className={cn(
                        template.active ? "bg-green-100 text-green-800" : "text-gray-500"
                      )}
                    >
                      {template.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell className="max-w-md truncate">
                    {template.description}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Link href={`/builder/${template.templateKey}/${template.version}`}>
                        <Button size="sm" variant="outline">
                          <Eye className="h-4 w-4" />
                        </Button>
                      </Link>
                      <Button size="sm" variant="outline" className="text-destructive">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
