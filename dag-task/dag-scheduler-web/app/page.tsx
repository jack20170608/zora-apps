import { StatsDashboard } from "@/components/monitoring/StatsDashboard";
import Link from "next/link";
import { ChevronRight } from "lucide-react";

export default function Home() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Welcome to DAG Scheduler. Monitor tasks and manage workflow templates.
        </p>
      </div>
      <StatsDashboard />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Link
          href="/templates"
          className="group rounded-lg border p-6 hover:border-primary transition-colors"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Template Management</h2>
            <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </div>
          <p className="text-sm text-muted-foreground mt-2">
            Browse, create, and manage workflow templates.
          </p>
        </Link>
        <Link
          href="/builder/new"
          className="group rounded-lg border p-6 hover:border-primary transition-colors"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">New DAG</h2>
            <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </div>
          <p className="text-sm text-muted-foreground mt-2">
            Create a new workflow template with visual DAG builder.
          </p>
        </Link>
        <Link
          href="/monitoring"
          className="group rounded-lg border p-6 hover:border-primary transition-colors"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Monitoring</h2>
            <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </div>
          <p className="text-sm text-muted-foreground mt-2">
            Monitor running tasks and view execution statistics.
          </p>
        </Link>
      </div>
    </div>
  );
}
