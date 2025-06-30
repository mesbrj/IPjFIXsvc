#!/bin/bash

# Docker Build and Deployment Script for IPFIX OData Service
# Usage: ./docker-build.sh [OPTIONS]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
IMAGE_NAME="ipfix-odata-service"
TAG="latest"
PUSH_TO_REGISTRY=false
REGISTRY=""
BUILD_ARGS=""
DOCKER_CONTEXT="."

# Functions
print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -t, --tag TAG          Docker image tag (default: latest)"
    echo "  -n, --name NAME        Docker image name (default: ipfix-odata-service)"
    echo "  -r, --registry URL     Docker registry URL for pushing"
    echo "  -p, --push             Push image to registry after build"
    echo "  --no-cache             Build without using cache"
    echo "  --build-arg ARG=VALUE  Pass build argument to Docker"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Basic build"
    echo "  $0 -t v1.0.0 -p -r docker.io/myrepo  # Build, tag as v1.0.0 and push"
    echo "  $0 --no-cache                        # Build without cache"
}

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -n|--name)
            IMAGE_NAME="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_TO_REGISTRY=true
            shift
            ;;
        --no-cache)
            BUILD_ARGS="$BUILD_ARGS --no-cache"
            shift
            ;;
        --build-arg)
            BUILD_ARGS="$BUILD_ARGS --build-arg $2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Set full image name with registry if provided
if [[ -n "$REGISTRY" ]]; then
    FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${TAG}"
else
    FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"
fi

# Pre-build checks
log "Starting Docker build process..."
log "Image: $FULL_IMAGE_NAME"

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    error "Docker is not running or not accessible"
    exit 1
fi

# Check if Dockerfile exists
if [[ ! -f "Dockerfile" ]]; then
    error "Dockerfile not found in current directory"
    exit 1
fi

# Get build metadata
BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
VCS_REF=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
VERSION=$(grep -E "^\s*<version>" pom.xml | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/' || echo "0.0.1-SNAPSHOT")

log "Build metadata:"
log "  Build date: $BUILD_DATE"
log "  VCS ref: $VCS_REF"
log "  Version: $VERSION"

# Build the Docker image
log "Building Docker image..."
if docker build \
    --build-arg BUILD_DATE="$BUILD_DATE" \
    --build-arg VCS_REF="$VCS_REF" \
    --build-arg VERSION="$VERSION" \
    $BUILD_ARGS \
    -t "$FULL_IMAGE_NAME" \
    "$DOCKER_CONTEXT"; then
    success "Docker image built successfully: $FULL_IMAGE_NAME"
else
    error "Docker build failed"
    exit 1
fi

# Show image details
log "Image details:"
docker images "$FULL_IMAGE_NAME" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}\t{{.CreatedAt}}"

# Security scan (if available)
if command -v docker scan >/dev/null 2>&1; then
    log "Running security scan..."
    docker scan "$FULL_IMAGE_NAME" || warning "Security scan failed or found vulnerabilities"
fi

# Push to registry if requested
if [[ "$PUSH_TO_REGISTRY" == true ]]; then
    if [[ -z "$REGISTRY" ]]; then
        error "Registry URL is required for pushing (use -r or --registry)"
        exit 1
    fi
    
    log "Pushing image to registry: $REGISTRY"
    if docker push "$FULL_IMAGE_NAME"; then
        success "Image pushed successfully to $REGISTRY"
    else
        error "Failed to push image to registry"
        exit 1
    fi
fi

# Show next steps
echo ""
success "Build completed successfully!"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Test locally: docker run -p 8888:8888 $FULL_IMAGE_NAME"
echo "  2. Use with compose: docker-compose up"
echo "  3. Check health: curl http://localhost:8888/odata/"
echo "  4. View metrics: curl http://localhost:8888/actuator/health"
echo ""
echo -e "${BLUE}Available test commands:${NC}"
echo "  • OData functionality: docker exec <container> ./odata_functionality_test.sh"
echo "  • Stress testing: docker exec <container> ./stress_test.sh"
echo "  • Intensive testing: docker exec <container> ./intensive_stress_test.sh"
