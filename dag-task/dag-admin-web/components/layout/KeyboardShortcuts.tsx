"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { X, Keyboard, Command } from "lucide-react"
import { Button } from "@/components/ui/button"

interface Shortcut {
  keys: string[]
  description: string
  category: string
}

const shortcuts: Shortcut[] = [
  { keys: ["⌘", "K"], description: "Open Command Palette", category: "Global" },
  { keys: ["?"], description: "Show Keyboard Shortcuts", category: "Global" },
  { keys: ["Esc"], description: "Close Modal / Panel", category: "Global" },
  { keys: ["⌘", "S"], description: "Save Workflow", category: "Studio" },
  { keys: ["⌘", "Z"], description: "Undo", category: "Studio" },
  { keys: ["⌘", "Shift", "Z"], description: "Redo", category: "Studio" },
  { keys: ["Delete"], description: "Delete Selected Node", category: "Studio" },
  { keys: ["Space", "Drag"], description: "Pan Canvas", category: "Studio" },
  { keys: ["Scroll"], description: "Zoom Canvas", category: "Studio" },
  { keys: ["G"], description: "Toggle Grid", category: "Studio" },
  { keys: ["M"], description: "Toggle Minimap", category: "Studio" },
  { keys: ["F"], description: "Fit View", category: "Studio" },
  { keys: ["R"], description: "Refresh List", category: "Executions" },
]

export function KeyboardShortcuts() {
  const [isOpen, setIsOpen] = useState(false)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "?" && !e.metaKey && !e.ctrlKey) {
        e.preventDefault()
        setIsOpen(prev => !prev)
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [])

  const grouped = shortcuts.reduce((acc, shortcut) => {
    if (!acc[shortcut.category]) acc[shortcut.category] = []
    acc[shortcut.category].push(shortcut)
    return acc
  }, {} as Record<string, Shortcut[]>)

  return (
    <>
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm"
            onClick={() => setIsOpen(false)}
          >
            <div className="flex items-start justify-center pt-[15vh]" onClick={e => e.stopPropagation()}>
              <motion.div
                initial={{ opacity: 0, y: -20, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -20, scale: 0.95 }}
                className="w-full max-w-2xl bg-card border rounded-xl shadow-2xl overflow-hidden"
              >
                <div className="flex items-center justify-between p-4 border-b">
                  <div className="flex items-center gap-2">
                    <Keyboard className="h-5 w-5" />
                    <h2 className="text-lg font-semibold">Keyboard Shortcuts</h2>
                  </div>
                  <Button variant="ghost" size="icon" onClick={() => setIsOpen(false)}>
                    <X className="h-4 w-4" />
                  </Button>
                </div>

                <div className="p-4 max-h-[500px] overflow-y-auto">
                  <div className="grid gap-6">
                    {Object.entries(grouped).map(([category, items]) => (
                      <div key={category}>
                        <h3 className="text-sm font-medium text-muted-foreground mb-3">
                          {category}
                        </h3>
                        <div className="space-y-2">
                          {items.map((shortcut, index) => (
                            <div
                              key={index}
                              className="flex items-center justify-between py-2 px-3 rounded-lg hover:bg-muted/50 transition-colors"
                            >
                              <span className="text-sm">{shortcut.description}</span>
                              <div className="flex items-center gap-1">
                                {shortcut.keys.map((key, keyIndex) => (
                                  <kbd
                                    key={keyIndex}
                                    className="inline-flex h-7 min-w-[28px] items-center justify-center rounded border bg-muted px-1.5 font-mono text-xs"
                                  >
                                    {key}
                                  </kbd>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="p-3 border-t text-xs text-muted-foreground text-center">
                  Press <kbd className="inline-flex h-5 items-center rounded border bg-muted px-1 font-mono">?</kbd> to toggle this panel
                </div>
              </motion.div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}
