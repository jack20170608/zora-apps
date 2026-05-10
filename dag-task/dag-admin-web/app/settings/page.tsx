"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import {
  Settings,
  Bell,
  Shield,
  Database,
  Globe,
  Key,
  Users,
  Save,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Separator } from "@/components/ui/separator"

export default function SettingsPage() {
  const [saved, setSaved] = useState(false)

  const handleSave = () => {
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
        <p className="text-muted-foreground">
          Manage system configuration and preferences.
        </p>
      </div>

      <Tabs defaultValue="general" className="space-y-6">
        <TabsList>
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="notifications">Notifications</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
          <TabsTrigger value="advanced">Advanced</TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>System Configuration</CardTitle>
              <CardDescription>Configure basic system settings</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <label className="text-sm font-medium">System Name</label>
                  <Input defaultValue="DAG Scheduler" />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Default Timezone</label>
                  <Input defaultValue="Asia/Shanghai" />
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">API Base URL</label>
                <Input defaultValue="http://localhost:8081/api/v1" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Execution Settings</CardTitle>
              <CardDescription>Configure default execution behavior</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Default Timeout (minutes)</label>
                  <Input type="number" defaultValue="60" />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Max Retries</label>
                  <Input type="number" defaultValue="3" />
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="notifications" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Notification Channels</CardTitle>
              <CardDescription>Configure how you receive alerts</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Webhook URL</label>
                <Input placeholder="https://hooks.example.com/..." />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Email Recipients</label>
                <Input placeholder="admin@example.com, ops@example.com" />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>API Keys</CardTitle>
              <CardDescription>Manage API access keys</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <label className="text-sm font-medium">API Key</label>
                <div className="flex gap-2">
                  <Input type="password" defaultValue="sk-xxxxxxxxxxxxxxxx" readOnly />
                  <Button variant="outline">Regenerate</Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="advanced" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Advanced Settings</CardTitle>
              <CardDescription>System-level configuration</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Log Level</label>
                <Input defaultValue="INFO" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Max Concurrent Executions</label>
                <Input type="number" defaultValue="50" />
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="flex justify-end gap-4">
        <Button variant="outline">Cancel</Button>
        <Button onClick={handleSave}>
          <Save className="mr-2 h-4 w-4" />
          {saved ? "Saved!" : "Save Changes"}
        </Button>
      </div>
    </div>
  )
}
