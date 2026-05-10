"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { motion, AnimatePresence } from "framer-motion"
import {
  Search,
  X,
  LayoutDashboard,
  GitBranch,
  Play,
  Radio,
  Server,
  Settings,
  FileText,
  ArrowRight,
} from "lucide-react"
import { Input } from "@/components/ui/input"

interface SearchItem {
  id: string
  title: string
  subtitle: string
  icon: any
  href: string
  category: string
}

const searchItems: SearchItem[] = [
  { id: "1", title: "Overview", subtitle: "Dashboard and system metrics", icon: LayoutDashboard, href: "/overview", category: "Navigation" },
  { id: "2", title: "Workflows", subtitle: "Manage workflow templates", icon: GitBranch, href: "/workflows", category: "Navigation" },
  { id: "3", title: "Studio", subtitle: "Design DAG workflows", icon: Play, href: "/studio", category: "Navigation" },
  { id: "4", title: "Executions", subtitle: "Monitor task executions", icon: Radio, href: "/executions", category: "Navigation" },
  { id: "5", title: "Agents", subtitle: "Manage execution agents", icon: Server, href: "/agents", category: "Navigation" },
  { id: "6", title: "Settings", subtitle: "System configuration", icon: Settings, href: "/settings", category: "Navigation" },
  { id: "7", title: "Daily ETL Pipeline", subtitle: "Workflow template", icon: FileText, href: "/workflows/etl-daily", category: "Workflows" },
  { id: "8", title: "Weekly Database Backup", subtitle: "Workflow template", icon: FileText, href: "/workflows/backup-weekly", category: "Workflows" },
  { id: "9", title: "ML Model Training", subtitle: "Workflow template", icon: FileText, href: "/workflows/ml-training", category: "Workflows" },
  { id: "10", title: "Execution #001", subtitle: "Running - Daily ETL Pipeline", icon: Play, href: "/executions/exec-001", category: "Executions" },
  { id: "11", title: "Execution #002", subtitle: "Success - Weekly Backup", icon: FileText, href: "/executions/exec-002", category: "Executions" },
]

export function CommandPalette() {
  const [isOpen, setIsOpen] = useState(false)
  const [query, setQuery] = useState("")
  const [selectedIndex, setSelectedIndex] = useState(0)
  const router = useRouter()

  const filteredItems = searchItems.filter(item =>
    item.title.toLowerCase().includes(query.toLowerCase()) ||
    item.subtitle.toLowerCase().includes(query.toLowerCase())
  )

  const groupedItems = filteredItems.reduce((acc, item) => {
    if (!acc[item.category]) acc[item.category] = []
    acc[item.category].push(item)
    return acc
  }, {} as Record<string, SearchItem[]>)

  const allItems = Object.values(groupedItems).flat()

  const handleSelect = useCallback((item: SearchItem) => {
    router.push(item.href)
    setIsOpen(false)
    setQuery("")
  }, [router])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault()
        setIsOpen(prev => !prev)
      }
      if (e.key === "Escape") {
        setIsOpen(false)
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [])

  useEffect(() => {
    setSelectedIndex(0)
  }, [query])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return

      if (e.key === "ArrowDown") {
        e.preventDefault()
        setSelectedIndex(prev => (prev + 1) % allItems.length)
      } else if (e.key === "ArrowUp") {
        e.preventDefault()
        setSelectedIndex(prev => (prev - 1 + allItems.length) % allItems.length)
      } else if (e.key === "Enter") {
        e.preventDefault()
        if (allItems[selectedIndex]) {
          handleSelect(allItems[selectedIndex])
        }
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [isOpen, allItems, selectedIndex, handleSelect])

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm"
        onClick={() => setIsOpen(false)}
      >
        <div className="flex items-start justify-center pt-[20vh]" onClick={e => e.stopPropagation()}>
          <motion.div
            initial={{ opacity: 0, y: -20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            className="w-full max-w-2xl bg-card border rounded-xl shadow-2xl overflow-hidden"
          >
            <div className="flex items-center gap-3 p-4 border-b">
              <Search className="h-5 w-5 text-muted-foreground" />
              <Input
                value={query}
                onChange={e => setQuery(e.target.value)}
                placeholder="Search workflows, executions, pages..."
                className="border-0 focus-visible:ring-0 text-lg"
                autoFocus
              />
              <kbd className="hidden sm:inline-flex h-8 items-center gap-1 rounded border bg-muted px-2 font-mono text-xs">
                <span>ESC</span>
              </kbd>
            </div>

            <div className="max-h-[400px] overflow-y-auto">
              {allItems.length === 0 ? (
                <div className="p-8 text-center text-muted-foreground">
                  <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  <p>No results found</p>
                </div>
              ) : (
                Object.entries(groupedItems).map(([category, items]) => (
                  <div key={category}>
                    <div className="px-4 py-2 text-xs font-medium text-muted-foreground bg-muted/50">
                      {category}
                    </div>
                    {items.map((item) => {
                      const Icon = item.icon
                      const globalIndex = allItems.indexOf(item)
                      const isSelected = globalIndex === selectedIndex

                      return (
                        <button
                          key={item.id}
                          onClick={() => handleSelect(item)}
                          className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors ${
                            isSelected ? "bg-accent" : "hover:bg-accent/50"
                          }`}
                        >
                          <div className="p-2 rounded-md bg-muted">
                            <Icon className="h-4 w-4" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="font-medium truncate">{item.title}</p>
                            <p className="text-sm text-muted-foreground truncate">{item.subtitle}</p>
                          </div>
                          {isSelected && (
                            <ArrowRight className="h-4 w-4 text-muted-foreground" />
                          )}
                        </button>
                      )
                    })}
                  </div>
                ))
              )}
            </div>

            <div className="flex items-center gap-4 p-3 border-t text-xs text-muted-foreground">
              <div className="flex items-center gap-1">
                <kbd className="h-6 items-center rounded border bg-muted px-1.5 font-mono">↑↓</kbd>
                <span>Navigate</span>
              </div>
              <div className="flex items-center gap-1">
                <kbd className="h-6 items-center rounded border bg-muted px-1.5 font-mono">↵</kbd>
                <span>Select</span>
              </div>
              <div className="flex items-center gap-1">
                <kbd className="h-6 items-center rounded border bg-muted px-1.5 font-mono">ESC</kbd>
                <span>Close</span>
              </div>
            </div>
          </motion.div>
        </div>
      </motion.div>
    </AnimatePresence>
  )
}
