# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy as build
WORKDIR /app

# 1. Copy the maven wrapper and pom first (Optimization: Caches dependencies)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# By doing this before copying your source code,
# Docker won't re-download your 300MB of dependencies every time you fix a typo in your Java code.
# It only re-downloads if you change the pom.xml.
RUN ./mvnw dependency:go-offline

# 2. Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl for health checks
# rm -rf /var/lib/apt/lists/* serves to delete cache(?)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy only the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Run with memory-aware JVM settings
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# Layer Caching: Rebuilds take seconds instead of minutes because dependencies are stored separately.
# Small Image Size:	Your final image will be ~200MB instead of ~800MB because it doesn't contain Maven.
# Security:	Your source code is not inside the final container that runs in production.