# DAG Scheduler Web UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a complete Next.js 15 + TypeScript frontend project for DAG task scheduling system with visual DAG builder, template management, and task monitoring.

**Architecture:** Standalone frontend project in `dag-task/dag-scheduler-web` that connects to existing backend REST APIs. Uses Next.js App Router with TypeScript, Tailwind CSS + shadcn/ui for components, React Flow for DAG visualization.

**Tech Stack:** Next.js 15, TypeScript, Tailwind CSS, shadcn/ui, React Flow, Axios, React Query, Lucide React

---

## File Structure Overview

```
dag-task/
├── pom.xml (modify - add new module)
└── dag-scheduler-web/
    ├── .gitignore
    ├── package.json
    ├── tsconfig.json
    ├── next.config.ts
    ├── tailwind.config.ts
    ├── postcss.config.js
    ├── .eslintrc.json
    ├── app/
    │   ├── globals.css
    │   ├── layout.tsx
    │   ├── page.tsx (dashboard)
    │   ├── templates/
    │   │   └── page.tsx
    │   ├── builder/
    │   │   └── [id]/
    │   │       └── page.tsx
    │   └── monitoring/
    │       └── page.tsx
    ├── components/
    │   ├── ui/ (shadcn base components)
    │   ├── dag-builder/
    │   │   ├── DagCanvas.tsx
    │   │   ├── NodePanel.tsx
    │   │   ├── NodeConfigPanel.tsx
    │   │   └── Toolbar.tsx
    │   ├── template-list/
    │   │   ├── TemplateTable.tsx
    │   │   └── VersionControl.tsx
    │   ├── monitoring/
    │   │   ├── TaskList.tsx
    │   │   └── StatsDashboard.tsx
    │   └── common/
    │       ├── Sidebar.tsx
    │       └── Header.tsx
    ├── lib/
    │   ├── api-client.ts
    │   └── utils.ts
    └── types/
        ├── template.ts
        ├── dag.ts
        └── task.ts
```

---

## Tasks

### Task 1: Scaffold Next.js 15 TypeScript project

**Files:**
- Create: `dag-scheduler-web/.gitignore`
- Create: `dag-scheduler-web/package.json`
- Create: `dag-scheduler-web/tsconfig.json`
- Create: `dag-scheduler-web/next.config.ts`
- Create: `dag-scheduler-web/.eslintrc.json`
- Modify: `pom.xml` - add `dag-scheduler-web` module

- [ ] **Step 1: Create project directory**

```bash
mkdir -p D:/project/nas_gogs/zora-apps/dag-task/dag-scheduler-web
```

- [ ] **Step 2: Create package.json**

```json
{
  "name": "dag-scheduler-web",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint"
  },
  "dependencies": {
    "@types/node": "^22.8.1",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "axios": "^1.7.7",
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "lucide-react": "^0.462.0",
    "next": "15.0.3",
    "react": "19.0.0-rc-69d4b800-20241021",
    "react-dom": "19.0.0-rc-69d4b800-20241021",
    "reactflow": "^11.11.4",
    "@tanstack/react-query": "^5.62.0",
    "tailwind-merge": "^2.5.5",
    "tailwindcss-animate": "^1.0.7",
    "typescript": "^5.6.3"
  },
  "devDependencies": {
    "eslint": "^9.15.0",
    "eslint-config-next": "15.0.3",
    "postcss": "^8.4.49",
    "tailwindcss": "^3.4.15"
  }
}
```

- [ ] **Step 3: Create tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "jsx": "preserve",
    "incremental": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "allowImportingTsExtensions": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./*"],
      "lib/*": ["./lib/*"],
      "types/*": ["./types/*"],
      "components/*": ["./components/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx"],
  "exclude": ["node_modules"]
}
```

- [ ] **Step 4: Create next.config.ts**

```typescript
/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  rewrites: async () => {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
};

module.exports = nextConfig;
```

- [ ] **Step 5: Create .eslintrc.json**

```json
{
  "extends": ["next/core-web-vitals"],
  "rules": {
    "@typescript-eslint/no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }]
  }
}
```

- [ ] **Step 6: Create .gitignore (standard Next.js)**

```
# dependencies
node_modules
.pnp
*.pnp.js

# testing
coverage

# production
build
.next
out

# misc
.DS_Store
*.pem
*.log

# debug
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# local env files
.env*.local
.env

# vercel
.vercel

# typescript
*.tsbuildinfo
next-env.d.ts
```

- [ ] **Step 7: Create tailwind.config.ts**

```typescript
import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    './pages/**/*.{ts,tsx}',
    './components/**/*.{ts,tsx}',
    './app/**/*.{ts,tsx}',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};

export default config;
```

- [ ] **Step 8: Create postcss.config.js**

```javascript
module.exports = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

- [ ] **Step 9: Add module to parent pom.xml**

Modify `dag-task/pom.xml` → add `<module>dag-scheduler-web</module>` to `<modules>` section.

- [ ] **Step 10: Create pom.xml for dag-scheduler-web (empty packaging, maven skips build)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>1.0.1-SNAPSHOT</version>
    </parent>

    <modelVersion>4.0.0</modelVersion>
    <artifactId>dag-scheduler-web</artifactId>
    <name>dag-scheduler-web - DAG Scheduler Web UI</name>
    <description>Frontend UI for DAG task scheduler with visual DAG builder</description>
    <packaging>pom</packaging>

    <!-- This is a Node.js/Next.js project, Maven does not build it -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>flatten-maven-plugin</artifactId>
                <version>1.6.0</version>
                <configuration>
                    <updatePomFile>true</updatePomFile>
                    <flattenMode>resolveCiFriendliesOnly</flattenMode>
                </configuration>
                <executions>
                    <execution>
                        <id>flatten</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>flatten</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>flatten-clean</id>
                        <phase>clean</phase>
                        <goals>
                            <goal>flatten</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 11: Create README.md**

````markdown
# dag-scheduler-web

Frontend UI for the DAG-based task scheduling system.

## Features

- **Visual DAG Builder** - Drag-and-drop workflow creation
- **Template Management** - Versioned template management
- **Task Monitoring** - Real-time execution monitoring and statistics
- **Agent Status** - View connected execution agents

