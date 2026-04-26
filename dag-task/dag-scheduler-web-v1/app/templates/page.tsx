import { TemplateTable } from "@/components/template-list/TemplateTable";

export default function TemplatesPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Templates</h1>
        <p className="text-muted-foreground">
          Manage your DAG workflow templates. Create new templates or edit existing ones.
        </p>
      </div>
      <TemplateTable />
    </div>
  );
}
