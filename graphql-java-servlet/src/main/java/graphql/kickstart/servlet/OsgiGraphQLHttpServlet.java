package graphql.kickstart.servlet;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.GraphQLRootObjectBuilder;
import graphql.kickstart.execution.config.DefaultExecutionStrategyProvider;
import graphql.kickstart.execution.config.ExecutionStrategyProvider;
import graphql.kickstart.execution.config.InstrumentationProvider;
import graphql.kickstart.execution.error.DefaultGraphQLErrorHandler;
import graphql.kickstart.execution.error.GraphQLErrorHandler;
import graphql.kickstart.execution.instrumentation.NoOpInstrumentationProvider;
import graphql.kickstart.servlet.config.DefaultGraphQLSchemaServletProvider;
import graphql.kickstart.servlet.config.GraphQLSchemaServletProvider;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.kickstart.servlet.core.DefaultGraphQLRootObjectBuilder;
import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.kickstart.servlet.core.GraphQLServletRootObjectBuilder;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.kickstart.servlet.osgi.GraphQLCodeRegistryProvider;
import graphql.kickstart.servlet.osgi.GraphQLMutationProvider;
import graphql.kickstart.servlet.osgi.GraphQLProvider;
import graphql.kickstart.servlet.osgi.GraphQLQueryProvider;
import graphql.kickstart.servlet.osgi.GraphQLSubscriptionProvider;
import graphql.kickstart.servlet.osgi.GraphQLTypesProvider;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

