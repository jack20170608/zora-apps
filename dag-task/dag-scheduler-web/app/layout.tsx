import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Sidebar } from "@/components/common/Sidebar";
import { Header } from "@/components/common/Header";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "DAG Scheduler",
  description: "DAG-based task scheduling system - Visual workflow builder",
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
    },
  },
});

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <QueryClientProvider client={queryClient}>
          <div className="flex h-screen bg-background">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
              <Header />
              <main className="flex-1 overflow-auto p-4">
                {children}
              </main>
            </div>
          </div>
        </QueryClientProvider>
      </body>
    </html>
  );
}
