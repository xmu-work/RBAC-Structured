# RBAC-Structured 当前阶段部署手册：4 台 + SWR + 先跑后端

这是**当前阶段的操作手册**。目标形态（8 台、含前端）见 `DEPLOYMENT_SWARM.md`；systemd 裸机方案见 `DEPLOYMENT_8_NODES.md`（暂不使用）。服务器：`manage`、`compiler`、`course-node1`、`course-node2`，Swarm 集群已建好。

## 1. 四台怎么用（与 8 台方案的衔接）

| 服务器 | Swarm 角色 | 标签 | 现在跑什么 | 扩到 8 台时 |
| --- | --- | --- | --- | --- |
| manage | manager | — | 部署控制台；前端就绪后跑 iam-web | 再加 2 台新 manager，凑 3 台仲裁 |
| compiler | worker | （不打） | **构建机**：拉代码、build、push 到 SWR | 可保持 worker，或升级为 manager |
| course-node1 | worker | `role=db` | iam-mysql + iam-redis（本地数据盘） | 不变 |
| course-node2 | worker | `role=app` | iam-app ×1 | 加 2 台 role=app，副本扩到 3 |

分工逻辑：代码只在 compiler 上编译成镜像推到华为云 SWR；集群节点只从 SWR 拉镜像跑，不在节点上构建；数据库钉死在 course-node1；应用钉死在 course-node2。后端先跑起来，前端 web 镜像做好后用完整栈文件 `iam-stack.yml` 补上。

后端栈文件：`deploy/swarm/iam-stack-backend.yml`（只含 mysql/redis/app）。

## 2. 集群准备（在 manage 上执行）

### 2.1 打标签

```bash
docker node ls                          # 确认 4 台都在，manage 是 Leader
docker node update --label-add role=db  course-node1
docker node update --label-add role=app course-node2
docker node inspect course-node1 --format '{{ json .Spec.Labels }}'
```

### 2.2 建 overlay 网络

```bash
docker network create --driver overlay iam-net
```

### 2.3 数据目录（在 course-node1 上执行）

```bash
sudo mkdir -p /srv/iam/mysql /srv/iam/redis
```

## 3. 把代码传到 compiler

项目目前只在你的 Windows 上，没有 git 远程仓库，直接打包传：

```bash
# Windows Git Bash（本机）执行
cd /d/Downloads/Courses
tar --exclude=RBAC-Structured/target \
    --exclude=RBAC-Structured/web/node_modules \
    --exclude=RBAC-Structured/web/dist \
    --exclude=RBAC-Structured/design \
    -czf /tmp/rbac-src.tar.gz RBAC-Structured
scp /tmp/rbac-src.tar.gz root@<compiler的IP>:~/
```

```bash
# compiler 上执行
tar xzf ~/rbac-src.tar.gz -C ~/
cd ~/RBAC-Structured
```

服务器上已有的 `spring-fundamentals` 项目不用动，互不影响。

## 4. 准备 SWR（华为云容器镜像服务）

### 4.1 创建组织

SWR 控制台 → 「组织管理」→ 创建组织（例如 `iam-course`，**必须全小写字母开头**）。后面镜像地址形如：

```text
swr.<region>.myhuaweicloud.com/<组织名>/<镜像名>:<标签>
```

region 从 SWR 控制台地址栏确认（如 `cn-north-4`，北京四；你的 ECS 公网 IP 段 120.46.x 通常就是北京四）。

### 4.2 生成 docker login 指令（两种方式任选）

**方式 A（推荐，最省事）**：SWR 控制台右上角「客户端上传 → 生成临时登录指令」，复制输出的 `docker login ...` 命令，在 compiler 和 manage 上**各执行一次**（有效期 24 小时）。

**方式 B（用已有 AK/SK 计算）**：工作区 `credentials.csv` 里的 AK/SK 就能算登录密码。在 compiler 上执行：

