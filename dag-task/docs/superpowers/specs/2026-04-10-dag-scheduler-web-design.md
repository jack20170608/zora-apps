# DAG Scheduler Web UI - Design Specification

## Overview

`dag-scheduler-web` is a frontend UI module for the DAG-based task scheduling system. It provides:

- Visual DAG workflow building (drag-and-drop)
- Task template management
- Real-time task monitoring and statistics
- Agent cluster status overview

## Project Location

- Directory: `dag-task/dag-scheduler-web`
- Standalone Node.js/Next.js project (separate from Java modules)
- Added to parent Maven `pom.xml` but build is skipped (handled by npm)

## Technology Stack

| Component               | Choice                      | Reasoning                                                                 |
|-------------------------|-----------------------------|---------------------------------------------------------------------------|
| Framework               | **Next.js 15 (App Router)** | Industry standard React framework, excellent for admin dashboards        |
| Language                | **TypeScript**              | Type safety improves maintainability for larger projects                 |
| Styling                 | **Tailwind CSS + shadcn/ui**| Utility-first CSS with high-quality accessible components                |
| DAG Visualization       | **React Flow**              | De facto standard for interactive node-based graphs and workflows        |
| State Management        | **React Query + Context**   | Sufficient for this use case - avoids complexity of heavy state managers |
| HTTP Client             | **Axios**                   | Mature, simple, widely adopted                                           |
| Icons                   | **Lucide React**            | Modern, consistent, tree-shakable icon set                               |

## Application Structure

```
dag-scheduler-web/
├── app/                          # Next.js App Router pages
│   ├── (dashboard)/             # Main authenticated routes
│   │   ├── page.tsx             # Dashboard/home
│   │   ├── templates/           # Template management
│   │   ├── builder/[id]/        # DAG visual builder
│   │   └── monitoring/          # Task monitoring
│   ├── globals.css              # Global styles
│   └── layout.tsx               # Root layout
├── components/
│   ├── ui/                       # Base UI components (from shadcn)
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   └── ...
│   ├── dag-builder/              # DAG builder specific components
│   │   ├── DagCanvas.tsx        # Main React Flow canvas
│   │   ├── NodePanel.tsx        # Sidebar with node types
│   │   ├── NodeConfigPanel.tsx  # Configuration panel for selected node
│   │   └── Toolbar.tsx          # Save, validate, zoom controls
│   ├── template-list/            # Template management components
│   │   ├── TemplateTable.tsx
│   │   ├── TemplateDetailDrawer.tsx
│   │   └── VersionControl.tsx
│   ├── monitoring/               # Monitoring components
│   │   ├── TaskList.tsx
│   │   ├── TaskStatusCard.tsx
│   │   ├── ExecutionProgress.tsx
│   │   └── StatsDashboard.tsx
│   └── common/                   # Shared common components
│       ├── Sidebar.tsx
│       ├── Header.tsx
│       └── ApiErrorBoundary.tsx
├── hooks/                        # Custom React hooks
│   ├── use-api.ts
│   ├── use-templates.ts
│   └── use-task-execution.ts
├── lib/                          # Utilities and clients
│   ├── api-client.ts             # Axios instance configured
│   ├── types.ts                  # Shared TypeScript interfaces
│   └── utils.ts                  # Helper functions
├── types/                        # TypeScript type definitions
│   ├── template.ts               # Template types
│   ├── dag.ts                    # DAG node/edge types
│   └── task.ts                   # Task execution types
├── public/                        # Static assets
└── package.json
```

## Core Features

### 1. Template Management

**Features:**
- List all templates with search and filtering
- View template details (DAG preview, parameter schema, version history)
- Create new templates via visual builder
- Version management (activate/deactivate, delete old versions)
- Export template definition as JSON

### 2. Visual DAG Builder

**Features:**
- Drag-and-drop node creation from palette
- Click-to-create edges between nodes
- Click node to open configuration panel
- Real-time DAG validation:
  - Detect cycles (invalid DAG)
  - Validate required node properties
  - Check for disconnected nodes
- JSON preview of final DAG definition
- Auto-save as draft
- Parameter schema editor

### 3. Task Monitoring & Statistics

**Features:**
- Dashboard with key metrics:
  - Total templates
  - Tasks running today
  - Success/failure rate
  - Agent count (online/offline)
- Task execution list with filtering by status
- Task detail view with:
  - DAG progress visualization (which nodes completed/failed)
  - Execution logs
  - Timing information
- Agent status list: shows connected agents, their capacity and current load

### 4. API Integration

Integrates with existing REST API endpoints from `dag-scheduler-muserver`:

| Feature                  | Endpoints Used                    |
|--------------------------|-----------------------------------|
| Template CRUD            | `TaskTemplateApi` (already exists) |
| List versions            | `GET /api/v1/template/{key}/versions` |
| Activate/Deactivate      | `POST /api/v1/template/{key}/v/{version}/deactivate` |
| Instantiate task         | `POST /api/v1/template/{key}/instantiate` |
| Task list/status         | Will use existing TaskExecution endpoints |
| Agent list/status        | Will use existing AgentRegistry endpoints |

## Project Configuration

- Node.js: 20+ LTS
- Package manager: npm (can use pnpm or yarn if preferred)
- ESLint configuration included with Next.js defaults
- Build output: Static export can be served by any web server or integrated with the backend server

## CORS Considerations

- During development: Next.js dev server runs on `localhost:3000`, backend on `localhost:8080`
- CORS needs to be enabled on the backend MuServer for `localhost:3000`
- In production: Can be served from same origin or behind reverse proxy

## Success Criteria

- Can create a new task template visually with DAG builder
- Can view, edit, and manage template versions
- Can monitor running tasks and see progress on the DAG
- UI is responsive and works on desktop
- All TypeScript types match backend API models
- Project builds successfully with `npm run build`
