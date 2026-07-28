# ==========================================
# Stage 1: Build stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper & pom.xml first to leverage Docker layer caching for dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Ensure maven wrapper is executable
RUN chmod +x mvnw

# Resolve dependencies offline to speed up subsequent builds
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build the application executable JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Runtime stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root system group and user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot default application port
EXPOSE 8080

# Configure JVM for optimal container performance and memory usage
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
