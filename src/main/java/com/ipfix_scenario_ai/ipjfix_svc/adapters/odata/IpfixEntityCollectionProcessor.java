package com.ipfix_scenario_ai.ipjfix_svc.adapters.odata;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ipfix_scenario_ai.ipjfix_svc.adapters.ignite.UserRepository;
import com.ipfix_scenario_ai.ipjfix_svc.adapters.lucene.core.LuceneService;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.FlowRecord;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

@Component
public class IpfixEntityCollectionProcessor implements EntityCollectionProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(IpfixEntityCollectionProcessor.class);
    
    private final UserRepository userRepository;
    private final LuceneService luceneService;
    private OData odata;
    private ServiceMetadata serviceMetadata;
    private org.apache.olingo.commons.api.edm.EdmEntityContainer edmEntityContainer;
    
    public IpfixEntityCollectionProcessor(UserRepository userRepository, LuceneService luceneService) {
        this.userRepository = userRepository;
        this.luceneService = luceneService;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        logger.info("=== IpfixEntityCollectionProcessor init() called ===");
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
        
        // Debug the service metadata structure
        try {
            var edm = serviceMetadata.getEdm();
            logger.info("ServiceMetadata.getEdm(): {}", edm != null ? edm.getClass().getSimpleName() : "null");
            
            if (edm != null) {
                var entityContainer = edm.getEntityContainer();
                logger.info("EDM.getEntityContainer(): {}", entityContainer != null ? entityContainer.getName() : "null");
                
                // Try to get the container by name
                var schemas = edm.getSchemas();
                logger.info("EDM has {} schemas", schemas.size());
                
                for (var schema : schemas) {
                    logger.info("Schema namespace: {}, entityContainer: {}", 
                        schema.getNamespace(), 
                        schema.getEntityContainer() != null ? schema.getEntityContainer().getName() : "null");
                }
            }
        } catch (Exception e) {
            logger.error("Error accessing EDM in init", e);
        }
        
        this.edmEntityContainer = serviceMetadata.getEdm().getEntityContainer();
        logger.info("Processor initialized successfully");
        logger.info("EDM Entity Container: {}", this.edmEntityContainer != null ? this.edmEntityContainer.getName() : "null");
    }

    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, 
                                   UriInfo uriInfo, ContentType responseFormat) 
                                   throws ODataApplicationException, ODataLibraryException {
        
        logger.info("=== readEntityCollection called ===");
        logger.info("Request URI: {}", request.getRawRequestUri());
        logger.info("Request method: {}", request.getMethod());
        
        try {
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            logger.info("Resource parts count: {}", resourceParts.size());
            
            if (resourceParts.isEmpty()) {
                logger.error("No resource parts found in URI");
                throw new ODataApplicationException("Invalid URI", 
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            UriResource firstResourceSegment = resourceParts.get(0);
            logger.info("First resource segment type: {}", firstResourceSegment.getClass().getSimpleName());
            
            if (!(firstResourceSegment instanceof UriResourceEntitySet)) {
                logger.error("First resource segment is not UriResourceEntitySet");
                throw new ODataApplicationException("Expected entity set", 
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) firstResourceSegment;
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
            
            logger.info("Entity set name: '{}'", edmEntitySet.getName());
            logger.info("Comparing with ES_USERS_NAME: '{}'", IpfixEdmProvider.ES_USERS_NAME);
            
            EntityCollection entityCollection = new EntityCollection();
            
            // Route to appropriate handler based on entity set name
            switch (edmEntitySet.getName()) {
                case IpfixEdmProvider.ES_USERS_NAME:
                    logger.info("Routing to handleUsersCollection");
                    handleUsersCollection(entityCollection, uriInfo);
                    break;
                case IpfixEdmProvider.ES_FLOW_RECORDS_NAME:
                    logger.info("Routing to handleFlowRecordsCollection");
                    handleFlowRecordsCollection(entityCollection, uriInfo);
                    break;
                default:
                    logger.error("Unknown entity set: '{}'", edmEntitySet.getName());
                    throw new ODataApplicationException("Unknown entity set: " + edmEntitySet.getName(), 
                        HttpStatusCode.NOT_FOUND.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            // Serialize and respond
            logger.info("Serializing response with {} entities", entityCollection.getEntities().size());
            serializeAndRespond(response, responseFormat, edmEntitySet, entityCollection, request);
            
        } catch (ODataApplicationException e) {
            throw e;
        } catch (ODataLibraryException e) {
            throw new ODataApplicationException("Internal server error", 
                HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), java.util.Locale.ENGLISH, e);
        }
    }
    
    private void handleUsersCollection(EntityCollection entityCollection, UriInfo uriInfo) {
        logger.info("Handling Users collection request");
        
        // Extract query options from UriInfo
        String tenantFilter = extractTenantFromUriInfo(uriInfo);
        String roleFilter = extractRoleFromUriInfo(uriInfo);
        boolean activeOnly = extractActiveFilterFromUriInfo(uriInfo);

        logger.debug("Query filters - tenant: {}, role: {}, activeOnly: {}", tenantFilter, roleFilter, activeOnly);

        List<User> users;

        // Apply filters based on query parameters
        if (tenantFilter != null && roleFilter != null) {
            users = userRepository.findUsersByTenantAndRole(tenantFilter, roleFilter);
        } else if (tenantFilter != null) {
            users = userRepository.findByTenantId(tenantFilter);
        } else if (roleFilter != null) {
            users = userRepository.findByRole(roleFilter);
        } else if (activeOnly) {
            users = userRepository.findActiveUsers();
        } else {
            logger.info("No filters applied, calling findAll()");
            users = userRepository.findAll();
        }

        logger.info("Retrieved {} users from repository", users.size());

        // Apply sorting if specified in UriInfo
        users = applySorting(users, uriInfo);

        // Apply paging if specified
        users = applyPaging(users, uriInfo);

        logger.info("After sorting and paging: {} users", users.size());

        for (User user : users) {
            entityCollection.getEntities().add(createUserEntity(user));
        }
        
        logger.info("Added {} entities to collection", entityCollection.getEntities().size());
    }
    
    private void handleFlowRecordsCollection(EntityCollection entityCollection, UriInfo uriInfo) {
        logger.info("Handling FlowRecords collection request");
        
        try {
            String tenantId = extractTenantFromUriInfo(uriInfo);
            if (tenantId == null) {
                tenantId = "default";
            }

            logger.debug("FlowRecords query for tenant: {}", tenantId);

            // Get all flow records first
            List<FlowRecord> flowRecords = luceneService.searchFlowRecords(tenantId, "*:*");
            logger.info("Retrieved {} total flow records from Lucene", flowRecords.size());

            // Apply OData filters if present
            if (uriInfo.getFilterOption() != null) {
                flowRecords = applyODataFilter(flowRecords, uriInfo.getFilterOption());
                logger.info("After applying OData filter: {} flow records", flowRecords.size());
            } else {
                // Fall back to custom filters for backward compatibility
                flowRecords = applyCustomFilters(flowRecords, uriInfo);
                logger.info("After applying custom filters: {} flow records", flowRecords.size());
            }

            // Apply sorting and paging
            flowRecords = applySortingToFlowRecords(flowRecords, uriInfo);
            flowRecords = applyPagingToFlowRecords(flowRecords, uriInfo);

            logger.info("After sorting and paging: {} flow records", flowRecords.size());

            for (FlowRecord flowRecord : flowRecords) {
                entityCollection.getEntities().add(createFlowRecordEntity(flowRecord));
            }
            
            logger.info("Added {} flow record entities to collection", entityCollection.getEntities().size());
            
        } catch (Exception e) {
            logger.error("Error handling FlowRecords collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve flow records", e);
        }
    }

    /**
     * Apply OData $filter expressions to flow records using filter text parsing
     */
    private List<FlowRecord> applyODataFilter(List<FlowRecord> flowRecords, FilterOption filterOption) 
            throws ODataApplicationException {
        
        logger.info("Applying OData filter: {}", filterOption.getText());
        
        try {
            String filterText = filterOption.getText();
            
            // Filter the records using our custom filter parser
            return flowRecords.stream()
                .filter(flowRecord -> evaluateFilterText(filterText, flowRecord))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error applying OData filter: {}", e.getMessage(), e);
            throw new ODataApplicationException("Invalid filter expression: " + e.getMessage(), 
                HttpStatusCode.BAD_REQUEST.getStatusCode(), java.util.Locale.ENGLISH);
        }
    }
    
    /**
     * Evaluate a filter text against a FlowRecord using simple text parsing
     */
    private boolean evaluateFilterText(String filterText, FlowRecord flowRecord) {
        try {
            // Handle simple expressions like "Protocol eq 'UDP'"
            filterText = filterText.trim();
            
            // Handle AND expressions
            if (filterText.contains(" and ")) {
                String[] parts = filterText.split(" and ");
                for (String part : parts) {
                    if (!evaluateSimpleFilter(part.trim(), flowRecord)) {
                        return false;
                    }
                }
                return true;
            }
            
            // Handle OR expressions  
            if (filterText.contains(" or ")) {
                String[] parts = filterText.split(" or ");
                for (String part : parts) {
                    if (evaluateSimpleFilter(part.trim(), flowRecord)) {
                        return true;
                    }
                }
                return false;
            }
            
            // Handle single expression
            return evaluateSimpleFilter(filterText, flowRecord);
            
        } catch (Exception e) {
            logger.warn("Error evaluating filter '{}' for record {}: {}", filterText, flowRecord.getId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluate a simple filter expression like "Protocol eq 'UDP'" or "Bytes gt 1000"
     */
    private boolean evaluateSimpleFilter(String filterExpression, FlowRecord flowRecord) {
        filterExpression = filterExpression.trim();
        
        // Parse expressions like "property operator value"
        String[] parts = filterExpression.split("\\s+");
        if (parts.length < 3) {
            logger.warn("Invalid filter expression format: {}", filterExpression);
            return false;
        }
        
        String property = parts[0];
        String operator = parts[1];
        String value = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
        
        // Remove quotes from string values
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        
        // Get the actual property value from the flow record
        Object propertyValue = getFlowRecordProperty(property, flowRecord);
        if (propertyValue == null) {
            return false;
        }
        
        // Evaluate based on operator
        switch (operator.toLowerCase()) {
            case "eq":
                return compareEqual(propertyValue, value);
            case "ne":
                return !compareEqual(propertyValue, value);
            case "gt":
                return compareNumeric(propertyValue, value, ">", false);
            case "ge":
                return compareNumeric(propertyValue, value, ">", true);
            case "lt":
                return compareNumeric(propertyValue, value, "<", false);
            case "le":
                return compareNumeric(propertyValue, value, "<", true);
            default:
                logger.warn("Unsupported operator: {}", operator);
                return false;
        }
    }
    
    /**
     * Get a property value from a FlowRecord by property name
     */
    private Object getFlowRecordProperty(String propertyName, FlowRecord flowRecord) {
        switch (propertyName.toLowerCase()) {
            case "id":
                return flowRecord.getId();
            case "sourceip":
                return flowRecord.getSourceIP();
            case "destip":
                return flowRecord.getDestIP();
            case "sourceport":
                return flowRecord.getSourcePort();
            case "destport":
                return flowRecord.getDestPort();
            case "protocol":
                return flowRecord.getProtocol();
            case "bytes":
                return flowRecord.getBytes();
            case "packets":
                return flowRecord.getPackets();
            case "reversebytes":
                return flowRecord.getReverseBytes();
            case "reversepackets":
                return flowRecord.getReversePackets();
            case "tcpflags":
                return flowRecord.getTcpFlags();
            case "tosvalue":
                return flowRecord.getTosValue();
            default:
                logger.warn("Unknown property: {}", propertyName);
                return null;
        }
    }
    
    /**
     * Compare two values for equality
     */
    private boolean compareEqual(Object propertyValue, String filterValue) {
        if (propertyValue == null) return false;
        
        // Handle string comparison (case-insensitive for protocol)
        if (propertyValue instanceof String) {
            return ((String) propertyValue).equalsIgnoreCase(filterValue);
        }
        
        // Handle numeric comparison
        if (propertyValue instanceof Number) {
            try {
                if (filterValue.contains(".")) {
                    return ((Number) propertyValue).doubleValue() == Double.parseDouble(filterValue);
                } else {
                    return ((Number) propertyValue).longValue() == Long.parseLong(filterValue);
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return propertyValue.toString().equals(filterValue);
    }
    
    /**
     * Compare numeric values
     */
    private boolean compareNumeric(Object propertyValue, String filterValue, String operator, boolean orEqual) {
        if (!(propertyValue instanceof Number)) {
            return false;
        }
        
        try {
            double propVal = ((Number) propertyValue).doubleValue();
            double filterVal = Double.parseDouble(filterValue);
            
            if (operator.equals(">")) {
                return orEqual ? propVal >= filterVal : propVal > filterVal;
            } else if (operator.equals("<")) {
                return orEqual ? propVal <= filterVal : propVal < filterVal;
            }
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid numeric value in filter: {}", filterValue);
        }
        
        return false;
    }
    
    /**
     * Apply custom filters for backward compatibility
     */
    private List<FlowRecord> applyCustomFilters(List<FlowRecord> flowRecords, UriInfo uriInfo) {
        String sourceIP = extractSourceIPFromUriInfo(uriInfo);
        String destIP = extractDestIPFromUriInfo(uriInfo);
        String protocol = extractProtocolFromUriInfo(uriInfo);
        Long minBytes = extractMinBytesFromUriInfo(uriInfo);
        
        logger.debug("Applying custom filters - sourceIP: {}, destIP: {}, protocol: {}, minBytes: {}", 
            sourceIP, destIP, protocol, minBytes);
            
        return flowRecords.stream()
            .filter(record -> sourceIP == null || sourceIP.equals(record.getSourceIP()))
            .filter(record -> destIP == null || destIP.equals(record.getDestIP()))
            .filter(record -> protocol == null || protocol.equalsIgnoreCase(record.getProtocol()))
            .filter(record -> minBytes == null || record.getBytes() >= minBytes)
            .collect(Collectors.toList());
    }

// Extract tenant ID from query parameters or custom headers
  private String extractTenantFromUriInfo(UriInfo uriInfo) {
      // Check for $filter parameter: tenantId eq 'value'
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          if (filterExpression.contains("tenantId eq")) {
              return extractValueFromFilter(filterExpression, "tenantId eq");
          }
      }

      // Check for custom query parameters
      if (uriInfo.getCustomQueryOptions() != null) {
          for (org.apache.olingo.server.api.uri.queryoption.CustomQueryOption option : uriInfo.getCustomQueryOptions()) {
              if ("tenant".equals(option.getName())) {
                  return option.getText();
              }
          }
      }

      return null;
  }

  private String extractSourceIPFromUriInfo(UriInfo uriInfo) {
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          if (filterExpression.contains("sourceIP eq")) {
              return extractValueFromFilter(filterExpression, "sourceIP eq");
          }
      }
      
      // Check custom query options for IPFIX-specific parameters
      if (uriInfo.getCustomQueryOptions() != null) {
          for (org.apache.olingo.server.api.uri.queryoption.CustomQueryOption option : uriInfo.getCustomQueryOptions()) {
              if ("sourceIP".equals(option.getName())) {
                  return option.getText();
              }
          }
      }
      
      return null;
  }

  private String extractDestIPFromUriInfo(UriInfo uriInfo) {
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          if (filterExpression.contains("destIP eq")) {
              return extractValueFromFilter(filterExpression, "destIP eq");
          }
      }

      if (uriInfo.getCustomQueryOptions() != null) {
          for (org.apache.olingo.server.api.uri.queryoption.CustomQueryOption option : uriInfo.getCustomQueryOptions()) {
              if ("destIP".equals(option.getName())) {
                  return option.getText();
              }
          }
      }

      return null;
  }

  private String extractProtocolFromUriInfo(UriInfo uriInfo) {
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          if (filterExpression.contains("protocol eq")) {
              return extractValueFromFilter(filterExpression, "protocol eq");
          }
      }

      if (uriInfo.getCustomQueryOptions() != null) {
          for (org.apache.olingo.server.api.uri.queryoption.CustomQueryOption option : uriInfo.getCustomQueryOptions()) {
              if ("protocol".equals(option.getName())) {
                  return option.getText();
              }
          }
      }

      return null;
  }

  @SuppressWarnings("UnnecessaryTemporaryOnConversionFromString")
  private Long extractMinBytesFromUriInfo(UriInfo uriInfo) {
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          if (filterExpression.contains("bytes ge")) {
              String value = extractValueFromFilter(filterExpression, "bytes ge");
              if (value != null) {
                  try {
                      return Long.parseLong(value);
                  } catch (NumberFormatException e) {
                      return null;
                  }
              }
          }
      }

      if (uriInfo.getCustomQueryOptions() != null) {
          for (org.apache.olingo.server.api.uri.queryoption.CustomQueryOption option : uriInfo.getCustomQueryOptions()) {
              if ("minBytes".equals(option.getName())) {
                  try {
                      return Long.parseLong(option.getText());
                  } catch (NumberFormatException e) {
                      return null;
                  }
              }
          }
      }

      return null;
  }

  private String extractRoleFromUriInfo(UriInfo uriInfo) {
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          if (filterExpression.contains("role eq")) {
              return extractValueFromFilter(filterExpression, "role eq");
          }
      }
      return null;
  }

  private List<User> applySorting(List<User> users, UriInfo uriInfo) {
      if (uriInfo.getOrderByOption() != null) {
          String orderBy = uriInfo.getOrderByOption().getText();

          if (orderBy.contains("username")) {
              users.sort((u1, u2) -> {
                  String name1 = u1.getUsername() != null ? u1.getUsername() : "";
                  String name2 = u2.getUsername() != null ? u2.getUsername() : "";
                  return orderBy.contains("desc") ? name2.compareTo(name1) : name1.compareTo(name2);
              });
          } else if (orderBy.contains("email")) {
              users.sort((u1, u2) -> {
                  String email1 = u1.getEmail() != null ? u1.getEmail() : "";
                  String email2 = u2.getEmail() != null ? u2.getEmail() : "";
                  return orderBy.contains("desc") ? email2.compareTo(email1) : email1.compareTo(email2);
              });
          } else if (orderBy.contains("createdAt")) {
              users.sort((u1, u2) -> {
                  if (u1.getCreatedAt() == null) return 1;
                  if (u2.getCreatedAt() == null) return -1;
                  return orderBy.contains("desc") ? 
                      u2.getCreatedAt().compareTo(u1.getCreatedAt()) : 
                      u1.getCreatedAt().compareTo(u2.getCreatedAt());
              });
          }
      }
      return users;
  }

  private List<User> applyPaging(List<User> users, UriInfo uriInfo) {
      int skip = 0;
      int top = Integer.MAX_VALUE;

      if (uriInfo.getSkipOption() != null) {
          skip = uriInfo.getSkipOption().getValue();
      }

      if (uriInfo.getTopOption() != null) {
          top = uriInfo.getTopOption().getValue();
      }

      return users.stream()
              .skip(skip)
              .limit(top)
              .collect(java.util.stream.Collectors.toList());
  }

  private List<FlowRecord> applySortingToFlowRecords(List<FlowRecord> flowRecords, UriInfo uriInfo) {
      if (uriInfo.getOrderByOption() != null) {
          String orderBy = uriInfo.getOrderByOption().getText();

          if (orderBy.contains("timestamp")) {
              flowRecords.sort((f1, f2) -> {
                  if (f1.getTimestamp() == null) return 1;
                  if (f2.getTimestamp() == null) return -1;
                  return orderBy.contains("desc") ? 
                      f2.getTimestamp().compareTo(f1.getTimestamp()) : 
                      f1.getTimestamp().compareTo(f2.getTimestamp());
              });
          } else if (orderBy.contains("bytes")) {
              flowRecords.sort((f1, f2) -> {
                  long bytes1 = f1.getBytes();
                  long bytes2 = f2.getBytes();
                  return orderBy.contains("desc") ? 
                      Long.compare(bytes2, bytes1) : 
                      Long.compare(bytes1, bytes2);
              });
          }
      }
      return flowRecords;
  }

  private List<FlowRecord> applyPagingToFlowRecords(List<FlowRecord> flowRecords, UriInfo uriInfo) {
    int skip = 0;
    int top = Integer.MAX_VALUE;
    
    if (uriInfo.getSkipOption() != null) {
        skip = uriInfo.getSkipOption().getValue();
    }
    
    if (uriInfo.getTopOption() != null) {
        top = uriInfo.getTopOption().getValue();
    }
    
    return flowRecords.stream()
            .skip(skip)
            .limit(top)
            .collect(java.util.stream.Collectors.toList());
  }

  private boolean extractActiveFilterFromUriInfo(UriInfo uriInfo) {
      if (uriInfo.getFilterOption() != null) {
          String filterExpression = uriInfo.getFilterOption().getText();
          return filterExpression.contains("active eq true");
      }
      return false;
  }

  private String extractValueFromFilter(String filterExpression, String operator) {
      int index = filterExpression.indexOf(operator);
      if (index != -1) {
          String remainder = filterExpression.substring(index + operator.length()).trim();
          // Remove quotes if present
          if (remainder.startsWith("'") && remainder.contains("'")) {
              int endQuote = remainder.indexOf("'", 1);
              if (endQuote != -1) {
                  return remainder.substring(1, endQuote);
              }
          }
          // Handle unquoted values
          String[] parts = remainder.split("\\s+");
          if (parts.length > 0) {
              return parts[0];
          }
      }
      return null;
  }



    private void serializeAndRespond(ODataResponse response, ContentType responseFormat, 
                                    EdmEntitySet edmEntitySet, EntityCollection entityCollection,
                                    ODataRequest request) throws ODataLibraryException {
        
        try {
            logger.info("Starting serialization of {} entities", entityCollection.getEntities().size());
            
            ODataSerializer serializer = odata.createSerializer(responseFormat);
            
            // Create context URL for OData v4 compliance
            String baseUrl = request.getRawBaseUri();
            String contextUrl = baseUrl + "/$metadata#" + edmEntitySet.getName();
            
            EntityCollectionSerializerOptions options = EntityCollectionSerializerOptions.with()
                .id(baseUrl + "/" + edmEntitySet.getName())
                .contextURL(ContextURL.with().entitySet(edmEntitySet).build())
                .build();
            
            logger.debug("Created serializer options with ID: {} and context URL: {}", 
                baseUrl + "/" + edmEntitySet.getName(), contextUrl);
            
            SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, 
                edmEntitySet.getEntityType(), entityCollection, options);
            
            logger.info("Serialization completed successfully");
            
            response.setContent(serializerResult.getContent());
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
            
        } catch (Exception e) {
            logger.error("Error during serialization: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private Entity createUserEntity(User user) {
        try {
            logger.debug("Creating entity for user: {} ({})", user.getUsername(), user.getId());
            
            Entity entity = new Entity();
            
            entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, user.getId()));
            entity.addProperty(new Property(null, "Username", ValueType.PRIMITIVE, user.getUsername()));
            entity.addProperty(new Property(null, "Email", ValueType.PRIMITIVE, user.getEmail()));
            entity.addProperty(new Property(null, "FirstName", ValueType.PRIMITIVE, user.getFirstName()));
            entity.addProperty(new Property(null, "LastName", ValueType.PRIMITIVE, user.getLastName()));
            entity.addProperty(new Property(null, "Role", ValueType.PRIMITIVE, user.getRole()));
            entity.addProperty(new Property(null, "Active", ValueType.PRIMITIVE, user.isActive()));
            entity.addProperty(new Property(null, "TenantId", ValueType.PRIMITIVE, user.getTenantId()));
            
            // Handle datetime fields more carefully
            if (user.getCreatedAt() != null) {
                try {
                    java.time.Instant createdAtInstant = user.getCreatedAt().toInstant(ZoneOffset.UTC);
                    logger.debug("Converting CreatedAt: {} -> {}", user.getCreatedAt(), createdAtInstant);
                    entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE, createdAtInstant));
                } catch (Exception e) {
                    logger.warn("Failed to convert CreatedAt for user {}: {}", user.getId(), e.getMessage());
                    entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE, null));
                }
            } else {
                entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE, null));
            }
            
            if (user.getLastLoginAt() != null) {
                try {
                    java.time.Instant lastLoginInstant = user.getLastLoginAt().toInstant(ZoneOffset.UTC);
                    logger.debug("Converting LastLoginAt: {} -> {}", user.getLastLoginAt(), lastLoginInstant);
                    entity.addProperty(new Property(null, "LastLoginAt", ValueType.PRIMITIVE, lastLoginInstant));
                } catch (Exception e) {
                    logger.warn("Failed to convert LastLoginAt for user {}: {}", user.getId(), e.getMessage());
                    entity.addProperty(new Property(null, "LastLoginAt", ValueType.PRIMITIVE, null));
                }
            } else {
                entity.addProperty(new Property(null, "LastLoginAt", ValueType.PRIMITIVE, null));
            }
            
            entity.setId(createId("Users", user.getId()));
            logger.debug("Created entity with ID: {}", entity.getId());
            
            return entity;
        } catch (Exception e) {
            logger.error("Error creating entity for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create entity for user: " + user.getId(), e);
        }
    }
    
    private Entity createFlowRecordEntity(FlowRecord flowRecord) {
        Entity entity = new Entity();
        
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, flowRecord.getId()));
        entity.addProperty(new Property(null, "SourceIP", ValueType.PRIMITIVE, flowRecord.getSourceIP()));
        entity.addProperty(new Property(null, "DestIP", ValueType.PRIMITIVE, flowRecord.getDestIP()));
        entity.addProperty(new Property(null, "SourcePort", ValueType.PRIMITIVE, flowRecord.getSourcePort()));
        entity.addProperty(new Property(null, "DestPort", ValueType.PRIMITIVE, flowRecord.getDestPort()));
        entity.addProperty(new Property(null, "Protocol", ValueType.PRIMITIVE, flowRecord.getProtocol()));
        entity.addProperty(new Property(null, "Bytes", ValueType.PRIMITIVE, flowRecord.getBytes()));
        entity.addProperty(new Property(null, "Packets", ValueType.PRIMITIVE, flowRecord.getPackets()));
        entity.addProperty(new Property(null, "ReverseBytes", ValueType.PRIMITIVE, flowRecord.getReverseBytes()));
        entity.addProperty(new Property(null, "ReversePackets", ValueType.PRIMITIVE, flowRecord.getReversePackets()));
        entity.addProperty(new Property(null, "TcpFlags", ValueType.PRIMITIVE, flowRecord.getTcpFlags()));
        entity.addProperty(new Property(null, "TosValue", ValueType.PRIMITIVE, flowRecord.getTosValue()));
        
        if (flowRecord.getTimestamp() != null) {
            entity.addProperty(new Property(null, "Timestamp", ValueType.PRIMITIVE,
                flowRecord.getTimestamp().toInstant()));
        }
        
        if (flowRecord.getFlowStartTime() != null) {
            entity.addProperty(new Property(null, "FlowStartTime", ValueType.PRIMITIVE,
                flowRecord.getFlowStartTime().toInstant()));
        }
        
        if (flowRecord.getFlowEndTime() != null) {
            entity.addProperty(new Property(null, "FlowEndTime", ValueType.PRIMITIVE,
                flowRecord.getFlowEndTime().toInstant()));
        }
        
        entity.setId(createId("FlowRecords", flowRecord.getId()));
        return entity;
    }
    
    // SINGLE createId method - remove any duplicates
    private java.net.URI createId(String entitySetName, Object id) {
        try {
            String uriString = String.format("%s('%s')", entitySetName, id.toString());
            return java.net.URI.create(uriString);
        } catch (Exception e) {
            throw new RuntimeException("Error creating entity ID", e);
        }
    }
}