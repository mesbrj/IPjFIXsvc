# Solr Configuration Guide for IPFIX Service

This guide explains how to configure and deploy the IPFIX OData Service with Apache Solr for cluster and high-availability scenarios.

## Overview

The IPFIX service supports two search engine options through its hexagonal architecture:
- **Apache Lucene Core**: Embedded search for standalone deployments
- **Apache Solr**: Distributed search for cluster and high-availability deployments

## Quick Start with Solr

### 1. **Configure for Solr**
```properties
# application.properties
search.provider=solr
solr.url=http://localhost:8983/solr
solr.collection=ipfix-flows
solr.connection.timeout=5000
solr.socket.timeout=10000
```

### 2. **Start Solr and Create Collection**
```bash
# Start Solr
docker run -d -p 8983:8983 --name solr solr:9.3

# Create collection
docker exec solr solr create_core -c ipfix-flows

# Or for SolrCloud
docker exec solr solr create -c ipfix-flows -s 2 -rf 2
```

### 3. **Configure Schema**
```bash
# Create schema configuration
curl -X POST -H 'Content-type:application/json' \
  --data-binary @solr-schema.json \
  http://localhost:8983/solr/ipfix-flows/schema
```

### 4. **Start IPFIX Service**
```bash
./mvnw spring-boot:run -Dspring.profiles.active=solr
```

## Detailed Configuration

### Collection Schema

The IPFIX service requires specific fields in the Solr collection. Create the schema using:

```json
{
  "add-field": [
    {
      "name": "id",
      "type": "string",
      "indexed": true,
      "stored": true,
      "required": true
    },
    {
      "name": "tenantId",
      "type": "string",
      "indexed": true,
      "stored": true,
      "required": true
    },
    {
      "name": "sourceIP",
      "type": "string",
      "indexed": true,
      "stored": true
    },
    {
      "name": "destIP",
      "type": "string",
      "indexed": true,
      "stored": true
    },
    {
      "name": "sourcePort",
      "type": "pint",
      "indexed": true,
      "stored": true
    },
    {
      "name": "destPort",
      "type": "pint",
      "indexed": true,
      "stored": true
    },
    {
      "name": "protocol",
      "type": "string",
      "indexed": true,
      "stored": true
    },
    {
      "name": "bytes",
      "type": "plong",
      "indexed": true,
      "stored": true
    },
    {
      "name": "packets",
      "type": "plong",
      "indexed": true,
      "stored": true
    },
    {
      "name": "flowStartTime",
      "type": "pdate",
      "indexed": true,
      "stored": true
    },
    {
      "name": "flowEndTime",
      "type": "pdate",
      "indexed": true,
      "stored": true
    },
    {
      "name": "duration",
      "type": "pint",
      "indexed": true,
      "stored": true
    },
    {
      "name": "tcpFlags",
      "type": "string",
      "indexed": true,
      "stored": true
    },
    {
      "name": "application",
      "type": "string",
      "indexed": true,
      "stored": true
    }
  ]
}
```

### Advanced Configuration

#### **SolrCloud Configuration**
```xml
<!-- solrconfig.xml -->
<config>
  <luceneMatchVersion>9.3.0</luceneMatchVersion>
  
  <requestHandler name="/select" class="solr.SearchHandler">
    <lst name="defaults">
      <str name="echoParams">explicit</str>
      <int name="rows">100</int>
      <str name="df">text</str>
    </lst>
  </requestHandler>
  
  <!-- Update handler for real-time indexing -->
  <updateHandler class="solr.DirectUpdateHandler2">
    <updateLog>
      <str name="dir">${solr.ulog.dir:}</str>
      <int name="numVersionBuckets">${solr.ulog.numVersionBuckets:65536}</int>
    </updateLog>
    <autoCommit>
      <maxTime>30000</maxTime>
      <openSearcher>false</openSearcher>
    </autoCommit>
    <autoSoftCommit>
      <maxTime>1000</maxTime>
    </autoSoftCommit>
  </updateHandler>
</config>
```

### Performance Tuning

