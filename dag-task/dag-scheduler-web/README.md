# DAG Scheduler Web

Enterprise-grade DAG-based task scheduling and workflow management platform.

## Overview

DAG Scheduler Web is a modern, professional workflow management interface designed for building, monitoring, and managing complex task workflows using Directed Acyclic Graphs (DAGs).

## Features

### Core Features
- **Visual DAG Builder** - Drag-and-drop workflow designer with real-time validation
- **Workflow Management** - Create, version, and manage workflow templates
- **Execution Monitoring** - Real-time tracking of task executions with live logs
- **Agent Cluster Management** - Monitor and manage distributed execution agents
- **System Settings** - Configure global parameters and notification channels

### Advanced Features
- **Command Palette** - Global search with keyboard shortcuts (Cmd/Ctrl + K)
- **Gantt Charts** - Visual timeline of task execution
- **Real-time Logs** - Live log streaming with syntax highlighting
- **Undo/Redo** - Full history stack in DAG builder
- **Dark Mode** - Professional dark theme optimized for long sessions
- **Responsive Design** - Adapts to different screen sizes

## Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript 5.6
- **Styling**: Tailwind CSS 3.4
- **State Management**: Zustand
- **Data Fetching**: TanStack Query (React Query)
- **Visualization**: React Flow (DAG canvas), Recharts (charts)
- **Animation**: Framer Motion
- **UI Components**: Custom shadcn/ui-inspired components

## Getting Started

### Prerequisites
- Node.js 20+
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

### Development

The development server will start at `http://localhost:3000`.

### Environment Variables

Create a `.env.local` file:

```env
NEXT_PUBLIC_API_URL=http://localhost:8081/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8081/ws
```

## Project Structure

```
dag-scheduler-web/
├── app/                    # Next.js App Router pages
│   ├── overview/          # Dashboard overview
│   ├── workflows/         # Workflow management
│   ├── studio/            # DAG builder
│   ├── executions/        # Execution monitoring
│   ├── agents/            # Agent management
│   └── settings/          # System settings
├── components/
│   ├── ui/                # Base UI components
│   ├── layout/            # Layout components
│   │   ├── AppLayout.tsx
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   └── CommandPalette.tsx
│   ├── overview/          # Overview components
│   ├── workflows/         # Workflow components
│   ├── studio/            # Studio components
│   └── executions/        # Execution components
├── lib/
│   ├── api/               # API clients
│   ├── stores/            # Zustand stores
│   └── utils.ts           # Utility functions
├── types/                 # TypeScript types
└── docs/                  # Design documents
```

## Design Philosophy

### References
- **Apache Airflow 2.x** - Industry standard workflow management
- **Temporal Web UI** - Modern execution tracing
- **n8n** - Intuitive node editor UX
- **Dagster** - Data-aware workflow design
- **GitHub Actions** - Clean pipeline visualization

### Principles
1. **Immediate Feedback** - Every action provides instant visual response
2. **Context Preservation** - Users always know their location in the system
3. **Progressive Disclosure** - Complex features unfold on demand
4. **Visualization First** - Graphs over tables, animations over static
5. **Keyboard Friendly** - Full keyboard navigation and shortcuts

## Keyboard Shortcuts

### Global
- `Cmd/Ctrl + K` - Open Command Palette
- `Cmd/Ctrl + /` - Keyboard shortcuts help

### Studio (DAG Builder)
- `Cmd/Ctrl + S` - Save workflow
- `Cmd/Ctrl + Z` - Undo
- `Cmd/Ctrl + Shift + Z` - Redo
- `Delete` - Delete selected node
- `Space + Drag` - Pan canvas
- `Scroll` - Zoom canvas

## API Integration

The frontend expects a REST API at the configured base URL with the following endpoints:

### Workflows
- `GET /api/v1/workflows` - List workflows
- `POST /api/v1/workflows` - Create workflow
- `GET /api/v1/workflows/:id` - Get workflow detail
- `PUT /api/v1/workflows/:id` - Update workflow
- `POST /api/v1/workflows/:id/execute` - Trigger execution

### Executions
- `GET /api/v1/executions` - List executions
- `GET /api/v1/executions/:id` - Get execution detail
- `GET /api/v1/executions/:id/logs` - Stream logs
- `POST /api/v1/executions/:id/retry` - Retry execution
- `POST /api/v1/executions/:id/cancel` - Cancel execution

### Agents
- `GET /api/v1/agents` - List agents
- `GET /api/v1/agents/:id/metrics` - Agent metrics

### Stats
- `GET /api/v1/stats/overview` - Dashboard statistics
- `GET /api/v1/stats/trends` - Historical trends

## WebSocket Events

- `ws://.../executions/:id/stream` - Real-time execution updates
- `ws://.../agents/stream` - Agent heartbeat stream

## Customization

### Themes
The application supports three theme modes:
- Light
- Dark (default)
- System

### Node Types
The DAG builder supports the following node types:
- Task (general purpose)
- Shell (command execution)
- Python (script execution)
- Java (application)
- Docker (container)
- Database (SQL operations)
- Decision (conditional branch)
- Fork (parallel execution)
- Join (synchronization)

## Development Guidelines

### Adding New Pages
1. Create a new directory under `app/`
2. Add a `page.tsx` file with the page component
3. Add navigation item in `components/layout/AppLayout.tsx`

### Adding New Components
1. Create component file in appropriate `components/` subdirectory
2. Export component as default or named export
3. Update imports where needed

### State Management
- Use Zustand for global UI state
- Use React Query for server state
- Use local state for component-specific state

## License

MIT License

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## Support

For issues and feature requests, please use the GitHub issue tracker.
