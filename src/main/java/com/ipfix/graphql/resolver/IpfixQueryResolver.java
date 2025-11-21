package com.ipfix.graphql.resolver;

import com.ipfix.graphql.model.IpfixRecord;
import com.ipfix.graphql.repository.IpfixRecordRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;

/**
 * GraphQL Query resolver for IPFIX records
 */
@Controller
public class IpfixQueryResolver {
    
    private final IpfixRecordRepository repository;
    
    public IpfixQueryResolver(IpfixRecordRepository repository) {
        this.repository = repository;
    }
    
    @QueryMapping
    public IpfixRecord ipfixRecord(@Argument String id) {
        return repository.findById(id).orElse(null);
    }
    
    @QueryMapping
    public List<IpfixRecord> ipfixRecords(
            @Argument Integer limit,
            @Argument Integer offset) {
        
        int actualLimit = limit != null ? limit : 100;
        int actualOffset = offset != null ? offset : 0;
        
        return repository.findAll(actualLimit, actualOffset);
    }
    
    @QueryMapping
    public List<IpfixRecord> ipfixRecordsBySourceIp(@Argument String sourceIp) {
        return repository.findBySourceIp(sourceIp);
    }
    
    @QueryMapping
    public List<IpfixRecord> ipfixRecordsByDestinationIp(@Argument String destinationIp) {
        return repository.findByDestinationIp(destinationIp);
    }
    
    @QueryMapping
    public List<IpfixRecord> ipfixRecordsByProtocol(@Argument Integer protocolId) {
        return repository.findByProtocol(protocolId);
    }
    
    @QueryMapping
    public List<IpfixRecord> ipfixRecordsByTimeRange(
            @Argument String startTime,
            @Argument String endTime) {
        Instant start = Instant.parse(startTime);
        Instant end = Instant.parse(endTime);
        return repository.findByTimeRange(start, end);
    }
    
    @QueryMapping
    public Long ipfixRecordsCount() {
        return repository.count();
    }
}
