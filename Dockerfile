# Multi-stage Dockerfile for Smart Attendance Platform Backend
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy maven wrapper & pom
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Resolve dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root application user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/target/smart-attendance-platform-1.0.0.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

