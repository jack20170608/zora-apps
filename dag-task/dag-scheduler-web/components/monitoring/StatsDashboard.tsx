'use client';

import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { statsApi } from "@/lib/api-client";
import type { DashboardStats } from "@/types/task";
import { FileText, CheckCircle, Server } from "lucide-react";

// Mock data for when backend API doesn't exist yet
const MOCK_STATS: DashboardStats = {
  totalTemplates: 3,
  activeTemplates: 2,
  totalTasksToday: 8,
  completedTasks: 7,
  failedTasks: 1,
  successRate: 87.5,
  onlineAgents: 3,
  totalAgents: 4,
};

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
    retry: false,
  });

  // Use mock data if API request fails (backend doesn't have endpoint yet)
  const stats: DashboardStats = error ? MOCK_STATS : (data?.data ?? MOCK_STATS);

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
