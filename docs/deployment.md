# StableFlow 部署说明

## 1. 文档目标

本文档只说明一件事：如何按当前已经验证通过的方式，把 StableFlow 部署到一台云服务器上。

当前推荐方案：

- 单机 Docker Compose 部署
- 前端和后端部署在同一台服务器
- 前端由 Nginx 容器对外提供访问
- 后端由 Spring Boot 容器提供 API
- PostgreSQL 与 Redis 使用外部实例
- 先跑通 `HTTP + IP/域名`，演示稳定后再考虑 HTTPS

这份文档优先服务 Demo、比赛提交和 MVP 快速上线。

---

## 2. 当前部署结构

项目根目录已经提供以下文件：

- `backend/Dockerfile`
- `deploy.sh`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `docker-compose.yml`
- `.env.example`

整体结构如下：

```text
Browser
  -> frontend container (nginx, port 80)
       -> /            前端页面
       -> /api/*       反向代理到 backend:8080
       -> /swagger-ui* 反向代理到 backend:8080
  -> backend container (spring boot, internal 8080)
       -> PostgreSQL   外部数据库
       -> Redis        外部缓存
       -> Solana RPC   外部链上节点
```

---

## 3. 部署前需要准备什么

部署前请先准备好：

- 一台 Linux 云服务器
- 已安装 Docker Engine 和 Docker Compose Plugin
- 一个可访问的 PostgreSQL
- 一个可访问的 Redis
- 一个可访问的 Solana RPC

建议至少开放以下端口：

- `22`：SSH
- `80`：站点访问

如果后续接 HTTPS，再开放 `443`。

---

## 4. 实际部署步骤

### 4.1 服务器目录约定

建议统一放在下面这个目录：

```bash
/opt/stableflow
```

目录里应包含：

- 项目代码
- `deploy.sh`
- `docker-compose.yml`
- `.env`

也就是说，推荐让 `/opt/stableflow` 本身就是 Git 工作目录，后续更新时直接在服务器执行 `git pull`。

### 4.2 从 Git 拉取代码到服务器

首次部署时，把仓库克隆到固定目录：

```bash
git clone <your-repo-url> /opt/stableflow
cd /opt/stableflow
```

如果目录已经存在，并且已经是 Git 工作目录，直接进入目录即可：

```bash
cd /opt/stableflow
```

### 4.3 配置环境变量

先复制模板：

```bash
cp .env.example .env
```

然后编辑：

```bash
nano .env
```

至少确认以下字段：

```env
FRONTEND_PORT=80
PUBLIC_BASE_URL=http://your-server-ip-or-domain
CORS_ALLOWED_ORIGINS=http://your-server-ip-or-domain

SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=replace-with-a-long-random-secret

DB_URL=jdbc:postgresql://your-postgres-host:5432/stableflow
DB_USERNAME=postgres
DB_PASSWORD=replace-with-your-postgres-password

REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=

SOLANA_NETWORK=DEVNET
SOLANA_RPC_URL=https://your-rpc-url
SOLANA_USDC_MINT=
```

字段含义可以这样理解：

- `PUBLIC_BASE_URL`：系统对外访问地址，会用于公共支付页链接生成
- `CORS_ALLOWED_ORIGINS`：允许访问后端的前端来源地址
- `JWT_SECRET`：登录鉴权密钥，必须替换成真实随机字符串
- `DB_URL / DB_USERNAME / DB_PASSWORD`：PostgreSQL 连接信息
- `REDIS_HOST / REDIS_PORT / REDIS_PASSWORD`：Redis 连接信息
- `SOLANA_NETWORK`：使用 `DEVNET` 或 `MAINNET`
- `SOLANA_RPC_URL`：建议显式填写可用 RPC 地址
- `SOLANA_USDC_MINT`：可以留空，后端会按网络使用默认值

如果使用主网，至少改成：

```env
SOLANA_NETWORK=MAINNET
SOLANA_RPC_URL=https://your-mainnet-rpc
```

