# 果语 H5 镜像。构建上下文是仓库根目录，这样才能同时拷贝
# client/ 的 S02 产物来源和 deploy/nginx/nginx.conf。
# Node 20 落在 client/package.json 的 engines 里。不要改页面逻辑来修镜像。
# 官方标签是多架构清单；本仓库在 linux/amd64 上构建过，ARM NAS 未验收。

FROM node:20.20.2-bookworm AS build

WORKDIR /src
COPY client/package.json client/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY client/index.html client/vite.config.ts client/tsconfig.json ./
COPY client/src ./src
RUN npm run build:h5

FROM nginx:1.30.6-alpine

COPY deploy/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /src/dist/build/h5 /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=12 \
    CMD wget -q -O /dev/null http://127.0.0.1/api/v1/system/health
