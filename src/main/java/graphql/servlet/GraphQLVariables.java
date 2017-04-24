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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.annotations.GraphQLObjectBackedByClass;
import graphql.language.NonNullType;
import graphql.language.OperationDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import java.io.IOException;
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
        // this will help combating issues with unknown fields like clientMutationId
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        new Parser().parseDocument(query).getDefinitions().stream()
                    .filter(d -> d instanceof OperationDefinition)
                    .map(d -> (OperationDefinition) d)
                    .flatMap(d -> d.getVariableDefinitions().stream())
                    .forEach(new Consumer<VariableDefinition>() {
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
                            if (type instanceof GraphQLObjectBackedByClass) {
                                String value;
                                Object val;
                                try {
                                    value = objectMapper.writeValueAsString(variables.get(d.getName()));
                                    val = objectMapper.readValue(value, ((GraphQLObjectBackedByClass) type).getObjectClass());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                GraphQLVariables.this.put(d.getName(), val);
                            } else {
                                GraphQLVariables.this.put(d.getName(), variables.get(d.getName()));
                            }
                        }
                    });
    }
}
