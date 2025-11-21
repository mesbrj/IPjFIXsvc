package com.ipfix.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTemplateListElementInput {
    private Integer templateId;
    private String templateName;
    private String semantic;
    private List<Map<String, Object>> entries;
    private Map<String, String> templateDefinition;
}
