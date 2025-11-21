package com.ipfix.graphql.resolver;

import com.ipfix.graphql.model.*;
import com.ipfix.graphql.repository.IpfixRecordRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL Mutation resolver for IPFIX records
 */
@Controller
public class IpfixMutationResolver {
    
    private final IpfixRecordRepository repository;
    
    public IpfixMutationResolver(IpfixRecordRepository repository) {
        this.repository = repository;
    }
    
    @MutationMapping
    public IpfixRecord ingestIpfixRecord(@Argument IpfixRecordInput input) {
        IpfixRecord record = IpfixRecord.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .octetDeltaCount(input.getOctetDeltaCount())
                .packetDeltaCount(input.getPacketDeltaCount())
                .deltaFlowCount(input.getDeltaFlowCount())
                .protocolIdentifier(input.getProtocolIdentifier())
                .ipClassOfService(input.getIpClassOfService())
                .tcpControlBits(input.getTcpControlBits())
                .sourceTransportPort(input.getSourceTransportPort())
                .sourceIPv4Address(input.getSourceIPv4Address())
                .sourceIPv4PrefixLength(input.getSourceIPv4PrefixLength())
                .ingressInterface(input.getIngressInterface())
                .destinationTransportPort(input.getDestinationTransportPort())
                .destinationIPv4Address(input.getDestinationIPv4Address())
                .destinationIPv4PrefixLength(input.getDestinationIPv4PrefixLength())
                .egressInterface(input.getEgressInterface())
                .ipNextHopIPv4Address(input.getIpNextHopIPv4Address())
                .sourceIPv6Address(input.getSourceIPv6Address())
                .destinationIPv6Address(input.getDestinationIPv6Address())
                .sourceIPv6PrefixLength(input.getSourceIPv6PrefixLength())
                .destinationIPv6PrefixLength(input.getDestinationIPv6PrefixLength())
                .flowStartMilliseconds(input.getFlowStartMilliseconds())
                .flowEndMilliseconds(input.getFlowEndMilliseconds())
                .flowStartSysUpTime(input.getFlowStartSysUpTime())
                .flowEndSysUpTime(input.getFlowEndSysUpTime())
                .mplsTopLabelStackSection(input.getMplsTopLabelStackSection())
                .mplsLabelStackSection2(input.getMplsLabelStackSection2())
                .mplsLabelStackSection3(input.getMplsLabelStackSection3())
                .applicationId(input.getApplicationId())
                .applicationName(input.getApplicationName())
                .applicationDescription(input.getApplicationDescription())
                .dpiInfo(convertDpiInfo(input.getDpiInfo()))
                .bidirectionalFlowInfo(convertBidirectionalFlowInfo(input.getBidirectionalFlowInfo()))
                .certInfo(convertCertInfo(input.getCertInfo()))
                .basicLists(convertBasicLists(input.getBasicLists()))
                .subTemplateLists(convertSubTemplateLists(input.getSubTemplateLists()))
                .subTemplateMultiLists(convertSubTemplateMultiLists(input.getSubTemplateMultiLists()))
                .observationDomainId(input.getObservationDomainId())
                .exporterIPv4Address(input.getExporterIPv4Address())
                .exporterIPv6Address(input.getExporterIPv6Address())
                .build();
        
        return repository.save(record);
    }
    
    @MutationMapping
    public Boolean deleteIpfixRecord(@Argument String id) {
        return repository.deleteById(id);
    }
    
    @MutationMapping
    public Boolean deleteAllIpfixRecords() {
        repository.deleteAll();
        return true;
    }
    
    private DpiInfo convertDpiInfo(DpiInfoInput input) {
        if (input == null) return null;
        return DpiInfo.builder()
                .httpRequestMethod(input.getHttpRequestMethod())
                .httpRequestHost(input.getHttpRequestHost())
                .httpRequestTarget(input.getHttpRequestTarget())
                .httpUserAgent(input.getHttpUserAgent())
                .httpStatusCode(input.getHttpStatusCode())
                .sslServerName(input.getSslServerName())
                .sslCertificateIssuer(input.getSslCertificateIssuer())
                .sslCertificateSubject(input.getSslCertificateSubject())
                .sslCipherSuite(input.getSslCipherSuite())
                .sslVersion(input.getSslVersion())
                .dnsQueryName(input.getDnsQueryName())
                .dnsQueryType(input.getDnsQueryType())
                .dnsResponseCode(input.getDnsResponseCode())
                .dnsResponseName(input.getDnsResponseName())
                .applicationProtocol(input.getApplicationProtocol())
                .applicationLayerProtocolId(input.getApplicationLayerProtocolId())
                .build();
    }
    
