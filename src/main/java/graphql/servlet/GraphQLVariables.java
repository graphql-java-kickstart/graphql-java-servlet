/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package graphql.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.annotations.GraphQLObjectTypeWrapper;
import graphql.language.*;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GraphQLVariables extends HashMap<String, Object> {

    private final GraphQLSchema schema;
    private final String query;

    public GraphQLVariables(GraphQLSchema schema, String query, Map<String, Object> variables) {
        super();
        this.schema = schema;
        this.query = query;
        ObjectMapper objectMapper = new ObjectMapper();
        new Parser().parseDocument(query).getDefinitions().stream()
                    .filter(d -> d instanceof OperationDefinition)
                    .map(d -> (OperationDefinition) d)
                    .flatMap(d -> d.getVariableDefinitions().stream())
                    .forEach(new Consumer<VariableDefinition>() {
                        @SneakyThrows
                        @Override public void accept(VariableDefinition d) {
                            GraphQLType type;
                            Type t = d.getType();
                            if (t instanceof TypeName) {
                                type = schema.getType(((TypeName) t).getName());
                            } else if (t instanceof NonNullType) {
                                accept(new VariableDefinition(d.getName(), ((NonNullType) t).getType()));
                                return;
                            } else {
                                type = null;
                            }
                            if (type instanceof GraphQLObjectTypeWrapper) {
                                String value = objectMapper.writeValueAsString(variables.get(d.getName()));
                                Object val = objectMapper.readValue(value, ((GraphQLObjectTypeWrapper) type).getObjectClass());
                                GraphQLVariables.this.put(d.getName(), val);
                            } else {
                                GraphQLVariables.this.put(d.getName(), variables.get(d.getName()));
                            }
                        }
                    });
    }
}
