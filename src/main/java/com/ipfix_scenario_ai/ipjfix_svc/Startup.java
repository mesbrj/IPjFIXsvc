package com.ipfix_scenario_ai.ipjfix_svc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.ipfix_scenario_ai.ipjfix_svc.adapters.ignite.UserRepository;
import com.ipfix_scenario_ai.ipjfix_svc.adapters.lucene.core.LuceneIndexer;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.FlowRecord;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

@Service
public class Startup implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(Startup.class);
    
    private final UserRepository userRepository;
    private final LuceneIndexer luceneIndexer;

    public Startup(UserRepository userRepository, LuceneIndexer luceneIndexer) {
        this.userRepository = userRepository;
        this.luceneIndexer = luceneIndexer;
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data bootstrap process...");
        
        createSampleUsers();
        createSampleFlowRecords();
        
        logger.info("Data bootstrap completed successfully");
    }
    
    private void createSampleUsers() {
        // Admin user
        User admin = new User();
        admin.setId("admin-001");
        admin.setUsername("admin");
        admin.setEmail("admin@ipfix.com");
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setRole("ADMIN");
        admin.setTenantId("default");
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(admin);
        
        // Analyst user
        User analyst = new User();
        analyst.setId("analyst-001");
        analyst.setUsername("analyst");
        analyst.setEmail("analyst@ipfix.com");
        analyst.setFirstName("Network");
        analyst.setLastName("Analyst");
        analyst.setRole("ANALYST");
        analyst.setTenantId("default");
        analyst.setActive(true);
        analyst.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(analyst);
        
        // Viewer user
        User viewer = new User();
        viewer.setId("viewer-001");
        viewer.setUsername("viewer");
        viewer.setEmail("viewer@ipfix.com");
        viewer.setFirstName("Data");
        viewer.setLastName("Viewer");
        viewer.setRole("VIEWER");
        viewer.setTenantId("tenant1");
        viewer.setActive(true);
        viewer.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(viewer);
        
        logger.info("Created {} sample users", 3);
    }
    
    private void createSampleFlowRecords() {
        logger.info("Starting Lucene data bootstrap...");
        
        try {
            List<FlowRecord> sampleFlows = createSampleFlowData();
            
            for (FlowRecord flow : sampleFlows) {
                try {
                    luceneIndexer.indexFlowRecord("default", flow);
                    logger.debug("Indexed flow record: {}", flow.getId());
                } catch (Exception e) {
                    logger.error("Failed to index flow record: {}", flow.getId(), e);
                }
            }
            
            logger.info("Created {} sample flow records in Lucene index", sampleFlows.size());
        } catch (Exception e) {
            logger.error("Failed to bootstrap Lucene data", e);
        }
    }
    
    private List<FlowRecord> createSampleFlowData() {
        Date now = new Date();
        Date fiveMinutesAgo = new Date(now.getTime() - 5 * 60 * 1000);
        Date tenMinutesAgo = new Date(now.getTime() - 10 * 60 * 1000);
        Date fifteenMinutesAgo = new Date(now.getTime() - 15 * 60 * 1000);
        
        return Arrays.asList(
            // HTTP Traffic
            createFlowRecord(
                "flow-001",
                "192.168.1.100", "93.184.216.34", // Example.com IP
                48392, 80,
                "TCP",
                1024L, 15L, 2048L, 20L,
                now, fiveMinutesAgo, now,
                0x18, 0 // TCP PSH+ACK flags
            ),
            
            // HTTPS Traffic
            createFlowRecord(
                "flow-002", 
                "192.168.1.101", "151.101.193.140", // Reddit IP
                52431, 443,
                "TCP",
                4096L, 32L, 8192L, 45L,
                now, tenMinutesAgo, now,
                0x18, 0
            ),
            
            // DNS Query
            createFlowRecord(
                "flow-003",
                "192.168.1.102", "8.8.8.8", // Google DNS
                53281, 53,
                "UDP", 
                64L, 1L, 120L, 1L,
                now, now, now,
                0, 0
            ),
            
            // Large File Transfer (FTP)
            createFlowRecord(
                "flow-004",
                "192.168.1.103", "203.0.113.42", // Test IP
                21432, 21,
                "TCP",
                524288L, 512L, 1048576L, 1024L,
                now, fifteenMinutesAgo, fiveMinutesAgo,
                0x18, 0
            ),
            
            // SSH Connection
            createFlowRecord(
                "flow-005",
                "192.168.1.104", "198.51.100.50", // Test IP
                58392, 22,
                "TCP",
                2048L, 25L, 1536L, 20L,
                now, tenMinutesAgo, fiveMinutesAgo,
                0x18, 0
            ),
            
            // Video Streaming (High bandwidth)
            createFlowRecord(
                "flow-006",
                "192.168.1.105", "23.246.226.99", // Netflix IP range
                45892, 443,
                "TCP",
                2097152L, 1500L, 4194304L, 3000L,
                now, fifteenMinutesAgo, now,
                0x18, 0
            ),
            
            // P2P Traffic
            createFlowRecord(
                "flow-007",
                "192.168.1.106", "185.125.190.29", // Random IP
                6881, 6881,
                "TCP",
                1048576L, 800L, 524288L, 400L,
                now, fifteenMinutesAgo, tenMinutesAgo,
                0x18, 0
            ),
            
            // Email (SMTP)
            createFlowRecord(
                "flow-008",
                "192.168.1.107", "74.125.224.108", // Gmail SMTP
                49231, 587,
                "TCP",
                512L, 8L, 256L, 4L,
                now, fiveMinutesAgo, now,
                0x18, 0
            ),
            
            // ICMP Ping
            createFlowRecord(
                "flow-009",
                "192.168.1.108", "1.1.1.1", // Cloudflare DNS
                0, 0,
                "ICMP",
                64L, 4L, 64L, 4L,
                now, now, now,
                0, 0
            ),
            
            // Database Connection
            createFlowRecord(
                "flow-010",
                "192.168.1.109", "192.168.1.200", // Internal DB server
                58431, 5432,
                "TCP",
                8192L, 64L, 16384L, 128L,
                now, tenMinutesAgo, now,
                0x18, 0
            )
        );
    }
    
    private FlowRecord createFlowRecord(String id, String sourceIP, String destIP,
                                      int sourcePort, int destPort, String protocol,
                                      long bytes, long packets, long reverseBytes, long reversePackets,
                                      Date timestamp, Date flowStartTime, Date flowEndTime,
                                      int tcpFlags, int tosValue) {
        
        FlowRecord flow = new FlowRecord();
        flow.setId(id);
        flow.setSourceIP(sourceIP);
        flow.setDestIP(destIP);
        flow.setSourcePort(sourcePort);
        flow.setDestPort(destPort);
        flow.setProtocol(protocol);
        flow.setBytes(bytes);
        flow.setPackets(packets);
        flow.setReverseBytes(reverseBytes);
        flow.setReversePackets(reversePackets);
        flow.setTimestamp(timestamp);
        flow.setFlowStartTime(flowStartTime);
        flow.setFlowEndTime(flowEndTime);
        flow.setTcpFlags(tcpFlags);
        flow.setTosValue(tosValue);
        
        return flow;
    }
}