package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SubTemplateMultiList - A list containing multiple SubTemplateLists
 * IE 293
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTemplateMultiListElement {
    
    private String id;
    private String semantic;  // e.g., "allOf", "exactlyOneOf", "oneOrMoreOf", "ordered"
    
    // Contains multiple sub-template lists
    private List<SubTemplateListElement> subTemplateLists;
    
    // Description of the multi-list purpose
    private String description;
}
