# OData API Reference

## Overview

The IPFIX OData Service provides a complete OData v4 implementation for querying IPFIX flow records. This document details all available operations, query options, and examples.

## Base URL

```
http://localhost:8888/odata/
```

## Entity Sets

### FlowRecords

Represents individual IPFIX flow records with comprehensive network flow information.

#### Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `flowId` | String | Unique identifier for the flow | `"550e8400-e29b-41d4-a716-446655440000"` |
| `sourceIp` | String | Source IP address | `"192.168.1.100"` |
| `destinationIp` | String | Destination IP address | `"10.0.0.50"` |
| `sourcePort` | Integer | Source port number | `443` |
| `destinationPort` | Integer | Destination port number | `58392` |
| `protocol` | Integer | IP protocol number | `6` (TCP), `17` (UDP) |
| `bytes` | Long | Total bytes transferred | `1024` |
| `packets` | Long | Total packets transferred | `8` |
| `timestamp` | DateTimeOffset | Flow start time | `"2025-06-30T10:30:00.000Z"` |
| `duration` | Integer | Flow duration in milliseconds | `1500` |
| `tcpFlags` | String | TCP flags (if TCP) | `"ACK,PSH"` |
| `application` | String | Detected application | `"HTTPS"` |

## OData Query Options

### $filter

Filter entities based on boolean expressions.

#### Comparison Operators

```http
# Equal
GET /odata/FlowRecords?$filter=protocol eq 6

# Not equal
GET /odata/FlowRecords?$filter=protocol ne 17

# Greater than
GET /odata/FlowRecords?$filter=bytes gt 1000

# Greater than or equal
GET /odata/FlowRecords?$filter=bytes ge 1000

# Less than
GET /odata/FlowRecords?$filter=bytes lt 10000

# Less than or equal
GET /odata/FlowRecords?$filter=bytes le 10000
```

#### Logical Operators

```http
# AND
GET /odata/FlowRecords?$filter=protocol eq 6 and bytes gt 1000

# OR
GET /odata/FlowRecords?$filter=protocol eq 6 or protocol eq 17

# NOT
GET /odata/FlowRecords?$filter=not (protocol eq 1)

# Parentheses for grouping
GET /odata/FlowRecords?$filter=(protocol eq 6 or protocol eq 17) and bytes gt 500
```

#### String Functions

```http
# Contains
GET /odata/FlowRecords?$filter=contains(sourceIp,'192.168')

# Starts with
GET /odata/FlowRecords?$filter=startswith(sourceIp,'192')

# Ends with
GET /odata/FlowRecords?$filter=endswith(destinationIp,'.1')

# String length
GET /odata/FlowRecords?$filter=length(application) gt 4

# Case-insensitive
GET /odata/FlowRecords?$filter=tolower(application) eq 'https'
```

#### Date/Time Functions

```http
# Year
GET /odata/FlowRecords?$filter=year(timestamp) eq 2025

# Month
GET /odata/FlowRecords?$filter=month(timestamp) eq 6

# Day
GET /odata/FlowRecords?$filter=day(timestamp) eq 30

# Hour
GET /odata/FlowRecords?$filter=hour(timestamp) ge 10

# Date range
GET /odata/FlowRecords?$filter=timestamp ge 2025-06-01T00:00:00Z and timestamp le 2025-06-30T23:59:59Z
```

#### Arithmetic Operations

```http
# Addition
GET /odata/FlowRecords?$filter=(bytes add 100) gt 1000

# Subtraction
GET /odata/FlowRecords?$filter=(bytes sub 100) lt 900

# Multiplication
GET /odata/FlowRecords?$filter=(packets mul 100) gt bytes

# Division
GET /odata/FlowRecords?$filter=(bytes div packets) gt 100

# Modulo
GET /odata/FlowRecords?$filter=(bytes mod 1000) eq 0
```

### $select

Choose which properties to include in the response.

```http
# Single property
GET /odata/FlowRecords?$select=sourceIp

# Multiple properties
GET /odata/FlowRecords?$select=sourceIp,destinationIp,protocol

# All except specified (not supported in this implementation)
# Use explicit property list instead
```

### $orderby

Sort entities by one or more properties.

```http
# Single property ascending
GET /odata/FlowRecords?$orderby=timestamp

# Single property descending
GET /odata/FlowRecords?$orderby=timestamp desc

# Multiple properties
GET /odata/FlowRecords?$orderby=sourceIp asc,timestamp desc

# With other query options
GET /odata/FlowRecords?$filter=protocol eq 6&$orderby=bytes desc&$top=10
```

