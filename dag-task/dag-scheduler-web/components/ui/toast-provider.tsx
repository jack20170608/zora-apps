"use client"

import { createContext, useContext, useState, useCallback } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { X, CheckCircle2, AlertCircle, Info } from "lucide-react"

interface Toast {
  id: string
  title: string
  description?: string
  type: "success" | "error" | "info"
}

interface ToastContextType {
  toasts: Toast[]
  addToast: (toast: Omit<Toast, "id">) => void
  removeToast: (id: string) => void
}

const ToastContext = createContext<ToastContextType | null>(null)

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const addToast = useCallback((toast: Omit<Toast, "id">) => {
    const id = Math.random().toString(36).substring(7)
    setToasts(prev => [...prev, { ...toast, id }])
    
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id))
    }, 5000)
  }, [])

  const removeToast = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
        <AnimatePresence>
          {toasts.map(toast => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 50, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 20, scale: 0.9 }}
              className={`flex items-start gap-3 p-4 rounded-lg shadow-lg border min-w-[320px] max-w-[420px] ${
                toast.type === 'success' ? 'bg-emerald-950/90 border-emerald-800' :
                toast.type === 'error' ? 'bg-red-950/90 border-red-800' :
                'bg-blue-950/90 border-blue-800'
              }`}
            >
              {toast.type === 'success' && <CheckCircle2 className="h-5 w-5 text-emerald-400 mt-0.5" />}
              {toast.type === 'error' && <AlertCircle className="h-5 w-5 text-red-400 mt-0.5" />}
              {toast.type === 'info' && <Info className="h-5 w-5 text-blue-400 mt-0.5" />}
              <div className="flex-1 min-w-0">
                <p className={`font-medium text-sm ${
                  toast.type === 'success' ? 'text-emerald-200' :
                  toast.type === 'error' ? 'text-red-200' :
                  'text-blue-200'
                }`}>
                  {toast.title}
                </p>
                {toast.description && (
                  <p className={`text-xs mt-1 ${
                    toast.type === 'success' ? 'text-emerald-300/70' :
                    toast.type === 'error' ? 'text-red-300/70' :
                    'text-blue-300/70'
                  }`}>
                    {toast.description}
                  </p>
                )}
              </div>
              <button
                onClick={() => removeToast(toast.id)}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider")
  }
  return context
}
