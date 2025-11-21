package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * BasicList - A list of zero or more instances of a single Information Element
 * IE 291
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicListElement {
    
    private String id;
    private Integer informationElementId;
    private String informationElementName;
    private String semantic;  // e.g., "allOf", "exactlyOneOf", "oneOrMoreOf", "ordered"
    private List<String> values;
    private String dataType;  // Type of the information element
}
