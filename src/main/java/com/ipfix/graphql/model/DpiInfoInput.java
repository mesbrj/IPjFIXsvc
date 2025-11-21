package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpiInfoInput {
    private String httpRequestMethod;
    private String httpRequestHost;
    private String httpRequestTarget;
    private String httpUserAgent;
    private Integer httpStatusCode;
    private String sslServerName;
    private String sslCertificateIssuer;
    private String sslCertificateSubject;
    private String sslCipherSuite;
    private Integer sslVersion;
    private String dnsQueryName;
    private Integer dnsQueryType;
    private Integer dnsResponseCode;
    private String dnsResponseName;
    private String applicationProtocol;
    private Integer applicationLayerProtocolId;
}
