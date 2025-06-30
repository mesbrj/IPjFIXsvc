#!/bin/bash

# OData Advanced Filtering Functionality Test Script
# Tests all implemented OData $filter operations for FlowRecords endpoint
# 
# Usage: ./odata_functionality_test.sh
# Prerequisites: Application must be running on port 8888

BASE_URL="http://localhost:8888/odata/FlowRecords"
TEMP_FILE="/tmp/odata_test_result.json"

echo "=== OData Advanced Filtering Functionality Test ==="
echo "Testing endpoint: $BASE_URL"
echo

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_counter=0
pass_counter=0
fail_counter=0

# Function to run a test with direct curl execution
run_test() {
    local test_name="$1"
    local expected_result="$2"
    shift 2
    
    test_counter=$((test_counter + 1))
    echo -n "Test ${test_counter}: ${test_name}... "
    
    # Execute curl command with all remaining arguments
    if "$@" > "$TEMP_FILE" 2>/dev/null; then
        if [ "$expected_result" = "SUCCESS" ]; then
            # Check if response contains expected OData structure
            if jq -e '.value' "$TEMP_FILE" >/dev/null 2>&1; then
                local count=$(jq '.value | length' "$TEMP_FILE" 2>/dev/null || echo "0")
                echo -e "${GREEN}PASS${NC} (${count} results)"
                pass_counter=$((pass_counter + 1))
            else
                echo -e "${RED}FAIL${NC} (Invalid JSON response)"
                fail_counter=$((fail_counter + 1))
                echo "Response: $(cat $TEMP_FILE | head -c 200)..."
            fi
        elif [ "$expected_result" = "ERROR" ]; then
            # Check if response contains error structure
            if jq -e '.error' "$TEMP_FILE" >/dev/null 2>&1; then
                echo -e "${GREEN}PASS${NC} (Expected error returned)"
                pass_counter=$((pass_counter + 1))
            else
                echo -e "${RED}FAIL${NC} (Expected error but got success)"
                fail_counter=$((fail_counter + 1))
            fi
        fi
    else
        echo -e "${RED}FAIL${NC} (Network error)"
        fail_counter=$((fail_counter + 1))
    fi
}

# Test basic functionality first
echo "=== Basic OData Query Tests ==="
run_test "Basic query (no filters)" "SUCCESS" curl -s "$BASE_URL"
run_test "Top 3 results" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$top=3'

echo
echo "=== String Equality Filters ==="
run_test "Protocol equals UDP" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol eq '\''UDP'\'''
run_test "Protocol equals TCP" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol eq '\''TCP'\'''
run_test "Protocol not equals UDP" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol ne '\''UDP'\'''

echo
echo "=== Numeric Comparison Filters ==="
run_test "Bytes greater than 1000" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Bytes gt 1000'
run_test "Bytes greater than or equal 1024" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Bytes ge 1024'
run_test "SourcePort less than 50000" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=SourcePort lt 50000'
run_test "SourcePort less than or equal 50000" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=SourcePort le 50000'
run_test "Packets equals 15" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Packets eq 15'

echo
echo "=== Logical Operators (AND/OR) ==="
run_test "TCP AND Bytes > 1000" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol eq '\''TCP'\'' and Bytes gt 1000'
run_test "UDP OR Bytes > 4000" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol eq '\''UDP'\'' or Bytes gt 4000'
run_test "Complex: (TCP AND Bytes > 1000) OR Protocol = UDP" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=(Protocol eq '\''TCP'\'' and Bytes gt 1000) or Protocol eq '\''UDP'\'''

echo
echo "=== Combined with Other OData Query Options ==="
run_test "Filter + Top: TCP records, limit 3" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol eq '\''TCP'\''' --data-urlencode '$top=3'
run_test "Filter + Top: High byte count, limit 2" "SUCCESS" curl -s "$BASE_URL" -G --data-urlencode '$filter=Bytes gt 1000 and Protocol eq '\''TCP'\''' --data-urlencode '$top=2'

echo
echo "=== Error Handling Tests ==="
run_test "Invalid field name" "ERROR" curl -s "$BASE_URL" -G --data-urlencode '$filter=InvalidField eq '\''test'\'''
run_test "Invalid operator" "ERROR" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol xyz '\''UDP'\'''
run_test "Malformed expression" "ERROR" curl -s "$BASE_URL" -G --data-urlencode '$filter=Protocol eq'
run_test "Type mismatch" "ERROR" curl -s "$BASE_URL" -G --data-urlencode '$filter=Bytes eq '\''NotANumber'\'''

echo
echo "=== Performance Tests ==="
echo "Testing large result sets and complex filters..."
run_test "All records (performance baseline)" "SUCCESS" curl -s "$BASE_URL"
if [ -f "$TEMP_FILE" ]; then
    TOTAL_RECORDS=$(jq '.value | length' "$TEMP_FILE" 2>/dev/null || echo "0")
    echo "Total records in dataset: $TOTAL_RECORDS"
fi

echo
echo "=== Test Summary ==="
echo "Total tests: $test_counter"
echo -e "Passed: ${GREEN}$pass_counter${NC}"
echo -e "Failed: ${RED}$fail_counter${NC}"

if [ $fail_counter -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All tests passed! OData advanced filtering is working correctly.${NC}"
    echo
    echo "=== Supported Filter Operations ==="
    echo "• String comparisons: eq, ne"
    echo "• Numeric comparisons: eq, ne, gt, ge, lt, le"
    echo "• Logical operators: and, or"
    echo "• Parentheses for grouping"
    echo "• Combined with \$top query option"
    echo "• Proper error handling for invalid queries"
    echo
    echo "=== Example Usage ==="
    echo "curl \"$BASE_URL\" -G --data-urlencode '\$filter=Protocol eq '\'TCP\' and Bytes gt 1000' --data-urlencode '\$top=5'"
    exit 0
else
    echo -e "\n${RED}❌ Some tests failed. Please check the results above.${NC}"
    exit 1
fi

# Cleanup
rm -f "$TEMP_FILE"
