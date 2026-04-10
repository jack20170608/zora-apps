'use client';

import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { statsApi } from "@/lib/api-client";
import type { DashboardStats } from "@/types/task";
import { FileText, CheckCircle, Server } from "lucide-react";

function StatCard({
  title,
  value,
  icon: Icon,
  description,
}: {
  title: string;
  value: string | number;
  icon: any;
  description?: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {description && (
          <p className="text-xs text-muted-foreground">{description}</p>
        )}
      </CardContent>
    </Card>
  );
}

export function StatsDashboard() {
  const { data, error } = useQuery({
    queryKey: ["dashboardStats"],
    queryFn: () => statsApi.getDashboardStats(),
    // Don't throw error - just use defaults when API doesn't exist yet
    retry: false,
  });

  const stats: DashboardStats = data?.data ?? {
    totalTemplates: 0,
    activeTemplates: 0,
    totalTasksToday: 0,
    completedTasks: 0,
    failedTasks: 0,
    successRate: 0,
    onlineAgents: 0,
    totalAgents: 0,
  };

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <StatCard
        title="Total Templates"
        value={stats.totalTemplates}
        icon={FileText}
        description={`${stats.activeTemplates} active`}
      />
      <StatCard
        title="Tasks Today"
        value={stats.totalTasksToday}
        icon={CheckCircle}
        description={`${stats.completedTasks} completed`}
      />
      <StatCard
        title="Success Rate"
        value={`${stats.successRate.toFixed(1)}%`}
        icon={CheckCircle}
      />
      <StatCard
        title="Agents Online"
        value={`${stats.onlineAgents}/${stats.totalAgents}`}
        icon={Server}
      />
    </div>
  );
}