## Getting Started

### Prerequisites

- Node.js 20+
- npm/yarn/pnpm

### Installation

```bash
cd dag-scheduler-web
npm install
```

### Development

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build

```bash
npm run build
```

The build output will be in `.next/` directory.

## Backend Connection

The UI connects to the DAG scheduler backend API. By default:
- Backend runs at `http://localhost:8080`
- API requests under `/api/*` are proxied automatically in development
- CORS must be enabled on the backend for `http://localhost:3000`

## Tech Stack

- Next.js 15 (App Router)
- TypeScript
- Tailwind CSS + shadcn/ui
- React Flow for DAG visualization
- TanStack React Query for data fetching
- Axios for HTTP client
````

- [ ] **Step 12: Commit**

```bash
git add dag-task/pom.xml
git add dag-task/dag-scheduler-web/
git commit -m "feat: scaffold dag-scheduler-web Next.js project"
```

---

### Task 2: Configure base app structure and styles

**Files:**
- Create: `dag-scheduler-web/app/globals.css`
- Create: `dag-scheduler-web/app/layout.tsx`
- Create: `dag-scheduler-web/app/page.tsx`
- Create: `dag-scheduler-web/lib/utils.ts`

- [ ] **Step 1: Create globals.css with tailwind directives and CSS variables**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --card: 0 0% 100%;
    --card-foreground: 222.2 84% 4.9%;
    --popover: 0 0% 100%;
    --popover-foreground: 222.2 84% 4.9%;
    --primary: 221.2 83.2% 53.3%;
    --primary-foreground: 210 40% 98%;
    --secondary: 210 40% 96.1%;
    --secondary-foreground: 222.2 47.4% 11.2%;
    --muted: 210 40% 96.1%;
    --muted-foreground: 215.4 16.3% 46.9%;
    --accent: 210 40% 96.1%;
    --accent-foreground: 222.2 47.4% 11.2%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 210 40% 98%;
    --border: 214.3 31.8% 91.4%;
    --input: 214.3 31.8% 91.4%;
    --ring: 221.2 83.2% 53.3%;
    --radius: 0.5rem;
  }

  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --card: 222.2 84% 4.9%;
    --card-foreground: 210 40% 98%;
    --popover: 222.2 84% 4.9%;
    --popover-foreground: 210 40% 98%;
    --primary: 217.2 91.2% 59.8%;
    --primary-foreground: 222.2 47.4% 11.2%;
    --secondary: 217.2 32.6% 17.5%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217.2 32.6% 17.5%;
    --muted-foreground: 215 20.2% 65.1%;
    --accent: 217.2 32.6% 17.5%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 62.8% 30.6%;
    --destructive-foreground: 210 40% 98%;
    --border: 217.2 32.6% 17.5%;
    --input: 217.2 32.6% 17.5%;
    --ring: 224.3 76.3% 48%;
  }
}

  * {
    @apply border-border;
  }
  body {
    @apply bg-background text-foreground;
  }
}
```

- [ ] **Step 2: Create lib/utils.ts**

```typescript
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 3: Create app/layout.tsx**

```typescript
import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Sidebar } from "@/components/common/Sidebar";
import { Header } from "@/components/common/Header";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "DAG Scheduler",
  description: "DAG-based task scheduling system - Visual workflow builder",
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
    },
  },
});

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <QueryClientProvider client={queryClient}>
          <div className="flex h-screen bg-background">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
              <Header />
              <main className="flex-1 overflow-auto p-4">
                {children}
              </main>
            </div>
          </div>
        </QueryClientProvider>
      </body>
    </html>
  );
}
```

- [ ] **Step 4: Create app/page.tsx (dashboard home page)**

```typescript
import { StatsDashboard } from "@/components/monitoring/StatsDashboard";
import Link from "next/link";
import { ChevronRight } from "lucide-react";

export default function Home() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Welcome to DAG Scheduler. Monitor tasks and manage workflow templates.
        </p>
      </div>
      <StatsDashboard />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Link
          href="/templates"
          className="group rounded-lg border p-6 hover:border-primary transition-colors"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Template Management</h2>
            <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </div>
          <p className="text-sm text-muted-foreground mt-2">
            Browse, create, and manage workflow templates.
          </p>
        </Link>
        <Link
          href="/builder/new"
          className="group rounded-lg border p-6 hover:border-primary transition-colors"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">New DAG</h2>
            <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </div>
          <p className="text-sm text-muted-foreground mt-2">
            Create a new workflow template with visual DAG builder.
          </p>
        </Link>
        <Link
          href="/monitoring"
          className="group rounded-lg border p-6 hover:border-primary transition-colors"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Monitoring</h2>
            <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </div>
          <p className="text-sm text-muted-foreground mt-2">
            Monitor running tasks and view execution statistics.
          </p>
        </Link>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Commit**

```bash
git add dag-scheduler-web/app/ dag-scheduler-web/lib/
git commit -m "feat: add base app structure and styles"
```

---

### Task 3: Create TypeScript type definitions

**Files:**
- Create: `dag-scheduler-web/types/template.ts`
- Create: `dag-scheduler-web/types/dag.ts`
- Create: `dag-scheduler-web/types/task.ts`

- [ ] **Step 1: Create types/template.ts** (matches backend TaskTemplate)

```typescript
export interface TaskTemplate {
  id?: number;
  templateKey: string;
  templateName: string;
  description: string;
  version: string;
  active: boolean;
  dagDefinition: string; // JSON string of DAG definition
  parameterSchema: string; // JSON string of parameter schema
  createDt?: string;
  lastUpdateDt?: string;
  versionSeq?: number;
}

export interface TemplateListResponse {
  code: number;
  message: string;
  data: TaskTemplate[];
}

export interface TemplateResponse {
  code: number;
  message: string;
  data: TaskTemplate;
}
```

- [ ] **Step 2: Create types/dag.ts** (for React Flow DAG builder)

```typescript
import type { Node, Edge } from 'reactflow';

export interface DagNodeData {
  label: string;
  type: string;
  config: Record<string, string>;
}

export type DagNode = Node<DagNodeData>;
export type DagEdge = Edge;

