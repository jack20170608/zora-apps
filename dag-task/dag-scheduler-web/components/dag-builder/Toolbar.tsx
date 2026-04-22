'use client';

import { Save, Trash, ZoomIn, ZoomOut, RotateCcw, CheckCircle, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import type { DagValidationResult } from "@/types/dag";

interface ToolbarProps {
  isValid: boolean;
  validationResult: DagValidationResult | null;
  onSave: () => void;
  onClear: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitView: () => void;
  onValidate: () => void;
  saving: boolean;
}

export function Toolbar({
  isValid,
  validationResult,
  onSave,
  onClear,
  onZoomIn,
  onZoomOut,
  onFitView,
  onValidate,
  saving,
}: ToolbarProps) {
  const errorCount = validationResult?.errors.length ?? 0;

  return (
    <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 bg-background/95 backdrop-blur-sm border rounded-lg shadow-lg px-4 py-2 flex items-center gap-2">
      {validationResult && (
        <Badge
          variant={isValid ? "success" : "danger"}
          className="gap-1 cursor-pointer"
          onClick={isValid ? undefined : onValidate}
          title={isValid ? "DAG is valid" : validationResult.errors.join("; ")}
        >
          {isValid ? (
            <CheckCircle className="h-3 w-3" />
          ) : (
            <AlertCircle className="h-3 w-3" />
          )}
          {isValid ? "Valid DAG" : `${errorCount} error${errorCount > 1 ? "s" : ""}`}
        </Badge>
      )}
      <div className="w-px h-5 bg-border" />
      <Button size="sm" variant="outline" onClick={onValidate} title="Validate DAG">
        <CheckCircle className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onZoomIn} title="Zoom in">
        <ZoomIn className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onZoomOut} title="Zoom out">
        <ZoomOut className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onFitView} title="Fit view">
        <RotateCcw className="h-4 w-4" />
      </Button>
      <div className="w-px h-5 bg-border" />
      <Button size="sm" variant="outline" onClick={onClear} title="Clear canvas">
        <Trash className="h-4 w-4 mr-1" />
        Clear
      </Button>
      <Button size="sm" onClick={onSave} disabled={!isValid || saving} title={isValid ? "Save template" : "Validate before saving"}>
        <Save className="h-4 w-4 mr-1" />
        {saving ? "Saving..." : "Save"}
      </Button>
    </div>
  );
}
