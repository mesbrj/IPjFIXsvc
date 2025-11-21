package com.ipfix.graphql.config;

import graphql.language.StringValue;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.Instant;

/**
 * GraphQL configuration for custom scalar types
 */
@Configuration
public class GraphQLConfig {
    
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(instantScalar())
                .scalar(jsonScalar())
                .scalar(longScalar());
    }
    
    private GraphQLScalarType instantScalar() {
        return GraphQLScalarType.newScalar()
                .name("Instant")
                .description("Java Instant scalar")
                .coercing(new Coercing<Instant, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof Instant) {
                            return ((Instant) dataFetcherResult).toString();
                        }
                        throw new CoercingSerializeException("Expected an Instant object.");
                    }
                    
                    @Override
                    public Instant parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof String) {
                            return Instant.parse((String) input);
                        }
                        throw new CoercingParseValueException("Expected a String");
                    }
                    
                    @Override
                    public Instant parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            return Instant.parse(((StringValue) input).getValue());
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue.");
                    }
                })
                .build();
    }
    
    private GraphQLScalarType jsonScalar() {
        return GraphQLScalarType.newScalar()
                .name("JSON")
                .description("JSON scalar type")
                .coercing(new Coercing<Object, Object>() {
                    @Override
                    public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        return dataFetcherResult;
                    }
                    
                    @Override
                    public Object parseValue(Object input) throws CoercingParseValueException {
                        return input;
                    }
                    
                    @Override
                    public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                        return input;
                    }
                })
                .build();
    }
    
    private GraphQLScalarType longScalar() {
        return GraphQLScalarType.newScalar()
                .name("Long")
                .description("Long scalar type")
                .coercing(new Coercing<Long, Long>() {
                    @Override
                    public Long serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof Long) {
                            return (Long) dataFetcherResult;
                        } else if (dataFetcherResult instanceof Integer) {
                            return ((Integer) dataFetcherResult).longValue();
                        }
                        throw new CoercingSerializeException("Expected a Long or Integer object.");
                    }
                    
                    @Override
                    public Long parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof Long) {
                            return (Long) input;
                        } else if (input instanceof Integer) {
                            return ((Integer) input).longValue();
                        }
                        throw new CoercingParseValueException("Expected a Long or Integer");
                    }
                    
                    @Override
                    public Long parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof graphql.language.IntValue) {
                            return ((graphql.language.IntValue) input).getValue().longValue();
                        }
                        throw new CoercingParseLiteralException("Expected an IntValue.");
                    }
                })
                .build();
    }
}
