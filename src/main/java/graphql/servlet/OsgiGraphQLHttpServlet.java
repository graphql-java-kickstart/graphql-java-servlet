package graphql.servlet;

import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

@Component(
        service={javax.servlet.http.HttpServlet.class,javax.servlet.Servlet.class},
        property = {"alias=/graphql", "jmx.objectname=graphql.servlet:type=graphql"}
)
public class OsgiGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    private final List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
    private final List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();
    private final List<GraphQLTypesProvider> typesProviders = new ArrayList<>();

    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final GraphQLObjectMapper graphQLObjectMapper;

    private GraphQLContextBuilder contextBuilder = new DefaultGraphQLContextBuilder();
    private GraphQLRootObjectBuilder rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
    private ExecutionStrategyProvider executionStrategyProvider = new DefaultExecutionStrategyProvider();
    private InstrumentationProvider instrumentationProvider = new NoOpInstrumentationProvider();
    private GraphQLErrorHandler errorHandler = new DefaultGraphQLErrorHandler();
    private PreparsedDocumentProvider preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;

    private GraphQLSchemaProvider schemaProvider;

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        return queryInvoker;
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        return invocationInputFactory;
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        return graphQLObjectMapper;
    }

    public OsgiGraphQLHttpServlet() {
        updateSchema();

        this.queryInvoker = GraphQLQueryInvoker.newBuilder()
            .withPreparsedDocumentProvider(this::getPreparsedDocumentProvider)
            .withInstrumentation(() -> this.getInstrumentationProvider().getInstrumentation())
            .withExecutionStrategyProvider(this::getExecutionStrategyProvider).build();

        this.invocationInputFactory = GraphQLInvocationInputFactory.newBuilder(this::getSchemaProvider)
            .withGraphQLContextBuilder(this::getContextBuilder)
            .withGraphQLRootObjectBuilder(this::getRootObjectBuilder)
            .build();

        this.graphQLObjectMapper = GraphQLObjectMapper.newBuilder()
            .withGraphQLErrorHandler(this::getErrorHandler)
            .build();
    }

    protected void updateSchema() {
        final GraphQLObjectType.Builder queryTypeBuilder = newObject().name("Query").description("Root query type");

        for (GraphQLQueryProvider provider : queryProviders) {
            if (provider.getQueries() != null && !provider.getQueries().isEmpty()) {
                provider.getQueries().forEach(queryTypeBuilder::field);
            }
        }

        final Set<GraphQLType> types = new HashSet<>();
        for (GraphQLTypesProvider typesProvider : typesProviders) {
            types.addAll(typesProvider.getTypes());
        }

        GraphQLObjectType mutationType = null;

        if (!mutationProviders.isEmpty()) {
            final GraphQLObjectType.Builder mutationTypeBuilder = newObject().name("Mutation").description("Root mutation type");

            for (GraphQLMutationProvider provider : mutationProviders) {
                provider.getMutations().forEach(mutationTypeBuilder::field);
            }

            if (!mutationTypeBuilder.build().getFieldDefinitions().isEmpty()) {
                mutationType = mutationTypeBuilder.build();
            }
        }

        this.schemaProvider = new DefaultGraphQLSchemaProvider(newSchema().query(queryTypeBuilder.build()).mutation(mutationType).build(types));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindProvider(GraphQLProvider provider) {
        if (provider instanceof GraphQLQueryProvider) {
            queryProviders.add((GraphQLQueryProvider) provider);
        }
        if (provider instanceof GraphQLMutationProvider) {
            mutationProviders.add((GraphQLMutationProvider) provider);
        }
        if (provider instanceof GraphQLTypesProvider) {
            typesProviders.add((GraphQLTypesProvider) provider);
        }
        updateSchema();
    }
    public void unbindProvider(GraphQLProvider provider) {
        if (provider instanceof GraphQLQueryProvider) {
            queryProviders.remove(provider);
        }
        if (provider instanceof GraphQLMutationProvider) {
            mutationProviders.remove(provider);
        }
        if (provider instanceof GraphQLTypesProvider) {
            typesProviders.remove(provider);
        }
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

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindServletListener(GraphQLServletListener listener) {
        this.addListener(listener);
    }
    public void unbindServletListener(GraphQLServletListener listener) {
        this.removeListener(listener);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    public void setContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }
    public void unsetContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = new DefaultGraphQLContextBuilder();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    public void setRootObjectBuilder(GraphQLRootObjectBuilder rootObjectBuilder) {
        this.rootObjectBuilder = rootObjectBuilder;
    }
    public void unsetRootObjectBuilder(GraphQLRootObjectBuilder rootObjectBuilder) {
        this.rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy= ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setExecutionStrategyProvider(ExecutionStrategyProvider provider) {
        executionStrategyProvider = provider;
    }
    public void unsetExecutionStrategyProvider(ExecutionStrategyProvider provider) {
        executionStrategyProvider = new DefaultExecutionStrategyProvider();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy= ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setInstrumentationProvider(InstrumentationProvider provider) {
        instrumentationProvider = provider;
    }
    public void unsetInstrumentationProvider(InstrumentationProvider provider) {
        instrumentationProvider = new NoOpInstrumentationProvider();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy= ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setErrorHandler(GraphQLErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    public void unsetErrorHandler(GraphQLErrorHandler errorHandler) {
        this.errorHandler = new DefaultGraphQLErrorHandler();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy= ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void setPreparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
        this.preparsedDocumentProvider = preparsedDocumentProvider;
    }
    public void unsetPreparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
        this.preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;
    }

    public GraphQLContextBuilder getContextBuilder() {
        return contextBuilder;
    }

    public GraphQLRootObjectBuilder getRootObjectBuilder() {
        return rootObjectBuilder;
    }

    public ExecutionStrategyProvider getExecutionStrategyProvider() {
        return executionStrategyProvider;
    }

    public InstrumentationProvider getInstrumentationProvider() {
        return instrumentationProvider;
    }

    public GraphQLErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public PreparsedDocumentProvider getPreparsedDocumentProvider() {
        return preparsedDocumentProvider;
    }

    public GraphQLSchemaProvider getSchemaProvider() {
        return schemaProvider;
    }
}
