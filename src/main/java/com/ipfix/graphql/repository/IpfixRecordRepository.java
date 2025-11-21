package com.ipfix.graphql.repository;

import com.ipfix.graphql.model.IpfixRecord;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for storing IPFIX records
 */
@Repository
public class IpfixRecordRepository {
    
    private final Map<String, IpfixRecord> records = new ConcurrentHashMap<>();
    
    public IpfixRecord save(IpfixRecord record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getTimestamp() == null) {
            record.setTimestamp(Instant.now());
        }
        records.put(record.getId(), record);
        return record;
    }
    
    public Optional<IpfixRecord> findById(String id) {
        return Optional.ofNullable(records.get(id));
    }
    
    public List<IpfixRecord> findAll() {
        return new ArrayList<>(records.values());
    }
    
    public List<IpfixRecord> findAll(int limit, int offset) {
        return records.values().stream()
                .sorted(Comparator.comparing(IpfixRecord::getTimestamp).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<IpfixRecord> findBySourceIp(String sourceIp) {
        return records.values().stream()
                .filter(r -> sourceIp.equals(r.getSourceIPv4Address()) || 
                            sourceIp.equals(r.getSourceIPv6Address()))
                .sorted(Comparator.comparing(IpfixRecord::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    public List<IpfixRecord> findByDestinationIp(String destinationIp) {
        return records.values().stream()
                .filter(r -> destinationIp.equals(r.getDestinationIPv4Address()) || 
                            destinationIp.equals(r.getDestinationIPv6Address()))
                .sorted(Comparator.comparing(IpfixRecord::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    public List<IpfixRecord> findByProtocol(Integer protocolId) {
        return records.values().stream()
                .filter(r -> protocolId.equals(r.getProtocolIdentifier()))
                .sorted(Comparator.comparing(IpfixRecord::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    public List<IpfixRecord> findByTimeRange(Instant start, Instant end) {
        return records.values().stream()
                .filter(r -> !r.getTimestamp().isBefore(start) && !r.getTimestamp().isAfter(end))
                .sorted(Comparator.comparing(IpfixRecord::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    public boolean deleteById(String id) {
        return records.remove(id) != null;
    }
    
    public void deleteAll() {
        records.clear();
    }
    
    public long count() {
        return records.size();
    }
}
