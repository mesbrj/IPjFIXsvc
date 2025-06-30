# Docker Deployment Guide for IPFIX OData Service

This document provides comprehensive instructions for deploying the IPFIX OData Service using Docker in production environments.

## 🚀 Quick Start

### Prerequisites
- Docker 20.10+ and Docker Compose 2.0+
- Git (for source code access)
- 4GB+ RAM and 2+ CPU cores recommended

### Basic Deployment
```bash
# Clone and build
git clone <repository-url>
cd IPjFIXsvc

# Build and run with Docker Compose
docker-compose up -d

# Verify deployment
curl http://localhost:8888/odata/
```

## 📦 Building the Docker Image

### Using the Build Script (Recommended)
```bash
# Basic build
./docker-build.sh

# Production build with version tag
./docker-build.sh -t v1.0.0

# Build and push to registry
./docker-build.sh -t v1.0.0 -r your-registry.com/repo -p

# Build without cache (for clean builds)
./docker-build.sh --no-cache
```

### Manual Docker Build
```bash
# Build with metadata
docker build \
  --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
  --build-arg VCS_REF="$(git rev-parse --short HEAD)" \
  --build-arg VERSION="1.0.0" \
  -t ipfix-odata-service:latest .
```

## 🏗️ Architecture Overview

### Multi-Stage Build
The Dockerfile uses a multi-stage build approach:

1. **Build Stage** (`eclipse-temurin:21-jdk-alpine`)
   - Compiles Java source code with Maven
   - Downloads dependencies with layer caching
   - Produces optimized JAR file

2. **Runtime Stage** (`eclipse-temurin:21-jre-alpine`)
   - Minimal JRE environment
   - Security hardening with non-root user
   - G1GC optimization and monitoring setup

### Performance Features
- **G1 Garbage Collector** with optimized pause times (200ms target)
- **Memory Management**: 512MB initial, 2GB max heap
- **JVM Module System** compatibility for Java 21
- **String Deduplication** for memory efficiency
- **Compressed OOPs** for reduced memory footprint

## 🔧 Configuration

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles | `docker,production` |
| `JVM_MAX_HEAP` | Maximum heap size | `2g` |
| `JVM_INITIAL_HEAP` | Initial heap size | `512m` |
| `IGNITE_WORK_DIRECTORY` | Ignite data directory | `/app/data/ignite-work` |
| `LUCENE_INDEX_PATH` | Lucene index location | `/app/data/lucene-indices` |

### Volume Mounts
```yaml
volumes:
  - ipfix-data:/app/data          # Persistent data storage
  - ipfix-logs:/app/logs          # Application logs
  - ./config:/app/config          # Custom configuration
```

### Port Mapping
- `8888`: Application HTTP port
- `9999`: JMX monitoring port
- `5005`: Debug port (development only)

## 🚦 Health Monitoring

### Health Checks
The container includes built-in health checks:
```bash
# Manual health check
curl -f http://localhost:8888/odata/FlowRecords?$top=1

# Detailed health endpoint
curl http://localhost:8888/actuator/health
```

### Monitoring Endpoints
- `/actuator/health` - Application health status
- `/actuator/metrics` - JVM and application metrics
- `/actuator/prometheus` - Prometheus-format metrics

## 🔍 Production Deployment

### Docker Compose Production Setup
```yaml
version: '3.8'
services:
  ipfix-odata-service:
    image: ipfix-odata-service:v1.0.0
    ports:
      - "8888:8888"
    environment:
      - SPRING_PROFILES_ACTIVE=docker,production
      - JVM_MAX_HEAP=4g
      - JVM_INITIAL_HEAP=1g
    volumes:
      - /opt/ipfix/data:/app/data
      - /opt/ipfix/logs:/app/logs
      - /opt/ipfix/config:/app/config:ro
    deploy:
      resources:
        limits:
          memory: 5g
          cpus: '4.0'
        reservations:
          memory: 2g
          cpus: '1.0'
    restart: unless-stopped
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ipfix-odata-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ipfix-odata-service
  template:
    metadata:
      labels:
        app: ipfix-odata-service
    spec:
      containers:
      - name: ipfix-odata-service
        image: ipfix-odata-service:v1.0.0
        ports:
        - containerPort: 8888
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "docker,production,kubernetes"
        - name: JVM_MAX_HEAP
          value: "3g"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8888
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /odata/
            port: 8888
          initialDelaySeconds: 30
          periodSeconds: 10
```

## 🧪 Testing in Containers

### Functionality Testing
```bash
# Run OData functionality tests
docker exec ipfix-odata-service ./odata_functionality_test.sh

# Run stress tests
docker exec ipfix-odata-service ./stress_test.sh

# Run intensive stress tests
docker exec ipfix-odata-service ./intensive_stress_test.sh
```

