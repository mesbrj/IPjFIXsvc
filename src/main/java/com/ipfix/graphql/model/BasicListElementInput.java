package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicListElementInput {
    private Integer informationElementId;
    private String informationElementName;
    private String semantic;
    private List<String> values;
    private String dataType;
}
