"use client"

import { useState, useEffect } from "react"

export function useRealTimeLogs(executionId: string, isRunning: boolean) {
  const [logs, setLogs] = useState<Array<{timestamp: string; level: string; message: string}>>([])

  useEffect(() => {
    if (!isRunning) return

    const baseLogs = [
      { timestamp: new Date().toISOString(), level: "INFO", message: `Starting execution ${executionId}` },
      { timestamp: new Date().toISOString(), level: "INFO", message: "Initializing task context" },
      { timestamp: new Date().toISOString(), level: "INFO", message: "Loading workflow definition" },
    ]
    setLogs(baseLogs)

    const messages = [
      "Task node-1: Extracting data from source",
      "Task node-1: Processing batch 1/10",
      "Task node-1: Processing batch 2/10",
      "Task node-1: Processing batch 3/10",
      "Task node-2: Transforming data",
      "Task node-2: Applying filters",
      "Task node-2: Validating schema",
      "Task node-3: Loading to warehouse",
      "Task node-3: Committing transaction",
      "Execution completed successfully"
    ]

    let index = 0
    const interval = setInterval(() => {
      if (index < messages.length) {
        setLogs(prev => [...prev, {
          timestamp: new Date().toISOString(),
          level: "INFO",
          message: messages[index]
        }])
        index++
      } else {
        clearInterval(interval)
      }
    }, 2000)

    return () => clearInterval(interval)
  }, [executionId, isRunning])

  return logs
}
