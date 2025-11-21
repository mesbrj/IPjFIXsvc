package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SubTemplateList - A list of zero or more instances of a structured data type
 * IE 292
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTemplateListElement {
    
    private String id;
    private Integer templateId;
    private String templateName;
    private String semantic;  // e.g., "allOf", "exactlyOneOf", "oneOrMoreOf", "ordered"
    
    // Each entry contains a map of field names to values following the template structure
    private List<Map<String, Object>> entries;
    
    // Template definition (field names and their types)
    private Map<String, String> templateDefinition;
}
