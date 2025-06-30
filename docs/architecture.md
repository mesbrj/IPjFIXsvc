# Architecture Overview

## System Architecture

The IPFIX OData Service implements a modern, cloud-native architecture designed for high performance, scalability, and maintainability. The system follows Domain-Driven Design (DDD) principles with a clean architecture pattern.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  Web Browsers  │  BI Tools  │  Applications  │  API Clients    │
│  (JavaScript)  │ (PowerBI)  │   (Python)     │    (REST)       │
└─────────────────────────────────────────────────────────────────┘
                                    │
                            ┌───────▼───────┐
                            │   Load        │
                            │  Balancer     │
                            │  (Optional)   │
                            └───────┬───────┘
                                    │
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                         │
├─────────────────────────────────────────────────────────────────┤
│                     OData v4 Endpoint                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │   $filter   │ │   $select   │ │  $orderby   │   Query      │
│  │   $expand   │ │    $top     │ │   $count    │  Processing  │
│  │    $skip    │ │  $metadata  │ │   $batch    │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                                    │
┌─────────────────────────────────────────────────────────────────┐
│                   Application Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │   Spring    │ │   Apache    │ │  Security   │              │
│  │    Boot     │ │   Olingo    │ │  & Audit    │              │
│  │  Framework  │ │  OData v4   │ │  Logging    │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                                    │
┌─────────────────────────────────────────────────────────────────┐
│                    Domain Layer                                │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │   Flow      │ │   Query     │ │  Business   │              │
│  │  Records    │ │ Processing  │ │   Rules     │              │
│  │  Entities   │ │   Engine    │ │ Validation  │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                                    │
┌─────────────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                           │
├─────────────────────────────────────────────────────────────────┤
│            Dual Persistence Architecture                       │
│  ┌─────────────────────────┐ ┌─────────────────────────┐      │
│  │     Apache Ignite       │ │     Apache Lucene       │      │
│  │   In-Memory Database    │ │    Search Engine        │      │
│  │                         │ │                         │      │
│  │ • Real-time Processing  │ │ • Full-text Search      │      │
│  │ • Distributed Caching   │ │ • Advanced Indexing     │      │
│  │ • ACID Transactions     │ │ • Analytics Engine      │      │
│  │ • SQL-like Queries      │ │ • Faceted Search        │      │
│  │ • Horizontal Scaling    │ │ • Relevance Scoring     │      │
│  │ • Persistent Storage    │ │ • Real-time Updates     │      │
│  └─────────────────────────┘ └─────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. Presentation Layer (Adapters)

#### OData Controller (`ODataController.java`)
- **Responsibility**: HTTP request handling and OData protocol implementation
- **Framework**: Spring Boot with Apache Olingo
- **Features**:
  - RESTful endpoint exposition
  - Content negotiation (JSON/XML)
  - Error handling and validation
  - Security integration

#### OData Processor (`IpfixEntityCollectionProcessor.java`)
- **Responsibility**: OData query processing and result formatting
- **Key Functions**:
  - Query parsing and validation
  - Filter expression evaluation
  - Result set pagination
  - Metadata generation

### 2. Application Layer (Use Cases)

#### Query Processing Engine
- **Filter Processing**: Complex boolean expression evaluation
- **Aggregation Engine**: Statistical operations and grouping
- **Caching Strategy**: Multi-level caching implementation
- **Performance Optimization**: Query plan optimization

#### Business Logic
- **Data Validation**: Input sanitization and business rule enforcement
- **Flow Classification**: Application detection and categorization
- **Security Policies**: Access control and audit logging

### 3. Domain Layer (Core)

#### Flow Record Entity
```java
public class FlowRecord {
    private String flowId;           // Unique identifier
    private String sourceIp;         // Source IP address
    private String destinationIp;    // Destination IP address
    private Integer sourcePort;      // Source port
    private Integer destinationPort; // Destination port
    private Integer protocol;        // IP protocol (TCP=6, UDP=17, etc.)
    private Long bytes;             // Total bytes
    private Long packets;           // Total packets
    private Instant timestamp;      // Flow start time
    private Integer duration;       // Flow duration (ms)
    private String tcpFlags;        // TCP flags
    private String application;     // Detected application
}
```

#### Domain Services
- **Flow Aggregation**: Statistical analysis and reporting
- **Pattern Detection**: Anomaly detection and classification
- **Data Enrichment**: GeoIP, DNS resolution, application detection

### 4. Infrastructure Layer (Adapters)

#### Apache Ignite Adapter
```java
@Configuration
public class IgniteConfig {
    @Bean
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setClusterStateOnStart(ClusterState.ACTIVE);
        cfg.setDataStorageConfiguration(dataStorageConfig());
        cfg.setCacheConfiguration(cacheConfigurations());
        return Ignition.start(cfg);
    }
}
```

