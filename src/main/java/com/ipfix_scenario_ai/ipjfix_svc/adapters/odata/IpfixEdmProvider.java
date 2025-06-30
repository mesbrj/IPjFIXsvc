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
import org.springframework.stereotype.Component;

@Component
public class IpfixEdmProvider extends CsdlAbstractEdmProvider {

    // Namespace and container names for your IPFIX service
    public static final String NAMESPACE = "IpfixService";
    public static final String CONTAINER_NAME = "Container";
    
    // Entity Type Names
    public static final String ET_FLOW_RECORD_NAME = "FlowRecord";
    
    // Entity Set Names  
    public static final String ES_FLOW_RECORDS_NAME = "FlowRecords";

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        if (entityTypeName.equals(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME))) {
            return getFlowRecordEntityType();
        }
        return null;
    }

    private CsdlEntityType getFlowRecordEntityType() {
        // Define properties for IPFIX flow record
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty sourceIp = new CsdlProperty().setName("SourceIP").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty destIp = new CsdlProperty().setName("DestIP").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty sourcePort = new CsdlProperty().setName("SourcePort").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty destPort = new CsdlProperty().setName("DestPort").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty protocol = new CsdlProperty().setName("Protocol").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty bytes = new CsdlProperty().setName("Bytes").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
        CsdlProperty packets = new CsdlProperty().setName("Packets").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
        CsdlProperty timestamp = new CsdlProperty().setName("Timestamp").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty reverseBytes = new CsdlProperty().setName("ReverseBytes").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
        CsdlProperty reversePackets = new CsdlProperty().setName("ReversePackets").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
        CsdlProperty flowStartTime = new CsdlProperty().setName("FlowStartTime").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty flowEndTime = new CsdlProperty().setName("FlowEndTime").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty tcpFlags = new CsdlProperty().setName("TcpFlags").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty tosValue = new CsdlProperty().setName("TosValue").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        

        // Define key
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        propertyRef.setName("Id");

        // Configure entity type
        CsdlEntityType entityType = new CsdlEntityType();
        entityType.setName(ET_FLOW_RECORD_NAME);
        entityType.setProperties(Arrays.asList(id, sourceIp, destIp, sourcePort, destPort, protocol, bytes, packets, timestamp, reverseBytes, reversePackets, flowStartTime, flowEndTime, tcpFlags, tosValue));
        entityType.setKey(Arrays.asList(propertyRef));

        return entityType;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        // Create schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // Add entity types
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME)));
        schema.setEntityTypes(entityTypes);

        // Add entity container
        schema.setEntityContainer(getEntityContainer());

        return Arrays.asList(schema);
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        // Create entity sets
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(new FullQualifiedName(NAMESPACE, CONTAINER_NAME), ES_FLOW_RECORDS_NAME));

        // Create entity container
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if (entitySetName.equals(ES_FLOW_RECORDS_NAME)) {
            CsdlEntitySet entitySet = new CsdlEntitySet();
            entitySet.setName(ES_FLOW_RECORDS_NAME);
            entitySet.setType(new FullQualifiedName(NAMESPACE, ET_FLOW_RECORD_NAME));
            return entitySet;
        }
        return null;
    }
}