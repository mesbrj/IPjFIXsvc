package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Input type for creating/ingesting IPFIX records via GraphQL mutation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpfixRecordInput {
    
    // Standard IANA IPFIX Info Elements
    private Long octetDeltaCount;
    private Long packetDeltaCount;
    private Long deltaFlowCount;
    private Integer protocolIdentifier;
    private Integer ipClassOfService;
    private Integer tcpControlBits;
    private Integer sourceTransportPort;
    private String sourceIPv4Address;
    private Integer sourceIPv4PrefixLength;
    private Integer ingressInterface;
    private Integer destinationTransportPort;
    private String destinationIPv4Address;
    private Integer destinationIPv4PrefixLength;
    private Integer egressInterface;
    private String ipNextHopIPv4Address;
    
    // IPv6 Support
    private String sourceIPv6Address;
    private String destinationIPv6Address;
    private Integer sourceIPv6PrefixLength;
    private Integer destinationIPv6PrefixLength;
    
    // Flow timing
    private Instant flowStartMilliseconds;
    private Instant flowEndMilliseconds;
    private Long flowStartSysUpTime;
    private Long flowEndSysUpTime;
    
    // MPLS labels
    private Integer mplsTopLabelStackSection;
    private Integer mplsLabelStackSection2;
    private Integer mplsLabelStackSection3;
    
    // Application info
    private Integer applicationId;
    private String applicationName;
    private String applicationDescription;
    
    // DPI, Bidirectional, and CERT info
    private DpiInfoInput dpiInfo;
    private BidirectionalFlowInfoInput bidirectionalFlowInfo;
    private CertEnterpriseInfoInput certInfo;
    
    // Data structures
    private List<BasicListElementInput> basicLists;
    private List<SubTemplateListElementInput> subTemplateLists;
    private List<SubTemplateMultiListElementInput> subTemplateMultiLists;
    
    // Metadata
    private Integer observationDomainId;
    private String exporterIPv4Address;
    private String exporterIPv6Address;
}
