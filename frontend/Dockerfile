# Build stage - compiles the Vite app into static assets.
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# VITE_API_BASE_URL is baked into the JS bundle at build time (Vite env
# vars are static, not runtime). It defaults to http://localhost:8080 in
# src/lib/apiClient.ts, which is correct for this compose setup: the
# BROWSER calls the Gateway directly on the host-mapped port, so it must
# never be a Docker-internal hostname like "api-gateway" (unresolvable
# outside the Docker network). Override only if the Gateway is exposed on
# a different host/port.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# Runtime stage - serves the static build with nginx.
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
