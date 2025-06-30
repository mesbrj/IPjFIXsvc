package com.ipfix_scenario_ai.ipjfix_svc.adapters.odata;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.apache.olingo.commons.api.data.Entity;
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
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ipfix_scenario_ai.ipjfix_svc.adapters.ignite.UserRepository;
import com.ipfix_scenario_ai.ipjfix_svc.adapters.lucene.core.LuceneService;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.FlowRecord;
import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

@Component
public class IpfixEntityProcessor implements EntityProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(IpfixEntityProcessor.class);
    
    private final UserRepository userRepository;
    private final LuceneService luceneService;
    private OData odata;
    private ServiceMetadata serviceMetadata;
    
    public IpfixEntityProcessor(UserRepository userRepository, LuceneService luceneService) {
        this.userRepository = userRepository;
        this.luceneService = luceneService;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        logger.info("=== IpfixEntityProcessor init() called ===");
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
        logger.info("Entity processor initialized successfully");
    }

    @Override
    public void readEntity(ODataRequest request, ODataResponse response, 
                          UriInfo uriInfo, ContentType responseFormat) 
                          throws ODataApplicationException, ODataLibraryException {
        
        try {
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            
            if (resourceParts.isEmpty()) {
                throw new ODataApplicationException("Invalid URI", 
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            UriResource firstResourceSegment = resourceParts.get(0);
            
            if (!(firstResourceSegment instanceof UriResourceEntitySet)) {
                throw new ODataApplicationException("Expected entity set", 
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) firstResourceSegment;
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
            List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
            
            Entity entity = null;
            
            switch (edmEntitySet.getName()) {
                case IpfixEdmProvider.ES_USERS_NAME:
                    entity = handleUserEntity(keyPredicates);
                    break;
                case IpfixEdmProvider.ES_FLOW_RECORDS_NAME:
                    entity = handleFlowRecordEntity(keyPredicates);
                    break;
                default:
                    throw new ODataApplicationException("Unknown entity set: " + edmEntitySet.getName(), 
                        HttpStatusCode.NOT_FOUND.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            if (entity == null) {
                throw new ODataApplicationException("Entity not found", 
                    HttpStatusCode.NOT_FOUND.getStatusCode(), java.util.Locale.ENGLISH);
            }
            
            serializeAndRespond(response, responseFormat, edmEntitySet, entity);
            
        } catch (ODataApplicationException e) {
            throw e;
        } catch (ODataLibraryException | RuntimeException e) {
            throw new ODataApplicationException("Internal server error", 
                HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), java.util.Locale.ENGLISH, e);
        }
    }

    private Entity handleUserEntity(List<UriParameter> keyPredicates) {
        if (keyPredicates.isEmpty()) {
            return null;
        }
        
        String userId = keyPredicates.get(0).getText().replace("'", "");
        Optional<User> userOpt = userRepository.findById(userId);
        
        return userOpt.map(this::createUserEntity).orElse(null);
    }

    private Entity handleFlowRecordEntity(List<UriParameter> keyPredicates) {
        if (keyPredicates.isEmpty()) {
            return null;
        }
        
        String flowRecordId = keyPredicates.get(0).getText().replace("'", "");
        
        // Use Lucene to find the specific flow record
        List<FlowRecord> flowRecords = luceneService.searchFlowRecords("default", "id:" + flowRecordId);
        
        if (!flowRecords.isEmpty()) {
            return createFlowRecordEntity(flowRecords.get(0));
        }
        
        return null;
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
        
        if (flowRecord.getTimestamp() != null) {
            entity.addProperty(new Property(null, "Timestamp", ValueType.PRIMITIVE,
                flowRecord.getTimestamp().toInstant()));
        }
        
        entity.setId(createId("FlowRecords", flowRecord.getId()));
        return entity;
    }

    private Entity createUserEntity(User user) {
        Entity entity = new Entity();
        
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, user.getId()));
        entity.addProperty(new Property(null, "Username", ValueType.PRIMITIVE, user.getUsername()));
        entity.addProperty(new Property(null, "Email", ValueType.PRIMITIVE, user.getEmail()));
        entity.addProperty(new Property(null, "FirstName", ValueType.PRIMITIVE, user.getFirstName()));
        entity.addProperty(new Property(null, "LastName", ValueType.PRIMITIVE, user.getLastName()));
        entity.addProperty(new Property(null, "Role", ValueType.PRIMITIVE, user.getRole()));
        entity.addProperty(new Property(null, "Active", ValueType.PRIMITIVE, user.isActive()));
        entity.addProperty(new Property(null, "TenantId", ValueType.PRIMITIVE, user.getTenantId()));
        
        if (user.getCreatedAt() != null) {
            entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE, 
                user.getCreatedAt().atOffset(ZoneOffset.UTC)));
        }
        
        if (user.getLastLoginAt() != null) {
            entity.addProperty(new Property(null, "LastLoginAt", ValueType.PRIMITIVE, 
                user.getLastLoginAt().atOffset(ZoneOffset.UTC)));
        }
        
        entity.setId(createId("Users", user.getId()));
        return entity;
    }

    private void serializeAndRespond(ODataResponse response, ContentType responseFormat, 
                                   EdmEntitySet edmEntitySet, Entity entity) throws ODataLibraryException {
        
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        EntitySerializerOptions options = EntitySerializerOptions.with().build();
        
        SerializerResult serializerResult = serializer.entity(serviceMetadata, 
            edmEntitySet.getEntityType(), entity, options);
        
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
    
    private java.net.URI createId(String entitySetName, Object id) {
        try {
            return new java.net.URI(entitySetName + "('" + id + "')");
        } catch (java.net.URISyntaxException e) {
            throw new RuntimeException("Error creating entity ID", e);
        }
    }

    // Implement required methods with default behavior
    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Create not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), null);
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Update not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), null);
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Delete not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), null);
    }
}