package graphql.servlet;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import graphql.servlet.config.DefaultExecutionStrategyProvider;
import graphql.servlet.context.DefaultGraphQLContextBuilder;
import graphql.servlet.core.DefaultGraphQLErrorHandler;
import graphql.servlet.core.DefaultGraphQLRootObjectBuilder;
import graphql.servlet.config.DefaultGraphQLSchemaProvider;
import graphql.servlet.config.ExecutionStrategyProvider;
import graphql.servlet.config.GraphQLCodeRegistryProvider;
import graphql.servlet.context.GraphQLContextBuilder;
import graphql.servlet.core.GraphQLErrorHandler;
import graphql.servlet.config.GraphQLMutationProvider;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.config.GraphQLProvider;
import graphql.servlet.core.GraphQLQueryInvoker;
import graphql.servlet.config.GraphQLQueryProvider;
import graphql.servlet.core.GraphQLRootObjectBuilder;
import graphql.servlet.config.GraphQLSchemaProvider;
import graphql.servlet.core.GraphQLServletListener;
import graphql.servlet.config.GraphQLSubscriptionProvider;
import graphql.servlet.config.GraphQLTypesProvider;
import graphql.servlet.config.InstrumentationProvider;
import graphql.servlet.instrumentation.NoOpInstrumentationProvider;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLCodeRegistry;

@Component(
        service={javax.servlet.http.HttpServlet.class,javax.servlet.Servlet.class},
        property = {"alias=/graphql", "jmx.objectname=graphql.servlet:type=graphql"}
)
public class OsgiGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    private final List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
    private final List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();
    private final List<GraphQLSubscriptionProvider> subscriptionProviders = new ArrayList<>();
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
    private GraphQLCodeRegistryProvider codeRegistryProvider = () -> GraphQLCodeRegistry.newCodeRegistry().build();

    private GraphQLSchemaProvider schemaProvider;

	private ScheduledExecutorService executor;
	private ScheduledFuture<?> updateFuture;
    private int schemaUpdateDelay;

	@interface Config {
		int schema_update_delay() default 0;
	}

	@Activate
	public void activate(Config config) {
		this.schemaUpdateDelay = config.schema_update_delay();
		if (schemaUpdateDelay!=0)
			executor = Executors.newSingleThreadScheduledExecutor();
	}

	@Deactivate
	public void deactivate() {
		if (executor!=null) executor.shutdown();
	}

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

    @Override
    protected boolean isAsyncServletMode() {
        return false;
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
    	if (schemaUpdateDelay==0) {
    		doUpdateSchema();
    	}
    	else {
    		if (updateFuture!=null)
    			updateFuture.cancel(true);

    		updateFuture = executor.schedule(new Runnable() {
				@Override
				public void run() {
					doUpdateSchema();
				}
			}, schemaUpdateDelay, TimeUnit.MILLISECONDS);
    	}
    }

    private void doUpdateSchema() {
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

        GraphQLObjectType subscriptionType = null;

        if (!subscriptionProviders.isEmpty()) {
            final GraphQLObjectType.Builder subscriptionTypeBuilder = newObject().name("Subscription").description("Root subscription type");

            for (GraphQLSubscriptionProvider provider : subscriptionProviders) {
                provider.getSubscriptions().forEach(subscriptionTypeBuilder::field);
            }

            if (!subscriptionTypeBuilder.build().getFieldDefinitions().isEmpty()) {
                subscriptionType = subscriptionTypeBuilder.build();
            }
        }

        this.schemaProvider = new DefaultGraphQLSchemaProvider(newSchema().query(queryTypeBuilder.build())
                .mutation(mutationType)
                .subscription(subscriptionType)
                .additionalTypes(types)
                .codeRegistry(codeRegistryProvider.getCodeRegistry())
                .build());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindProvider(GraphQLProvider provider) {
        if (provider instanceof GraphQLQueryProvider) {
            queryProviders.add((GraphQLQueryProvider) provider);
        }
        if (provider instanceof GraphQLMutationProvider) {
            mutationProviders.add((GraphQLMutationProvider) provider);
        }
        if (provider instanceof GraphQLSubscriptionProvider) {
            subscriptionProviders.add((GraphQLSubscriptionProvider) provider);
        }
        if (provider instanceof GraphQLTypesProvider) {
            typesProviders.add((GraphQLTypesProvider) provider);
        }
        if (provider instanceof GraphQLCodeRegistryProvider) {
            codeRegistryProvider = (GraphQLCodeRegistryProvider) provider;
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
        if (provider instanceof GraphQLSubscriptionProvider) {
            subscriptionProviders.remove(provider);
        }
        if (provider instanceof GraphQLTypesProvider) {
            typesProviders.remove(provider);
        }
        if (provider instanceof GraphQLCodeRegistryProvider) {
            codeRegistryProvider = () -> GraphQLCodeRegistry.newCodeRegistry().build();
        }
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.add(queryProvider);
        updateSchema();
    }
    public void unbindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.remove(queryProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindMutationProvider(GraphQLMutationProvider mutationProvider) {
        mutationProviders.add(mutationProvider);
        updateSchema();
    }
    public void unbindMutationProvider(GraphQLMutationProvider mutationProvider) {
        mutationProviders.remove(mutationProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindSubscriptionProvider(GraphQLSubscriptionProvider subscriptionProvider) {
        subscriptionProviders.add(subscriptionProvider);
        updateSchema();
    }
    public void unbindSubscriptionProvider(GraphQLSubscriptionProvider subscriptionProvider) {
        subscriptionProviders.remove(subscriptionProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindTypesProvider(GraphQLTypesProvider typesProvider) {
        typesProviders.add(typesProvider);
        updateSchema();
    }
    public void unbindTypesProvider(GraphQLTypesProvider typesProvider) {
        typesProviders.remove(typesProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindServletListener(GraphQLServletListener listener) {
        this.addListener(listener);
    }
    public void unbindServletListener(GraphQLServletListener listener) {
        this.removeListener(listener);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }
    public void unsetContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = new DefaultGraphQLContextBuilder();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy= ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void bindCodeRegistryProvider(GraphQLCodeRegistryProvider graphQLCodeRegistryProvider) {
        this.codeRegistryProvider = graphQLCodeRegistryProvider;
        updateSchema();
    }
    public void unbindCodeRegistryProvider(GraphQLCodeRegistryProvider graphQLCodeRegistryProvider) {
        this.codeRegistryProvider = () -> GraphQLCodeRegistry.newCodeRegistry().build();
        updateSchema();
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