**Features**:
- **In-Memory Processing**: Sub-millisecond data access
- **Distributed Computing**: Cluster-aware processing
- **Persistence**: Write-through/write-behind to disk
- **SQL Support**: Standard SQL queries
- **Indexing**: Automatic index management
- **Transactions**: ACID compliance

#### Apache Lucene Adapter
```java
@Configuration
public class LuceneConfig {
    @Bean
    public IndexWriter indexWriter() throws IOException {
        Directory directory = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(directory, config);
    }
}
```

**Features**:
- **Full-Text Search**: Advanced text analysis
- **Real-Time Indexing**: Immediate search availability
- **Faceted Search**: Multi-dimensional filtering
- **Analytics**: Statistical aggregations
- **Custom Scoring**: Relevance tuning

## Data Flow Architecture

### Write Path (Data Ingestion)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   IPFIX     │───▶│   Data      │───▶│   Dual      │
│   Flows     │    │ Validation  │    │   Write     │
│ (External)  │    │ Transform   │    │ Strategy    │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                           ┌──────────────────┼──────────────────┐
                           │                  │                  │
                   ┌───────▼──────┐  ┌────────▼────────┐  ┌─────▼─────┐
                   │    Ignite    │  │     Lucene      │  │   Cache   │
                   │   Primary    │  │    Search       │  │  Update   │
                   │   Storage    │  │    Index        │  │           │
                   └──────────────┘  └─────────────────┘  └───────────┘
```

### Read Path (Query Processing)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   OData     │───▶│   Query     │───▶│   Storage   │
│   Query     │    │ Processing  │    │  Selection  │
│ (Client)    │    │ & Planning  │    │  Strategy   │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                           ┌──────────────────┼──────────────────┐
                           │                  │                  │
                   ┌───────▼──────┐  ┌────────▼────────┐  ┌─────▼─────┐
                   │    Ignite    │  │     Lucene      │  │   Memory  │
                   │ (Structured  │  │ (Full-text &    │  │  Fallback │
                   │  Queries)    │  │  Analytics)     │  │           │
                   └──────┬───────┘  └────────┬────────┘  └─────┬─────┘
                          │                   │                 │
                          └───────────────────┼─────────────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │    Response       │
                                    │   Aggregation     │
                                    │   & Formatting    │
                                    └───────────────────┘
```

## Persistence Strategy

### Primary Storage: Apache Ignite

**Use Cases**:
- Real-time data access
- Complex relational queries
- ACID transactions
- Distributed computing

**Configuration**:
```java
@CacheConfig(names = "flowRecords")
public class FlowRecordCache {
    // Cache configuration
    private CacheConfiguration<String, FlowRecord> cacheConfig() {
        CacheConfiguration<String, FlowRecord> cfg = new CacheConfiguration<>();
        cfg.setName("flowRecords");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        cfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        cfg.setIndexedTypes(String.class, FlowRecord.class);
        return cfg;
    }
}
```

### Secondary Storage: Apache Lucene

**Use Cases**:
- Full-text search
- Complex analytics
- Faceted search
- Pattern matching

**Index Schema**:
```java
public class FlowRecordIndexer {
    private Document createDocument(FlowRecord flow) {
        Document doc = new Document();
        doc.add(new StringField("flowId", flow.getFlowId(), Field.Store.YES));
        doc.add(new TextField("sourceIp", flow.getSourceIp(), Field.Store.YES));
        doc.add(new TextField("destinationIp", flow.getDestinationIp(), Field.Store.YES));
        doc.add(new LongPoint("bytes", flow.getBytes()));
        doc.add(new LongPoint("timestamp", flow.getTimestamp().toEpochMilli()));
        doc.add(new TextField("application", flow.getApplication(), Field.Store.YES));
        return doc;
    }
}
```

### Fallback Strategy: In-Memory

**Use Cases**:
- Development environment
- Testing scenarios
- Degraded mode operation

## Query Processing Architecture

### OData Query Pipeline

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   HTTP      │───▶│   OData     │───▶│   Query     │───▶│   Storage   │
│  Request    │    │  Parsing    │    │ Planning    │    │  Execution  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │   Cost-Based      │
                                    │   Optimizer       │
                                    │                   │
                                    │ • Index Selection │
                                    │ • Join Strategy   │
                                    │ • Cache Utilization│
                                    └───────────────────┘
```

### Filter Processing

```java
public class FilterProcessor {
    public Predicate<FlowRecord> processFilter(String filterExpression) {
        // Parse OData filter expression
        FilterExpression expr = parseFilter(filterExpression);
        
        // Convert to predicate
        return convertToPredicate(expr);
    }
    
