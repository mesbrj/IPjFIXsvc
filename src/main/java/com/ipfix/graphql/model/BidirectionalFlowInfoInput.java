package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidirectionalFlowInfoInput {
    private String biflowDirection;
    private Long reverseOctetDeltaCount;
    private Long reversePacketDeltaCount;
    private Instant reverseFlowStartMilliseconds;
    private Instant reverseFlowEndMilliseconds;
    private Integer reverseTcpControlBits;
    private Long flowDurationMilliseconds;
    private Long flowDurationMicroseconds;
    private Long minimumIpTotalLength;
    private Long maximumIpTotalLength;
    private Long reverseMinimumIpTotalLength;
    private Long reverseMaximumIpTotalLength;
    private Integer minimumTTL;
    private Integer maximumTTL;
    private Integer reverseMinimumTTL;
    private Integer reverseMaximumTTL;
    private String flowEndReason;
    private Integer flowDirection;
}
