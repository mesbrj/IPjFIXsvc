package com.ipfix.graphql.repository;

import com.ipfix.graphql.model.IpfixRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IpfixRecordRepositoryTest {
    
    private IpfixRecordRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new IpfixRecordRepository();
        repository.deleteAll();
    }
    
    @Test
    void testSaveAndFindById() {
        IpfixRecord record = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.100")
                .destinationIPv4Address("10.0.0.50")
                .protocolIdentifier(6)
                .sourceTransportPort(54321)
                .destinationTransportPort(443)
                .build();
        
        IpfixRecord saved = repository.save(record);
        
        assertNotNull(saved.getId());
        assertNotNull(saved.getTimestamp());
        
        Optional<IpfixRecord> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("192.168.1.100", found.get().getSourceIPv4Address());
    }
    
    @Test
    void testFindBySourceIp() {
        IpfixRecord record1 = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.100")
                .destinationIPv4Address("10.0.0.50")
                .protocolIdentifier(6)
                .build();
        
        IpfixRecord record2 = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.100")
                .destinationIPv4Address("10.0.0.51")
                .protocolIdentifier(6)
                .build();
        
        IpfixRecord record3 = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.101")
                .destinationIPv4Address("10.0.0.52")
                .protocolIdentifier(6)
                .build();
        
        repository.save(record1);
        repository.save(record2);
        repository.save(record3);
        
        List<IpfixRecord> results = repository.findBySourceIp("192.168.1.100");
        assertEquals(2, results.size());
    }
    
    @Test
    void testFindByProtocol() {
        IpfixRecord tcpRecord = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.100")
                .protocolIdentifier(6) // TCP
                .build();
        
        IpfixRecord udpRecord = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.101")
                .protocolIdentifier(17) // UDP
                .build();
        
        repository.save(tcpRecord);
        repository.save(udpRecord);
        
        List<IpfixRecord> tcpResults = repository.findByProtocol(6);
        assertEquals(1, tcpResults.size());
        assertEquals(6, tcpResults.get(0).getProtocolIdentifier());
    }
    
    @Test
    void testFindByTimeRange() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant mid = Instant.parse("2024-06-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        
        IpfixRecord record1 = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.100")
                .timestamp(mid)
                .build();
        
        IpfixRecord record2 = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.101")
                .timestamp(Instant.parse("2023-01-01T00:00:00Z"))
                .build();
        
        repository.save(record1);
        repository.save(record2);
        
        List<IpfixRecord> results = repository.findByTimeRange(start, end);
        assertEquals(1, results.size());
        assertEquals("192.168.1.100", results.get(0).getSourceIPv4Address());
    }
    
    @Test
    void testDeleteById() {
        IpfixRecord record = IpfixRecord.builder()
                .sourceIPv4Address("192.168.1.100")
                .build();
        
        IpfixRecord saved = repository.save(record);
        assertTrue(repository.findById(saved.getId()).isPresent());
        
        boolean deleted = repository.deleteById(saved.getId());
        assertTrue(deleted);
        assertFalse(repository.findById(saved.getId()).isPresent());
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(IpfixRecord.builder().sourceIPv4Address("192.168.1.100").build());
        assertEquals(1, repository.count());
        
        repository.save(IpfixRecord.builder().sourceIPv4Address("192.168.1.101").build());
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        assertEquals(0, repository.count());
    }
}