### $top

Limit the number of entities returned.

```http
# Get first 10 records
GET /odata/FlowRecords?$top=10

# Get first 100 records
GET /odata/FlowRecords?$top=100

# Combined with filter
GET /odata/FlowRecords?$filter=protocol eq 6&$top=50
```

### $skip

Skip a specified number of entities.

```http
# Skip first 20 records
GET /odata/FlowRecords?$skip=20

# Pagination example (page 3, 10 items per page)
GET /odata/FlowRecords?$skip=20&$top=10

# Combined with filter and sort
GET /odata/FlowRecords?$filter=bytes gt 1000&$orderby=timestamp desc&$skip=100&$top=50
```

### $count

Include the total count of entities.

```http
# Include count in response
GET /odata/FlowRecords?$count=true

# Count with filter
GET /odata/FlowRecords?$filter=protocol eq 6&$count=true

# Just the count (returns plain number)
GET /odata/FlowRecords/$count

# Count with filter (returns plain number)
GET /odata/FlowRecords/$count?$filter=bytes gt 1000
```

## Complex Query Examples

### Network Security Analysis

```http
# Large data transfers (potential data exfiltration)
GET /odata/FlowRecords?$filter=bytes gt 100000000&$orderby=bytes desc&$top=20

# Suspicious port scanning (many different destination ports from same source)
GET /odata/FlowRecords?$filter=sourceIp eq '192.168.1.100'&$select=destinationPort&$orderby=destinationPort

# After-hours network activity
GET /odata/FlowRecords?$filter=hour(timestamp) ge 22 or hour(timestamp) le 6&$orderby=timestamp desc

# High-frequency connections
GET /odata/FlowRecords?$filter=packets gt 1000 and duration lt 1000&$top=50
```

### Performance Monitoring

```http
# High bandwidth consumers
GET /odata/FlowRecords?$filter=bytes gt 10000000&$select=sourceIp,destinationIp,bytes,application&$orderby=bytes desc

# Short-lived connections
GET /odata/FlowRecords?$filter=duration lt 100&$count=true

# Protocol distribution
GET /odata/FlowRecords?$filter=protocol eq 6&$count=true  # TCP count
GET /odata/FlowRecords?$filter=protocol eq 17&$count=true # UDP count

# Application usage patterns
GET /odata/FlowRecords?$filter=contains(application,'HTTP')&$select=sourceIp,bytes&$orderby=bytes desc
```

### Traffic Analysis

```http
# Internal vs External traffic
GET /odata/FlowRecords?$filter=startswith(sourceIp,'192.168') and not startswith(destinationIp,'192.168')

# Peak usage periods
GET /odata/FlowRecords?$filter=hour(timestamp) ge 9 and hour(timestamp) le 17&$select=timestamp,bytes

# Top talkers by bytes
GET /odata/FlowRecords?$orderby=bytes desc&$top=100&$select=sourceIp,destinationIp,bytes

# Connection patterns
GET /odata/FlowRecords?$filter=sourceIp eq '192.168.1.100'&$select=destinationIp,protocol,application&$orderby=timestamp desc
```

## Error Handling

### Error Response Format

```json
{
  "error": {
    "code": "400",
    "message": "Invalid filter expression",
    "details": [
      {
        "code": "FILTER_PARSE_ERROR",
        "message": "Unable to parse filter expression: unexpected token 'invalidop'"
      }
    ]
  }
}
```

### Common Error Codes

| Code | Description | Example |
|------|-------------|---------|
| `400` | Bad Request | Invalid filter syntax |
| `404` | Not Found | Invalid entity set name |
| `500` | Internal Server Error | Database connection failure |

### Filter Expression Errors

```http
# Invalid operator
GET /odata/FlowRecords?$filter=protocol invalidop 6
# Response: 400 - Invalid operator 'invalidop'

# Invalid property name
GET /odata/FlowRecords?$filter=invalidProperty eq 'value'
# Response: 400 - Property 'invalidProperty' not found

# Type mismatch
GET /odata/FlowRecords?$filter=bytes eq 'text'
# Response: 400 - Cannot compare Long with String
```

## Response Formats

### JSON (Default)

