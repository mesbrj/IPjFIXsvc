# IPFIX Service Architecture Guide

## Overview

The IPFIX OData Service implements a **hexagonal architecture** (also known as ports and adapters pattern) that provides exceptional flexibility in deployment scenarios. This architectural approach enables seamless switching between different persistence technologies without affecting the core business logic.

## Hexagonal Architecture Implementation

### Core Principles

The hexagonal architecture ensures clean separation of concerns by organizing the system into distinct layers:

```
                    ┌─────────────────────────────────────┐
                    │      External World                 │
                    │                                     │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │       Web, CLI/Terminal - Adapters  │
                    │  (OData Controller, REST APIs, ...) │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │      input ports                    │
                    │      Analysis, queries, commands... │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │       Business Logic Core / Models  │
                    │                                     │
                    │  ┌─────────────────────────────┐    │
                    │  │   FlowRecord Processing     │    │
                    │  │   User Management           │    │
                    │  │   Search Algorithms         │    │
                    │  │   Query Optimization        │    │
                    │  └─────────────────────────────┘    │
                    │                                     │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼─────────────────────────────────────────┐
                    │        Infrastructure Ports                           │
                    │  (SearchService, CacheService, in_memory data, ipfix) │
                    └─────────────┬─────────────────────────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │       Infrastructure Adapters       │
                    │                                     │
                    │  ┌──────────────┐ ┌─────────────┐   │
                    │  │    Lucene    │ │    Solr     │   │
                    │  │   Adapter    │ │  Adapter    │   │
                    │  └──────────────┘ └─────────────┘   │
                    │                                     │
                    │  ┌──────────────┐ ┌─────────────┐   │
                    │  │   Ignite     │ │  IPyFIXweb  │   │
                    │  │   Adapter    │ │    APIs     │   │
                    │  └──────────────┘ └─────────────┘   │
                    └─────────────────────────────────────┘
```

### Architecture Benefits

1. **Technology Independence**: Core business logic is completely isolated from infrastructure concerns
2. **Testability**: Easy to test business logic with mock adapters
3. **Flexibility**: Simple configuration changes enable different deployment modes
4. **Maintainability**: Clear separation of concerns reduces coupling
5. **Evolution**: Easy to add new persistence technologies without touching business logic

## Persistence Strategy Architecture

### Deployment Patterns

The service supports three primary deployment patterns, each optimized for different use cases. The hexagonal architecture enables switching between these patterns through simple configuration changes:

#### 1. **Standalone Mode (Lucene Core)**

```
┌─────────────────────────────────────────────────┐
│              Single Instance                    │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │         IPFIX Service                   │   │
│  │                                         │   │
│  │  ┌─────────────┐  ┌─────────────────┐  │   │
│  │  │   Ignite    │  │   Lucene Core   │  │   │
│  │  │   Cache     │  │   (Embedded)    │  │   │
│  │  │             │  │                 │  │   │
│  │  │ • Users     │  │ • FlowRecords   │  │   │
│  │  │ • Metadata  │  │ • Full-text     │  │   │
│  │  │ • Sessions  │  │ • Real-time     │  │   │
│  │  └─────────────┘  └─────────────────┘  │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Local Storage: ./data/                         │
└─────────────────────────────────────────────────┘

Configuration:
search.provider=lucene
lucene.index.path=./data/lucene-indices

Use Cases:
• Development and testing
• Small to medium deployments (< 1M records)
• Edge computing scenarios
• Embedded systems
• Quick proof of concepts
```

#### 2. **Cluster Mode (Solr Distributed)**