@Component(
    service = {javax.servlet.http.HttpServlet.class, javax.servlet.Servlet.class},
    property = {"service.description=GraphQL HTTP Servlet"}
)
@Designate(ocd = OsgiGraphQLHttpServletConfiguration.class, factory = true)
public class OsgiGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

  private final List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
  private final List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();
  private final List<GraphQLSubscriptionProvider> subscriptionProviders = new ArrayList<>();
  private final List<GraphQLTypesProvider> typesProviders = new ArrayList<>();

  private final GraphQLQueryInvoker queryInvoker;
  private final GraphQLInvocationInputFactory invocationInputFactory;
  private final GraphQLObjectMapper graphQLObjectMapper;

  private GraphQLServletContextBuilder contextBuilder = new DefaultGraphQLServletContextBuilder();
  private GraphQLServletRootObjectBuilder rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
  private ExecutionStrategyProvider executionStrategyProvider = new DefaultExecutionStrategyProvider();
  private InstrumentationProvider instrumentationProvider = new NoOpInstrumentationProvider();
  private GraphQLErrorHandler errorHandler = new DefaultGraphQLErrorHandler();
  private PreparsedDocumentProvider preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;
  private GraphQLCodeRegistryProvider codeRegistryProvider = () -> GraphQLCodeRegistry
      .newCodeRegistry().build();

  private GraphQLSchemaServletProvider schemaProvider;

  private ScheduledExecutorService executor;
  private ScheduledFuture<?> updateFuture;
  private int schemaUpdateDelay;

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

  @Activate
  public void activate(Config config) {
    this.schemaUpdateDelay = config.schema_update_delay();
    if (schemaUpdateDelay != 0) {
      executor = Executors.newSingleThreadScheduledExecutor();
    }
  }

  @Deactivate
  public void deactivate() {
    if (executor != null) {
      executor.shutdown();
    }
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

  protected void updateSchema() {
    if (schemaUpdateDelay == 0) {
      doUpdateSchema();
    } else {
      if (updateFuture != null) {
        updateFuture.cancel(true);
      }

      updateFuture = executor.schedule(new Runnable() {
        @Override
        public void run() {
          doUpdateSchema();
        }
      }, schemaUpdateDelay, TimeUnit.MILLISECONDS);
    }
  }

  private void doUpdateSchema() {
    final GraphQLObjectType.Builder queryTypeBuilder = newObject().name("Query")
        .description("Root query type");

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
      final GraphQLObjectType.Builder mutationTypeBuilder = newObject().name("Mutation")
          .description("Root mutation type");

      for (GraphQLMutationProvider provider : mutationProviders) {
        provider.getMutations().forEach(mutationTypeBuilder::field);
      }

      if (!mutationTypeBuilder.build().getFieldDefinitions().isEmpty()) {
        mutationType = mutationTypeBuilder.build();
      }
    }

    GraphQLObjectType subscriptionType = null;

    if (!subscriptionProviders.isEmpty()) {
      final GraphQLObjectType.Builder subscriptionTypeBuilder = newObject().name("Subscription")
          .description("Root subscription type");

      for (GraphQLSubscriptionProvider provider : subscriptionProviders) {
        provider.getSubscriptions().forEach(subscriptionTypeBuilder::field);
      }

      if (!subscriptionTypeBuilder.build().getFieldDefinitions().isEmpty()) {
        subscriptionType = subscriptionTypeBuilder.build();
      }
    }

    this.schemaProvider = new DefaultGraphQLSchemaServletProvider(
        newSchema().query(queryTypeBuilder.build())
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
  public void setContextProvider(GraphQLServletContextBuilder contextBuilder) {
    this.contextBuilder = contextBuilder;
  }

  public void unsetContextProvider(GraphQLServletContextBuilder contextBuilder) {
    this.contextBuilder = new DefaultGraphQLServletContextBuilder();
  }

  public void unsetRootObjectBuilder(GraphQLRootObjectBuilder rootObjectBuilder) {
    this.rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
  }

  public void unsetExecutionStrategyProvider(ExecutionStrategyProvider provider) {
    executionStrategyProvider = new DefaultExecutionStrategyProvider();
  }

  public void unsetInstrumentationProvider(InstrumentationProvider provider) {
    instrumentationProvider = new NoOpInstrumentationProvider();
  }

  public void unsetErrorHandler(GraphQLErrorHandler errorHandler) {
    this.errorHandler = new DefaultGraphQLErrorHandler();
  }

  public void unsetPreparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
    this.preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
  public void bindCodeRegistryProvider(GraphQLCodeRegistryProvider graphQLCodeRegistryProvider) {
    this.codeRegistryProvider = graphQLCodeRegistryProvider;
    updateSchema();
  }

  public void unbindCodeRegistryProvider(GraphQLCodeRegistryProvider graphQLCodeRegistryProvider) {
    this.codeRegistryProvider = () -> GraphQLCodeRegistry.newCodeRegistry().build();
    updateSchema();
  }

  public GraphQLServletContextBuilder getContextBuilder() {
    return contextBuilder;
  }

  public GraphQLServletRootObjectBuilder getRootObjectBuilder() {
    return rootObjectBuilder;
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  public void setRootObjectBuilder(GraphQLServletRootObjectBuilder rootObjectBuilder) {
    this.rootObjectBuilder = rootObjectBuilder;
  }

  public ExecutionStrategyProvider getExecutionStrategyProvider() {
    return executionStrategyProvider;
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
  public void setExecutionStrategyProvider(ExecutionStrategyProvider provider) {
    executionStrategyProvider = provider;
  }

  public InstrumentationProvider getInstrumentationProvider() {
    return instrumentationProvider;
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
  public void setInstrumentationProvider(InstrumentationProvider provider) {
    instrumentationProvider = provider;
  }

  public GraphQLErrorHandler getErrorHandler() {
    return errorHandler;
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
  public void setErrorHandler(GraphQLErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public PreparsedDocumentProvider getPreparsedDocumentProvider() {
    return preparsedDocumentProvider;
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
  public void setPreparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
    this.preparsedDocumentProvider = preparsedDocumentProvider;
  }

  public GraphQLSchemaServletProvider getSchemaProvider() {
    return schemaProvider;
  }

  @interface Config {

    int schema_update_delay() default 0;
  }
}