    private boolean evaluateComparison(Object left, String operator, Object right) {
        switch (operator) {
            case "eq": return Objects.equals(left, right);
            case "ne": return !Objects.equals(left, right);
            case "gt": return compare(left, right) > 0;
            case "ge": return compare(left, right) >= 0;
            case "lt": return compare(left, right) < 0;
            case "le": return compare(left, right) <= 0;
            default: throw new UnsupportedOperationException("Unknown operator: " + operator);
        }
    }
}
```

## Security Architecture

### Authentication & Authorization

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │───▶│   Spring    │───▶│   Method    │
│ Credentials │    │  Security   │    │  Security   │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │   Role-Based      │
                                    │   Access Control  │
                                    │                   │
                                    │ • Admin: Full     │
                                    │ • User: Read-Only │
                                    │ • Guest: Limited  │
                                    └───────────────────┘
```

### Audit Logging

```java
@Component
public class AuditLogger {
    public void logQuery(String user, String query, long executionTime) {
        AuditEvent event = AuditEvent.builder()
            .timestamp(Instant.now())
            .user(user)
            .operation("QUERY")
            .details(query)
            .executionTime(executionTime)
            .build();
        
        auditRepository.save(event);
    }
}
```

## Monitoring Architecture

### Metrics Collection

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Application │───▶│  Micrometer │───▶│ Prometheus  │
│   Metrics   │    │   Registry  │    │   Server    │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │     Grafana       │
                                    │   Dashboards      │
                                    │                   │
                                    │ • JVM Metrics     │
                                    │ • Query Performance│
                                    │ • Storage Stats   │
                                    └───────────────────┘
```

### Health Checks

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // Check Ignite cluster
            boolean igniteHealthy = ignite.cluster().active();
            
            // Check Lucene index
            boolean luceneHealthy = indexReader.numDocs() >= 0;
            
            if (igniteHealthy && luceneHealthy) {
                return Health.up()
                    .withDetail("ignite.status", "UP")
                    .withDetail("lucene.status", "UP")
                    .withDetail("records.count", getTotalRecords())
                    .build();
            } else {
                return Health.down()
                    .withDetail("ignite.status", igniteHealthy ? "UP" : "DOWN")
                    .withDetail("lucene.status", luceneHealthy ? "UP" : "DOWN")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

## Scalability Architecture

### Horizontal Scaling

```
┌─────────────────────────────────────────────────────────────────┐
│                      Load Balancer                             │
│                    (Nginx/HAProxy)                             │
└─────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
┌───────────────▼────┐ ┌────────▼────┐ ┌────────▼────┐
│   Service Node 1   │ │Service Node 2│ │Service Node 3│
│                    │ │              │ │              │
│ • OData Endpoint   │ │• OData Endpoint│ │• OData Endpoint│
│ • Query Processing │ │• Query Processing│ │• Query Processing│
│ • Local Caching    │ │• Local Caching │ │• Local Caching │
└────────────────────┘ └──────────────┘ └──────────────┘
                │               │               │
                └───────────────┼───────────────┘
                                │
┌─────────────────────────────────────────────────────────────────┐
│                 Shared Ignite Cluster                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │ Ignite Node │ │ Ignite Node │ │ Ignite Node │              │
│  │      1      │ │      2      │ │      3      │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

### Data Partitioning Strategy

```java
@Configuration
public class DataPartitioningConfig {
    @Bean
    public CacheConfiguration<String, FlowRecord> partitionedCacheConfig() {
        CacheConfiguration<String, FlowRecord> cfg = new CacheConfiguration<>();
        cfg.setName("flowRecords");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        
        // Partition by source IP for better locality
        cfg.setAffinity(new RendezvousAffinityFunction(false, 1024));
        
        return cfg;
    }
}
```

## Deployment Architecture

### Container Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Host                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐  │
│  │   Application   │ │   Monitoring    │ │   Storage       │  │
│  │   Container     │ │   Container     │ │   Container     │  │
│  │                 │ │                 │ │                 │  │
│  │ • Java 21 JRE   │ │ • Prometheus    │ │ • Volume Mounts │  │
│  │ • G1GC Tuned    │ │ • Grafana       │ │ • Data Persist  │  │
│  │ • Non-root User │ │ • Alertmanager  │ │ • Backup Jobs   │  │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ipfix-odata-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: ipfix-odata-service
        image: ipfix-odata-service:v1.0.0
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8888
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8888
          initialDelaySeconds: 60
          periodSeconds: 30
```

This architecture provides a solid foundation for a high-performance, scalable IPFIX OData service that can handle enterprise-level workloads while maintaining data consistency and providing excellent query performance through the dual persistence strategy.
