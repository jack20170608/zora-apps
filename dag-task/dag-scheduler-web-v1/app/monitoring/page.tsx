import { TaskList } from "@/components/monitoring/TaskList";

export default function MonitoringPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Monitoring</h1>
        <p className="text-muted-foreground">
          Monitor task execution status and view recent runs.
        </p>
      </div>
      <TaskList />
    </div>
  );
}
