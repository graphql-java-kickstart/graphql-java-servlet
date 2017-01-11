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

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

@Component(property = {"alias=/graphql", "jmx.objectname=graphql.servlet:type=graphql"})
public class GraphQLServlet extends AbstractGraphQLServlet {

    private List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
    private List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();
    private List<GraphQLTypesProvider> typesProviders = new ArrayList<>();

    protected void updateSchema() {
        GraphQLObjectType.Builder object = newObject().name("query");

        for (GraphQLQueryProvider provider : queryProviders) {
            GraphQLObjectType query = provider.getQuery();
            object.field(newFieldDefinition().
                    type(query).
                    staticValue(provider.context()).
                    name(provider.getName()).
                    description(query.getDescription()).
                    build());
        }

        Set<GraphQLType> types = new HashSet<>();
        for (GraphQLTypesProvider typesProvider : typesProviders) {
            types.addAll(typesProvider.getTypes());
        }

        readOnlySchema = newSchema().query(object.build()).build(types);

        if (mutationProviders.isEmpty()) {
            schema = readOnlySchema;
        } else {
            GraphQLObjectType.Builder mutationObject = newObject().name("mutation");

            for (GraphQLMutationProvider provider : mutationProviders) {
                provider.getMutations().forEach(mutationObject::field);
            }

            GraphQLObjectType mutationType = mutationObject.build();
            if (mutationType.getFieldDefinitions().size() == 0) {
                schema = readOnlySchema;
            } else {
                schema = newSchema().query(object.build()).mutation(mutationType).build();
            }
        }
    }

    public GraphQLServlet() {
        updateSchema();
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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    public void setContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }
    public void unsetContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = new DefaultGraphQLContextBuilder();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setExecutionStrategyProvider(ExecutionStrategyProvider provider) {
        executionStrategyProvider = provider;
    }
    public void unsetExecutionStrategyProvider(ExecutionStrategyProvider provider) {
        executionStrategyProvider = new EnhancedExecutionStrategyProvider();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindOperationListener(GraphQLOperationListener listener) {
        operationListeners.add(listener);
    }

    public void unbindOperationListener(GraphQLOperationListener listener) {
        operationListeners.remove(listener);
    }
}