    private BidirectionalFlowInfo convertBidirectionalFlowInfo(BidirectionalFlowInfoInput input) {
        if (input == null) return null;
        return BidirectionalFlowInfo.builder()
                .biflowDirection(input.getBiflowDirection())
                .reverseOctetDeltaCount(input.getReverseOctetDeltaCount())
                .reversePacketDeltaCount(input.getReversePacketDeltaCount())
                .reverseFlowStartMilliseconds(input.getReverseFlowStartMilliseconds())
                .reverseFlowEndMilliseconds(input.getReverseFlowEndMilliseconds())
                .reverseTcpControlBits(input.getReverseTcpControlBits())
                .flowDurationMilliseconds(input.getFlowDurationMilliseconds())
                .flowDurationMicroseconds(input.getFlowDurationMicroseconds())
                .minimumIpTotalLength(input.getMinimumIpTotalLength())
                .maximumIpTotalLength(input.getMaximumIpTotalLength())
                .reverseMinimumIpTotalLength(input.getReverseMinimumIpTotalLength())
                .reverseMaximumIpTotalLength(input.getReverseMaximumIpTotalLength())
                .minimumTTL(input.getMinimumTTL())
                .maximumTTL(input.getMaximumTTL())
                .reverseMinimumTTL(input.getReverseMinimumTTL())
                .reverseMaximumTTL(input.getReverseMaximumTTL())
                .flowEndReason(input.getFlowEndReason())
                .flowDirection(input.getFlowDirection())
                .build();
    }
    
    private CertEnterpriseInfo convertCertInfo(CertEnterpriseInfoInput input) {
        if (input == null) return null;
        return CertEnterpriseInfo.builder()
                .silkAppLabel(input.getSilkAppLabel())
                .payloadEntropy(input.getPayloadEntropy())
                .initialTCPFlags(input.getInitialTCPFlags())
                .unionTCPFlags(input.getUnionTCPFlags())
                .reverseFlowDeltaMilliseconds(input.getReverseFlowDeltaMilliseconds())
                .flowAttributes(input.getFlowAttributes())
                .flowKeyHash(input.getFlowKeyHash())
                .osName(input.getOsName())
                .osVersion(input.getOsVersion())
                .osFingerprint(input.getOsFingerprint())
                .payloadContent(input.getPayloadContent())
                .payloadLength(input.getPayloadLength())
                .reversePayloadContent(input.getReversePayloadContent())
                .reversePayloadLength(input.getReversePayloadLength())
                .vlanId(input.getVlanId())
                .reverseVlanId(input.getReverseVlanId())
                .sourceMacAddress(input.getSourceMacAddress())
                .destinationMacAddress(input.getDestinationMacAddress())
                .ingressInterfaceName(input.getIngressInterfaceName())
                .egressInterfaceName(input.getEgressInterfaceName())
                .build();
    }
    
    private java.util.List<BasicListElement> convertBasicLists(java.util.List<BasicListElementInput> inputs) {
        if (inputs == null) return new ArrayList<>();
        return inputs.stream()
                .map(input -> BasicListElement.builder()
                        .id(UUID.randomUUID().toString())
                        .informationElementId(input.getInformationElementId())
                        .informationElementName(input.getInformationElementName())
                        .semantic(input.getSemantic())
                        .values(input.getValues())
                        .dataType(input.getDataType())
                        .build())
                .collect(Collectors.toList());
    }
    
    private java.util.List<SubTemplateListElement> convertSubTemplateLists(java.util.List<SubTemplateListElementInput> inputs) {
        if (inputs == null) return new ArrayList<>();
        return inputs.stream()
                .map(input -> SubTemplateListElement.builder()
                        .id(UUID.randomUUID().toString())
                        .templateId(input.getTemplateId())
                        .templateName(input.getTemplateName())
                        .semantic(input.getSemantic())
                        .entries(input.getEntries())
                        .templateDefinition(input.getTemplateDefinition())
                        .build())
                .collect(Collectors.toList());
    }
    
    private java.util.List<SubTemplateMultiListElement> convertSubTemplateMultiLists(java.util.List<SubTemplateMultiListElementInput> inputs) {
        if (inputs == null) return new ArrayList<>();
        return inputs.stream()
                .map(input -> SubTemplateMultiListElement.builder()
                        .id(UUID.randomUUID().toString())
                        .semantic(input.getSemantic())
                        .subTemplateLists(convertSubTemplateLists(input.getSubTemplateLists()))
                        .description(input.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
