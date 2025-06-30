package com.ipfix_scenario_ai.ipjfix_svc.adapters.lucene.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ipfix_scenario_ai.ipjfix_svc.core.models.FlowRecord;

@Service
public class LuceneService {
    
    private static final Logger logger = LoggerFactory.getLogger(LuceneService.class);
    
    private final LuceneIndexManager indexManager;
    private final Analyzer analyzer = new StandardAnalyzer();

    public LuceneService(LuceneIndexManager indexManager) {
        this.indexManager = indexManager;
    }

    public List<String> search(String tenantId, String field, String queryStr) {
        return search(tenantId, field, queryStr, 10);
    }

    public List<String> search(String tenantId, String field, String queryStr, int maxResults) {
        List<String> results = new ArrayList<>();
        try {
            Directory dir = indexManager.getDirectory(tenantId);
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                QueryParser parser = new QueryParser(field, analyzer);
                Query query = parser.parse(queryStr);
                TopDocs hits = searcher.search(query, maxResults);
                
                for (ScoreDoc hit : hits.scoreDocs) {
                    Document doc = searcher.storedFields().document(hit.doc);
                    String value = doc.get(field);
                    if (value != null) {
                        results.add(value);
                    }
                }
                
                logger.debug("Search for '{}' in field '{}' returned {} results", queryStr, field, results.size());
            }
        } catch (IOException | ParseException e) {
            logger.error("Lucene search failed for tenant: {}, field: {}, query: {}", tenantId, field, queryStr, e);
            throw new RuntimeException("Lucene search failed", e);
        }
        return results;
    }

    // IPFIX-specific search methods
    public List<FlowRecord> searchFlowRecords(String tenantId, String queryStr) {
        return searchFlowRecords(tenantId, queryStr, 100);
    }

    public List<FlowRecord> searchFlowRecords(String tenantId, String queryStr, int maxResults) {
        List<FlowRecord> results = new ArrayList<>();
        try {
            Directory dir = indexManager.getDirectory(tenantId);
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                QueryParser parser = new QueryParser("sourceIP", analyzer); // Default search field
                Query query = parser.parse(queryStr);
                TopDocs hits = searcher.search(query, maxResults);
                
                for (ScoreDoc hit : hits.scoreDocs) {
                    Document doc = searcher.storedFields().document(hit.doc); // Correct method
                    FlowRecord flowRecord = documentToFlowRecord(doc);
                    if (flowRecord != null) {
                        results.add(flowRecord);
                    }
                }
                
                logger.info("Found {} flow records for query: {}", results.size(), queryStr);
            }
        } catch (IOException | ParseException e) {
            logger.error("Flow record search failed for tenant: {}, query: {}", tenantId, queryStr, e);
            throw new RuntimeException("Flow record search failed", e);
        }
        return results;
    }

    // Search by IP address (common IPFIX use case)
    public List<FlowRecord> searchBySourceIP(String tenantId, String sourceIP) {
        return searchFlowRecords(tenantId, "sourceIP:" + sourceIP);
    }

    public List<FlowRecord> searchByDestIP(String tenantId, String destIP) {
        return searchFlowRecords(tenantId, "destIP:" + destIP);
    }

    // Search by port ranges
    public List<FlowRecord> searchByPortRange(String tenantId, int minPort, int maxPort) {
        String query = String.format("sourcePort:[%d TO %d] OR destPort:[%d TO %d]", 
                                    minPort, maxPort, minPort, maxPort);
        return searchFlowRecords(tenantId, query);
    }

    // Search by time range
    public List<FlowRecord> searchByTimeRange(String tenantId, Date startTime, Date endTime) {
        String query = String.format("timestamp:[%d TO %d]", 
                                    startTime.getTime(), endTime.getTime());
        return searchFlowRecords(tenantId, query);
    }

    // Search flows with high traffic volume
    public List<FlowRecord> searchHighVolumeFlows(String tenantId, long minBytes) {
        String query = String.format("bytes:[%d TO *]", minBytes);
        return searchFlowRecords(tenantId, query);
    }

    private FlowRecord documentToFlowRecord(Document doc) {
        try {
            FlowRecord record = new FlowRecord();
            record.setId(doc.get("id"));
            record.setSourceIP(doc.get("sourceIP"));
            record.setDestIP(doc.get("destIP"));
            record.setProtocol(doc.get("protocol"));
            
            // Parse numeric fields safely
            String sourcePort = doc.get("sourcePort");
            if (sourcePort != null) {
                record.setSourcePort(Integer.parseInt(sourcePort));
            }
            
            String destPort = doc.get("destPort");
            if (destPort != null) {
                record.setDestPort(Integer.parseInt(destPort));
            }
            
            String bytes = doc.get("bytes");
            if (bytes != null) {
                record.setBytes(Long.parseLong(bytes));
            }
            
            String packets = doc.get("packets");
            if (packets != null) {
                record.setPackets(Long.parseLong(packets));
            }
            
            String reverseBytes = doc.get("reverseBytes");
            if (reverseBytes != null) {
                record.setReverseBytes(Long.parseLong(reverseBytes));
            }
            
            String reversePackets = doc.get("reversePackets");
            if (reversePackets != null) {
                record.setReversePackets(Long.parseLong(reversePackets));
            }
            
            String timestamp = doc.get("timestamp");
            if (timestamp != null) {
                record.setTimestamp(new Date(Long.parseLong(timestamp)));
            }
            
            return record;
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Failed to convert document to FlowRecord", e);
            return null;
        }
    }

    public boolean hasIndex(String tenantId) {
        try {
            Directory dir = indexManager.getDirectory(tenantId);
            return DirectoryReader.indexExists(dir);
        } catch (IOException e) {
            logger.warn("Error checking index existence for tenant: {}", tenantId, e);
            return false;
        }
    }

    // Get index statistics
    public long getIndexSize(String tenantId) {
        try {
            Directory dir = indexManager.getDirectory(tenantId);
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                return reader.numDocs();
            }
        } catch (IOException e) {
            logger.warn("Error getting index size for tenant: {}", tenantId, e);
            return 0;
        }
    }
}