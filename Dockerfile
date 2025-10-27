FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# Копируем Maven wrapper и конфигурации
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Даем права на выполнение и скачиваем зависимости
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Создаем пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем JAR из builder stage
COPY --from=builder /app/target/vacancy-backend-*.jar app.jar

# Настройки JVM для production
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]