### 4.4 给脚本执行权限

第一次部署时执行：

```bash
cd /opt/stableflow
chmod +x deploy.sh
```

### 4.5 一键启动服务

在项目根目录直接执行：

```bash
cd /opt/stableflow
./deploy.sh
```

当前脚本内容非常简单，本质上就是执行：

- `docker compose up -d --build`
- `docker compose ps`

如果你不想用脚本，也可以手动执行：

```bash
docker compose up -d --build
```

---

## 5. 最简部署方式

如果你只想记住最短流程，可以直接按下面做：

```bash
mkdir -p /opt/stableflow
git clone <your-repo-url> /opt/stableflow
cd /opt/stableflow
cp .env.example .env
nano .env
chmod +x deploy.sh
./deploy.sh
```

查看容器状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

---

## 6. 部署完成后怎么验证

默认访问地址：

- 首页：`http://your-server-ip-or-domain`
- Swagger：`http://your-server-ip-or-domain/swagger-ui.html`

建议按下面顺序检查：

1. 打开首页，确认前端能正常加载
2. 打开 `swagger-ui.html`，确认后端已启动
3. 注册或登录一个账号
4. 进入收款配置页，保存固定收款地址
5. 创建一张发票
6. 激活发票
7. 打开公共支付页，确认支付信息和二维码可见
8. 查看仪表盘和账单列表

如果这 8 步都能走通，说明当前 Demo 环境已经基本可用。

---

## 7. 后续更新代码怎么做

```bash
cd /opt/stableflow
git pull
./deploy.sh
```

如果只改了 `.env`，通常可以直接执行：

```bash
./deploy.sh
```

推荐更新节奏是：本地提交并推送到远端仓库，服务器执行 `git pull` 拉取最新代码，然后执行 `./deploy.sh` 重建并启动容器。

---

## 8. 常用命令

一键部署：

```bash
./deploy.sh
```

重新构建并启动：

```bash
docker compose up -d --build
```

停止服务：

```bash
docker compose down
```

查看容器状态：

```bash
docker compose ps
```

查看后端日志：

```bash
docker compose logs -f backend
```

查看前端日志：

```bash
docker compose logs -f frontend
```

---

## 9. 常见问题

### 9.1 首页能打开，但登录失败

优先检查：

- `backend` 容器是否正常启动
- `.env` 中 `JWT_SECRET` 是否已正确配置
- `docker compose logs -f backend`

### 9.2 页面能打开，但接口请求失败

优先检查：

- `frontend/nginx.conf` 是否正确代理 `/api/`
- `backend` 容器是否仍在运行
- `docker compose ps`

### 9.3 后端启动失败

优先检查：

- PostgreSQL 是否可访问
- Redis 是否可访问
- `DB_URL / DB_USERNAME / DB_PASSWORD` 是否正确
- `REDIS_HOST / REDIS_PORT / REDIS_PASSWORD` 是否正确

### 9.4 链上接口超时或扫链无结果

优先检查：

- `SOLANA_NETWORK` 是否和当前测试环境一致
- `SOLANA_RPC_URL` 是否可用
- 后端日志里是否有 RPC timeout

如果服务器无法直连 Solana RPC，可以在宿主机上配置代理，再让后端容器通过代理访问外部 RPC。

### 9.5 Swagger 打不开

优先检查：

- `backend` 容器是否正常启动
- `frontend/nginx.conf` 是否包含 `/swagger-ui.html`、`/swagger-ui/`、`/v3/api-docs` 的反向代理配置

---

## 10. 当前文档覆盖范围

本文档当前只覆盖：

- 单机 Docker Compose 部署
- 前后端同机部署
- PostgreSQL 与 Redis 使用外部实例
- 适合 Demo / 比赛 / MVP 环境

本文档当前不覆盖：

- Kubernetes
- 多实例水平扩容
- 自动化 CI/CD 发布
- HTTPS 自动签发

建议先按本文档把可演示环境跑通，再逐步补后续能力。
