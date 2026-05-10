"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { motion, AnimatePresence } from "framer-motion"
import {
  LayoutDashboard,
  GitBranch,
  Play,
  Radio,
  Settings,
  Menu,
  Search,
  Bell,
  ChevronDown,
  Sun,
  Moon,
  Monitor,
} from "lucide-react"
import { useTheme } from "next-themes"
import { useAppStore } from "@/lib/stores/app-store"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { CommandPalette } from "./CommandPalette"
import { Breadcrumb } from "./Breadcrumb"
import { KeyboardShortcuts } from "./KeyboardShortcuts"
import { ErrorBoundary } from "@/components/error-boundary"
import { cn } from "@/lib/utils"

const navigation = [
  { name: "Overview", href: "/overview", icon: LayoutDashboard },
  { name: "Workflows", href: "/workflows", icon: GitBranch },
  { name: "Studio", href: "/studio", icon: Play },
  { name: "Executions", href: "/executions", icon: Radio },
  { name: "Agents", href: "/agents", icon: Settings },
]

export function Header() {
  const { theme, setTheme, resolvedTheme } = useTheme()
  const [mounted, setMounted] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  return (
    <header className="fixed top-0 left-0 right-0 z-40 h-16 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-full items-center justify-between px-4 lg:px-6">
        <div className="flex items-center gap-4">
          <Link href="/overview" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <GitBranch className="h-5 w-5 text-primary-foreground" />
            </div>
            <span className="text-lg font-bold hidden sm:inline-block">DAG Scheduler</span>
          </Link>
        </div>

        <div className="flex items-center gap-3">
          {/* Search */}
          <AnimatePresence>
            {searchOpen ? (
              <motion.div
                initial={{ width: 0, opacity: 0 }}
                animate={{ width: 280, opacity: 1 }}
                exit={{ width: 0, opacity: 0 }}
                transition={{ duration: 0.2 }}
                className="overflow-hidden"
              >
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Search workflows, executions..."
                    className="pl-9 w-full"
                    autoFocus
                    onBlur={() => setSearchOpen(false)}
                  />
                </div>
              </motion.div>
            ) : (
              <Button
                variant="ghost"
                size="icon"
                className="relative"
                onClick={() => setSearchOpen(true)}
              >
                <Search className="h-5 w-5" />
                <span className="sr-only">Search</span>
              </Button>
            )}
          </AnimatePresence>

          {/* Notifications */}
          <Button variant="ghost" size="icon" className="relative">
            <Bell className="h-5 w-5" />
            <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-red-500" />
            <span className="sr-only">Notifications</span>
          </Button>

          {/* Theme Toggle */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon">
                {!mounted ? (
                  <Monitor className="h-5 w-5" />
                ) : resolvedTheme === "dark" ? (
                  <Moon className="h-5 w-5" />
                ) : (
                  <Sun className="h-5 w-5" />
                )}
                <span className="sr-only">Toggle theme</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setTheme("light")}>
                <Sun className="mr-2 h-4 w-4" />
                Light
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setTheme("dark")}>
                <Moon className="mr-2 h-4 w-4" />
                Dark
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setTheme("system")}>
                <Monitor className="mr-2 h-4 w-4" />
                System
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* User Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="relative h-8 w-8 rounded-full">
                <Avatar className="h-8 w-8">
                  <AvatarImage src="/avatar.png" alt="User" />
                  <AvatarFallback>U</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem>Profile</DropdownMenuItem>
              <DropdownMenuItem>Settings</DropdownMenuItem>
              <DropdownMenuItem>Sign out</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  )
}

export function Sidebar() {
  const pathname = usePathname()
  const sidebarCollapsed = useAppStore((state) => state.sidebarCollapsed)
  const toggleSidebar = useAppStore((state) => state.toggleSidebar)

  return (
    <aside
      className={cn(
        "fixed left-0 top-16 z-30 h-[calc(100vh-4rem)] border-r bg-card transition-all duration-300",
        sidebarCollapsed ? "w-16" : "w-64"
      )}
    >
      <div className="flex h-full flex-col">
        <nav className="flex-1 space-y-1 p-3">
          {navigation.map((item) => {
            const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`)
            const Icon = item.icon

            return (
              <Link
                key={item.name}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
              >
                <Icon className="h-5 w-5 shrink-0" />
                {!sidebarCollapsed && <span>{item.name}</span>}
              </Link>
            )
          })}
        </nav>

        <div className="border-t p-3">
          <Button
            variant="ghost"
            size="icon"
            className="w-full justify-center"
            onClick={toggleSidebar}
          >
            <Menu className="h-5 w-5" />
          </Button>
        </div>
      </div>
    </aside>
  )
}

export function AppLayout({ children }: { children: React.ReactNode }) {
  const sidebarCollapsed = useAppStore((state) => state.sidebarCollapsed)

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <Sidebar />
      <CommandPalette />
      <KeyboardShortcuts />
      <main
        className={cn(
          "pt-16 transition-all duration-300",
          sidebarCollapsed ? "pl-16" : "pl-64"
        )}
      >
        <div className="p-6 lg:p-8">
          <Breadcrumb />
          <ErrorBoundary>
            {children}
          </ErrorBoundary>
        </div>
      </main>
    </div>
  )
}