#### **JVM Settings for Solr**
```bash
# For Solr nodes
SOLR_JAVA_MEM="-Xms4g -Xmx4g"
SOLR_OPTS="$SOLR_OPTS -XX:+UseG1GC"
SOLR_OPTS="$SOLR_OPTS -XX:+UnlockExperimentalVMOptions"
SOLR_OPTS="$SOLR_OPTS -XX:G1HeapRegionSize=16m"
SOLR_OPTS="$SOLR_OPTS -XX:+UseStringDeduplication"
```

#### **Collection Configuration**
```bash
# Create optimized collection
solr create -c ipfix-flows \
  -s 3 \
  -rf 2 \
  -p 8983 \
  -d /opt/solr/server/solr/configsets/ipfix_config
```

## Production Deployment

### Docker Compose Setup
```yaml
version: '3.8'
services:
  zookeeper:
    image: zookeeper:3.8
    hostname: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOO_MY_ID: 1
      ZOO_SERVERS: server.1=0.0.0.0:2888:3888;2181

  solr1:
    image: solr:9.3
    hostname: solr1
    ports:
      - "8983:8983"
    environment:
      - ZK_HOST=zookeeper:2181
      - SOLR_HOST=solr1
      - SOLR_PORT=8983
      - SOLR_JAVA_MEM=-Xms2g -Xmx2g
    depends_on:
      - zookeeper
    volumes:
      - solr1_data:/var/solr

  solr2:
    image: solr:9.3
    hostname: solr2
    ports:
      - "8984:8983"
    environment:
      - ZK_HOST=zookeeper:2181
      - SOLR_HOST=solr2
      - SOLR_PORT=8983
      - SOLR_JAVA_MEM=-Xms2g -Xmx2g
    depends_on:
      - zookeeper
    volumes:
      - solr2_data:/var/solr

  solr3:
    image: solr:9.3
    hostname: solr3
    ports:
      - "8985:8983"
    environment:
      - ZK_HOST=zookeeper:2181
      - SOLR_HOST=solr3
      - SOLR_PORT=8983
      - SOLR_JAVA_MEM=-Xms2g -Xmx2g
    depends_on:
      - zookeeper
    volumes:
      - solr3_data:/var/solr

  ipfix-service:
    image: ipfix-odata-service:latest
    ports:
      - "8888:8888"
    environment:
      - SEARCH_PROVIDER=solr
      - SOLR_URL=http://solr1:8983/solr
      - SOLR_COLLECTION=ipfix-flows
      - SPRING_PROFILES_ACTIVE=prod,solr
    depends_on:
      - solr1
      - solr2
      - solr3

volumes:
  solr1_data:
  solr2_data:
  solr3_data:
```

### Kubernetes Deployment
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: solr-config
data:
  solr.xml: |
    <?xml version="1.0" encoding="UTF-8" ?>
    <solr>
      <solrcloud>
        <str name="host">${host:}</str>
        <int name="hostPort">${jetty.port:8983}</int>
        <str name="hostContext">${hostContext:solr}</str>
        <bool name="genericCoreNodeNames">${genericCoreNodeNames:true}</bool>
        <int name="zkClientTimeout">${zkClientTimeout:30000}</int>
        <int name="distribUpdateSoTimeout">${distribUpdateSoTimeout:600000}</int>
        <int name="distribUpdateConnTimeout">${distribUpdateConnTimeout:60000}</int>
        <str name="zkCredentialsProvider">${zkCredentialsProvider:org.apache.solr.common.cloud.DefaultZkCredentialsProvider}</str>
        <str name="zkACLProvider">${zkACLProvider:org.apache.solr.common.cloud.DefaultZkACLProvider}</str>
      </solrcloud>
      <shardHandlerFactory name="shardHandlerFactory"
                           class="HttpShardHandlerFactory">
        <int name="socketTimeout">${socketTimeout:600000}</int>
        <int name="connTimeout">${connTimeout:60000}</int>
      </shardHandlerFactory>
    </solr>

