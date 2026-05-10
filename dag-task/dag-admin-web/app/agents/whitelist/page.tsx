"use client"

import { useState } from "react"
import Link from "next/link"
import { motion, AnimatePresence } from "framer-motion"
import { Shield, ShieldCheck, ShieldX, Plus, Pencil, Trash2, X, Check, ArrowLeft, Network, Fingerprint } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { useToast } from "@/components/ui/toast-provider"
import { useAgentWhitelists, useCreateAgentWhitelist, useUpdateAgentWhitelist, useDeleteAgentWhitelist } from "@/hooks/use-api"
import type { AgentWhitelist } from "@/types"
import { formatDate } from "@/lib/utils"

interface WhitelistFormData {
  ipSegment: string
  agentId: string
  description: string
  enabled: boolean
}

const initialFormData: WhitelistFormData = {
  ipSegment: "",
  agentId: "",
  description: "",
  enabled: true,
}

function WhitelistForm({ initialData, onSubmit, onCancel, isSubmitting }: {
  initialData?: WhitelistFormData
  onSubmit: (data: WhitelistFormData) => void
  onCancel: () => void
  isSubmitting: boolean
}) {
  const [form, setForm] = useState<WhitelistFormData>(initialData || initialFormData)
  const [error, setError] = useState<string>("")

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    if (!form.ipSegment.trim() && !form.agentId.trim()) {
      setError("Either IP Segment or Agent ID must be provided")
      return
    }
    onSubmit(form)
  }

  return (
    <motion.form initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }} transition={{ duration: 0.2 }} onSubmit={handleSubmit} className="overflow-hidden">
      <Card className="border-primary/20 bg-primary/5">
        <CardContent className="p-4 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-1.5"><Network className="h-3.5 w-3.5 text-muted-foreground" />IP Segment</label>
              <Input placeholder="e.g. 192.168.1.0/24" value={form.ipSegment} onChange={(e) => setForm((prev) => ({ ...prev, ipSegment: e.target.value }))} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-1.5"><Fingerprint className="h-3.5 w-3.5 text-muted-foreground" />Agent ID</label>
              <Input placeholder="e.g. agent-001" value={form.agentId} onChange={(e) => setForm((prev) => ({ ...prev, agentId: e.target.value }))} />
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Description</label>
            <Input placeholder="Enter a description for this whitelist entry" value={form.description} onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))} />
          </div>
          <div className="flex items-center gap-2">
            <Button type="button" variant={form.enabled ? "default" : "outline"} size="sm" onClick={() => setForm((prev) => ({ ...prev, enabled: true }))}><ShieldCheck className="mr-1.5 h-3.5 w-3.5" />Enabled</Button>
            <Button type="button" variant={!form.enabled ? "default" : "outline"} size="sm" onClick={() => setForm((prev) => ({ ...prev, enabled: false }))}><ShieldX className="mr-1.5 h-3.5 w-3.5" />Disabled</Button>
          </div>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" size="sm" onClick={onCancel}><X className="mr-1.5 h-3.5 w-3.5" />Cancel</Button>
            <Button type="submit" size="sm" disabled={isSubmitting}><Check className="mr-1.5 h-3.5 w-3.5" />{isSubmitting ? "Saving..." : initialData ? "Update" : "Create"}</Button>
          </div>
        </CardContent>
      </Card>
    </motion.form>
  )
}

function WhitelistRow({ entry, onEdit, onDelete }: { entry: AgentWhitelist; onEdit: (entry: AgentWhitelist) => void; onDelete: (id: number) => void }) {
  const [confirmDelete, setConfirmDelete] = useState(false)

  const handleDelete = () => {
    if (confirmDelete) {
      onDelete(entry.id)
      setConfirmDelete(false)
    } else {
      setConfirmDelete(true)
      setTimeout(() => setConfirmDelete(false), 3000)
    }
  }

  return (
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
      <Card className="hover:shadow-md transition-shadow">
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className={`p-2.5 rounded-lg shrink-0 ${entry.enabled ? "bg-emerald-500/10" : "bg-gray-500/10"}`}>
                <Shield className={`h-5 w-5 ${entry.enabled ? "text-emerald-400" : "text-gray-400"}`} />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  {entry.ipSegment && <span className="text-sm font-medium flex items-center gap-1"><Network className="h-3 w-3 text-muted-foreground" />{entry.ipSegment}</span>}
                  {entry.agentId && <span className="text-sm font-medium flex items-center gap-1"><Fingerprint className="h-3 w-3 text-muted-foreground" />{entry.agentId}</span>}
                </div>
                {entry.description && <p className="text-xs text-muted-foreground mt-0.5 truncate">{entry.description}</p>}
                <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground"><span>ID: {entry.id}</span><span>·</span><span>Updated: {formatDate(entry.updatedAt)}</span></div>
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <Badge variant={entry.enabled ? "success" : "failure"}>{entry.enabled ? "Enabled" : "Disabled"}</Badge>
              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onEdit(entry)}><Pencil className="h-3.5 w-3.5" /></Button>
              <Button variant={confirmDelete ? "destructive" : "ghost"} size="icon" className="h-8 w-8" onClick={handleDelete}><Trash2 className="h-3.5 w-3.5" /></Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  )
}

