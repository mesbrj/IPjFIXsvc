package com.ipfix_scenario_ai.ipjfix_svc.adapters.lucene.core;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Component;

import com.ipfix_scenario_ai.ipjfix_svc.core.models.FlowRecord;

@Component
public class LuceneIndexer {
  private final LuceneIndexManager indexManager;
  private final Analyzer analyzer = new StandardAnalyzer();

  public LuceneIndexer(LuceneIndexManager indexManager) {
    this.indexManager = indexManager;
  }

  // Method for indexing IPFIX flow records
  public void indexFlowRecord(String tenantId, FlowRecord flowRecord) {
    try {
      Directory dir = indexManager.getDirectory(tenantId);
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      try (IndexWriter writer = new IndexWriter(dir, config)) {
        Document doc = new Document();

        // Core flow identifiers
        doc.add(new StringField("id", flowRecord.getId(), Field.Store.YES));
        doc.add(new StringField("sourceIP", flowRecord.getSourceIP(), Field.Store.YES));
        doc.add(new StringField("destIP", flowRecord.getDestIP(), Field.Store.YES));
        doc.add(new IntPoint("sourcePort", flowRecord.getSourcePort()));
        doc.add(new StoredField("sourcePort", flowRecord.getSourcePort()));
        doc.add(new IntPoint("destPort", flowRecord.getDestPort()));
        doc.add(new StoredField("destPort", flowRecord.getDestPort()));

        // Protocol and traffic metrics
        doc.add(new StringField("protocol", flowRecord.getProtocol().toLowerCase(), Field.Store.NO)); // Searchable field (lowercase)
        doc.add(new StoredField("protocol", flowRecord.getProtocol())); // Stored field (original case)
        doc.add(new LongPoint("bytes", flowRecord.getBytes()));
        doc.add(new StoredField("bytes", flowRecord.getBytes()));
        doc.add(new LongPoint("packets", flowRecord.getPackets()));
        doc.add(new StoredField("packets", flowRecord.getPackets()));
        doc.add(new LongPoint("reverseBytes", flowRecord.getReverseBytes()));
        doc.add(new StoredField("reverseBytes", flowRecord.getReverseBytes()));
        doc.add(new LongPoint("reversePackets", flowRecord.getReversePackets()));
        doc.add(new StoredField("reversePackets", flowRecord.getReversePackets()));

        // Timestamp for time-based queries
        doc.add(new LongPoint("timestamp", flowRecord.getTimestamp().getTime()));
        doc.add(new StoredField("timestamp", flowRecord.getTimestamp().getTime()));
        
        // Flow start and end times
        if (flowRecord.getFlowStartTime() != null) {
            doc.add(new LongPoint("flowStartTime", flowRecord.getFlowStartTime().getTime()));
            doc.add(new StoredField("flowStartTime", flowRecord.getFlowStartTime().getTime()));
        }
        
        if (flowRecord.getFlowEndTime() != null) {
            doc.add(new LongPoint("flowEndTime", flowRecord.getFlowEndTime().getTime()));
            doc.add(new StoredField("flowEndTime", flowRecord.getFlowEndTime().getTime()));
        }
        
        // TCP flags and ToS value
        doc.add(new IntPoint("tcpFlags", flowRecord.getTcpFlags()));
        doc.add(new StoredField("tcpFlags", flowRecord.getTcpFlags()));
        doc.add(new IntPoint("tosValue", flowRecord.getTosValue()));
        doc.add(new StoredField("tosValue", flowRecord.getTosValue()));
        
        writer.updateDocument(new Term("id", flowRecord.getId()), doc);
      }
    } catch (IOException e) {
    throw new RuntimeException("Failed to index flow record for tenant: " + tenantId + 
                  ", flowId: " + flowRecord.getId(), e);
  } catch (IllegalArgumentException e) {
    throw new IllegalArgumentException("Invalid flow record data: " + flowRecord.getId(), e);
  }
}

  // Method to delete a flow record by ID
  public void deleteFlowRecord(String tenantId, String flowRecordId) {
    try {
      Directory dir = indexManager.getDirectory(tenantId);
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      try (IndexWriter writer = new IndexWriter(dir, config)) {
        writer.deleteDocuments(new Term("id", flowRecordId));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete flow record for tenant: " + tenantId, e);
    }
  }
}