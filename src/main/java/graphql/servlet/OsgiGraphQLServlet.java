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

import graphql.execution.ExecutionStrategy;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

@Slf4j
@Component(
        service=javax.servlet.http.HttpServlet.class,
        property = {"alias=/graphql", "jmx.objectname=graphql.servlet:type=graphql"}
)
public class OsgiGraphQLServlet extends GraphQLServlet {

    private final List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
    private final List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();
    private final List<GraphQLTypesProvider> typesProviders = new ArrayList<>();

    private GraphQLContextBuilder contextBuilder = new DefaultGraphQLContextBuilder();
    private ExecutionStrategyProvider executionStrategyProvider = new EnhancedExecutionStrategyProvider();
    
    private GraphQLSchema schema;
    private GraphQLSchema readOnlySchema;

    protected void updateSchema() {
        final GraphQLObjectType.Builder object = newObject().name("Query").description("Root query type");

        for (GraphQLQueryProvider provider : queryProviders) {
            if (provider.getQueries() != null && !provider.getQueries().isEmpty()) {
                for (GraphQLFieldDefinition graphQLFieldDefinition : provider.getQueries()) {
                    object.field(graphQLFieldDefinition);
                }
            }
        }

        final Set<GraphQLType> types = new HashSet<>();
        for (GraphQLTypesProvider typesProvider : typesProviders) {
            types.addAll(typesProvider.getTypes());
        }

        readOnlySchema = newSchema().query(object.build()).build(types);

        if (mutationProviders.isEmpty()) {
            schema = readOnlySchema;
        } else {
            final GraphQLObjectType.Builder mutationObject = newObject().name("Mutation").description("Root mutation type");

            for (GraphQLMutationProvider provider : mutationProviders) {
                provider.getMutations().forEach(mutationObject::field);
            }

            final GraphQLObjectType mutationType = mutationObject.build();
            if (mutationType.getFieldDefinitions().isEmpty()) {
                schema = readOnlySchema;
            } else {
                schema = newSchema().query(object.build()).mutation(mutationType).build();
            }
        }
    }

    public OsgiGraphQLServlet() {
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

    protected GraphQLContext createContext(Optional<HttpServletRequest> req, Optional<HttpServletResponse> resp) {
        return contextBuilder.build(req, resp);
    }

    @Override
    protected ExecutionStrategy getExecutionStrategy() {
        return executionStrategyProvider.getExecutionStrategy();
    }

    @Override
    protected Map<String, Object> transformVariables(GraphQLSchema schema, String query, Map<String, Object> variables) {
        return new GraphQLVariables(schema, query, variables);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindOperationListener(GraphQLOperationListener listener) {
        addOperationListener(listener);
    }

    public void unbindOperationListener(GraphQLOperationListener listener) {
        removeOperationListener(listener);
    }

    @Override
    public GraphQLSchema getSchema() {
        return schema;
    }

    @Override
    public GraphQLSchema getReadOnlySchema() {
        return readOnlySchema;
    }
}
