#!/bin/bash

# Intensive IPFIX Service Stress Test
# This script generates high concurrent load to stress test G1GC performance

SERVICE_URL="http://localhost:8888/odata/"
CONCURRENT_USERS=100
REQUESTS_PER_USER=50
TOTAL_REQUESTS=$((CONCURRENT_USERS * REQUESTS_PER_USER))

echo "=== INTENSIVE IPFIX Service Stress Test ==="
echo "Target URL: $SERVICE_URL"
echo "Concurrent Users: $CONCURRENT_USERS"
echo "Requests per User: $REQUESTS_PER_USER"
echo "Total Requests: $TOTAL_REQUESTS"
echo "Starting intensive stress test..."
echo

# Function to make HTTP requests with different endpoints
make_requests() {
    local user_id=$1
    local success_count=0
    local error_count=0
    
    for ((i=1; i<=REQUESTS_PER_USER; i++)); do
        # Vary the endpoints to create different memory patterns
        if [ $((i % 3)) -eq 0 ]; then
            endpoint="${SERVICE_URL}\$metadata"
        elif [ $((i % 3)) -eq 1 ]; then
            endpoint="${SERVICE_URL}Users"
        else
            endpoint="$SERVICE_URL"
        fi
        
        response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 "$endpoint" 2>/dev/null)
        if [ "$response" = "200" ] || [ "$response" = "404" ] || [ "$response" = "500" ]; then
            ((success_count++))
        else
            ((error_count++))
        fi
        
        # Small delay to create sustained load
        sleep 0.01
    done
    
    echo "User $user_id: $success_count responses, $error_count timeouts/errors"
}

# Record start time
start_time=$(date +%s)

# Get initial memory stats
echo "=== Initial Memory Stats ==="
ps -p $(pgrep -f "com.ipfix_scenario_ai.ipjfix_svc.Application") -o pid,pcpu,pmem,rss,vsz --no-headers 2>/dev/null || echo "Application process not found"
echo

# Launch concurrent users
for ((user=1; user<=CONCURRENT_USERS; user++)); do
    make_requests $user &
done

# Wait for all background processes to complete
wait

# Record end time
end_time=$(date +%s)
duration=$((end_time - start_time))

echo
echo "=== Intensive Stress Test Completed ==="
echo "Total Duration: ${duration} seconds"
if [ $duration -gt 0 ]; then
    echo "Requests per second: $((TOTAL_REQUESTS / duration))"
else
    echo "Test completed in less than 1 second"
fi

echo
echo "=== Final Memory Stats ==="
ps -p $(pgrep -f "com.ipfix_scenario_ai.ipjfix_svc.Application") -o pid,pcpu,pmem,rss,vsz --no-headers 2>/dev/null || echo "Application process not found"

echo
echo "=== G1GC Statistics ==="
APP_PID=$(pgrep -f "com.ipfix_scenario_ai.ipjfix_svc.Application")
if [ -n "$APP_PID" ]; then
    jstat -gc $APP_PID
else
    echo "Application process not found"
fi
