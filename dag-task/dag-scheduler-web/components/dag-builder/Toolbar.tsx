'use client';

import { Save, Trash, ZoomIn, ZoomOut, RotateCcw, CheckCircle } from "lucide-react";
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
    <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 bg-background border rounded-lg shadow-lg px-4 py-2 flex items-center gap-2">
      {validationResult && (
        <Badge variant={isValid ? "success" : "danger"} className="gap-1">
          <CheckCircle className="h-3 w-3" />
          {isValid ? "Valid DAG" : `${errorCount} error${errorCount > 1 ? "s" : ""}`}
        </Badge>
      )}
      <Button size="sm" variant="outline" onClick={onZoomIn}>
        <ZoomIn className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onZoomOut}>
        <ZoomOut className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onFitView}>
        <RotateCcw className="h-4 w-4" />
      </Button>
      <div className="w-px h-6 bg-border mx-1" />
      <Button size="sm" variant="outline" onClick={onClear}>
        <Trash className="h-4 w-4 mr-1" />
        Clear
      </Button>
      <Button size="sm" onClick={onSave} disabled={!isValid || saving}>
        <Save className="h-4 w-4 mr-1" />
        {saving ? "Saving..." : "Save"}
      </Button>
    </div>
  );
}
