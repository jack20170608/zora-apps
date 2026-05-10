"use client"

import { Component, type ReactNode } from "react"
import { motion } from "framer-motion"
import { AlertTriangle, RefreshCw, Home } from "lucide-react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("ErrorBoundary caught an error:", error, errorInfo)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="max-w-md w-full text-center space-y-6"
          >
            <div className="flex justify-center">
              <div className="p-4 rounded-full bg-red-500/10">
                <AlertTriangle className="h-12 w-12 text-red-400" />
              </div>
            </div>
            
            <div className="space-y-2">
              <h1 className="text-2xl font-bold">Something went wrong</h1>
              <p className="text-muted-foreground">
                An unexpected error occurred. Please try again or contact support.
              </p>
            </div>

            {this.state.error && (
              <div className="p-4 rounded-lg bg-muted text-left">
                <p className="text-xs font-mono text-red-400 break-all">
                  {this.state.error.message}
                </p>
              </div>
            )}

            <div className="flex gap-3 justify-center">
              <Button
                onClick={() => this.setState({ hasError: false })}
                variant="outline"
              >
                <RefreshCw className="mr-2 h-4 w-4" />
                Try Again
              </Button>
              <Link href="/overview">
                <Button>
                  <Home className="mr-2 h-4 w-4" />
                  Go Home
                </Button>
              </Link>
            </div>
          </motion.div>
        </div>
      )
    }

    return this.props.children
  }
}
