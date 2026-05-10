"use client"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { ThemeProvider } from "next-themes"
import { ToastProvider } from "@/components/ui/toast-provider"
import { AppLayout } from "@/components/layout/AppLayout"
import { useState } from "react"

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 5 * 60 * 1000,
        refetchOnWindowFocus: false,
      },
    },
  }))

  return (
    <ThemeProvider attribute="class" defaultTheme="dark" enableSystem>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <AppLayout>{children}</AppLayout>
        </ToastProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