export interface DagDefinition {
  nodes: DagNode[];
  edges: DagEdge[];
}

export interface DagValidationResult {
  valid: boolean;
  errors: string[];
}

// Standard node types available in the palette
export interface NodeTypeInfo {
  type: string;
  label: string;
  description: string;
  defaultConfig: Record<string, string>;
}

export const NODE_TYPES: NodeTypeInfo[] = [
  { type: 'task', label: 'Task', description: 'Generic task node', defaultConfig: {} },
  { type: 'shell', label: 'Shell Command', description: 'Execute shell command', defaultConfig: { command: '' } },
  { type: 'python', label: 'Python Script', description: 'Run Python script', defaultConfig: { scriptPath: '' } },
  { type: 'java', label: 'Java Job', description: 'Run Java job', defaultConfig: { mainClass: '', args: '' } },
  { type: 'docker', label: 'Docker Container', description: 'Run Docker container', defaultConfig: { image: '', command: '' } },
];
```

- [ ] **Step 3: Create types/task.ts** (for task monitoring)

```typescript
export interface TaskExecution {
  id: number;
  orderKey: string;
  orderName: string;
  templateKey: string;
  templateVersion: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  startTime: string;
  endTime?: string;
  durationMs?: number;
  agentId?: string;
}

export interface AgentInfo {
  agentId: string;
  agentUrl: string;
  maxConcurrentTasks: number;
  maxPendingTasks: number;
  supportedExecutionKeys: string[];
  running: boolean;
  pendingTasks: number;
  runningTasks: number;
  finishedTasks: number;
}

export interface DashboardStats {
  totalTemplates: number;
  activeTemplates: number;
  totalTasksToday: number;
  completedTasks: number;
  failedTasks: number;
  successRate: number;
  onlineAgents: number;
  totalAgents: number;
}
```

- [ ] **Step 4: Commit**

```bash
git add dag-scheduler-web/types/
git commit -m "feat: add TypeScript type definitions"
```

---

### Task 4: Create API client

**Files:**
- Create: `dag-scheduler-web/lib/api-client.ts`

- [ ] **Step 1: Create api-client.ts with axios instance**

```typescript
import axios from 'axios';
import type { TaskTemplate, DashboardStats, TaskExecution, AgentInfo } from '@/types/template';
import type { DashboardStats } from '@/types/task';

const apiClient = axios.create({
  baseURL: typeof window === 'undefined' ? 'http://localhost:8080/api' : '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  // Add basic auth if needed - will use same credentials from browser
  withCredentials: true,
});

// API Response wrapper matches backend ResEntityHelper
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// Template API methods
export const templateApi = {
  listAllActive: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template').then(res => res.data),

  listAll: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template/all').then(res => res.data),

  listVersions: (templateKey: string): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get(`/v1/template/${templateKey}/versions`).then(res => res.data),

  getActive: (templateKey: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${templateKey}/active`).then(res => res.data),

  getByVersion: (templateKey: string, version: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${templateKey}/v/${version}`).then(res => res.data),

  create: (template: TaskTemplate, setActive: boolean): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.post(`/v1/template?setActive=${setActive}`, template).then(res => res.data),

  update: (templateKey: string, version: string, template: TaskTemplate): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.put(`/v1/template/${templateKey}/v/${version}`, template).then(res => res.data),

  deactivate: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.post(`/v1/template/${templateKey}/v/${version}/deactivate`).then(res => res.data),

  delete: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.delete(`/v1/template/${templateKey}/v/${version}`).then(res => res.data),

  instantiate: (
    templateKey: string,
    orderKey: string,
    orderName: string,
    params: Record<string, string>
  ): Promise<ApiResponse<any>> =>
    apiClient.post(`/v1/template/${templateKey}/instantiate?orderKey=${orderKey}&orderName=${orderName}`, params)
      .then(res => res.data),
};

// Stats API methods
export const statsApi = {
  getDashboardStats: (): Promise<ApiResponse<DashboardStats>> =>
    apiClient.get('/v1/stats/dashboard').then(res => res.data),
};

// Task API methods
export const taskApi = {
  listExecutions: (): Promise<ApiResponse<TaskExecution[]>> =>
    apiClient.get('/v1/tasks').then(res => res.data),
};

// Agent API methods
export const agentApi = {
  listAgents: (): Promise<ApiResponse<AgentInfo[]>> =>
    apiClient.get('/v1/agent/all').then(res => res.data),
};

export default apiClient;
```

- [ ] **Step 2: Fix import (remove duplicate DashboardStats import)**

```typescript
// Correct version after edit:
import axios from 'axios';
import type { TaskTemplate } from '@/types/template';
import type { DashboardStats, TaskExecution, AgentInfo } from '@/types/task';
import type { ApiResponse } from './types';

