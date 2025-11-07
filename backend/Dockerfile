# ===== Build Stage =====
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
# 実行権限を付与（Windowsからpushした場合に必要）
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# ===== Run Stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app
# JARをビルド結果からコピー
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]