export default function AgentWhitelistPage() {
  const { data: whitelists, isLoading } = useAgentWhitelists()
  const createMutation = useCreateAgentWhitelist()
  const updateMutation = useUpdateAgentWhitelist()
  const deleteMutation = useDeleteAgentWhitelist()
  const { addToast } = useToast()

  const [showForm, setShowForm] = useState(false)
  const [editingEntry, setEditingEntry] = useState<AgentWhitelist | null>(null)

  const list: AgentWhitelist[] = whitelists || []
  const enabledCount = list.filter((w) => w.enabled).length
  const disabledCount = list.filter((w) => !w.enabled).length

  const handleCreate = (form: WhitelistFormData) => {
    createMutation.mutate(
      { ipSegment: form.ipSegment || undefined, agentId: form.agentId || undefined, description: form.description || undefined, enabled: form.enabled },
      {
        onSuccess: () => { addToast({ title: "Success", description: "Whitelist entry created", type: "success" }); setShowForm(false) },
        onError: () => { addToast({ title: "Error", description: "Failed to create whitelist entry", type: "error" }) },
      }
    )
  }

  const handleUpdate = (form: WhitelistFormData) => {
    if (!editingEntry) return
    updateMutation.mutate(
      { id: editingEntry.id, data: { ipSegment: form.ipSegment || undefined, agentId: form.agentId || undefined, description: form.description || undefined, enabled: form.enabled } },
      {
        onSuccess: () => { addToast({ title: "Success", description: "Whitelist entry updated", type: "success" }); setEditingEntry(null) },
        onError: () => { addToast({ title: "Error", description: "Failed to update whitelist entry", type: "error" }) },
      }
    )
  }

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id, {
      onSuccess: () => { addToast({ title: "Success", description: "Whitelist entry deleted", type: "success" }) },
      onError: () => { addToast({ title: "Error", description: "Failed to delete whitelist entry", type: "error" }) },
    })
  }

  const handleEdit = (entry: AgentWhitelist) => { setEditingEntry(entry); setShowForm(false) }
  const handleCancel = () => { setShowForm(false); setEditingEntry(null) }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Link href="/agents" className="text-muted-foreground hover:text-foreground transition-colors"><ArrowLeft className="h-4 w-4" /></Link>
            <h1 className="text-3xl font-bold tracking-tight">Agent Whitelist</h1>
          </div>
          <p className="text-muted-foreground">Manage IP segments and agent IDs that are allowed to register automatically.</p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-lg bg-primary/10"><Shield className="h-6 w-6 text-primary" /></div>
            <div><p className="text-2xl font-bold">{list.length}</p><p className="text-xs text-muted-foreground">Total Entries</p></div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-lg bg-emerald-500/10"><ShieldCheck className="h-6 w-6 text-emerald-400" /></div>
            <div><p className="text-2xl font-bold">{enabledCount}</p><p className="text-xs text-muted-foreground">Enabled</p></div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-lg bg-gray-500/10"><ShieldX className="h-6 w-6 text-gray-400" /></div>
            <div><p className="text-2xl font-bold">{disabledCount}</p><p className="text-xs text-muted-foreground">Disabled</p></div>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Whitelist Entries</h2>
          <Button onClick={() => { setEditingEntry(null); setShowForm((prev) => !prev) }} disabled={!!editingEntry}>
            <Plus className="mr-1.5 h-4 w-4" />{showForm ? "Cancel" : "Add Entry"}
          </Button>
        </div>

        <AnimatePresence>
          {showForm && <WhitelistForm onSubmit={handleCreate} onCancel={handleCancel} isSubmitting={createMutation.isPending} />}
          {editingEntry && (
            <WhitelistForm
              initialData={{ ipSegment: editingEntry.ipSegment || "", agentId: editingEntry.agentId || "", description: editingEntry.description || "", enabled: editingEntry.enabled }}
              onSubmit={handleUpdate}
              onCancel={handleCancel}
              isSubmitting={updateMutation.isPending}
            />
          )}
        </AnimatePresence>

        <div className="space-y-2">
          {isLoading ? (
            <><WhitelistRowSkeleton /><WhitelistRowSkeleton /><WhitelistRowSkeleton /></>
          ) : list.length === 0 ? (
            <Card><CardContent className="p-8 text-center text-muted-foreground">No whitelist entries found. Click &quot;Add Entry&quot; to create one.</CardContent></Card>
          ) : (
            list.map((entry) => <WhitelistRow key={entry.id} entry={entry} onEdit={handleEdit} onDelete={handleDelete} />)
          )}
        </div>
      </div>
    </div>
  )
}

function WhitelistRowSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-lg" />
            <div className="space-y-1.5"><Skeleton className="h-4 w-32" /><Skeleton className="h-3 w-48" /></div>
          </div>
          <div className="flex items-center gap-2"><Skeleton className="h-5 w-16" /><Skeleton className="h-8 w-8" /><Skeleton className="h-8 w-8" /></div>
        </div>
      </CardContent>
    </Card>
  )
}