const apiClient = axios.create({
  baseURL: typeof window === 'undefined' ? 'http://localhost:8080/api' : '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// Template API methods
export const templateApi = {
  listAllActive: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template').then(res => res.data),

  listAll: (): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get('/v1/template/all').then(res => res.data),

  listVersions: (templateKey: string): Promise<ApiResponse<TaskTemplate[]>> =>
    apiClient.get(`/v1/template/${templateKey}/versions`).then(res => res.data),

  getActive: (templateKey: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${templateKey}/active`).then(res => res.data),

  getByVersion: (templateKey: string, version: string): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.get(`/v1/template/${templateKey}/v/${version}`).then(res => res.data),

  create: (template: TaskTemplate, setActive: boolean): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.post(`/v1/template?setActive=${setActive}`, template).then(res => res.data),

  update: (templateKey: string, version: string, template: TaskTemplate): Promise<ApiResponse<TaskTemplate>> =>
    apiClient.put(`/v1/template/${templateKey}/v/${version}`, template).then(res => res.data),

  deactivate: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.post(`/v1/template/${templateKey}/v/${version}/deactivate`).then(res => res.data),

  delete: (templateKey: string, version: string): Promise<ApiResponse<null>> =>
    apiClient.delete(`/v1/template/${templateKey}/v/${version}`).then(res => res.data),

  instantiate: (
    templateKey: string,
    orderKey: string,
    orderName: string,
    params: Record<string, string>
  ): Promise<ApiResponse<any>> =>
    apiClient.post(`/v1/template/${templateKey}/instantiate?orderKey=${orderKey}&orderName=${orderName}`, params)
      .then(res => res.data),
};

// Stats API methods
export const statsApi = {
  getDashboardStats: (): Promise<ApiResponse<DashboardStats>> =>
    apiClient.get('/v1/stats/dashboard').then(res => res.data),
};

// Task API methods
export const taskApi = {
  listExecutions: (): Promise<ApiResponse<TaskExecution[]>> =>
    apiClient.get('/v1/tasks').then(res => res.data),
};

// Agent API methods
export const agentApi = {
  listAgents: (): Promise<ApiResponse<AgentInfo[]>> =>
    apiClient.get('/v1/agent/all').then(res => res.data),
};

export default apiClient;
```

- [ ] **Step 3: Create lib/types.ts**

```typescript
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
```

- [ ] **Step 4: Fix import again**

Update api-client.ts to import `ApiResponse` from `./types.ts`

- [ ] **Step 5: Commit**

```bash
git add dag-scheduler-web/lib/
git commit -m "feat: add API client for backend integration"
```

---

### Task 5: Create common layout components (Sidebar, Header)

**Files:**
- Create: `dag-scheduler-web/components/common/Sidebar.tsx`
- Create: `dag-scheduler-web/components/common/Header.tsx`
- Create: base shadcn components (button, card, etc.)

*Note: we need the following base components from shadcn:*
- button
- card
- input
- dialog
- table
- badge

- [ ] **Step 1: Create components/ui/button.tsx**

```typescript
import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive:
          "bg-destructive text-destructive-foreground hover:bg-destructive/90",
        outline:
          "border border-input bg-background hover:bg-accent hover:text-accent-foreground",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost: "hover:bg-accent hover:text-accent-foreground",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 rounded-md px-3",
        lg: "h-11 rounded-md px-8",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

export function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: ButtonProps) {
  const Comp = asChild ? Slot : "button";
  return (
    <Comp
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  );
}
```

- [ ] **Step 2: Create components/ui/card.tsx**

```typescript
import * as React from "react";
import { cn } from "@/lib/utils";

const Card = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "rounded-lg border bg-card text-card-foreground shadow-sm",
      className
    )}
    {...props}
  />
));
Card.displayName = "Card";

const CardHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex flex-col space-y-1.5 p-6", className)}
    {...props}
  />
));
CardHeader.displayName = "CardHeader";

const CardTitle = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cn(
      "text-2xl font-semibold leading-none tracking-tight",
      className
    )}
    {...props}
  />
));
CardTitle.displayName = "CardTitle";

const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn("text-sm text-muted-foreground", className)}
    {...props}
  />
));
CardDescription.displayName = "CardDescription";

const CardContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn("p-6 pt-0", className)} {...props} />
));
CardContent.displayName = "CardContent";

const CardFooter = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex items-center p-6 pt-0", className)}
    {...props}
  />
));
CardFooter.displayName = "CardFooter";

export { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter };
```

- [ ] **Step 3: Create components/ui/badge.tsx**

```typescript
import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground hover:bg-primary/90",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        destructive:
          "bg-destructive text-destructive-foreground hover:bg-destructive/80",
        outline: "text-foreground",
        success:
          "bg-green-100 text-green-800 border-green-200 dark:bg-green-900 dark:text-green-100",
        warning:
          "bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900 dark:text-yellow-100",
        danger:
          "bg-red-100 text-red-800 border-red-200 dark:bg-red-900 dark:text-red-100",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}
```

- [ ] **Step 4: Create components/ui/input.tsx**

```typescript
import * as React from "react";
import { cn } from "@/lib/utils";

const Input = React.forwardRef<HTMLInputElement, React.ComponentProps<"input">>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-base ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
          className
        )}
        ref={ref}
        {...props}
      />
    );
  }
);
Input.displayName = "Input";

export { Input };
```

- [ ] **Step 5: Create components/ui/table.tsx**

```typescript
import * as React from "react";
import { cn } from "@/lib/utils";

const Table = React.forwardRef<
  HTMLTableElement,
  React.HTMLAttributes<HTMLTableElement>
>(({ className, ...props }, ref) => (
  <div className="relative w-full overflow-auto">
    <table
      ref={ref}
      className={cn("w-full caption-bottom text-sm", className)}
      {...props}
    />
  </div>
));
Table.displayName = "Table";

const TableHeader = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <thead ref={ref} className={cn("[&_tr]:border-b", className)} {...props} />
));
TableHeader.displayName = "TableHeader";

const TableBody = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <tbody
    ref={ref}
    className={cn("[&_tr:last-child]:border-0", className)}
    {...props}
  />
));
TableBody.displayName = "TableBody";

const TableRow = React.forwardRef<
  HTMLTableRowElement,
  React.HTMLAttributes<HTMLTableRowElement>
>(({ className, ...props }, ref) => (
  <tr
    ref={ref}
    className={cn(
      "border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted",
      className
    )}
    {...props}
  />
));
TableRow.displayName = "TableRow";

const TableHead = React.forwardRef<
  HTMLTableCellElement,
  React.ThHTMLAttributes<HTMLTableCellElement>
>(({ className, ...props }, ref) => (
  <th
    ref={ref}
    className={cn(
      "h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0",
      className
    )}
    {...props}
  />
));
TableHead.displayName = "TableHead";

const TableCell = React.forwardRef<
  HTMLTableCellElement,
  React.TdHTMLAttributes<HTMLTableCellElement>
>(({ className, ...props }, ref) => (
  <td
    ref={ref}
    className={cn("p-4 align-middle [&:has([role=checkbox])]:pr-0", className)}
    {...props}
  />
));
TableCell.displayName = "TableCell";

export { Table, TableHeader, TableBody, TableRow, TableHead, TableCell };
```

- [ ] **Step 6: Create components/common/Sidebar.tsx**

```typescript
'use client';

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, FileText, GitBranch, Activity } from "lucide-react";
import { cn } from "@/lib/utils";

const menuItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/templates", label: "Templates", icon: FileText },
  { href: "/builder/new", label: "DAG Builder", icon: GitBranch },
  { href: "/monitoring", label: "Monitoring", icon: Activity },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed inset-y-0 left-0 z-10 w-64 border-r bg-card">
      <div className="flex h-16 items-center border-b px-4">
        <h1 className="text-xl font-bold">DAG Scheduler</h1>
      </div>
      <nav className="space-y-1 p-4">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href ||
            (item.href !== "/" && pathname.startsWith(item.href));
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
              )}
            >
              <Icon className="h-5 w-5" />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
```

- [ ] **Step 7: Create components/common/Header.tsx**

```typescript
'use client';

export function Header() {
  return (
    <header className="h-16 border-b flex items-center justify-end px-4 bg-card">
      <div className="flex items-center space-x-4">
        <span className="text-sm text-muted-foreground">
          DAG Task Scheduler UI
        </span>
      </div>
    </header>
  );
}
```

- [ ] **Step 8: Commit**

```bash
git add dag-scheduler-web/components/
git commit -m "feat: add common layout components and base shadcn components"
```

---

### Task 6: Create Template Management page

**Files:**
- Create: `dag-scheduler-web/app/templates/page.tsx`
- Create: `dag-scheduler-web/components/template-list/TemplateTable.tsx`

- [ ] **Step 1: Create components/template-list/TemplateTable.tsx**

```typescript
'use client';

import { useQuery } from "@tanstack/react-query";
import { Plus, Trash2, Play, Eye } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { templateApi } from "@/lib/api-client";
import type { TaskTemplate } from "@/types/template";
import { cn } from "@/lib/utils";

export function TemplateTable() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["templates"],
    queryFn: () => templateApi.listAll(),
  });

  if (isLoading) {
    return <div className="p-4 text-muted-foreground">Loading templates...</div>;
  }

  if (error) {
    return <div className="p-4 text-destructive">Error loading templates</div>;
  }

  const templates: TaskTemplate[] = data?.data ?? [];

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Templates</CardTitle>
        <Link href="/builder/new">
          <Button size="sm">
            <Plus className="mr-2 h-4 w-4" />
            New Template
          </Button>
        </Link>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Key</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Version</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Description</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {templates.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  No templates found. Create your first template with the DAG builder.
                </TableCell>
              </TableRow>
            ) : (
              templates.map((template) => (
                <TableRow key={`${template.templateKey}-${template.version}`}>
                  <TableCell className="font-medium">{template.templateKey}</TableCell>
                  <TableCell>{template.templateName}</TableCell>
                  <TableCell>{template.version}</TableCell>
                  <TableCell>
                    <Badge variant={template.active ? "success" : "outline"}
                      className={cn(
                        template.active ? "bg-green-100 text-green-800" : "text-gray-500"
                      )}
                    >
                      {template.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell className="max-w-md truncate">
                    {template.description}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Link href={`/builder/${template.templateKey}/${template.version}`}>
                        <Button size="sm" variant="outline">
                          <Eye className="h-4 w-4" />
                        </Button>
                      </Link>
                      <Button size="sm" variant="outline" className="text-destructive">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: Create app/templates/page.tsx**

```typescript
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
```

- [ ] **Step 3: Commit**

```bash
git add dag-scheduler-web/app/templates dag-scheduler-web/components/template-list
git commit -m "feat: add template management page"
```

---

### Task 7: Create Dashboard Statistics component

**Files:**
- Create: `dag-scheduler-web/components/monitoring/StatsDashboard.tsx`

- [ ] **Step 1: Create StatsDashboard.tsx**

```typescript
'use client';

import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { statsApi } from "@/lib/api-client";
import type { DashboardStats } from "@/types/task";
import { FileText, CheckCircle, XCircle, Server } from "lucide-react";

function StatCard({
  title,
  value,
  icon: Icon,
  description,
}: {
  title: string;
  value: string | number;
  icon: any;
  description?: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {description && (
          <p className="text-xs text-muted-foreground">{description}</p>
        )}
      </CardContent>
    </Card>
  );
}

export function StatsDashboard() {
  const { data } = useQuery({
    queryKey: ["dashboardStats"],
    queryFn: () => statsApi.getDashboardStats(),
  });

  const stats: DashboardStats = data?.data ?? {
    totalTemplates: 0,
    activeTemplates: 0,
    totalTasksToday: 0,
    completedTasks: 0,
    failedTasks: 0,
    successRate: 0,
    onlineAgents: 0,
    totalAgents: 0,
  };

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <StatCard
        title="Total Templates"
        value={stats.totalTemplates}
        icon={FileText}
        description={`${stats.activeTemplates} active`}
      />
      <StatCard
        title="Tasks Today"
        value={stats.totalTasksToday}
        icon={CheckCircle}
        description={`${stats.completedTasks} completed`}
      />
      <StatCard
        title="Success Rate"
        value={`${stats.successRate.toFixed(1)}%`}
        icon={CheckCircle}
      />
      <StatCard
        title="Agents Online"
        value={`${stats.onlineAgents}/${stats.totalAgents}`}
        icon={Server}
      />
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add dag-scheduler-web/components/monitoring
git commit -m "feat: add dashboard statistics component"
```

---

### Task 8: Create Task Monitoring page

**Files:**
- Create: `dag-scheduler-web/app/monitoring/page.tsx`
- Create: `dag-scheduler-web/components/monitoring/TaskList.tsx`

- [ ] **Step 1: Create components/monitoring/TaskList.tsx**

```typescript
'use client';

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { taskApi } from "@/lib/api-client";
import type { TaskExecution } from "@/types/task";
import { cn } from "@/lib/utils";

const statusVariants: Record<TaskExecution["status"], any> = {
  PENDING: { label: "Pending", variant: "outline" },
  RUNNING: { label: "Running", variant: "warning" },
  COMPLETED: { label: "Completed", variant: "success" },
  FAILED: { label: "Failed", variant: "danger" },
  CANCELLED: { label: "Cancelled", variant: "outline" },
};

export function TaskList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["tasks"],
    queryFn: () => taskApi.listExecutions(),
  });

  if (isLoading) {
    return <div className="p-4 text-muted-foreground">Loading tasks...</div>;
  }

  if (error) {
    return <div className="p-4 text-destructive">Error loading tasks</div>;
  }

  const tasks: TaskExecution[] = data?.data ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Executions</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Template</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Started</TableHead>
              <TableHead>Duration</TableHead>
              <TableHead>Agent</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tasks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  No task executions found.
                </TableCell>
              </TableRow>
            ) : (
              tasks.map((task) => {
                const statusInfo = statusVariants[task.status];
                return (
                  <TableRow key={task.id}>
                    <TableCell>{task.id}</TableCell>
                    <TableCell className="font-medium">{task.orderName}</TableCell>
                    <TableCell>
                      {task.templateKey} v{task.templateVersion}
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusInfo.variant}>
                        {statusInfo.label}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {new Date(task.startTime).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      {task.durationMs ? `${(task.durationMs / 1000).toFixed(1)}s` : "-"}
                    </TableCell>
                    <TableCell>{task.agentId ?? "-"}</TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: Create app/monitoring/page.tsx**

```typescript
import { TaskList } from "@/components/monitoring/TaskList";

export default function MonitoringPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Monitoring</h1>
        <p className="text-muted-foreground">
          Monitor task execution status and view recent runs.
        </p>
      </div>
      <TaskList />
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add dag-scheduler-web/app/monitoring dag-scheduler-web/components/monitoring
git commit -m "feat: add task monitoring page"
```

---

### Task 9: Create DAG Builder components

**Files:**
- Create: `dag-scheduler-web/app/builder/[id]/page.tsx`
- Create: `dag-scheduler-web/components/dag-builder/DagCanvas.tsx`
- Create: `dag-scheduler-web/components/dag-builder/NodePanel.tsx`
- Create: `dag-scheduler-web/components/dag-builder/NodeConfigPanel.tsx`
- Create: `dag-scheduler-web/components/dag-builder/Toolbar.tsx`

- [ ] **Step 1: Create NodePanel.tsx (sidebar with available node types)**

```typescript
'use client';

import { NODE_TYPES } from "@/types/dag";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function NodePanel() {
  const onDragStart = (event: React.DragEvent, nodeType: string) => {
    event.dataTransfer.setData("application/reactflow/type", nodeType);
    event.dataTransfer.effectAllowed = "move";
  };

  return (
    <Card className="w-64 h-full overflow-y-auto">
      <CardHeader>
        <CardTitle>Node Types</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {NODE_TYPES.map((nodeType) => (
          <div
            key={nodeType.type}
            className={cn(
              "border rounded-md p-3 cursor-grab bg-background hover:bg-accent transition-colors",
              "active:cursor-grabbing"
            )}
            onDragStart={(e) => onDragStart(e, nodeType.type)}
            draggable
          >
            <div className="font-medium text-sm">{nodeType.label}</div>
            <div className="text-xs text-muted-foreground mt-1">
              {nodeType.description}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: Create Toolbar.tsx (save/validate/zoom controls)**

```typescript
'use client';

import { Save, Trash, ZoomIn, ZoomOut, RotateCcw, CheckCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import type { DagValidationResult } from "@/types/dag";

interface ToolbarProps {
  isValid: boolean;
  validationResult: DagValidationResult | null;
  onSave: () => void;
  onClear: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitView: () => void;
  onValidate: () => void;
  saving: boolean;
}

export function Toolbar({
  isValid,
  validationResult,
  onSave,
  onClear,
  onZoomIn,
  onZoomOut,
  onFitView,
  onValidate,
  saving,
}: ToolbarProps) {
  const errorCount = validationResult?.errors.length ?? 0;

  return (
    <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 bg-background border rounded-lg shadow-lg px-4 py-2 flex items-center gap-2">
      {validationResult && (
        <Badge variant={isValid ? "success" : "danger"} className="gap-1">
          <CheckCircle className="h-3 w-3" />
          {isValid ? "Valid DAG" : `${errorCount} error${errorCount > 1 ? "s" : ""}`}
        </Badge>
      )}
      <Button size="sm" variant="outline" onClick={onZoomIn}>
        <ZoomIn className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onZoomOut}>
        <ZoomOut className="h-4 w-4" />
      </Button>
      <Button size="sm" variant="outline" onClick={onFitView}>
        <RotateCcw className="h-4 w-4" />
      </Button>
      <div className="w-px h-6 bg-border mx-1" />
      <Button size="sm" variant="outline" onClick={onClear}>
        <Trash className="h-4 w-4 mr-1" />
        Clear
      </Button>
      <Button size="sm" onClick={onSave} disabled={!isValid || saving}>
        <Save className="h-4 w-4 mr-1" />
        {saving ? "Saving..." : "Save"}
      </Button>
    </div>
  );
}
```

- [ ] **Step 3: Create NodeConfigPanel.tsx (configuration for selected node)**

```typescript
'use client';

import { useState, useEffect } from "react";
import type { DagNode } from "@/types/dag";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Trash2 } from "lucide-react";

interface NodeConfigPanelProps {
  node: DagNode | null;
  onChange: (id: string, data: Partial<DagNode["data"]>) => void;
  onDelete: (id: string) => void;
}

export function NodeConfigPanel({ node, onChange, onDelete }: NodeConfigPanelProps) {
  const [label, setLabel] = useState("");
  const [config, setConfig] = useState<Record<string, string>>({});

  useEffect(() => {
    if (node) {
      setLabel(node.data.label);
      setConfig(node.data.config);
    }
  }, [node]);

  if (!node) {
    return (
      <Card className="w-72 h-full">
        <CardHeader>
          <CardTitle>Configuration</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Select a node on the canvas to configure it.
          </p>
        </CardContent>
      </Card>
    );
  }

  const handleConfigChange = (key: string, value: string) => {
    const newConfig = { ...config, [key]: value };
    setConfig(newConfig);
    onChange(node.id, { config: newConfig });
  };

  const handleLabelChange = (value: string) => {
    setLabel(value);
    onChange(node.id, { label: value });
  };

  const handleDelete = () => {
    onDelete(node.id);
  };

  return (
    <Card className="w-72 h-full overflow-y-auto">
      <CardHeader>
        <CardTitle>Node Configuration</CardTitle>
        <p className="text-xs text-muted-foreground">
          ID: {node.id} ({node.data.type})
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="node-label">Node Label</Label>
          <Input
            id="node-label"
            value={label}
            onChange={(e) => handleLabelChange(e.target.value)}
            placeholder="Display name"
          />
        </div>

        {Object.entries(config).map(([key, value]) => (
          <div key={key} className="space-y-2">
            <Label htmlFor={`config-${key}`}>{key}</Label>
            <Input
              id={`config-${key}`}
              value={value}
              onChange={(e) => handleConfigChange(key, e.target.value)}
              placeholder={`Enter ${key}`}
            />
          </div>
        ))}

        {Object.keys(config).length === 0 && (
          <p className="text-xs text-muted-foreground italic">
            No configuration properties defined.
          </p>
        )}

        <div className="pt-4">
          <Button
            variant="destructive"
            size="sm"
            className="w-full"
            onClick={handleDelete}
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Delete Node
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 4: Create DagCanvas.tsx (main React Flow canvas)**

```typescript
'use client';

import { useCallback, useRef, useState, useMemo } from "react";
import ReactFlow, {
  ReactFlowProvider,
  addEdge,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  MiniMap,
  type Connection,
  type Edge,
  type Node,
  Panel,
} from "reactflow";
import "reactflow/dist/style.css";

import { NodePanel } from "./NodePanel";
import { NodeConfigPanel } from "./NodeConfigPanel";
import { Toolbar } from "./Toolbar";
import type { DagNode, DagEdge, DagDefinition, DagValidationResult, DagNodeData, NodeTypeInfo } from "@/types/dag";
import { NODE_TYPES } from "@/types/dag";
import { Button } from "@/components/ui/button";

let nodeIdCounter = 0;

interface DagCanvasProps {
  initialNodes?: DagNode[];
  initialEdges?: DagEdge[];
  onSave: (definition: DagDefinition) => void;
  saving: boolean;
}

function getNextNodeId() {
  return `node_${++nodeIdCounter}`;
}

function validateDag(nodes: DagNode[], edges: DagEdge[]): DagValidationResult {
  const errors: string[] = [];

  // Check for cycles using DFS
  const adjacency = new Map<string, string[]>();
  nodes.forEach(node => adjacency.set(node.id, []));
  edges.forEach(edge => {
    adjacency.get(edge.source)?.push(edge.target);
  });

  const visited = new Set<string>();
  const recursionStack = new Set<string>();

  function hasCycle(nodeId: string): boolean {
    if (!visited.has(nodeId)) {
      visited.add(nodeId);
      recursionStack.add(nodeId);

      const neighbors = adjacency.get(nodeId) || [];
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor) && hasCycle(neighbor)) {
          return true;
        } else if (recursionStack.has(neighbor)) {
          return true;
        }
      }
    }
    recursionStack.delete(nodeId);
    return false;
  }

  for (const node of nodes) {
    if (!visited.has(node.id) && hasCycle(node.id)) {
      errors.push("DAG contains a cycle (not a valid Directed Acyclic Graph)");
      break;
    }
  }

  // Check for disconnected nodes
  if (nodes.length > 1) {
    const connectedNodes = new Set<string>();
    edges.forEach(edge => {
      connectedNodes.add(edge.source);
      connectedNodes.add(edge.target);
    });
    const disconnected = nodes.filter(n => !connectedNodes.has(n.id));
    if (disconnected.length > 0) {
      errors.push(`${disconnected.length} disconnected node(s) found`);
    }
  }

  // Check for empty graph
  if (nodes.length === 0) {
    errors.push("DAG is empty (no nodes)");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

function DagCanvasContent({ initialNodes = [], initialEdges = [], onSave, saving }: DagCanvasProps) {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [reactFlowInstance, setReactFlowInstance] = useState<any>(null);
  const [selectedNode, setSelectedNode] = useState<DagNode | null>(null);
  const [validationResult, setValidationResult] = useState<DagValidationResult | null>(null);
  const [isValid, setIsValid] = useState(false);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type = event.dataTransfer.getData("application/reactflow/type");
      if (!type) return;

      const position = reactFlowInstance.screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      const nodeTypeInfo = NODE_TYPES.find(t => t.type === type);
      const newNode: DagNode = {
        id: getNextNodeId(),
        type: "default",
        position,
        data: {
          label: nodeTypeInfo?.label || type,
          type,
          config: { ...(nodeTypeInfo?.defaultConfig || {}) },
        },
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [reactFlowInstance, setNodes]
  );

  const onNodeClick = useCallback((_: React.MouseEvent, node: DagNode) => {
    setSelectedNode(node);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
  }, []);

  const updateNodeData = useCallback(
    (nodeId: string, newData: Partial<DagNodeData>) => {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === nodeId) {
            return { ...node, data: { ...node.data, ...newData } };
          }
          return node;
        })
      );
      setSelectedNode((prev) =>
        prev?.id === nodeId ? { ...prev, data: { ...prev.data, ...newData } } : prev
      );
    },
    [setNodes]
  );

  const deleteSelectedNode = useCallback(
    (nodeId: string) => {
      setNodes((nds) => nds.filter((n) => n.id !== nodeId));
      setEdges((eds) => eds.filter((e) => e.source !== nodeId && e.target !== nodeId));
      setSelectedNode(null);
    },
    [setNodes, setEdges]
  );

  const clearCanvas = useCallback(() => {
    if (confirm("Clear all nodes and edges from canvas?")) {
      setNodes([]);
      setEdges([]);
      setSelectedNode(null);
      setValidationResult(null);
      setIsValid(false);
    }
  }, [setNodes, setEdges]);

  const validateCurrent = useCallback(() => {
    const result = validateDag(nodes, edges);
    setValidationResult(result);
    setIsValid(result.valid);
    return result;
  }, [nodes, edges]);

  const handleSave = useCallback(() => {
    const result = validateCurrent();
    if (!result.valid) {
      alert(`Validation failed:\n${result.errors.join("\n")}`);
      return;
    }
    onSave({ nodes, edges });
  }, [nodes, edges, validateCurrent, onSave]);

  const zoomIn = useCallback(() => {
    reactFlowInstance?.zoomIn();
  }, [reactFlowInstance]);

  const zoomOut = useCallback(() => {
    reactFlowInstance?.zoomOut();
  }, [reactFlowInstance]);

  const fitView = useCallback(() => {
    reactFlowInstance?.fitView({ padding: 0.2 });
  }, [reactFlowInstance]);

  const nodeTypes = useMemo(() => ({
    default: ({ data, selected }) => (
      <div className={`shadow-md rounded-md px-4 py-2 border-2 bg-background ${selected ? 'border-primary' : 'border-border'}`}>
        <div className="font-medium text-sm">{data.label}</div>
        <div className="text-xs text-muted-foreground">{data.type}</div>
      </div>
    ),
  }), []);

  return (
    <div className="flex h-full gap-4">
      <NodePanel />
      <div className="flex-1 relative border rounded-lg overflow-hidden" ref={reactFlowWrapper}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onInit={setReactFlowInstance}
          onDrop={onDrop}
          onDragOver={onDragOver}
          onNodeClick={onNodeClick}
          onPaneClick={onPaneClick}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap />
          <Toolbar
            isValid={isValid}
            validationResult={validationResult}
            onSave={handleSave}
            onClear={clearCanvas}
            onZoomIn={zoomIn}
            onZoomOut={zoomOut}
            onFitView={fitView}
            onValidate={validateCurrent}
            saving={saving}
          />
        </ReactFlow>
      </div>
      <NodeConfigPanel
        node={selectedNode}
        onChange={updateNodeData}
        onDelete={deleteSelectedNode}
      />
    </div>
  );
}

export function DagCanvas(props: DagCanvasProps) {
  return (
    <ReactFlowProvider>
      <DagCanvasContent {...props} />
    </ReactFlowProvider>
  );
}
```

- [ ] **Step 5: Create builder page app/builder/[id]/page.tsx**

```typescript
'use client';

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { DagCanvas } from "@/components/dag-builder/DagCanvas";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { templateApi } from "@/lib/api-client";
import type { DagDefinition } from "@/types/dag";
import type { TaskTemplate } from "@/types/template";
import { useQuery } from "@tanstack/react-query";

export default function DagBuilderPage() {
  const params = useParams();
  const router = useRouter();
  const [templateKey, setTemplateKey] = useState("");
  const [templateName, setTemplateName] = useState("");
  const [description, setDescription] = useState("");
  const [version, setVersion] = useState("1.0.0");
  const [saving, setSaving] = useState(false);
  const [setActive, setSetActive] = useState(true);

  const id = params?.id as string;
  const isNew = id === "new";

  // If editing existing template by key/version, we need extra path segment
  // Handle the case where id is actually templateKey/version
  useEffect(() => {
    // If URL is /builder/etl/1.0.0, Next.js gives id as "etl/1.0.0"
    if (!isNew && id.includes('/')) {
      const [templateKeyFromUrl, versionFromUrl] = id.split('/');
      // Load existing template here
      templateApi.getByVersion(templateKeyFromUrl, versionFromUrl).then(res => {
        const template = res.data;
        setTemplateKey(template.templateKey);
        setTemplateName(template.templateName);
        setDescription(template.description || "");
        setVersion(template.version);
      });
    }
  }, [id]);

  const handleSave = async (dagDefinition: DagDefinition) => {
    if (!templateKey || !templateName || !version) {
      alert("Please fill in all required fields: template key, name, version");
      return;
    }

    setSaving(true);
    try {
      const template: TaskTemplate = {
        templateKey,
        templateName,
        description,
        version,
        active: false,
        dagDefinition: JSON.stringify(dagDefinition),
        parameterSchema: "{}",
      };

      const response = await templateApi.create(template, setActive);
      if (response.code === 200 || response.code === 0) {
        alert("Template created successfully!");
        router.push("/templates");
      } else {
        alert(`Error: ${response.message}`);
      }
    } catch (error) {
      console.error(error);
      alert("Error saving template");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4 h-[calc(100vh-100px)] flex flex-col">
      <div className="flex-none">
        <h1 className="text-3xl font-bold tracking-tight">DAG Builder</h1>
        <p className="text-muted-foreground">
          Create a new workflow template by dragging nodes onto the canvas.
        </p>
      </div>
      <div className="flex gap-4 flex-none">
        <Card className="flex-1">
          <CardHeader>
            <CardTitle>Template Information</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="templateKey">Template Key</Label>
              <Input
                id="templateKey"
                placeholder="etl-data-processing"
                value={templateKey}
                onChange={(e) => setTemplateKey(e.target.value)}
                disabled={!isNew}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="templateName">Template Name</Label>
              <Input
                id="templateName"
                placeholder="ETL Data Processing"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="version">Version</Label>
              <Input
                id="version"
                placeholder="1.0.0"
                value={version}
                onChange={(e) => setVersion(e.target.value)}
              />
            </div>
            <div className="space-y-2 col-span-3">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Description of what this template does..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
              />
            </div>
          </CardContent>
        </Card>
      </div>
      <div className="flex-1 min-h-0">
        <DagCanvas onSave={handleSave} saving={saving} />
      </div>
    </div>
  );
}
```

- [ ] **Step 6: Commit**

```bash
git add dag-scheduler-web/app/builder dag-scheduler-web/components/dag-builder
git commit -m "feat: add visual DAG builder with drag-and-drop"
```

---

### Task 10: Add .env.example and final touches

**Files:**
- Create: `dag-scheduler-web/.env.example`
- Create: `dag-scheduler-web/next-env.d.ts`

- [ ] **Step 1: Create .env.example**

```
# Next.js
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

- [ ] **Step 2: Create next-env.d.ts**

```typescript
/// <reference types="next" />
/// <reference types="next/image-types/global" />

// NOTE: This file is auto-included by Next.js
declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}
```

- [ ] **Step 3: Commit**

```bash
git add dag-scheduler-web
git commit -m "docs: add .env.example and complete initial scaffold"
```

---

## Done

Project scaffold complete with all core features:
- Project setup with Next.js 15 + TypeScript
- Layout with sidebar navigation
- Template management page
- Dashboard with statistics
- Task monitoring page
- Visual DAG builder with drag-and-drop, validation, configuration
- API client connecting to existing backend
