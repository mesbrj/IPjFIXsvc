package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deep Packet Inspection Information Elements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpiInfo {
    
    // HTTP-related elements
    private String httpRequestMethod;       // IE 351
    private String httpRequestHost;         // IE 352
    private String httpRequestTarget;       // IE 353
    private String httpUserAgent;           // IE 358
    private Integer httpStatusCode;         // IE 361
    
    // SSL/TLS elements
    private String sslServerName;           // IE 187
    private String sslCertificateIssuer;    // IE 188
    private String sslCertificateSubject;   // IE 189
    private String sslCipherSuite;          // IE 185
    private Integer sslVersion;             // IE 186
    
    // DNS elements
    private String dnsQueryName;            // IE 401
    private Integer dnsQueryType;           // IE 402
    private Integer dnsResponseCode;        // IE 403
    private String dnsResponseName;         // IE 404
    
    // Application layer protocol
    private String applicationProtocol;     // IE 193
    private Integer applicationLayerProtocolId; // IE 195
}