### Load Testing with Multiple Containers
```bash
# Scale up for load testing
docker-compose up -d --scale ipfix-odata-service=3

# Use nginx for load balancing (add to docker-compose.yml)
```

## 📊 Performance Tuning

### JVM Memory Tuning
For different workloads, adjust memory settings:

**Small workload** (< 1000 requests/minute):
```bash
JVM_INITIAL_HEAP=256m
JVM_MAX_HEAP=1g
```

**Medium workload** (1000-10000 requests/minute):
```bash
JVM_INITIAL_HEAP=512m
JVM_MAX_HEAP=2g
```

**High workload** (10000+ requests/minute):
```bash
JVM_INITIAL_HEAP=1g
JVM_MAX_HEAP=4g
```

### G1GC Tuning Parameters
The container includes optimized G1GC settings:
- `MaxGCPauseMillis=200`: Target pause time
- `G1HeapRegionSize=16m`: Heap region size
- `G1NewSizePercent=20`: Young generation minimum
- `G1MaxNewSizePercent=40`: Young generation maximum

## 🔒 Security Considerations

### Container Security
- Runs as non-root user (uid: 1001)
- Minimal Alpine Linux base image
- No unnecessary capabilities
- Read-only root filesystem where possible
- Security scanning integration

### Network Security
```yaml
# Recommended network configuration
networks:
  ipfix-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

### Data Security
- Use Docker secrets for sensitive configuration
- Mount configuration files as read-only
- Regular security updates via base image updates

## 🚀 Scaling and High Availability

### Horizontal Scaling
```bash
# Scale with Docker Compose
docker-compose up -d --scale ipfix-odata-service=5

# With load balancer
docker-compose -f docker-compose.yml -f docker-compose.lb.yml up -d
```

### Data Persistence
- Use named volumes for data persistence
- Consider distributed storage for multi-node deployments
- Backup strategies for Lucene indices and Ignite data

## 🐛 Troubleshooting

### Common Issues

**Container fails to start:**
```bash
# Check logs
docker logs ipfix-odata-service

# Check resource constraints
docker stats ipfix-odata-service
```

**High memory usage:**
```bash
# Monitor JVM heap
docker exec ipfix-odata-service jstat -gc 1

# Generate heap dump
docker exec ipfix-odata-service jcmd 1 GC.run_finalization
docker exec ipfix-odata-service jcmd 1 VM.classloader_stats
```

**Performance issues:**
```bash
# Monitor GC logs
docker exec ipfix-odata-service tail -f /app/logs/gc.log

# JVM thread dump
docker exec ipfix-odata-service jstack 1
```

### Debug Mode
To run with debug enabled:
```bash
docker run -p 8888:8888 -p 5005:5005 \
  -e JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  ipfix-odata-service:latest
```

## 📈 Monitoring Setup

### Prometheus Integration
The service exposes metrics at `/actuator/prometheus`. Use the included monitoring stack:

```bash
# Start with monitoring
docker-compose --profile monitoring up -d

# Access Grafana
open http://localhost:3000  # admin/admin
```

### Custom Dashboards
Pre-configured Grafana dashboards include:
- JVM memory and GC metrics
- Application request rates and response times
- OData query performance
- Container resource utilization

## 🔄 CI/CD Integration

### GitLab CI Example
```yaml
build-docker:
  stage: build
  script:
    - ./docker-build.sh -t $CI_COMMIT_SHA
    - ./docker-build.sh -t latest
  only:
    - main

deploy-production:
  stage: deploy
  script:
    - docker-compose -f docker-compose.prod.yml up -d
  only:
    - main
  when: manual
```

### Jenkins Pipeline Example
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './docker-build.sh -t ${BUILD_NUMBER}'
            }
        }
        stage('Test') {
            steps {
                sh 'docker run --rm ipfix-odata-service:${BUILD_NUMBER} ./odata_functionality_test.sh'
            }
        }
        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
            }
        }
    }
}
```

## 📋 Maintenance

### Regular Updates
```bash
# Update base images
docker pull eclipse-temurin:21-jre-alpine
./docker-build.sh --no-cache

# Backup data
docker run --rm -v ipfix-data:/data -v $(pwd):/backup alpine tar czf /backup/ipfix-backup.tar.gz /data

# Update containers
docker-compose pull
docker-compose up -d
```

### Log Management
```bash
# Configure log rotation
docker run -d --log-driver=json-file --log-opt max-size=10m --log-opt max-file=5 ipfix-odata-service

# Centralized logging with ELK stack
# See docker-compose.logging.yml
```

## 📞 Support

For issues and questions:
- Check container logs: `docker logs ipfix-odata-service`
- Review health endpoints: `/actuator/health`
- Monitor resource usage: `docker stats`
- Validate configuration: `/actuator/configprops`

This deployment has been tested with:
- Docker 20.10.x+
- Docker Compose 2.x
- Kubernetes 1.20+
- Production workloads up to 10,000 requests/minute
