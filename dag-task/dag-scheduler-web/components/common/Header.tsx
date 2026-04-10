'use client';

export function Header() {
  return (
    <header className="h-16 border-b flex items-center justify-end px-4 bg-card">
      <div className="flex items-center space-x-4">
        <span className="text-sm text-muted-foreground">
          DAG Task Scheduler UI
        </span>
      </div>
    </header>
  );
}
