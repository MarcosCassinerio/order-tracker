# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Uses a full JDK + Maven image to compile and package the app.
# This stage is discarded after building — it never ships to production.
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first and download dependencies separately.
# Docker caches this layer — if pom.xml didn't change, Maven won't re-download.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source and build the jar
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# Minimal JRE image — no Maven, no JDK, no source code.
# eclipse-temurin is the standard production-grade OpenJDK distribution.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user — running as root inside containers is a security risk
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the final jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Use exec form (not shell form) so SIGTERM from Kubernetes reaches the JVM directly.
# This enables graceful shutdown (configured in application.yml).
ENTRYPOINT ["java", "-jar", "app.jar"]
