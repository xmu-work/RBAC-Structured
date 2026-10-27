# 后端升级方案 V1.1：上线组织机构接口

接在 `DEPLOYMENT_4NODES.md` 之后执行。目标：把线上 `iam-app:1.0.0`（旧版，组织接口为占位实现，调用即 500）升级到包含组织接口的 `iam-app:1.0.1`。

## 0. 现状与变更范围

| 项 | 线上现状 | 升级后 |
| --- | --- | --- |
| 镜像 | `iam-app:1.0.0`（hao1417 分支 `74c350a` 构建） | `iam-app:1.0.1`（main 最新） |
| 组织接口 `/orgs` | `UnsupportedOperationException: 待实现` → 500 | 已实现（树、详情、下级、新建、移动） |
| 表结构 | 14 张表 | **不变**（`schema.sql` 与实体自 `74c350a` 起未改） |
| 功能点 | 25 个（10001~10008、20001~20017） | 30 个（补 20018~20022） |
| 超级管理员绑定 | 25 条 | 30 条 |
| 组织 | 0 条（admin 的 `org_id=1` 悬空） | 2 条（1 总公司、2 信息中心） |

数据变更只有 `src/main/resources/db/upgrade_v1.1_org.sql`：全部 `INSERT IGNORE`，可重复执行。本地已在 MySQL 8.0 上验证：旧 schema + 旧 seed 执行升级脚本（两次）后，功能点、角色绑定、组织三张表的数据与全新 `schema.sql + seed.sql` 逐行一致。

审计日志接口（`AuditController`）新版本仍未实现，升级后访问审计页面依然 500，不属于本次范围。

## 1. 前置条件

1. 本文与 `upgrade_v1.1_org.sql` 已合入 main（服务器只跟 main）。
2. compiler、manage 上的 SWR 登录未过期（24 小时有效，过期按 `DEPLOYMENT_4NODES.md` 4.2 重新 `docker login`）。
3. 选一个没人在测的时间段：`update_config.order: stop-first`，更新期间后端停 1~2 分钟。

## 2. 备份数据库（course-node1）

```bash
mkdir -p /srv/iam/backup
CID=$(docker ps -qf name=iam_mysql)
docker exec "$CID" sh -c 'exec mysqldump -uroot -p"$(cat /run/secrets/mysql_root_password)" --single-transaction --default-character-set=utf8mb4 iam_platform' \
  > /srv/iam/backup/iam_platform_$(date +%Y%m%d_%H%M).sql
ls -lh /srv/iam/backup
```

## 3. 构建并推送 1.0.1（compiler）

```bash
cd ~/RBAC-Structured
git status --short | grep -v '^?? web/mock/' # 确认没有本地改动（web/mock 下的临时 .mjs 可忽略）
git pull
git log --oneline -1                          # 应为合入本文的那次提交

export JAVA_HOME=$HOME/jdk-21.0.2
$HOME/apache-maven-3.9.16/bin/mvn -B package  # 带测试构建：72 个测试应全部通过，不依赖数据库
cp target/iam-platform-1.0.0-SNAPSHOT.jar app.jar

REG=swr.cn-north-4.myhuaweicloud.com/ooad-mytx
docker build -f deploy/swarm/Dockerfile.app-prebuilt -t $REG/iam-app:1.0.1 .
docker push $REG/iam-app:1.0.1
```

## 4. 执行增量 SQL（course-node1）

旧版应用不读取新增的这几行数据，所以先改数据、后换镜像也不影响线上。

```bash
# 从 compiler 转传：scp ~/RBAC-Structured/src/main/resources/db/upgrade_v1.1_org.sql <course-node1>:/tmp/
CID=$(docker ps -qf name=iam_mysql)
docker exec -i "$CID" sh -c 'exec mysql -uroot -p"$(cat /run/secrets/mysql_root_password)" --default-character-set=utf8mb4 iam_platform' \
  < /tmp/upgrade_v1.1_org.sql
```

最后一行输出应为 `30  30  2`（功能点 / 超级管理员绑定 / 组织）。

## 5. 滚动更新应用（manage）

```bash
cd ~/RBAC-Structured && git pull              # 栈文件里的镜像标签已改为 1.0.1
docker service update --with-registry-auth \
  --image swr.cn-north-4.myhuaweicloud.com/ooad-mytx/iam-app:1.0.1 iam_app
docker service ps iam_app --no-trunc | head -3 # 新任务 Running，旧任务 Shutdown
```

栈文件 `deploy/swarm/iam-stack-backend.yml` 已同步改成 `1.0.1`，以后重新执行 `docker stack deploy` 不会把镜像退回 1.0.0。

## 6. 验证（manage）

```bash
docker run --rm --network iam-net curlimages/curl -s http://app:8081/actuator/health
# {"status":"UP"}

TOKEN=$(docker run --rm --network iam-net curlimages/curl -s -X POST http://app:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123","clientIp":"127.0.0.1"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

docker run --rm --network iam-net curlimages/curl -s http://app:8080/api/v1/orgs -H "Authorization: Bearer $TOKEN"
# code=0，data 中有"总公司"
docker run --rm --network iam-net curlimages/curl -s http://app:8080/api/v1/orgs/1/children -H "Authorization: Bearer $TOKEN"
# data 中有"信息中心"

docker service logs iam_app --since 5m 2>&1 | grep -E 'ERROR|Exception' || echo "无错误日志"
```

登录前端，进入「组织管理」，应能看到组织树「总公司 → 信息中心」。

## 7. 回滚

| 场景 | 操作 |
| --- | --- |
| 新镜像起不来或接口异常 | manage：`docker service rollback iam_app`（回到 1.0.0）。增量 SQL 新增的行不影响旧版本，可以保留 |
| 数据需要恢复 | course-node1：用第 2 步的备份导回 `docker exec -i "$CID" sh -c 'exec mysql -uroot -p"$(cat /run/secrets/mysql_root_password)" iam_platform' < /srv/iam/backup/<备份文件>.sql` |

回滚后把栈文件里的镜像标签改回 `1.0.0`，避免下次 `stack deploy` 又拉起 1.0.1。
