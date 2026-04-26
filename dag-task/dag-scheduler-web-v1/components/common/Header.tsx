'use client';

import { Activity, Cpu } from "lucide-react";

export function Header() {
  return (
    <header className="h-16 border-b flex items-center justify-between px-4 bg-card">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Cpu className="h-3.5 w-3.5" />
          <span>Scheduler: localhost:8081</span>
        </div>
      </div>
      <div className="flex items-center space-x-4">
        <div className="flex items-center gap-1.5 text-xs text-green-600 bg-green-50 px-2 py-1 rounded-full border border-green-200">
          <Activity className="h-3 w-3" />
          <span>Online</span>
        </div>
        <span className="text-sm text-muted-foreground">
          DAG Task Scheduler UI
        </span>
      </div>
    </header>
  );
}
