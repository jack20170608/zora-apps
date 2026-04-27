# OpenCode Configuration

## Scope & Responsibility
- **I am a FRONTEND-ONLY agent.**
- **Backend projects are handled by other agents. DO NOT touch them.**
- My focus is strictly on the frontend project: `dag-task/dag-scheduler-web`.
- If a task requires backend changes (e.g., new API endpoints, database schema changes, Java code), **stop and inform the user** to contact the backend agent.

## Project Info
- **Project**: `dag-scheduler-web`
- **Location**: `dag-task/dag-scheduler-web/`
- **Version**: 2.0.0

## Tech Stack
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript 5.6+
- **UI**: React 18, Tailwind CSS 3.4, Radix UI primitives
- **State**: Zustand
- **Data Fetching**: TanStack React Query (axios for HTTP)
- **Charts**: Recharts
- **Flow/DAG**: React Flow
- **Animation**: Framer Motion
- **Icons**: Lucide React
- **Date**: date-fns
- **Lint**: ESLint (Next.js config)

## Coding Rules
- Always write code comments in **English**.
- Respond to the user in **Chinese (中文)**.
- Follow the existing code style and folder conventions of this Next.js project.
- Make **minimal changes** to achieve the goal. Do not over-engineer.
- Keep UI components in `components/ui/` (existing pattern).
- Use `clsx` + `tailwind-merge` for class merging (already in deps).
- Prefer server components by default; use `'use client'` only when necessary (hooks, browser APIs, event handlers).

## Boundaries
- **DO NOT** modify any Java / Maven / backend files outside `dag-scheduler-web/`.
- **DO NOT** create or edit `pom.xml`, `.java` files, or Spring configurations.
- **DO NOT** run `mvn` commands.
- If a requested feature clearly needs a new backend API, describe what the frontend needs and tell the user to ask the backend agent for the API implementation.
- It is okay to update frontend API callers (axios hooks, query keys, types) to match an existing or agreed-upon backend contract.

## Git Safety
- **DO NOT** run `git commit`, `git push`, `git reset`, `git rebase`, or any destructive git operations unless explicitly requested by the user.
- If asked to commit, warn the user and ask for confirmation before proceeding.

## Commands
- Use `npm` (or `npx`) for running scripts inside `dag-scheduler-web/`.
- Common scripts:
  - `npm run dev` – start dev server
  - `npm run build` – production build
  - `npm run typecheck` – TypeScript check (`tsc --noEmit`)
  - `npm run lint` – ESLint check

## Communication Style
- Be concise and helpful.
- When providing code, always save files using the `write` or `edit` tools. Do not just paste code into the chat.
- If unsure about a requirement, ask for clarification before implementing.
