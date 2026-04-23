# StableFlow 部署说明

## 1. 文档目标

本文档用于说明如何把 StableFlow 以前后端分离但同域反向代理的方式部署到一台云服务器上。

当前推荐方案为：

- 前端使用 Docker 构建静态产物，并由 Nginx 容器提供访问
- 前端容器将 `/api` 请求反向代理到后端容器
- 后端使用 Docker 构建并运行 Spring Boot JAR
- PostgreSQL 与 Redis 使用外部已部署实例

本方案优先服务当前 MVP 和 Demo 演示，目标是：

- 部署路径简单
- 便于快速上云
- 不需要修改前端接口地址代码

---

## 2. 当前部署结构

项目根目录已提供以下 Docker 部署文件：

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `docker-compose.yml`
- `.env.example`

运行结构如下：

```text
Browser
  -> frontend container (nginx, port 80)
       -> /            静态前端资源
       -> /api/*       proxy 到 backend:8080
  -> backend container (spring boot, internal 8080)
       -> PostgreSQL   外部数据库
       -> Redis        外部缓存 / 分布式锁
       -> Solana RPC   外部链上访问节点
```

---

## 3. 前置条件

部署前需要准备：

- 一台 Linux 云服务器，推荐 Ubuntu 22.04+
- 服务器已安装 Docker Engine 和 Docker Compose Plugin
- 一套可访问的 PostgreSQL 实例
- 一套可访问的 Redis 实例
- 一个可用的 Solana RPC 地址

当前方案默认先使用：

- `HTTP + 服务器 IP` 或 `HTTP + 域名`

建议先跑通 HTTP，再补 HTTPS。

---

## 4. 服务器安装 Docker

若服务器尚未安装 Docker，可执行：

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
```

验证：

```bash
docker --version
docker compose version
```

---

## 5. 拉取代码

在服务器上拉取项目代码：

```bash
git clone <your-repo-url>
cd stableflow
```

如果不是通过 Git 直接拉取，也可以把当前仓库压缩后上传到服务器解压。

---

## 6. 准备环境变量

将根目录模板复制为真实配置文件：

```bash
cp .env.example .env
```

然后编辑：

```bash
nano .env
```

至少需要修改以下字段：

```env
FRONTEND_PORT=80
PUBLIC_BASE_URL=http://your-server-ip-or-domain
CORS_ALLOWED_ORIGINS=http://your-server-ip-or-domain

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

说明：

- `PUBLIC_BASE_URL`
  - 对外访问地址，用于支付页与链接生成
- `CORS_ALLOWED_ORIGINS`
  - 当前允许访问后端的前端来源地址
- `JWT_SECRET`
  - 必须替换为足够长的随机字符串
- `DB_URL`
  - 指向外部 PostgreSQL
- `REDIS_HOST`
  - 指向外部 Redis
- `SOLANA_NETWORK`
  - `DEVNET` 或 `MAINNET`
- `SOLANA_USDC_MINT`
  - 可留空，后端会按网络默认值解析

若切换主网，建议至少修改：

```env
SOLANA_NETWORK=MAINNET
SOLANA_RPC_URL=https://your-mainnet-rpc
```

---

## 7. 启动服务

在项目根目录执行：

```bash
docker compose up -d --build
```

说明：

- `backend` 容器会在启动时构建 Spring Boot JAR
- `frontend` 容器会构建前端静态资源，并由 Nginx 提供服务
- 前端请求 `/api/*` 时会自动转发到 `backend:8080`

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

## 8. 访问与验证

部署成功后，默认访问地址如下：

- 前端首页：

```text
http://your-server-ip-or-domain
```

- Swagger UI：

```text
http://your-server-ip-or-domain/swagger-ui.html
```

建议按以下顺序验证：

1. 打开首页，确认前端能正常加载
2. 打开 `swagger-ui.html`，确认后端已启动
3. 完成注册或登录
4. 进入收款配置页，确认能正常保存
5. 创建一张发票，确认支付页与支付信息可用
6. 观察后端日志，确认扫描任务、验证任务与核销任务正常运行

---

## 9. 防火墙与端口

默认对外只需要放开：

- `80`：前端与 API 入口
- `22`：SSH 登录

如果后续接 HTTPS，还需要放开：

- `443`

若使用 `ufw`：

```bash
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
```

---

## 10. 常用运维命令

重新构建并启动：

```bash
docker compose up -d --build
```

停止服务：

```bash
docker compose down
```

只看后端日志：

```bash
docker compose logs -f backend
```

只看前端日志：

```bash
docker compose logs -f frontend
```

查看容器状态：

```bash
docker compose ps
```

---

## 11. 版本更新流程

当代码更新后，建议按以下步骤升级：

```bash
git pull
docker compose up -d --build
```

若只改了环境变量：

```bash
docker compose up -d
```

---

## 12. 常见问题排查

### 12.1 前端能打开，但登录失败

优先检查：

- 后端容器是否正常启动
- `.env` 中 `JWT_SECRET` 是否已配置
- 浏览器访问 `/api/auth/login` 是否返回 200 / 401 / 500
- `docker compose logs -f backend`

### 12.2 页面打开正常，但接口请求失败

优先检查：

- `frontend/nginx.conf` 是否已正确反代 `/api`
- 后端容器是否仍在运行
- `docker compose ps`

### 12.3 后端启动失败

优先检查：

- PostgreSQL 是否可访问
- Redis 是否可访问
- `DB_URL / DB_USERNAME / DB_PASSWORD` 是否正确
- `REDIS_HOST / REDIS_PORT / REDIS_PASSWORD` 是否正确

### 12.4 链上扫描没有结果

优先检查：

- `SOLANA_NETWORK` 是否与当前测试环境一致
- `SOLANA_RPC_URL` 是否可用
- 后端日志中是否有 RPC timeout
- 收款配置中的钱包地址与当前网络是否匹配

### 12.5 Swagger 打不开

优先检查：

- `backend` 容器是否正常启动
- `frontend/nginx.conf` 是否包含 `/swagger-ui.html`、`/swagger-ui/`、`/v3/api-docs` 反代配置

---

## 13. 当前部署边界

当前部署文档只覆盖：

- 单机 Docker Compose 部署
- 前后端同机部署
- PostgreSQL 与 Redis 使用外部实例

当前不覆盖：

- Kubernetes
- 多实例水平扩容
- HTTPS / Let's Encrypt 自动化
- CI/CD 自动发布

建议先按本方案把 Demo 环境跑通，再逐步补后续能力。
