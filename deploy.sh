#!/bin/bash

# IPFIX OData Service Deployment Script
# Simplified deployment for various environments

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}" >&2
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║                IPFIX OData Service Deployer              ║"
    echo "║            Production-Ready Docker Deployment            ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  build      Build Docker image"
    echo "  start      Start the service (build + run)"
    echo "  stop       Stop the service"
    echo "  restart    Restart the service"
    echo "  status     Show service status"
    echo "  test       Run functionality tests"
    echo "  stress     Run stress tests"
    echo "  logs       Show service logs"
    echo "  clean      Clean up containers and images"
    echo "  monitor    Start with monitoring stack"
    echo ""
    echo "Options:"
    echo "  -p, --production  Use production configuration"
    echo "  -d, --detached    Run in detached mode"
    echo "  -v, --verbose     Enable verbose output"
    echo "  -h, --help        Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 start                 # Quick start for development"
    echo "  $0 start --production    # Start with production settings"
    echo "  $0 monitor --detached    # Start with monitoring in background"
    echo "  $0 test                  # Run all tests"
}

check_prerequisites() {
    log "Checking prerequisites..."
    
    if ! command -v docker >/dev/null 2>&1; then
        error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker-compose >/dev/null 2>&1 && ! docker compose version >/dev/null 2>&1; then
        error "Docker Compose is not installed"
        exit 1
    fi
    
    if ! docker info >/dev/null 2>&1; then
        error "Docker daemon is not running"
        exit 1
    fi
    
    success "Prerequisites check passed"
}

build_image() {
    log "Building Docker image..."
    ./docker-build.sh "$@"
}

start_service() {
    local compose_file="docker-compose.yml"
    local extra_args=""
    
    if [[ "$PRODUCTION" == "true" ]]; then
        log "Starting in production mode..."
        export SPRING_PROFILES_ACTIVE="docker,production"
        extra_args="--profile production"
    else
        log "Starting in development mode..."
    fi
    
    if [[ "$DETACHED" == "true" ]]; then
        extra_args="$extra_args -d"
    fi
    
    docker-compose -f "$compose_file" up $extra_args
}

stop_service() {
    log "Stopping IPFIX OData Service..."
    docker-compose down
    success "Service stopped"
}

restart_service() {
    log "Restarting IPFIX OData Service..."
    stop_service
    start_service
}

show_status() {
    log "Service Status:"
    echo ""
    
    # Container status
    if docker-compose ps | grep -q "ipfix-odata-service"; then
        docker-compose ps
        echo ""
        
        # Health check
        log "Health Check:"
        if curl -s -f http://localhost:8888/odata/ >/dev/null 2>&1; then
            success "Service is healthy and responding"
        else
            warning "Service may be starting or unhealthy"
        fi
        
        # Resource usage
        log "Resource Usage:"
        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" | grep ipfix || true
        
    else
        warning "Service is not running"
    fi
}

run_tests() {
    log "Running functionality tests..."
    
    if ! docker-compose ps | grep -q "ipfix-odata-service.*Up"; then
        error "Service is not running. Start it first with: $0 start"
        exit 1
    fi
    
    local container_name="ipfix-odata-service"
    
    log "Running OData functionality tests..."
    if docker exec "$container_name" ./odata_functionality_test.sh; then
        success "OData functionality tests passed"
    else
        error "OData functionality tests failed"
        return 1
    fi
    
    log "Running basic stress test..."
    if docker exec "$container_name" timeout 60 ./stress_test.sh; then
        success "Basic stress test passed"
    else
        warning "Basic stress test encountered issues"
    fi
}

run_stress_tests() {
    log "Running comprehensive stress tests..."
    
    if ! docker-compose ps | grep -q "ipfix-odata-service.*Up"; then
        error "Service is not running. Start it first with: $0 start"
        exit 1
    fi
    
    local container_name="ipfix-odata-service"
    
    log "Running standard stress test..."
    docker exec "$container_name" ./stress_test.sh
    
    log "Running intensive stress test..."
    docker exec "$container_name" ./intensive_stress_test.sh
    
    success "All stress tests completed"
}

show_logs() {
    log "Showing service logs (Ctrl+C to exit)..."
    docker-compose logs -f ipfix-odata-service
}

start_monitoring() {
    log "Starting with monitoring stack..."
    
    local extra_args="--profile monitoring"
    if [[ "$DETACHED" == "true" ]]; then
        extra_args="$extra_args -d"
    fi
    
    docker-compose up $extra_args
    
    if [[ "$DETACHED" == "true" ]]; then
        echo ""
        log "Monitoring URLs:"
        echo "  • Application: http://localhost:8888/odata/"
        echo "  • Grafana:     http://localhost:3000 (admin/admin)"
        echo "  • Prometheus:  http://localhost:9090"
        echo "  • Metrics:     http://localhost:8888/actuator/prometheus"
    fi
}

clean_up() {
    log "Cleaning up containers and images..."
    
    # Stop and remove containers
    docker-compose down -v --remove-orphans
    
    # Remove unused images
    docker image prune -f
    
    # Remove unused volumes
    docker volume prune -f
    
    success "Cleanup completed"
}

# Parse command line arguments
COMMAND=""
PRODUCTION=false
DETACHED=false
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        build|start|stop|restart|status|test|stress|logs|clean|monitor)
            COMMAND="$1"
            shift
            ;;
        -p|--production)
            PRODUCTION=true
            shift
            ;;
        -d|--detached)
            DETACHED=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            set -x
            shift
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

# Main execution
print_banner

if [[ -z "$COMMAND" ]]; then
    error "No command specified"
    print_usage
    exit 1
fi

# Change to script directory
cd "$(dirname "$0")"

case $COMMAND in
    build)
        check_prerequisites
        build_image
        ;;
    start)
        check_prerequisites
        build_image
        start_service
        if [[ "$DETACHED" == "true" ]]; then
            echo ""
            log "Service started in background"
            log "Check status with: $0 status"
            log "View logs with: $0 logs"
            log "Test with: $0 test"
        fi
        ;;
    stop)
        stop_service
        ;;
    restart)
        restart_service
        ;;
    status)
        show_status
        ;;
    test)
        run_tests
        ;;
    stress)
        run_stress_tests
        ;;
    logs)
        show_logs
        ;;
    monitor)
        check_prerequisites
        build_image
        start_monitoring
        ;;
    clean)
        clean_up
        ;;
    *)
        error "Unknown command: $COMMAND"
        exit 1
        ;;
esac

success "Operation completed successfully!"