```
┌─────────────────────────────────────────────────────────────────┐
│                    Load Balancer                               │
└─────────────┬───────────────┬───────────────┬───────────────────┘
              │               │               │
┌─────────────▼─┐   ┌─────────▼─┐   ┌─────────▼─┐
│ IPFIX Node 1  │   │ IPFIX Node 2│   │ IPFIX Node 3│
│               │   │             │   │             │
│ ┌───────────┐ │   │ ┌─────────┐ │   │ ┌─────────┐ │
│ │  Ignite   │ │   │ │ Ignite  │ │   │ │ Ignite  │ │
│ │ Cluster   │◄┼───┼►│Cluster  │◄┼───┼►│Cluster  │ │
│ └───────────┘ │   │ └─────────┘ │   │ └─────────┘ │
│               │   │             │   │             │
│ ┌───────────┐ │   │ ┌─────────┐ │   │ ┌─────────┐ │
│ │Solr Client│ │   │ │Solr     │ │   │ │Solr     │ │
│ │           │ │   │ │Client   │ │   │ │Client   │ │
│ └─────┬─────┘ │   │ └────┬────┘ │   │ └────┬────┘ │
└───────┼───────┘   └──────┼──────┘   └──────┼──────┘
        │                  │                 │
        └──────────────────┼─────────────────┘
                           │
┌──────────────────────────▼──────────────────────────┐
│                Solr Cluster                        │
│                                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ Solr Node 1 │  │ Solr Node 2 │  │ Solr Node 3 │ │
│  │             │  │             │  │             │ │
│  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │ │
│  │ │ Shard 1 │ │  │ │ Shard 2 │ │  │ │ Shard 3 │ │ │
│  │ │Replica A│ │  │ │Replica A│ │  │ │Replica A│ │ │
│  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │ │
│  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │ │
│  │ │ Shard 2 │ │  │ │ Shard 3 │ │  │ │ Shard 1 │ │ │
│  │ │Replica B│ │  │ │Replica B│ │  │ │Replica B│ │ │
│  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │ │
│  └─────────────┘  └─────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────┘

Configuration:
search.provider=solr
solr.url=http://solr-cluster:8983/solr
solr.collection=ipfix-flows

Use Cases:
• High availability production systems
• Large-scale data processing (10M+ records)
• Enterprise deployments
• Multi-tenant environments
• Global distributed systems
```

#### 3. **Hybrid Mode (Standalone + External Solr)**

```
┌─────────────────────────────────────────────────┐
│              Single Instance                    │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │         IPFIX Service                   │   │
│  │                                         │   │
│  │  ┌─────────────┐  ┌─────────────────┐  │   │
│  │  │   Ignite    │  │   Solr Client   │  │   │
│  │  │   Cache     │  │                 │  │   │
│  │  │             │  │ • External Solr │  │   │
│  │  │ • Users     │  │ • Shared Index  │  │   │
│  │  │ • Metadata  │  │ • Advanced      │  │   │
│  │  │ • Sessions  │  │   Features      │  │   │
│  │  └─────────────┘  └────────┬────────┘  │   │
│  └─────────────────────────────┼───────────┘   │
└─────────────────────────────────┼───────────────┘
                                  │
                   Network        │
                                  │
┌─────────────────────────────────▼───────────────┐
│            External Solr Cluster               │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐              │
│  │ Solr Node 1 │  │ Solr Node 2 │              │
│  │             │  │             │              │
│  │ • Shared by │  │ • Shared by │              │
│  │   multiple  │  │   multiple  │              │
│  │   services  │  │   services  │              │
│  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────┘

Configuration:
search.provider=solr
solr.url=http://external-solr:8983/solr
solr.collection=shared-ipfix-flows

Use Cases:
• Shared search infrastructure
• Cost optimization scenarios
• Gradual migration to clusters
• Multi-service architectures
• Resource consolidation
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
                                    ┌─────────▼─────────┐
                                    │   Cost-Based      │
                                    │   Optimizer       │
                                    │                   │
                                    │ • Index Selection │
                                    │ • Join Strategy   │
                                    │ • Cache Utilization│
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

## Port and Adapter Pattern Implementation

### Port Interfaces

The hexagonal architecture defines clear contracts between the business logic and external systems:

#### **Primary Ports (Inbound)**
```java
// OData API entry point
public interface ODataController {
    ResponseEntity<?> getFlowRecords(String filter, String orderBy, Integer top);
    ResponseEntity<?> getUsers(String filter);
    ResponseEntity<?> getMetadata();
}

// REST API for direct access
public interface FlowRecordController {
    List<FlowRecord> findFlowRecords(SearchCriteria criteria);
    FlowRecord getFlowRecord(String id);
}
```

#### **Secondary Ports (Outbound)**
```java
// Search abstraction - core interface
public interface SearchService {
    List<FlowRecord> searchFlowRecords(String tenantId, String query);
    void indexFlowRecord(FlowRecord record);
    void deleteFlowRecord(String id);
    long count(String tenantId, String query);
    void createIndex(String tenantId);
}

// Cache abstraction
public interface CacheService {
    void put(String key, Object value);
    <T> T get(String key, Class<T> type);
    void evict(String key);
    void clear();
}