```bash
AK='HPUA9BZLO28QDNMTSRHJ'          # Access Key Id
SK='你的 Secret Access Key'
REGION='cn-north-4'                 # 换成你的 region

SIGN=$(python3 - "$SK" <<'PY'
import sys, hmac, hashlib, base64, datetime
sk = sys.argv[1]
t = datetime.datetime.utcnow().strftime('%Y%m%dT%H%M%SZ')
print(base64.b64encode(hmac.new(sk.encode(), t.encode(), hashlib.sha256).digest()).decode())
PY
)
docker login "swr.${REGION}.myhuaweicloud.com" -u "${REGION}@${AK}" -p "$SIGN"
```

成功会显示 `Login Succeeded`。manage 上也要执行同样的 login（部署时要把凭据分发给节点）。

> AK/SK 等同于密码：不要提交进 git、不要发给别人。`credentials.csv` 目前在工作区根目录，注意别把它打包进代码仓库。

## 5. 构建并推送镜像（在 compiler 上）

```bash
cd ~/RBAC-Structured

ORG=iam-course        # 换成你的组织名
REG=swr.cn-north-4.myhuaweicloud.com/$ORG

# 后端镜像（必做）：容器内 Maven 构建，无需在服务器装 JDK
docker build -f deploy/swarm/Dockerfile.app -t $REG/iam-app:1.0.0 .

# 基础镜像转推进 SWR（节点拉取更稳，不依赖 Docker Hub）
docker pull mysql:8.0      && docker tag mysql:8.0      $REG/mysql:8.0      && docker push $REG/mysql:8.0
docker pull redis:7-alpine && docker tag redis:7-alpine $REG/redis:7-alpine && docker push $REG/redis:7-alpine

# 推送业务镜像
docker push $REG/iam-app:1.0.0
```

前端镜像（可选，现在可以不做）：

```bash
docker build -f deploy/swarm/Dockerfile.web -t $REG/iam-web:1.0.0 .
docker push $REG/iam-web:1.0.0
```

推送后在 SWR 控制台「我的镜像」里应能看到 `iam-app` 等。镜像默认私有，节点拉取时靠第 6 步的 `--with-registry-auth` 分发凭据。

## 6. 创建 secrets（在 manage 上执行）

```bash
JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
DB_APP_PASSWORD="$(openssl rand -hex 24)"
MYSQL_ROOT_PASSWORD="$(openssl rand -hex 24)"
REDIS_PASSWORD="$(openssl rand -hex 24)"
IAM_DEFAULT_PASSWORD='替换为创建用户的初始强密码'

printf '%s' "$JWT_SECRET"         | docker secret create jwt_secret -
# 首次部署必须用仓库 application.yml 里的原值（seed.sql 的管理员姓名用它加密，换了会登录 500）
printf '%s' 'iam-aes-256-key-please-change-in-production' | docker secret create crypto_aes_key -
printf '%s' "$DB_APP_PASSWORD"      | docker secret create db_app_password -
printf '%s' "$MYSQL_ROOT_PASSWORD"  | docker secret create mysql_root_password -
printf '%s' "$REDIS_PASSWORD"       | docker secret create redis_password -
printf '%s' "$IAM_DEFAULT_PASSWORD" | docker secret create iam_default_password -

docker secret ls   # 应有 6 个
```

## 7. 部署后端栈（在 manage 上）

把仓库也传到 manage 一份（栈文件在仓库里；可以用第 3 节同样的 tar + scp），然后：

```bash
cd ~/RBAC-Structured/deploy/swarm
# 先把文件里的 ORG 全部替换成你的组织名（两处：mysql、redis、app 镜像地址）
sed -i 's|/ORG/|/iam-course/|g' iam-stack-backend.yml   # 换成实际组织名

docker stack deploy -c iam-stack-backend.yml --with-registry-auth iam
watch docker stack ps iam
```

