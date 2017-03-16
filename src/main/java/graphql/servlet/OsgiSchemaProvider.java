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

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OsgiSchemaProvider extends GraphQLSchemaProvider implements Serializable {
    private List<GraphQLQueryProvider> queryProviders = new ArrayList<GraphQLQueryProvider>();
    private List<GraphQLMutationProvider> mutationProviders = new ArrayList<GraphQLMutationProvider>();
    private List<GraphQLTypesProvider> typesProviders = new ArrayList<GraphQLTypesProvider>();

    private void updateSchema() {
        GraphQLObjectType.Builder object = GraphQLObjectType.newObject().name("query");

        for (GraphQLQueryProvider provider : queryProviders) {
            GraphQLObjectType query = provider.getQuery();
            object.field(GraphQLFieldDefinition.newFieldDefinition().
                type(query).
                staticValue(provider.context()).
                name(provider.getName()).
                description(query.getDescription()).
                build());
        }

        Set<GraphQLType> types = new HashSet<GraphQLType>();
        for (GraphQLTypesProvider typesProvider : typesProviders) {
            types.addAll(typesProvider.getTypes());
        }

        GraphQLSchema readOnlySchema = GraphQLSchema.newSchema().query(object.build()).build(types);
        GraphQLSchema schema;

        if (mutationProviders.isEmpty()) {
            schema = readOnlySchema;
        } else {
            GraphQLObjectType.Builder mutationObject = GraphQLObjectType.newObject().name("mutation");

            for (GraphQLMutationProvider provider : mutationProviders) {
                provider.getMutations().forEach(mutationObject::field);
            }

            GraphQLObjectType mutationType = mutationObject.build();
            if (mutationType.getFieldDefinitions().size() == 0) {
                schema = readOnlySchema;
            } else {
                updateSchema(readOnlySchema, readOnlySchema);
                schema = GraphQLSchema.newSchema().query(object.build()).mutation(mutationType).build();
            }
        }

        super.updateSchema(schema, readOnlySchema);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.add(queryProvider);
        updateSchema();
    }

    public void unbindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.remove(queryProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindMutationProvider(GraphQLMutationProvider mutationProvider) {
        mutationProviders.add(mutationProvider);
        updateSchema();
    }

    public void unbindMutationProvider(GraphQLMutationProvider mutationProvider) {
        mutationProviders.remove(mutationProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void typesProviders(GraphQLTypesProvider typesProvider) {
        typesProviders.add(typesProvider);
        updateSchema();
    }

    public void unbindTypesProvider(GraphQLTypesProvider typesProvider) {
        typesProviders.remove(typesProvider);
        updateSchema();
    }
}