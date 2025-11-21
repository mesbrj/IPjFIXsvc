package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Represents an IPFIX Flow Record with standard IANA and CERT Enterprise info elements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpfixRecord {
    
    // Core identification
    private String id;
    private Instant timestamp;
    
    // Standard IANA IPFIX Info Elements (Common ones)
    private Long octetDeltaCount;           // IE 1
    private Long packetDeltaCount;          // IE 2
    private Long deltaFlowCount;            // IE 3
    private Integer protocolIdentifier;     // IE 4
    private Integer ipClassOfService;       // IE 5
    private Integer tcpControlBits;         // IE 6
    private Integer sourceTransportPort;    // IE 7
    private String sourceIPv4Address;       // IE 8
    private Integer sourceIPv4PrefixLength; // IE 9
    private Integer ingressInterface;       // IE 10
    private Integer destinationTransportPort; // IE 11
    private String destinationIPv4Address;  // IE 12
    private Integer destinationIPv4PrefixLength; // IE 13
    private Integer egressInterface;        // IE 14
    private String ipNextHopIPv4Address;    // IE 15
    
    // IPv6 Support
    private String sourceIPv6Address;       // IE 27
    private String destinationIPv6Address;  // IE 28
    private Integer sourceIPv6PrefixLength; // IE 29
    private Integer destinationIPv6PrefixLength; // IE 30
    
    // Flow timing
    private Instant flowStartMilliseconds;  // IE 152
    private Instant flowEndMilliseconds;    // IE 153
    private Long flowStartSysUpTime;        // IE 22
    private Long flowEndSysUpTime;          // IE 21
    
    // MPLS labels
    private Integer mplsTopLabelStackSection; // IE 70
    private Integer mplsLabelStackSection2;   // IE 71
    private Integer mplsLabelStackSection3;   // IE 72
    
    // Application and service info
    private Integer applicationId;          // IE 95
    private String applicationName;         // IE 96
    private String applicationDescription;  // IE 94
    
    // Deep Packet Inspection (DPI) elements
    private DpiInfo dpiInfo;
    
    // Bidirectional flow elements
    private BidirectionalFlowInfo bidirectionalFlowInfo;
    
    // CERT Enterprise elements
    private CertEnterpriseInfo certInfo;
    
    // Data structures
    private List<BasicListElement> basicLists;
    private List<SubTemplateListElement> subTemplateLists;
    private List<SubTemplateMultiListElement> subTemplateMultiLists;
    
    // Metadata
    private Integer observationDomainId;    // IE 149
    private String exporterIPv4Address;     // IE 130
    private String exporterIPv6Address;     // IE 131
}
