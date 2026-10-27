#!/bin/sh
# 容器启动时把 Swarm secrets 转成 Spring Boot 环境变量。
# 变量名与 application.yml 的 relaxed binding 对应：
#   jwt.secret / crypto.aes-key / spring.datasource.password /
#   spring.data.redis.password / iam.default-password
set -eu

export JWT_SECRET="$(cat /run/secrets/jwt_secret)"
export CRYPTO_AES_KEY="$(cat /run/secrets/crypto_aes_key)"
export SPRING_DATASOURCE_PASSWORD="$(cat /run/secrets/db_app_password)"
export SPRING_DATA_REDIS_PASSWORD="$(cat /run/secrets/redis_password)"
export IAM_DEFAULT_PASSWORD="$(cat /run/secrets/iam_default_password)"

exec java -Xms512m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError -jar /app/app.jar
