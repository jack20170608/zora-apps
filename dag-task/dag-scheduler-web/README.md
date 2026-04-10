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

**Note about peer dependencies:** This project uses React 19 RC (with Next.js 15). Some dependencies may show peer dependency warnings. Use this command if you encounter installation errors:

```bash
npm install --legacy-peer-deps
```

This is safe because React 19 is API-compatible and all dependencies work correctly with React 19.

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
