package com.ipfix_scenario_ai.ipjfix_svc.adapters.odata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IpfixEdmProvider extends CsdlAbstractEdmProvider {

    private static final Logger logger = LoggerFactory.getLogger(IpfixEdmProvider.class);

    // Namespace and container names
    public static final String NAMESPACE = "IpfixService";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
    
    // Entity Type Names
    public static final String ET_FLOW_RECORD_NAME = "FlowRecord";
    public static final String ET_USER_NAME = "User";
    
    // Entity Set Names  
    public static final String ES_FLOW_RECORDS_NAME = "FlowRecords";
    public static final String ES_USERS_NAME = "Users";

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        logger.info("Getting entity type for: {}", entityTypeName);
        if (entityTypeName.equals(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME))) {
            logger.info("Returning FlowRecord entity type");
            return getFlowRecordEntityType();
        } else if (entityTypeName.equals(new FullQualifiedName(NAMESPACE, ET_USER_NAME))) {
            logger.info("Returning User entity type");
            return getUserEntityType();
        }
        logger.warn("Entity type not found: {}", entityTypeName);
        return null;
    }

    private CsdlEntityType getFlowRecordEntityType() {
        // Your existing FlowRecord entity type implementation
        List<CsdlProperty> properties = Arrays.asList(
            createProperty("Id", EdmPrimitiveTypeKind.String),
            createProperty("SourceIP", EdmPrimitiveTypeKind.String),
            createProperty("DestIP", EdmPrimitiveTypeKind.String),
            createProperty("SourcePort", EdmPrimitiveTypeKind.Int32),
            createProperty("DestPort", EdmPrimitiveTypeKind.Int32),
            createProperty("Protocol", EdmPrimitiveTypeKind.String),
            createProperty("Bytes", EdmPrimitiveTypeKind.Int64),
            createProperty("Packets", EdmPrimitiveTypeKind.Int64),
            createProperty("ReverseBytes", EdmPrimitiveTypeKind.Int64),
            createProperty("ReversePackets", EdmPrimitiveTypeKind.Int64),
            createProperty("Timestamp", EdmPrimitiveTypeKind.DateTimeOffset),
            createProperty("FlowStartTime", EdmPrimitiveTypeKind.DateTimeOffset),
            createProperty("FlowEndTime", EdmPrimitiveTypeKind.DateTimeOffset),
            createProperty("TcpFlags", EdmPrimitiveTypeKind.Int32),
            createProperty("TosValue", EdmPrimitiveTypeKind.Int32)
        );

        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        propertyRef.setName("Id");

        CsdlEntityType entityType = new CsdlEntityType();
        entityType.setName(ET_FLOW_RECORD_NAME);
        entityType.setProperties(properties);
        entityType.setKey(Arrays.asList(propertyRef));

        return entityType;
    }

    private CsdlEntityType getUserEntityType() {
        // Define properties for User entity
        List<CsdlProperty> properties = Arrays.asList(
            createProperty("Id", EdmPrimitiveTypeKind.String),
            createProperty("Username", EdmPrimitiveTypeKind.String),
            createProperty("Email", EdmPrimitiveTypeKind.String),
            createProperty("FirstName", EdmPrimitiveTypeKind.String),
            createProperty("LastName", EdmPrimitiveTypeKind.String),
            createProperty("Role", EdmPrimitiveTypeKind.String),
            createProperty("Active", EdmPrimitiveTypeKind.Boolean),
            createProperty("CreatedAt", EdmPrimitiveTypeKind.DateTimeOffset),
            createProperty("LastLoginAt", EdmPrimitiveTypeKind.DateTimeOffset),
            createProperty("TenantId", EdmPrimitiveTypeKind.String)
        );

        // Define key
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        propertyRef.setName("Id");

        // Configure entity type
        CsdlEntityType entityType = new CsdlEntityType();
        entityType.setName(ET_USER_NAME);
        entityType.setProperties(properties);
        entityType.setKey(Arrays.asList(propertyRef));

        return entityType;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        logger.info("=== IpfixEdmProvider.getSchemas() called ===");
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // Add both entity types
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, ET_USER_NAME)));
        entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME)));
        schema.setEntityTypes(entityTypes);
        logger.info("Added {} entity types to schema", entityTypes.size());

        // Add entity container
        schema.setEntityContainer(getEntityContainer());
        
        logger.info("Schema created successfully with namespace: {}", NAMESPACE);
        return Arrays.asList(schema);
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        logger.debug("Creating default entity container: {}", CONTAINER_NAME);
        
        // Create entity sets
        CsdlEntitySet usersEntitySet = new CsdlEntitySet();
        usersEntitySet.setName(ES_USERS_NAME);
        usersEntitySet.setType(new FullQualifiedName(NAMESPACE, ET_USER_NAME));

        CsdlEntitySet flowRecordsEntitySet = new CsdlEntitySet();
        flowRecordsEntitySet.setName(ES_FLOW_RECORDS_NAME);
        flowRecordsEntitySet.setType(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME));

        // Create entity container
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(Arrays.asList(usersEntitySet, flowRecordsEntitySet));
        
        logger.debug("Created entity container with {} entity sets", entityContainer.getEntitySets().size());
        return entityContainer;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        logger.debug("Getting entity set: {} in container: {}", entitySetName, entityContainer);
        if (entitySetName.equals(ES_FLOW_RECORDS_NAME)) {
            CsdlEntitySet entitySet = new CsdlEntitySet();
            entitySet.setName(ES_FLOW_RECORDS_NAME);
            entitySet.setType(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME));
            logger.debug("Created FlowRecords entity set");
            return entitySet;
        } else if (entitySetName.equals(ES_USERS_NAME)) {
            CsdlEntitySet entitySet = new CsdlEntitySet();
            entitySet.setName(ES_USERS_NAME);
            entitySet.setType(new FullQualifiedName(NAMESPACE, ET_USER_NAME));
            logger.debug("Created Users entity set");
            return entitySet;
        }
        logger.warn("Entity set not found: {}", entitySetName);
        return null;
    }

    private CsdlProperty createProperty(String name, EdmPrimitiveTypeKind type) {
        return new CsdlProperty().setName(name).setType(type.getFullQualifiedName());
    }
}