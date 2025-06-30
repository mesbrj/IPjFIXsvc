#!/bin/bash

# IPFIX Service Stress Test Script
# This script generates concurrent HTTP requests to test application stability

SERVICE_URL="http://localhost:8888/odata/"
CONCURRENT_USERS=50
REQUESTS_PER_USER=10
TOTAL_REQUESTS=$((CONCURRENT_USERS * REQUESTS_PER_USER))

echo "=== IPFIX Service Stress Test ==="
echo "Target URL: $SERVICE_URL"
echo "Concurrent Users: $CONCURRENT_USERS"
echo "Requests per User: $REQUESTS_PER_USER"
echo "Total Requests: $TOTAL_REQUESTS"
echo "Starting stress test..."
echo

# Function to make HTTP requests
make_requests() {
    local user_id=$1
    local success_count=0
    local error_count=0
    
    for ((i=1; i<=REQUESTS_PER_USER; i++)); do
        response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$SERVICE_URL" 2>/dev/null)
        if [ "$response" = "200" ]; then
            ((success_count++))
        else
            ((error_count++))
        fi
    done
    
    echo "User $user_id: $success_count successful, $error_count errors"
}

# Record start time
start_time=$(date +%s)

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
echo "=== Stress Test Completed ==="
echo "Total Duration: ${duration} seconds"
echo "Requests per second: $((TOTAL_REQUESTS / duration))"
echo

# Check application memory usage
echo "=== Current Application Memory Usage ==="
ps -p $(pgrep -f "com.ipfix_scenario_ai.ipjfix_svc.Application") -o pid,pcpu,pmem,rss,vsz,comm --no-headers 2>/dev/null || echo "Application process not found"

echo
echo "=== JVM Memory Status ==="
curl -s "http://localhost:9999/jolokia/read/java.lang:type=Memory" | jq '.value.HeapMemoryUsage' 2>/dev/null || echo "JMX endpoint not accessible"
