"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { ChevronRight, Home } from "lucide-react"
import { cn } from "@/lib/utils"

const routeNames: Record<string, string> = {
  "overview": "Overview",
  "workflows": "Workflows",
  "studio": "Studio",
  "executions": "Executions",
  "agents": "Agents",
  "settings": "Settings",
}

export function Breadcrumb() {
  const pathname = usePathname()
  const segments = pathname.split("/").filter(Boolean)

  if (segments.length === 0 || (segments.length === 1 && segments[0] === "overview")) {
    return null
  }

  return (
    <nav className="flex items-center gap-2 text-sm text-muted-foreground mb-4">
      <Link 
        href="/overview" 
        className="flex items-center gap-1 hover:text-foreground transition-colors"
      >
        <Home className="h-3.5 w-3.5" />
        <span className="hidden sm:inline">Home</span>
      </Link>
      
      {segments.map((segment, index) => {
        const isLast = index === segments.length - 1
        const href = "/" + segments.slice(0, index + 1).join("/")
        const name = routeNames[segment] || segment

        return (
          <div key={segment} className="flex items-center gap-2">
            <ChevronRight className="h-3.5 w-3.5" />
            {isLast ? (
              <span className="font-medium text-foreground">{name}</span>
            ) : (
              <Link 
                href={href}
                className="hover:text-foreground transition-colors"
              >
                {name}
              </Link>
            )}
          </div>
        )
      })}
    </nav>
  )
}
