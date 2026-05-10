"use client"

import { useState, useEffect, useRef, useCallback } from "react"

interface WebSocketMessage {
  type: string
  data: any
  timestamp: string
}

export function useWebSocket(url: string) {
  const [isConnected, setIsConnected] = useState(false)
  const [lastMessage, setLastMessage] = useState<WebSocketMessage | null>(null)
  const ws = useRef<WebSocket | null>(null)
  const reconnectAttempts = useRef(0)
  const maxReconnectAttempts = 5

  const connect = useCallback(() => {
    try {
      // 模拟 WebSocket 连接
      console.log(`Connecting to ${url}...`)
      
      // 使用模拟数据代替真实 WebSocket
      const mockConnection = {
        readyState: WebSocket.CONNECTING,
        send: (data: string) => {
          console.log("WS Send:", data)
        },
        close: () => {
          console.log("WS Closed")
        }
      }

      // 模拟连接成功
      setTimeout(() => {
        setIsConnected(true)
        reconnectAttempts.current = 0
        
        // 模拟接收消息
        const interval = setInterval(() => {
          const mockMessages: WebSocketMessage[] = [
            {
              type: "agent_heartbeat",
              data: {
                agentId: "agent-001",
                cpuUsage: Math.random() * 100,
                memoryUsage: Math.random() * 100,
                currentTasks: Math.floor(Math.random() * 5)
              },
              timestamp: new Date().toISOString()
            },
            {
              type: "execution_update",
              data: {
                executionId: "exec-001",
                progress: Math.floor(Math.random() * 100),
                status: "running"
              },
              timestamp: new Date().toISOString()
            },
            {
              type: "system_alert",
              data: {
                level: "warning",
                message: "High CPU usage detected on agent-002"
              },
              timestamp: new Date().toISOString()
            }
          ]
          
          const randomMessage = mockMessages[Math.floor(Math.random() * mockMessages.length)]
          setLastMessage(randomMessage)
        }, 5000)

        // 保存 interval ID 用于清理
        ;(mockConnection as any).intervalId = interval
      }, 1000)

      ws.current = mockConnection as any
    } catch (error) {
      console.error("WebSocket connection error:", error)
      handleReconnect()
    }
  }, [url])

  const handleReconnect = useCallback(() => {
    if (reconnectAttempts.current < maxReconnectAttempts) {
      reconnectAttempts.current++
      const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000)
      setTimeout(connect, delay)
    }
  }, [connect])

  const sendMessage = useCallback((message: any) => {
    if (ws.current && isConnected) {
      ws.current.send(JSON.stringify(message))
    }
  }, [isConnected])

  useEffect(() => {
    connect()
    return () => {
      if (ws.current) {
        ws.current.close()
      }
    }
  }, [connect])

  return { isConnected, lastMessage, sendMessage }
}

// Hook for real-time execution updates
export function useExecutionRealtime(executionId: string) {
  const [progress, setProgress] = useState(0)
  const [status, setStatus] = useState("running")
  const [logs, setLogs] = useState<string[]>([])

  useEffect(() => {
    // 模拟实时进度更新
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          setStatus("success")
          return 100
        }
        return prev + Math.random() * 10
      })
    }, 3000)

    // 模拟日志流
    const logInterval = setInterval(() => {
      const mockLogs = [
        "Processing batch...",
        "Validating data...",
        "Writing to database...",
        "Updating cache...",
        "Sending notifications..."
      ]
      setLogs(prev => [...prev, mockLogs[Math.floor(Math.random() * mockLogs.length)]])
    }, 5000)

    return () => {
      clearInterval(interval)
      clearInterval(logInterval)
    }
  }, [executionId])

  return { progress, status, logs }
}

// Hook for real-time agent metrics
export function useAgentRealtime(agentId: string) {
  const [metrics, setMetrics] = useState({
    cpuUsage: 0,
    memoryUsage: 0,
    diskUsage: 0,
    currentTasks: 0
  })

  useEffect(() => {
    const interval = setInterval(() => {
      setMetrics({
        cpuUsage: Math.floor(Math.random() * 100),
        memoryUsage: Math.floor(Math.random() * 100),
        diskUsage: Math.floor(Math.random() * 100),
        currentTasks: Math.floor(Math.random() * 10)
      })
    }, 3000)

    return () => clearInterval(interval)
  }, [agentId])

  return metrics
}