---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: solr
spec:
  serviceName: solr-headless
  replicas: 3
  selector:
    matchLabels:
      app: solr
  template:
    metadata:
      labels:
        app: solr
    spec:
      containers:
      - name: solr
        image: solr:9.3
        ports:
        - containerPort: 8983
        env:
        - name: SOLR_JAVA_MEM
          value: "-Xms2g -Xmx2g"
        - name: ZK_HOST
          value: "zookeeper:2181"
        volumeMounts:
        - name: solr-config
          mountPath: /opt/solr/server/solr/solr.xml
          subPath: solr.xml
        - name: solr-data
          mountPath: /var/solr
      volumes:
      - name: solr-config
        configMap:
          name: solr-config
  volumeClaimTemplates:
  - metadata:
      name: solr-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
```

## Migration from Lucene

### Preparation Phase
1. **Set up Solr cluster** with proper configuration
2. **Create collection schema** matching Lucene index structure
3. **Test Solr adapter** with sample data
4. **Verify connectivity** between IPFIX service and Solr

### Migration Strategy

#### **Option 1: Dual Write Migration**
```properties
# Enable dual write mode
search.provider=dual
search.primary=lucene
search.secondary=solr
migration.mode=dual-write
```

#### **Option 2: Bulk Export/Import**
```bash
#!/bin/bash
# migration-script.sh

# Export from Lucene
curl "http://localhost:8888/odata/FlowRecords" > flowrecords-export.json

# Import to Solr
curl -X POST -H 'Content-Type: application/json' \
  --data-binary @flowrecords-export.json \
  "http://localhost:8983/solr/ipfix-flows/update?commit=true"
```

#### **Option 3: Zero-Downtime Migration**
```yaml
# Step 1: Deploy with dual write
spec:
  template:
    spec:
      containers:
      - name: ipfix-service
        env:
        - name: SEARCH_PROVIDER
          value: "dual"
        - name: MIGRATION_MODE
          value: "dual-write"

# Step 2: Switch reads to Solr
# Step 3: Disable dual write
```

### Validation
```bash
# Compare record counts
LUCENE_COUNT=$(curl -s "http://localhost:8888/odata/FlowRecords/\$count")
SOLR_COUNT=$(curl -s "http://localhost:8983/solr/ipfix-flows/select?q=*:*&rows=0" | jq '.response.numFound')

echo "Lucene: $LUCENE_COUNT records"
echo "Solr: $SOLR_COUNT records"

if [ "$LUCENE_COUNT" -eq "$SOLR_COUNT" ]; then
  echo "✅ Migration validation successful"
else
  echo "❌ Record count mismatch detected"
fi
```

## Monitoring and Maintenance

### Health Checks
```bash
# Solr cluster health
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS"

# Collection status
curl "http://localhost:8983/solr/ipfix-flows/admin/ping"

# IPFIX service health with Solr
curl "http://localhost:8888/actuator/health"
```

### Performance Monitoring
```bash
# Query performance
curl "http://localhost:8983/solr/ipfix-flows/admin/mbeans?stats=true&cat=QUERY"

# JVM metrics
curl "http://localhost:8983/solr/ipfix-flows/admin/mbeans?stats=true&cat=JVM"

# Index statistics
curl "http://localhost:8983/solr/ipfix-flows/admin/luke"
```

### Backup and Restore
```bash
# Create backup
curl "http://localhost:8983/solr/admin/collections?action=BACKUP&name=ipfix-backup&collection=ipfix-flows&location=/backups"

# Restore backup
curl "http://localhost:8983/solr/admin/collections?action=RESTORE&name=ipfix-backup&collection=ipfix-flows-restored&location=/backups"
```

## Troubleshooting

### Common Issues

**Connection timeouts:**
```properties
# Increase timeouts
solr.connection.timeout=10000
solr.socket.timeout=30000
```

**Memory issues:**
```bash
# Increase Solr heap
SOLR_JAVA_MEM="-Xms4g -Xmx4g"
```

**Split brain scenarios:**
```bash
# Check ZooKeeper connectivity
echo stat | nc zookeeper 2181
```

### Log Analysis
```bash
# Solr logs
tail -f /var/solr/logs/solr.log

# IPFIX service logs (Solr-related)
tail -f logs/application.log | grep -i solr
```

For more detailed architecture information, see the [Architecture Guide](../docs/architecture.md).
