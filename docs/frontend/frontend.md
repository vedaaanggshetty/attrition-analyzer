# Frontend

Kept intentionally short — see the backend service docs for API details. This answers: what is the frontend connected to, what routes exist, and what happens when a user does the main things.

## Structure

React 19 + TypeScript + Vite 8, Tailwind CSS 4. Key directories under `src/`:

- `pages/` — one component per route (Dashboard, EmployeeList, EmployeeDetail, Notifications, Profile, Login, Register, Landing, About, NotFound)
- `context/AuthContext.tsx` — the single source of truth for auth state
- `components/routing/ProtectedRoute.tsx` — route guard
- `lib/` — `apiClient.ts` (Gateway fetch wrapper), `authApi.ts`, `employeeApi.ts`, `notificationApi.ts` (typed wrappers per backend service)
- `components/layout/` — `PublicLayout` (landing/about, has the marketing `Navbar`), `AppLayout` (authenticated shell with the sidebar), `AuthLayout` (login/register)

## Routes

```
/                    PublicLayout → Landing        (public)
/about               PublicLayout → About          (public)
/login               Login                          (public)
/register            Register                       (public)
/dashboard           ProtectedRoute → AppLayout → Dashboard
/employees           ProtectedRoute → AppLayout → EmployeeList
/employees/:id       ProtectedRoute → AppLayout → EmployeeDetail
/notifications       ProtectedRoute → AppLayout → Notifications
/profile             ProtectedRoute → AppLayout → Profile
*                    NotFound
```

## Authentication state

`AuthContext` (`src/context/AuthContext.tsx`) holds the current user, derived from a JWT stored in `localStorage`:

- On login, the token is stored (`setToken`) and the user object is decoded straight from its claims (`userFromToken` — `sub`, `email`, `role`); `fullName` is fetched separately from `GET /users/me` once, and shared by every component that reads `useAuth().user.fullName` (the sidebar, the Dashboard greeting).
- A client-side timer (`scheduleExpiry`) auto-logs-out at the JWT's `exp` claim; any `401` from a protected API call also ends the session immediately (`SESSION_EXPIRED_EVENT`), not just expiry.
- `ProtectedRoute` (`src/components/routing/ProtectedRoute.tsx`) wraps every authenticated route: if `isAuthenticated` is false, it redirects to `/login` (preserving the original destination in route state) instead of rendering the page.

## Guest vs. HR access

A **Guest** is simply someone with no valid token. There is no separate "Guest" route tree or Guest-specific page — `/dashboard`, `/employees`, `/employees/:id`, `/notifications`, and `/profile` are all behind `ProtectedRoute`, so a guest clicking any of them (via the Navbar's Analytics/Employees links, or a direct URL) is redirected to `/login`. Only `/`, `/about`, `/login`, and `/register` render without a token.

The one guest-visible **business data** the app shows is the landing page's analytics preview section, which calls the Gateway's `GET /employees/analysis/**` endpoints directly with `authenticated: false` (see `employeeApi.ts`) — those are the only backend routes the Gateway permits without a JWT (see [api-gateway.md](../backend/api-gateway.md)). This is a marketing teaser on the public landing page, separate from the authenticated Dashboard/Attrition Explorer.

## API connection

Every request goes through `apiRequest` in `src/lib/apiClient.ts`, which:

- Targets `API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"` — **the API Gateway's host-mapped port**, never an individual service's port, and never a Docker-internal hostname (see [docker.md](../backend/docker.md) for why that distinction matters for the containerized build).
- Attaches `Authorization: Bearer <token>` automatically when a token is stored and the call doesn't opt out (`authenticated: false` — used only by the public attrition-analysis calls above).
- Normalizes every backend error response (`{message: string, ...}`) into a single `ApiError` type that pages display directly.

## How employee data reaches the UI

`Employees` page → `getAllEmployees()` / `getEmployeeById()` (`employeeApi.ts`) → `GET /employees` or `GET /employees/{id}` through the Gateway → employee-service → external Survey API. Filtering by attrition/department/job role/etc. on the Employee List page happens **client-side**, over the full employee list already fetched — there's no separate filtered-search request per filter change (the single-field `?property=&value=` search endpoint exists on the backend but isn't what the list page's filter UI uses).

## How notifications work from the frontend

- Flagging an employee (Employee Detail page) calls `flagEmployee(id, comment)` → `POST /employees/{id}/flag` → returns immediately once employee-service has published the Kafka event (see [kafka.md](../backend/kafka.md)) — the frontend never talks to Kafka directly or waits for the notification to actually be created.
- The Notifications page calls `getMyNotifications()` → `GET /notifications`, and delete calls `deleteNotification(id)` → `DELETE /notifications/{id}`.
- `createNotification()` (a direct `POST /notifications` wrapper) exists in `notificationApi.ts` but isn't called from any page — the only UI path to a new notification is flagging an employee.

## Docker

The frontend runs as its own container (`frontend/Dockerfile`, multi-stage: `npm run build` → static files served by nginx), separate from every backend service. See [docker.md](../backend/docker.md) for the full build/runtime breakdown and why `VITE_API_BASE_URL` must resolve from the **browser**, not from inside the container.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | The Gateway address the browser calls. Baked into the build at build time (Vite env vars aren't runtime-configurable) — set it as a Docker build `ARG`/`ENV` or in `frontend/.env.local` for local dev, only if the Gateway isn't at the default |

## Running it

```bash
cd frontend
npm install
npm run dev      # Vite dev server, default http://localhost:5173
npm run build    # tsc -b && vite build → dist/
npm run lint     # oxlint
```
