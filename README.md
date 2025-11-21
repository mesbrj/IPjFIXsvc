# GraphQL IPFIX API

A Java Spring Boot GraphQL API for ingesting and querying IPFIX (IP Flow Information Export) records. This implementation supports standard IANA IPFIX information elements and CERT NetSA Enterprise info elements.

# IPFIX Record lifecycle management

**3 layers of storage** and **automated data migration (and removal)** between layers based on configurable retention policies:
- **Solr (Lucene)** - Newly ingested records for fast querying
- **MongoDB** - Mid-term storage or larger datasets
- **depuplicated storage (*to be defined*)** - Long-term archival
  - **OpenDedupe / SDFS**: file-system level, object (S3, Swift, Azure Blob) or block storage backends. Open-source project with a smaller ecosystem. Docs and GitHub are primary sources.
  - **VDO (Virtual Data Optimizer)**: kernel module level only block storage backend. Enterprise-grade solution.
  - **OpenZFS**: file-system level (only block storage backend) native deduplication ([fast dedup](https://openzfs.org/w/images/3/3b/Klara_-_Introducing_Fast_Dedup_-_OpenZFS2023.pdf)).




## Working in progress:

- **GraphQL API**: Query and mutation(ingest) operations with GraphiQL interface
- **IPFIX Support**: Information elements from [IANA IPFIX registry](https://www.iana.org/assignments/ipfix/ipfix.xhtml) and **Data Structures** (BasicList, SubTemplateList, SubTemplateMultiList)
- **CERT Enterprise Elements**: Custom information elements from [CERT NetSA IPFIX Registry](https://tools.netsa.cert.org/cert-ipfix-registry/cert_ipfix_formatted.html)
- **Deep Packet Inspection**: DPI information elements from [CERT NetSA YAF DPI](https://tools.netsa.cert.org/yaf/deeppacketinspection.html)
- **Bidirectional Flows**: Forward and reverse flow statistics
- **In-Memory Storage**: Fast storage for testing

```bash
mvn clean package
mvn spring-boot:run
```

GraphQL Endpoints

- **GraphQL API**: `http://localhost:8080/graphql`
- **GraphiQL Interface**: `http://localhost:8080/graphiql`

## Usage

### Queries examples

```graphql
query {
  ipfixRecords(limit: 10, offset: 0) {
    id
    timestamp
    sourceIPv4Address
    destinationIPv4Address
    sourceTransportPort
    destinationTransportPort
    protocolIdentifier
    octetDeltaCount
    packetDeltaCount
  }
}
```

```graphql
query {
  ipfixRecord(id: "your-record-id") {
    id
    timestamp
    sourceIPv4Address
    destinationIPv4Address
    protocolIdentifier
    dpiInfo {
      httpRequestMethod
      httpRequestHost
      sslServerName
    }
    bidirectionalFlowInfo {
      reverseOctetDeltaCount
      reversePacketDeltaCount
    }
    certInfo {
      silkAppLabel
      osName
      osVersion
    }
  }
}
```

```graphql
query {
  ipfixRecordsBySourceIp(sourceIp: "192.168.1.100") {
    id
    sourceIPv4Address
    destinationIPv4Address
    octetDeltaCount
  }
}
```

```graphql
query {
  ipfixRecordsByProtocol(protocolId: 6) {
    id
    protocolIdentifier
    sourceTransportPort
    destinationTransportPort
    tcpControlBits
  }
}
```

```graphql
query {
  ipfixRecordsByTimeRange(
    startTime: "2024-01-01T00:00:00Z"
    endTime: "2024-12-31T23:59:59Z"
  ) {
    id
    timestamp
    sourceIPv4Address
  }
}
```

### Mutations / Ingest examples

```graphql
mutation {
  ingestIpfixRecord(input: {
    sourceIPv4Address: "192.168.1.100"
    destinationIPv4Address: "10.0.0.50"
    sourceTransportPort: 54321
    destinationTransportPort: 443
    protocolIdentifier: 6
    octetDeltaCount: 15000
    packetDeltaCount: 25
    tcpControlBits: 24
    flowStartMilliseconds: "2024-01-15T10:30:00Z"
    flowEndMilliseconds: "2024-01-15T10:35:00Z"
    dpiInfo: {
      httpRequestMethod: "GET"
      httpRequestHost: "example.com"
      httpRequestTarget: "/api/data"
      sslServerName: "example.com"
      sslVersion: 3
    }
    bidirectionalFlowInfo: {
      reverseOctetDeltaCount: 5000
      reversePacketDeltaCount: 10
      flowDurationMilliseconds: 300000
    }
    certInfo: {
      silkAppLabel: 80
      osName: "Linux"
      osVersion: "5.4"
    }
  }) {
    id
    timestamp
    sourceIPv4Address
    destinationIPv4Address
  }
}
```

```graphql
mutation {
  ingestIpfixRecord(input: {
    sourceIPv4Address: "172.16.0.10"
    destinationIPv4Address: "172.16.0.20"
    protocolIdentifier: 17
    basicLists: [
      {
        informationElementId: 8
        informationElementName: "sourceIPv4Address"
        semantic: "allOf"
        values: ["172.16.0.10", "172.16.0.11"]
        dataType: "ipv4Address"
      }
    ]
    subTemplateLists: [
      {
        templateId: 256
        templateName: "httpTemplate"
        semantic: "ordered"
        entries: [
          {
            "method": "GET",
            "path": "/index.html",
            "statusCode": 200
          }
        ]
      }
    ]
  }) {
    id
    basicLists {
      id
      informationElementName
      values
    }
  }
}
```

```graphql
mutation {
  deleteIpfixRecord(id: "your-record-id")
}
```

## References

- [IANA IPFIX Information Elements](https://www.iana.org/assignments/ipfix/ipfix.xhtml)
- [CERT NetSA IPFIX Registry](https://tools.netsa.cert.org/cert-ipfix-registry/cert_ipfix_formatted.html)
- [RFC 7011 - IPFIX Protocol Specification](https://tools.ietf.org/html/rfc7011)
- [RFC 7012 - IPFIX Information Elements](https://tools.ietf.org/html/rfc7012)
- [RFC 6313 - Export of Structured Data in IPFIX](https://tools.ietf.org/html/rfc6313)
