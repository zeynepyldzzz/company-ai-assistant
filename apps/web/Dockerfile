# syntax=docker/dockerfile:1
#
# DIKKAT: Bu Dockerfile'in build context'i REPO KOKU olmalidir, apps/web degil:
#   docker build -t assistant-web:local -f apps/web/Dockerfile .
#
# Sebep: apps/web, @company/shared paketine "workspace:^" ile bagli ve o paket
# derlenmis cikti degil HAM TypeScript sunuyor (packages/shared/package.json ->
# main: "src/index.ts"). Yani web'i derlemek icin packages/shared kaynagi,
# pnpm-workspace.yaml ve kok lockfile ayni context'te bulunmak zorunda.

# ---------- 1. asama: derleme ----------
# alpine DEGIL slim: alpine musl libc kullaniyor, Vite/Rollup ise platforma ozgu
# native ikili dosyalar cekiyor. Lockfile Windows'ta uretildigi icin musl varyanti
# eksik kalabilir ve build anlasilmaz sekilde kirilir. Debian tabanli slim bu
# riski tamamen ortadan kaldirir.
FROM node:22-slim AS builder
WORKDIR /repo

# CI ile ayni pnpm surumu (.github/workflows/ci.yml -> pnpm/action-setup, version: 9)
RUN corepack enable && corepack prepare pnpm@9 --activate

# Once YALNIZCA manifest dosyalari: bagimliliklar nadiren, kaynak kod surekli
# degisir. Bu sirayla, kodda yapilan degisiklik `pnpm install` katmanini
# cache'ten getirir.
COPY pnpm-workspace.yaml pnpm-lock.yaml package.json ./
COPY packages/shared/package.json packages/shared/
COPY apps/web/package.json apps/web/
RUN pnpm install --frozen-lockfile

# Kaynak kod
COPY packages/shared/ packages/shared/
COPY apps/web/ apps/web/

# `tsc -b && vite build` -> apps/web/dist
RUN pnpm --filter web build

# ---------- 2. asama: servis ----------
# Uretilen sey statik HTML/CSS/JS. Node calistirmaya gerek yok; nginx hem bu
# dosyalari servis eder hem de /api/v1 isteklerini backend'e yonlendirir.
FROM nginx:1.27-alpine

COPY --from=builder /repo/apps/web/dist /usr/share/nginx/html
COPY infra/nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