```json
{
  "@odata.context": "http://localhost:8888/odata/$metadata#FlowRecords",
  "@odata.count": 150,
  "value": [
    {
      "flowId": "550e8400-e29b-41d4-a716-446655440000",
      "sourceIp": "192.168.1.100",
      "destinationIp": "10.0.0.50",
      "sourcePort": 443,
      "destinationPort": 58392,
      "protocol": 6,
      "bytes": 1024,
      "packets": 8,
      "timestamp": "2025-06-30T10:30:00.000Z",
      "duration": 1500,
      "tcpFlags": "ACK,PSH",
      "application": "HTTPS"
    }
  ]
}
```

### Metadata

```http
GET /odata/$metadata
```

Returns XML schema describing the entity model:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<edmx:Edmx xmlns:edmx="http://docs.oasis-open.org/odata/ns/edmx" Version="4.0">
  <edmx:DataServices>
    <Schema xmlns="http://docs.oasis-open.org/odata/ns/edm" Namespace="IPFIXService">
      <EntityContainer Name="Container">
        <EntitySet Name="FlowRecords" EntityType="IPFIXService.FlowRecord"/>
      </EntityContainer>
      <EntityType Name="FlowRecord">
        <Key>
          <PropertyRef Name="flowId"/>
        </Key>
        <Property Name="flowId" Type="Edm.String" Nullable="false"/>
        <Property Name="sourceIp" Type="Edm.String"/>
        <Property Name="destinationIp" Type="Edm.String"/>
        <!-- ... other properties ... -->
      </EntityType>
    </Schema>
  </edmx:DataServices>
</edmx:Edmx>
```

## Rate Limiting and Pagination

### Best Practices

1. **Use $top**: Limit result sets to reasonable sizes (recommended: 100-1000)
2. **Implement Pagination**: Use $skip and $top together for large datasets
3. **Filter Early**: Use $filter to reduce dataset size before sorting/pagination
4. **Select Only Needed Fields**: Use $select to reduce response size

### Pagination Pattern

```http
# Page 1 (first 100 records)
GET /odata/FlowRecords?$top=100&$skip=0&$count=true

# Page 2 (next 100 records)
GET /odata/FlowRecords?$top=100&$skip=100&$count=true

# Page 3 (next 100 records)
GET /odata/FlowRecords?$top=100&$skip=200&$count=true
```

## Performance Considerations

### Query Optimization

1. **Index-Friendly Filters**: Use equality filters on indexed fields
2. **Limit Result Sets**: Always use $top for large datasets
3. **Selective Projection**: Use $select to reduce network traffic
4. **Efficient Sorting**: Sort by indexed fields when possible

### Best Performing Queries

```http
# Good: Equality filter with small result set
GET /odata/FlowRecords?$filter=sourceIp eq '192.168.1.100'&$top=100

# Good: Range filter with sorting on indexed field
GET /odata/FlowRecords?$filter=timestamp ge 2025-06-30T00:00:00Z&$orderby=timestamp desc&$top=50

# Good: Selective projection
GET /odata/FlowRecords?$select=sourceIp,destinationIp,bytes&$top=1000
```

### Avoid for Large Datasets

```http
# Avoid: No limits on large datasets
GET /odata/FlowRecords

# Avoid: Complex string operations without other filters
GET /odata/FlowRecords?$filter=contains(sourceIp,'192')

# Avoid: Sorting without limits
GET /odata/FlowRecords?$orderby=timestamp desc
```

## Integration Examples

### JavaScript/TypeScript

```javascript
// Using fetch API
async function getFlowRecords(filter, top = 100) {
  const url = `http://localhost:8888/odata/FlowRecords?$filter=${encodeURIComponent(filter)}&$top=${top}`;
  const response = await fetch(url);
  return await response.json();
}

// Example usage
const tcpFlows = await getFlowRecords('protocol eq 6', 50);
const largeTransfers = await getFlowRecords('bytes gt 1000000', 20);
```

### Python

```python
import requests
import urllib.parse

def get_flow_records(filter_expr=None, top=100, select=None):
    url = "http://localhost:8888/odata/FlowRecords"
    params = {}
    
    if filter_expr:
        params['$filter'] = filter_expr
    if top:
        params['$top'] = top
    if select:
        params['$select'] = select
    
    response = requests.get(url, params=params)
    return response.json()

# Example usage
tcp_flows = get_flow_records(filter_expr='protocol eq 6', top=50)
large_transfers = get_flow_records(filter_expr='bytes gt 1000000', top=20)
```

### PowerBI/Excel

```
Data Source: OData Feed
URL: http://localhost:8888/odata/FlowRecords

Advanced Options:
- Include relationships: Checked
- Navigate using full hierarchy: Checked
```

For more information and examples, see the main [README.md](../README.md) and [performance tuning guide](performance-tuning.md).
