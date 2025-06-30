# Multi-stage Dockerfile for IPFIX OData Service
# Optimized for production deployment with G1GC and performance tuning

# Build stage
FROM eclipse-temurin:21.0.7_7-jdk-alpine AS build

# Set build-time variables
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# Add metadata labels
LABEL maintainer="IPFIX Scenario AI Team" \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="ipfix-odata-service" \
      org.label-schema.description="High-performance IPFIX OData service with G1GC optimization" \
      org.label-schema.version=$VERSION \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0"

# Set working directory
WORKDIR /app

# Install Maven in the build stage
RUN apk add --no-cache maven

# Copy Maven configuration files first (for better Docker layer caching)
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application with specific encoding and optimization flags
RUN mvn clean package -DskipTests -B \
    -Dfile.encoding=UTF-8 \
    -Dproject.build.sourceEncoding=UTF-8 \
    -Dmaven.compiler.encoding=UTF-8 \
    && mv target/*.jar app.jar

# Runtime stage
FROM eclipse-temurin:21.0.7_7-jre-alpine AS runtime

# Install necessary packages for monitoring and debugging
RUN apk add --no-cache \
    curl \
    jq \
    tini \
    && rm -rf /var/cache/apk/*

# Create application user and group for security
RUN addgroup -g 1001 appgroup && \
    adduser -D -s /bin/sh -u 1001 -G appgroup appuser

# Set working directory
WORKDIR /app

# Create necessary directories with proper permissions
RUN mkdir -p /app/data/lucene-indices \
    /app/logs \
    /tmp/ignite-work \
    && chown -R appuser:appgroup /app /tmp/ignite-work

# Copy the built JAR from build stage
COPY --from=build --chown=appuser:appgroup /app/app.jar ./

# Copy configuration files and scripts
COPY --chown=appuser:appgroup src/main/resources/application.properties ./config/
COPY --chown=appuser:appgroup odata_functionality_test.sh ./
COPY --chown=appuser:appgroup stress_test.sh ./
COPY --chown=appuser:appgroup intensive_stress_test.sh ./

# Make scripts executable
RUN chmod +x *.sh

# Switch to non-root user
USER appuser

# Set JVM environment variables for optimal G1GC performance
ENV JAVA_OPTS="\
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/sun.misc=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED \
    --add-opens=java.base/java.util=ALL-UNNAMED \
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
    --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
    --add-opens=java.base/java.net=ALL-UNNAMED \
    --add-opens=java.base/javax.net.ssl=ALL-UNNAMED \
    --add-opens=java.base/java.security=ALL-UNNAMED \
    --add-opens=java.base/java.time=ALL-UNNAMED \
    --add-opens=java.sql/java.sql=ALL-UNNAMED \
    --add-opens=java.management/javax.management=ALL-UNNAMED \
    --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
    --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-exports=java.base/sun.security.util=ALL-UNNAMED \
    --add-modules=java.se \
    --enable-native-access=ALL-UNNAMED"

# G1GC optimized JVM settings
ENV GC_OPTS="\
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:G1NewSizePercent=20 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+UseStringDeduplication"

# Memory and performance settings
ENV MEMORY_OPTS="\
    -Xms512m \
    -Xmx2g \
    -XX:MaxDirectMemorySize=1g \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=4"

# Monitoring and debugging settings
ENV MONITORING_OPTS="\
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9999 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=logs/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError"

# GC logging settings
ENV GC_LOG_OPTS="\
    -Xlog:gc*:logs/gc.log:time,tags \
    -XX:+UnlockExperimentalVMOptions"

# Network and system settings
ENV SYSTEM_OPTS="\
    -Djava.net.preferIPv4Stack=true \
    -Dsun.net.useExclusiveBind=false \
    -Dnetworkaddress.cache.ttl=60 \
    -Dfile.encoding=UTF-8"

# Spring Boot specific settings
ENV SPRING_OPTS="\
    -Dspring.config.location=classpath:/application.properties,optional:file:./config/application.properties \
    -Dspring.profiles.active=docker"

# Expose application port and JMX port
EXPOSE 8888 9999

# Health check using the application endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8888/odata/ || exit 1

# Use tini as PID 1 to handle signals properly
ENTRYPOINT ["/sbin/tini", "--"]

# Start the application with all optimizations
CMD java \
    $JAVA_OPTS \
    $GC_OPTS \
    $MEMORY_OPTS \
    $MONITORING_OPTS \
    $GC_LOG_OPTS \
    $SYSTEM_OPTS \
    $SPRING_OPTS \
    -jar app.jar

# Alternative debug mode (uncomment for development)
# CMD java \
#     $JAVA_OPTS \
#     $GC_OPTS \
#     $MEMORY_OPTS \
#     $MONITORING_OPTS \
#     $GC_LOG_OPTS \
#     $SYSTEM_OPTS \
#     $SPRING_OPTS \
#     -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
#     -jar app.jar
