package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CERT Enterprise IPFIX Information Elements
 * Based on CERT NetSA IPFIX Registry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertEnterpriseInfo {
    
    // CERT Private Enterprise Number is 6871
    
    // YAF (Yet Another Flowmeter) specific elements
    private Integer silkAppLabel;           // CERT IE 33
    private String payloadEntropy;          // CERT IE 35
    private String initialTCPFlags;         // CERT IE 14
    private String unionTCPFlags;           // CERT IE 15
    
    // Flow characteristics
    private Long reverseFlowDeltaMilliseconds; // CERT IE 21
    private String flowAttributes;          // CERT IE 40
    private Integer flowKeyHash;            // CERT IE 106
    
    // Application labeling
    private String osName;                  // CERT IE 36
    private String osVersion;               // CERT IE 37
    private String osFingerprint;           // CERT IE 107
    
    // Payload inspection
    private String payloadContent;          // CERT IE 18
    private Integer payloadLength;          // CERT IE 19
    private String reversePayloadContent;   // CERT IE 20
    private Integer reversePayloadLength;   // CERT IE 22
    
    // VLAN and MPLS
    private Integer vlanId;                 // CERT IE 51
    private Integer reverseVlanId;          // CERT IE 52
    
    // MAC addresses
    private String sourceMacAddress;        // CERT IE 80
    private String destinationMacAddress;   // CERT IE 81
    
    // Additional metadata
    private String ingressInterfaceName;    // CERT IE 100
    private String egressInterfaceName;     // CERT IE 101
}
