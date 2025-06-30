package com.ipfix_scenario_ai.ipjfix_svc.adapters.lucene.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class LuceneIndexManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LuceneIndexManager.class);
    
    private final ConcurrentHashMap<String, Directory> directories = new ConcurrentHashMap<>();
    private final String baseIndexPath;
    private final boolean useInMemoryFallback;
    
    public LuceneIndexManager(
            @Value("${lucene.index.path:./lucene-indices}") String baseIndexPath,
            @Value("${lucene.fallback.memory:true}") boolean useInMemoryFallback) {
        this.baseIndexPath = baseIndexPath;
        this.useInMemoryFallback = useInMemoryFallback;
        logger.info("LuceneIndexManager initialized with base path: {}", baseIndexPath);
    }
    
    public Directory getDirectory(String tenantId) {
        return directories.computeIfAbsent(tenantId, this::createDirectory);
    }
    
    private Directory createDirectory(String tenantId) {
        try {
            // Create file-based directory for persistent storage
            Path indexPath = Paths.get(baseIndexPath, tenantId);
            
            // Ensure parent directories exist
            Files.createDirectories(indexPath);
            
            logger.debug("Creating FSDirectory for tenant: {} at path: {}", tenantId, indexPath);
            return FSDirectory.open(indexPath);
            
        } catch (IOException e) {
            logger.warn("Failed to create FSDirectory for tenant: {}. Error: {}", tenantId, e.getMessage());
            
            if (useInMemoryFallback) {
                logger.info("Falling back to in-memory directory for tenant: {}", tenantId);
                return new ByteBuffersDirectory();
            } else {
                throw new RuntimeException("Cannot create directory for tenant: " + tenantId, e);
            }
        }
    }
    
    public void closeDirectory(String tenantId) {
        Directory dir = directories.remove(tenantId);
        if (dir != null) {
            try {
                dir.close();
                logger.debug("Closed directory for tenant: {}", tenantId);
            } catch (IOException e) {
                logger.error("Error closing directory for tenant: {}", tenantId, e);
            }
        }
    }
    
    @PreDestroy
    public void closeAllDirectories() {
        logger.info("Shutting down LuceneIndexManager, closing {} directories", directories.size());
        directories.forEach((tenantId, dir) -> {
            try {
                dir.close();
                logger.debug("Closed directory for tenant: {}", tenantId);
            } catch (IOException e) {
                logger.error("Error closing directory for tenant: {}", tenantId, e);
            }
        });
        directories.clear();
    }
    
    // Utility method to check if tenant has an active directory
    public boolean hasTenant(String tenantId) {
        return directories.containsKey(tenantId);
    }
    
    // Get statistics about active directories
    public int getActiveDirectoryCount() {
        return directories.size();
    }
}