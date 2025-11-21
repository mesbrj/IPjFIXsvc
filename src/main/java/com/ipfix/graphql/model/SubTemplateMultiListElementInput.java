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
public class SubTemplateMultiListElementInput {
    private String semantic;
    private List<SubTemplateListElementInput> subTemplateLists;
    private String description;
}