// Repository abstraction
public interface FlowRecordRepository {
    void save(FlowRecord record);
    Optional<FlowRecord> findById(String id);
    List<FlowRecord> findByTenant(String tenantId);
}
```

### Adapter Implementations

#### **Lucene Adapter** (Standalone Mode)
```java
@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "lucene")
@Profile({"standalone", "dev", "test"})
public class LuceneSearchService implements SearchService {
    
    private final LuceneIndexManager indexManager;
    private final LuceneQueryBuilder queryBuilder;
    
    @Override
    public List<FlowRecord> searchFlowRecords(String tenantId, String query) {
        try {
            IndexSearcher searcher = indexManager.getSearcher(tenantId);
            Query luceneQuery = queryBuilder.buildQuery(query);
            TopDocs results = searcher.search(luceneQuery, 1000);
            
            return Arrays.stream(results.scoreDocs)
                .map(scoreDoc -> documentToFlowRecord(searcher.doc(scoreDoc.doc)))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new SearchException("Lucene search failed", e);
        }
    }
    
    @Override
    public void indexFlowRecord(FlowRecord record) {
        try {
            IndexWriter writer = indexManager.getWriter(record.getTenantId());
            Document doc = flowRecordToDocument(record);
            writer.addDocument(doc);
            writer.commit();
        } catch (IOException e) {
            throw new SearchException("Lucene indexing failed", e);
        }
    }
    
    // Additional implementation details...
}
```

#### **Solr Adapter** (Cluster/Hybrid Mode)
```java
@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "solr")
@Profile({"cluster", "hybrid", "prod"})
public class SolrSearchService implements SearchService {
    
    private final SolrClient solrClient;
    private final SolrQueryBuilder queryBuilder;
    
    @Value("${solr.collection}")
    private String collectionName;
    
    @Override
    public List<FlowRecord> searchFlowRecords(String tenantId, String query) {
        try {
            SolrQuery solrQuery = queryBuilder.buildQuery(query, tenantId);
            QueryResponse response = solrClient.query(collectionName, solrQuery);
            
            return response.getBeans(FlowRecord.class);
        } catch (SolrServerException | IOException e) {
            throw new SearchException("Solr search failed", e);
        }
    }
    
    @Override
    public void indexFlowRecord(FlowRecord record) {
        try {
            solrClient.addBean(collectionName, record);
            solrClient.commit(collectionName);
        } catch (SolrServerException | IOException e) {
            throw new SearchException("Solr indexing failed", e);
        }
    }
    
    // Additional implementation details...
}
```

#### **Ignite Cache Adapter** (All Modes)
```java
@Component
@ConditionalOnProperty(name = "ignite.enabled", havingValue = "true", matchIfMissing = true)
public class IgniteCacheService implements CacheService {
    
    private final Ignite ignite;
    
    @Override
    public void put(String key, Object value) {
        IgniteCache<String, Object> cache = ignite.getOrCreateCache("default");
        cache.put(key, value);
    }
    
    @Override
    public <T> T get(String key, Class<T> type) {
        IgniteCache<String, Object> cache = ignite.getOrCreateCache("default");
        Object value = cache.get(key);
        return type.cast(value);
    }
    
    // Additional implementation details...
}
```

## Configuration-Driven Adapter Selection

### Spring Configuration
```java
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class PersistenceConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "search.provider", havingValue = "lucene")
    public SearchService luceneSearchService(LuceneProperties properties) {
        return new LuceneSearchService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "search.provider", havingValue = "solr")
    public SearchService solrSearchService(SolrProperties properties) {
        return new SolrSearchService(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean(SearchService.class)
    public SearchService memorySearchService() {
        return new InMemorySearchService(); // Fallback implementation
    }
}
```

### Configuration Properties
```java
@ConfigurationProperties(prefix = "search")
@Data
public class SearchProperties {
    private String provider = "lucene"; // Default to Lucene
    private LuceneProperties lucene = new LuceneProperties();
    private SolrProperties solr = new SolrProperties();
}

@Data
public class LuceneProperties {
    private String indexPath = "./data/lucene-indices";
    private boolean fallbackMemory = true;
    private int ramBufferSizeMB = 256;
    private int maxMergeMB = 500;
}

@Data
public class SolrProperties {
    private String url = "http://localhost:8983/solr";
    private String collection = "ipfix-flows";
    private int connectionTimeout = 5000;
    private int socketTimeout = 10000;
    private int maxConnections = 100;
}
```
