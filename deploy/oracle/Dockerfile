# Build on the destination architecture (A1 = linux/arm64); do not copy host node_modules.
FROM node:22-bookworm-slim AS build
RUN apt-get update && apt-get install -y --no-install-recommends python3 make g++ ca-certificates && rm -rf /var/lib/apt/lists/*
WORKDIR /app/api
COPY api/package.json api/package-lock.json ./
RUN npm ci --include=dev
COPY api/tsconfig.json ./
COPY api/src ./src
RUN npm run build && npm prune --omit=dev

FROM node:22-bookworm-slim
ENV NODE_ENV=production PORT=10000
WORKDIR /app
COPY --from=build --chown=node:node /app/api/package.json /app/api/package-lock.json ./api/
COPY --from=build --chown=node:node /app/api/node_modules ./api/node_modules
COPY --from=build --chown=node:node /app/api/dist ./api/dist
COPY --chown=node:node pwa/ ./pwa/
COPY --chown=node:node admin/ ./admin/
USER node
EXPOSE 10000
HEALTHCHECK --interval=30s --timeout=6s --start-period=120s --retries=3 CMD node -e "fetch('http://127.0.0.1:10000/health',{signal:AbortSignal.timeout(4000)}).then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node","api/dist/server.js"]
