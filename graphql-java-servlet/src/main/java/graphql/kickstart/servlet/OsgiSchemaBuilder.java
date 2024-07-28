package graphql.kickstart.servlet;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;
import static java.util.stream.Collectors.toSet;

import graphql.Scalars;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
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
import graphql.kickstart.servlet.osgi.GraphQLDirectiveProvider;
import graphql.kickstart.servlet.osgi.GraphQLConfigurationProvider;
import graphql.kickstart.servlet.osgi.GraphQLMutationProvider;
import graphql.kickstart.servlet.osgi.GraphQLQueryProvider;
import graphql.kickstart.servlet.osgi.GraphQLSubscriptionProvider;
import graphql.kickstart.servlet.osgi.GraphQLTypesProvider;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

@Setter
public class OsgiSchemaBuilder {

  private final List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
  private final List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();
  private final List<GraphQLSubscriptionProvider> subscriptionProviders = new ArrayList<>();
  private final List<GraphQLTypesProvider> typesProviders = new ArrayList<>();
  private final List<GraphQLDirectiveProvider> directiveProviders = new ArrayList<>();
  private final List<GraphQLServletListener> listeners = new ArrayList<>();

  private GraphQLServletContextBuilder contextBuilder = new DefaultGraphQLServletContextBuilder();
  private GraphQLServletRootObjectBuilder rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
  private ExecutionStrategyProvider executionStrategyProvider =
      new DefaultExecutionStrategyProvider();
  private InstrumentationProvider instrumentationProvider = new NoOpInstrumentationProvider();
  private GraphQLErrorHandler errorHandler = new DefaultGraphQLErrorHandler();
  private PreparsedDocumentProvider preparsedDocumentProvider =
      NoOpPreparsedDocumentProvider.INSTANCE;
  private GraphQLCodeRegistryProvider codeRegistryProvider =
      () -> GraphQLCodeRegistry.newCodeRegistry().build();
  private GraphQLConfigurationProvider configurationBuilderProvider = GraphQLConfiguration.Builder::new;

  private GraphQLSchemaServletProvider schemaProvider;

  private ScheduledExecutorService schemaExecutor;
  private ScheduledFuture<?> updateFuture;
  private int schemaUpdateDelay;
  @Getter private GraphQLConfiguration configuration;

  void activate(int schemaUpdateDelay) {
    this.schemaUpdateDelay = schemaUpdateDelay;
    if (schemaUpdateDelay != 0) {
      schemaExecutor = Executors.newSingleThreadScheduledExecutor();
    }
  }

  void deactivate() {
    if (schemaExecutor != null) {
      schemaExecutor.shutdown();
    }
  }

  void updateSchema() {
    if (schemaUpdateDelay == 0) {
      doUpdateSchema();
    } else {
      if (updateFuture != null) {
        updateFuture.cancel(true);
      }

      updateFuture =
          schemaExecutor.schedule(this::doUpdateSchema, schemaUpdateDelay, TimeUnit.MILLISECONDS);
    }
  }

  private void doUpdateSchema() {
    this.schemaProvider =
        new DefaultGraphQLSchemaServletProvider(
            newSchema()
                .query(buildQueryType())
                .mutation(buildMutationType())
                .subscription(buildSubscriptionType())
                .additionalTypes(buildTypes())
                .additionalDirectives(buildDirectives())
                .codeRegistry(codeRegistryProvider.getCodeRegistry())
                .build());
  }

  private GraphQLObjectType buildQueryType() {
    final GraphQLObjectType.Builder queryTypeBuilder =
        newObject().name("Query").description("Root query type");

    if (!queryProviders.isEmpty()) {
      for (GraphQLQueryProvider provider : queryProviders) {
        if (provider.getQueries() != null && !provider.getQueries().isEmpty()) {
          provider.getQueries().forEach(queryTypeBuilder::field);
        }
      }
    } else {
      // graphql-java enforces Query type to be there with at least some field.
      queryTypeBuilder.field(
          GraphQLFieldDefinition.newFieldDefinition()
              .name("_empty")
              .type(Scalars.GraphQLBoolean)
              .build());
    }
    return queryTypeBuilder.build();
  }

  private Set<GraphQLType> buildTypes() {
    return typesProviders.stream()
        .map(GraphQLTypesProvider::getTypes)
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  private GraphQLObjectType buildMutationType() {
    return buildObjectType("Mutation", mutationProviders.stream().flatMap(s -> s.getMutations().stream()));
  }

  private GraphQLObjectType buildSubscriptionType() {
    return buildObjectType("Subscription", subscriptionProviders.stream().flatMap(s -> s.getSubscriptions().stream()));
  }

  private GraphQLObjectType buildObjectType(String name, Stream<GraphQLFieldDefinition> fields) {
    final GraphQLObjectType.Builder typeBuilder =
        newObject().name(name).description("Root " + name.toLowerCase() + " type");

    fields.forEach(typeBuilder::field);

    if (!typeBuilder.build().getFieldDefinitions().isEmpty()) {
      return typeBuilder.build();
    }
    return null;
  }

  private Set<GraphQLDirective> buildDirectives() {
    return directiveProviders.stream()
        .map(GraphQLDirectiveProvider::getDirectives)
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  void add(GraphQLQueryProvider provider) {
    queryProviders.add(provider);
  }

  void add(GraphQLMutationProvider provider) {
    mutationProviders.add(provider);
  }

  void add(GraphQLSubscriptionProvider provider) {
    subscriptionProviders.add(provider);
  }

  void add(GraphQLTypesProvider provider) {
    typesProviders.add(provider);
  }

  void add(GraphQLDirectiveProvider provider) {
    directiveProviders.add(provider);
  }

  void remove(GraphQLQueryProvider provider) {
    queryProviders.remove(provider);
  }

  void remove(GraphQLMutationProvider provider) {
    mutationProviders.remove(provider);
  }

  void remove(GraphQLSubscriptionProvider provider) {
    subscriptionProviders.remove(provider);
  }

  void remove(GraphQLTypesProvider provider) {
    typesProviders.remove(provider);
  }

  void remove(GraphQLDirectiveProvider provider) {
    directiveProviders.remove(provider);
  }

  GraphQLSchemaServletProvider getSchemaProvider() {
    return schemaProvider;
  }

  void updateConfiguration() {
    configuration = configurationBuilderProvider.getConfigurationBuilder()
        .with(buildInvocationInputFactory())
        .with(buildQueryInvoker())
        .with(buildObjectMapper())
        .with(listeners)
        .build();
  }

  private GraphQLInvocationInputFactory buildInvocationInputFactory() {
    return GraphQLInvocationInputFactory.newBuilder(this::getSchemaProvider)
        .withGraphQLContextBuilder(contextBuilder)
        .withGraphQLRootObjectBuilder(rootObjectBuilder)
        .build();
  }

  private GraphQLQueryInvoker buildQueryInvoker() {
    return GraphQLQueryInvoker.newBuilder()
        .withPreparsedDocumentProvider(preparsedDocumentProvider)
        .withInstrumentation(() -> instrumentationProvider.getInstrumentation())
        .withExecutionStrategyProvider(executionStrategyProvider)
        .build();
  }

  private GraphQLObjectMapper buildObjectMapper() {
    return GraphQLObjectMapper.newBuilder().withGraphQLErrorHandler(errorHandler).build();
  }

  void add(GraphQLServletListener listener) {
    listeners.add(listener);
  }

  void remove(GraphQLServletListener listener) {
    listeners.remove(listener);
  }
}
