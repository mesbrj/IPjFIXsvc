package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Bidirectional Flow Information Elements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidirectionalFlowInfo {
    
    // Bidirectional flow identifiers
    private String biflowDirection;         // IE 239
    private Long reverseOctetDeltaCount;    // IE 1 reversed
    private Long reversePacketDeltaCount;   // IE 2 reversed
    
    // Reverse timing
    private Instant reverseFlowStartMilliseconds;
    private Instant reverseFlowEndMilliseconds;
    
    // Reverse TCP flags
    private Integer reverseTcpControlBits;
    
    // Flow duration and statistics
    private Long flowDurationMilliseconds;  // IE 161
    private Long flowDurationMicroseconds;  // IE 162
    
    // Min/Max packet sizes
    private Long minimumIpTotalLength;      // IE 25
    private Long maximumIpTotalLength;      // IE 26
    private Long reverseMinimumIpTotalLength;
    private Long reverseMaximumIpTotalLength;
    
    // Min/Max TTL
    private Integer minimumTTL;             // IE 52
    private Integer maximumTTL;             // IE 53
    private Integer reverseMinimumTTL;
    private Integer reverseMaximumTTL;
    
    // Flow state and direction
    private String flowEndReason;           // IE 136
    private Integer flowDirection;          // IE 61 (0=ingress, 1=egress)
}
