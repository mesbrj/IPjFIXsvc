# IPFIX OData Service (IPjFIXsvc)

> **High-Performance IPFIX Flow Data Service with Hexagonal Architecture and Flexible Persistence Strategies**

A production-ready, enterprise-grade service for processing and querying IPFIX (Internet Protocol Flow Information Export) flow records through a standards-compliant OData v4 API with embedded Apache Lucene (temp data storage/processing), Apache Solr, and Apache Ignite for high-speed in-memory processing.

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   OData Layer   │    │  Business Logic  │    │  Search Layer   │
│                 │    │  (Hexagonal Core)│    │                 │
├─────────────────┤    ├──────────────────┤    ├─────────────────┤
│ • $filter       │──> │ • FlowRecord     │──> │ Lucene Core /   │
│ • $orderby      │    │   Processing     │    │ Apache Solr     │
│ • $select       │    │ • User Management│    │                 │
│ • Complex Query │    │ • Search Logic   │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                ^                        ^
                                │                        │
                       ┌─────────────────┐    ┌─────────────────┐
                       │  Port/Adapter   │    │  Port/Adapter   │
                       │   Interface     │    │   Interface     │
                       ├─────────────────┤    ├─────────────────┤
                       │ Apache Ignite   │    │ SearchService   │
                       │ • In-memory     │    │ • Lucene Impl   │
                       │ • Distributed   │    │ • Solr Impl     │
                       │ • ACID Trans    │    │                 │
                       └─────────────────┘    └─────────────────┘
```

### **Complete OData v4 Implementation**

#### **Standard Query Operations**
- **`$filter`**: Complex filtering with boolean logic, comparison operators
- **`$select`**: Field projection for optimized data transfer
- **`$orderby`**: Multi-field sorting (ascending/descending)
- **`$top` / `$skip`**: Pagination support for large datasets
- **`$count`**: Record counting with optional filtering
- **`$expand`**: Related entity expansion (navigation properties)

#### **Advanced Filtering Capabilities**
- **Comparison Operators**: `eq`, `ne`, `gt`, `ge`, `lt`, `le`
- **Logical Operators**: `and`, `or`, `not`
- **String Functions**: `contains`, `startswith`, `endswith`
- **Arithmetic Operations**: `add`, `sub`, `mul`, `div`, `mod`
- **Date/Time Functions**: `year`, `month`, `day`, `hour`, `minute`, `second`
- **Collection Functions**: `any`, `all` for filtering collections

#### **Data Format Support**
- **JSON**: Default format with full metadata support
- **XML**: Alternative format
- **Custom Media Types**: Extensible content negotiation
- **Metadata**: `$metadata` endpoint for service discovery

## Testing

### Prerequisites
- **Java 21+** (OpenJDK recommended)
- **Maven 3.9+**
- **Docker & Docker Compose** (for containerized deployment)
- **4GB+ RAM** (recommended for optimal performance)

### Building 
```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Generate documentation
mvn site
```

```bash
git clone <repository-url>
cd IPjFIXsvc
mvn clean package -DskipTests
```

### Run Locally
```bash
# Development mode
mvn spring-boot:run

# Production mode with optimized JVM
java -jar target/ipjfix-svc-*.jar \
  --spring.profiles.active=production
```

### Verify Installation
```bash
# Health check
curl http://localhost:8888/actuator/health

# OData service document
curl http://localhost:8888/odata/

# Sample data query
curl "http://localhost:8888/odata/FlowRecords?\$top=5"
```

## OData API Usage Examples

### Basic Queries
```bash
# Get all flow records
GET /odata/FlowRecords

# Get specific fields only
GET /odata/FlowRecords?$select=sourceIp,destinationIp,protocol

# Get first 10 records
GET /odata/FlowRecords?$top=10

# Skip 20 records and get next 10
GET /odata/FlowRecords?$skip=20&$top=10

# Count total records
GET /odata/FlowRecords?$count=true
```

### Advanced Filtering
```bash
# Filter by source IP
GET /odata/FlowRecords?$filter=sourceIp eq '192.168.1.100'

# Multiple conditions with AND
GET /odata/FlowRecords?$filter=sourceIp eq '192.168.1.100' and protocol eq 6

# Range queries
GET /odata/FlowRecords?$filter=bytes gt 1000 and bytes lt 10000

# String operations
GET /odata/FlowRecords?$filter=contains(sourceIp,'192.168')

# Date/time filtering
GET /odata/FlowRecords?$filter=timestamp gt 2025-06-01T00:00:00Z

# Complex boolean logic
GET /odata/FlowRecords?$filter=(protocol eq 6 or protocol eq 17) and bytes gt 500
```

### Sorting and Aggregation
```bash
# Sort by timestamp (descending)
GET /odata/FlowRecords?$orderby=timestamp desc

# Multiple sort fields
GET /odata/FlowRecords?$orderby=sourceIp asc,timestamp desc

# Combined operations
GET /odata/FlowRecords?$filter=protocol eq 6&$orderby=bytes desc&$top=100&$select=sourceIp,destinationIp,bytes
```

### Metadata and Service Discovery
```bash
# Service metadata
GET /odata/$metadata

# Service document
GET /odata/

# Entity set schema
GET /odata/FlowRecords/$metadata
```

## Configuration

### **Deployment Mode Selection**
```properties
# Standalone mode (embedded Lucene)
search.provider=lucene
lucene.index.path=./data/lucene-indices
lucene.fallback.memory=true

# Cluster mode (external Solr)
search.provider=solr
solr.url=http://solr-cluster:8983/solr
solr.collection=ipfix-flows
solr.connection.timeout=5000

# Hybrid mode (standalone + external Solr)
search.provider=solr
solr.url=http://external-solr:8983/solr
solr.collection=shared-ipfix-flows
```

### **Ignite Configuration**
```properties
# Enable/disable Ignite
ignite.enabled=true
ignite.instance.name=ipfix-ignite-instance
ignite.work.directory=/tmp/ignite-work

# Cluster discovery (for HA deployments)
ignite.discovery.addresses=node1:47500,node2:47500,node3:47500
```

### **Application Profiles**
```bash
# Development with embedded Lucene
./mvnw spring-boot:run -Dspring.profiles.active=dev,lucene

# Production standalone
./mvnw spring-boot:run -Dspring.profiles.active=prod,standalone

# Production cluster with Solr
./mvnw spring-boot:run -Dspring.profiles.active=prod,cluster,solr
```

### **Application Properties**
```properties
# Server Configuration
server.port=8888
server.servlet.context-path=/

# Ignite Configuration
ignite.enabled=true
ignite.instance.name=ipfix-ignite-instance
ignite.work.directory=/tmp/ignite-work

# Lucene Configuration
lucene.index.path=./data/lucene-indices
lucene.fallback.memory=true

# OData Configuration
odata.service.base-path=/odata

# Performance Tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### Environment Variables
```bash
# JVM Memory Settings
export JVM_INITIAL_HEAP=512m
export JVM_MAX_HEAP=2g

# Application Profiles
export SPRING_PROFILES_ACTIVE=production,monitoring

# Storage Paths
export IGNITE_WORK_DIRECTORY=/opt/ipfix/ignite
export LUCENE_INDEX_PATH=/opt/ipfix/lucene
```

### Flow Record Schema (test)
```json
{
  "flowId": "uuid",
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
```
