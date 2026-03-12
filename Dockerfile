# Stage 1: Build (Upgraded to Java 21)
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Upgraded to Java 21)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create data directory for SQLite
RUN mkdir -p /app/data && chown -R spring:spring /app

# Copy JAR from build stage
COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Environment variables
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/degreeplanner.db \
    JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]