**`--with-registry-auth` 是关键**：它把 manage 上 `docker login` 的 SWR 凭据分发给 course-node1/course-node2，节点才能拉私有镜像。忘了加会报 `No such image` / `access denied`。

预期过程：

1. `iam_mysql.1` 先起，首次初始化约 1~2 分钟才健康；
2. `iam_app` 在 MySQL 就绪前会反复重启（连不上库自动退出重拉），**这是正常的**，DB 健康后自愈；
3. `iam_redis.1` 很快就绪。全部 `Running` 即部署完成。

## 8. 导入数据库表（在 course-node1 上，只做一次）

先把 SQL 文件传到 course-node1（本机 scp，或从 compiler 转传）：

```bash
# Windows 本机
scp ~/RBAC-Structured/src/main/resources/db/{schema.sql,seed.sql} root@<node1的IP>:/tmp/
```

```bash
# course-node1 上
CID=$(docker ps -qf name=iam_mysql)
docker exec -i "$CID" sh -c 'exec mysql -uroot -p"$(cat /run/secrets/mysql_root_password)" --default-character-set=utf8mb4 iam_platform' < /tmp/schema.sql
docker exec -i "$CID" sh -c 'exec mysql -uroot -p"$(cat /run/secrets/mysql_root_password)" --default-character-set=utf8mb4 iam_platform' < /tmp/seed.sql
```

验证（应列出 9 张表和默认管理员 admin）：

```bash
docker exec "$CID" sh -c 'exec mysql -uroot -p"$(cat /run/secrets/mysql_root_password)" -e "SHOW TABLES; SELECT id,user_no,account_status FROM iam_platform.iam_user;" iam_platform'
```

## 9. 验证后端（在 manage 上）

8080 没有对外发布，用一次性容器进网络里测：

```bash
docker run --rm --network iam-net curlimages/curl \
  -s -X POST http://app:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123","clientIp":"127.0.0.1"}'
```

返回 JSON 里有 `accessToken` 即后端跑通。也可以看三副本健康与日志：

```bash
docker service ls
docker service logs iam_app --tail 50
```

想在 Postman/浏览器直测 API，把 `iam-stack-backend.yml` 里 `ports` 注释解开（会发布 8080 到 course-node2），重新 deploy；防火墙只对自己的 IP 放行 8080。测完建议关掉。

## 10. 下一步

1. **前端**：构建 `iam-web` 镜像推 SWR → 用完整栈 `docker stack deploy -c iam-stack.yml --with-registry-auth iam`（web 以 global 模式跑在 manage 上，发布 80 端口）→ 浏览器 `http://<manage的IP>/`；
2. **首次安全初始化**：默认 `admin / Admin@123` 不能长期用，按 `DEPLOYMENT_8_NODES.md` 第 8 节流程创建强密码管理员后封存默认账号；
3. **扩到 8 台**：加 2 台新 manager（凑 3 台仲裁）、2 台新 worker 打 `role=app`，`docker service scale iam_app=3`；分配表见 `DEPLOYMENT_SWARM.md` 1.2 节；
4. **备份**：`DEPLOYMENT_SWARM.md` 12.4 的 cron 同样适用于 course-node1。

## 11. 这一阶段最容易踩的坑

- **SWR 登录指令 24 小时过期**，过期后 push/pull 会报 `unauthorized`，重新生成即可；
- **组织名、镜像名必须全小写**，大写会导致 push 报 `denied`；
- **`--with-registry-auth` 忘加**：节点拉不了私有镜像，任务卡在 Preparing/Rejected；
- **2G 内存一台只放一个 JVM**：app 副本数不要超过 role=app 节点数；
- **`crypto_aes_key` 首次必须用仓库原值**，否则默认管理员登录 500；后续完成管理员重建再轮换；
- 每个节点确认有 2G swap（`free -h`），没有就按 `DEPLOYMENT_SWARM.md` 第 3 节加。
