"use client"

import { useState } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Plus, X } from "lucide-react"

interface ParameterField {
  name: string
  label: string
  type: "string" | "number" | "boolean" | "select" | "multiselect"
  required: boolean
  default?: any
  options?: { label: string; value: string }[]
  description?: string
}

interface ParameterFormProps {
  fields: ParameterField[]
  values: Record<string, any>
  onChange: (values: Record<string, any>) => void
  readOnly?: boolean
}

export function ParameterForm({ fields, values, onChange, readOnly }: ParameterFormProps) {
  const handleChange = (name: string, value: any) => {
    onChange({ ...values, [name]: value })
  }

  return (
    <div className="space-y-4">
      {fields.map((field) => (
        <div key={field.name} className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label className="text-sm font-medium">
              {field.label}
              {field.required && <span className="text-red-400 ml-1">*</span>}
            </label>
            {field.type !== "boolean" && (
              <span className="text-xs text-muted-foreground font-mono">{field.type}</span>
            )}
          </div>
          
          {field.description && (
            <p className="text-xs text-muted-foreground">{field.description}</p>
          )}

          {field.type === "string" && (
            <Input
              value={values[field.name] || field.default || ""}
              onChange={(e) => handleChange(field.name, e.target.value)}
              placeholder={`Enter ${field.label.toLowerCase()}...`}
              disabled={readOnly}
            />
          )}

          {field.type === "number" && (
            <Input
              type="number"
              value={values[field.name] || field.default || ""}
              onChange={(e) => handleChange(field.name, parseFloat(e.target.value))}
              placeholder={`Enter ${field.label.toLowerCase()}...`}
              disabled={readOnly}
            />
          )}

          {field.type === "boolean" && (
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={values[field.name] || field.default || false}
                onChange={(e) => handleChange(field.name, e.target.checked)}
                disabled={readOnly}
                className="h-4 w-4 rounded border-gray-300"
              />
              <span className="text-sm">Enable</span>
            </label>
          )}

          {field.type === "select" && field.options && (
            <select
              value={values[field.name] || field.default || ""}
              onChange={(e) => handleChange(field.name, e.target.value)}
              disabled={readOnly}
              className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="">Select {field.label.toLowerCase()}...</option>
              {field.options.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          )}

          {field.type === "multiselect" && field.options && (
            <div className="space-y-2">
              <div className="flex flex-wrap gap-2">
                {(values[field.name] || field.default || []).map((value: string, index: number) => (
                  <Badge key={index} variant="secondary" className="gap-1">
                    {field.options?.find(o => o.value === value)?.label || value}
                    {!readOnly && (
                      <button
                        onClick={() => {
                          const current = values[field.name] || []
                          handleChange(
                            field.name,
                            current.filter((v: string) => v !== value)
                          )
                        }}
                        className="hover:text-destructive"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    )}
                  </Badge>
                ))}
              </div>
              {!readOnly && (
                <select
                  onChange={(e) => {
                    if (e.target.value) {
                      const current = values[field.name] || []
                      if (!current.includes(e.target.value)) {
                        handleChange(field.name, [...current, e.target.value])
                      }
                      e.target.value = ""
                    }
                  }}
                  className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">Add {field.label.toLowerCase()}...</option>
                  {field.options
                    .filter(o => !(values[field.name] || []).includes(o.value))
                    .map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                </select>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

// Example usage component
export function ParameterFormExample() {
  const [values, setValues] = useState<Record<string, any>>({})

  const fields: ParameterField[] = [
    {
      name: "source_database",
      label: "Source Database",
      type: "string",
      required: true,
      description: "Database connection string"
    },
    {
      name: "batch_size",
      label: "Batch Size",
      type: "number",
      required: false,
      default: 1000,
      description: "Number of records per batch"
    },
    {
      name: "parallel",
      label: "Parallel Execution",
      type: "boolean",
      required: false,
      default: true
    },
    {
      name: "environment",
      label: "Environment",
      type: "select",
      required: true,
      options: [
        { label: "Development", value: "dev" },
        { label: "Staging", value: "staging" },
        { label: "Production", value: "prod" }
      ]
    },
    {
      name: "tables",
      label: "Tables to Process",
      type: "multiselect",
      required: false,
      options: [
        { label: "Users", value: "users" },
        { label: "Orders", value: "orders" },
        { label: "Products", value: "products" },
        { label: "Inventory", value: "inventory" }
      ]
    }
  ]

  return (
    <div className="space-y-4">
      <ParameterForm fields={fields} values={values} onChange={setValues} />
      <div className="p-4 rounded-lg bg-muted">
        <p className="text-sm font-medium mb-2">Current Values</p>
        <pre className="text-xs font-mono overflow-auto">
          {JSON.stringify(values, null, 2)}
        </pre>
      </div>
    </div>
  )
}
