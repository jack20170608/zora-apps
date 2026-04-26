"use client";

import { Sidebar } from "@/components/common/Sidebar";
import { Header } from "@/components/common/Header";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
      throwOnError: false, // Don't throw errors to Next.js even if API fails - we handle it with mock data
    },
  },
});

export function RootLayoutClient({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="flex h-screen bg-background">
        <Sidebar />
        {/* pl-64 = padding-left matches sidebar width w-64 since sidebar is fixed positioning */}
        <div className="flex-1 flex flex-col overflow-hidden pl-64">
          <Header />
          <main className="flex-1 overflow-auto p-4">
            {children}
          </main>
        </div>
      </div>
    </QueryClientProvider>
  );
}
