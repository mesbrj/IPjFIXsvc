package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertEnterpriseInfoInput {
    private Integer silkAppLabel;
    private String payloadEntropy;
    private String initialTCPFlags;
    private String unionTCPFlags;
    private Long reverseFlowDeltaMilliseconds;
    private String flowAttributes;
    private Integer flowKeyHash;
    private String osName;
    private String osVersion;
    private String osFingerprint;
    private String payloadContent;
    private Integer payloadLength;
    private String reversePayloadContent;
    private Integer reversePayloadLength;
    private Integer vlanId;
    private Integer reverseVlanId;
    private String sourceMacAddress;
    private String destinationMacAddress;
    private String ingressInterfaceName;
    private String egressInterfaceName;
}
