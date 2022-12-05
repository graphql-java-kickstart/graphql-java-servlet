package graphql.kickstart.servlet;

import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.execution.GraphQLRootObjectBuilder;
import graphql.kickstart.execution.config.DefaultExecutionStrategyProvider;
import graphql.kickstart.execution.config.ExecutionStrategyProvider;
import graphql.kickstart.execution.config.InstrumentationProvider;
import graphql.kickstart.execution.error.DefaultGraphQLErrorHandler;
import graphql.kickstart.execution.error.GraphQLErrorHandler;
import graphql.kickstart.execution.instrumentation.NoOpInstrumentationProvider;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.kickstart.servlet.core.DefaultGraphQLRootObjectBuilder;
import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.kickstart.servlet.core.GraphQLServletRootObjectBuilder;
import graphql.kickstart.servlet.osgi.GraphQLCodeRegistryProvider;
import graphql.kickstart.servlet.osgi.GraphQLMutationProvider;
import graphql.kickstart.servlet.osgi.GraphQLProvider;
import graphql.kickstart.servlet.osgi.GraphQLQueryProvider;
import graphql.kickstart.servlet.osgi.GraphQLSubscriptionProvider;
import graphql.kickstart.servlet.osgi.GraphQLTypesProvider;
import graphql.schema.GraphQLCodeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

@Component(
    service = {jakarta.servlet.http.HttpServlet.class, jakarta.servlet.Servlet.class},
    property = {"service.description=GraphQL HTTP Servlet"})
@Designate(ocd = OsgiGraphQLHttpServletConfiguration.class, factory = true)
public class OsgiGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

  private final OsgiSchemaBuilder schemaBuilder = new OsgiSchemaBuilder();

  public OsgiGraphQLHttpServlet() {
    schemaBuilder.updateSchema();
  }

  @Activate
  public void activate(Config config) {
    schemaBuilder.activate(config.schema_update_delay());
  }

  @Deactivate
  public void deactivate() {
    schemaBuilder.deactivate();
  }

  @Override
  protected GraphQLConfiguration getConfiguration() {
    return schemaBuilder.buildConfiguration();
  }

  protected void updateSchema() {
    schemaBuilder.updateSchema();
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindProvider(GraphQLProvider provider) {
    if (provider instanceof GraphQLQueryProvider) {
      schemaBuilder.add((GraphQLQueryProvider) provider);
    }
    if (provider instanceof GraphQLMutationProvider) {
      schemaBuilder.add((GraphQLMutationProvider) provider);
    }
    if (provider instanceof GraphQLSubscriptionProvider) {
      schemaBuilder.add((GraphQLSubscriptionProvider) provider);
    }
    if (provider instanceof GraphQLTypesProvider) {
      schemaBuilder.add((GraphQLTypesProvider) provider);
    }
    if (provider instanceof GraphQLCodeRegistryProvider) {
      schemaBuilder.setCodeRegistryProvider((GraphQLCodeRegistryProvider) provider);
    }
    updateSchema();
  }

  public void unbindProvider(GraphQLProvider provider) {
    if (provider instanceof GraphQLQueryProvider) {
      schemaBuilder.remove((GraphQLQueryProvider) provider);
    }
    if (provider instanceof GraphQLMutationProvider) {
      schemaBuilder.remove((GraphQLMutationProvider) provider);
    }
    if (provider instanceof GraphQLSubscriptionProvider) {
      schemaBuilder.remove((GraphQLSubscriptionProvider) provider);
    }
    if (provider instanceof GraphQLTypesProvider) {
      schemaBuilder.remove((GraphQLTypesProvider) provider);
    }
    if (provider instanceof GraphQLCodeRegistryProvider) {
      schemaBuilder.setCodeRegistryProvider(() -> GraphQLCodeRegistry.newCodeRegistry().build());
    }
    updateSchema();
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindQueryProvider(GraphQLQueryProvider queryProvider) {
    schemaBuilder.add(queryProvider);
    updateSchema();
  }

  public void unbindQueryProvider(GraphQLQueryProvider queryProvider) {
    schemaBuilder.remove(queryProvider);
    updateSchema();
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindMutationProvider(GraphQLMutationProvider mutationProvider) {
    schemaBuilder.add(mutationProvider);
    updateSchema();
  }

  public void unbindMutationProvider(GraphQLMutationProvider mutationProvider) {
    schemaBuilder.remove(mutationProvider);
    updateSchema();
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindSubscriptionProvider(GraphQLSubscriptionProvider subscriptionProvider) {
    schemaBuilder.add(subscriptionProvider);
    updateSchema();
  }

  public void unbindSubscriptionProvider(GraphQLSubscriptionProvider subscriptionProvider) {
    schemaBuilder.remove(subscriptionProvider);
    updateSchema();
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindTypesProvider(GraphQLTypesProvider typesProvider) {
    schemaBuilder.add(typesProvider);
    updateSchema();
  }

  public void unbindTypesProvider(GraphQLTypesProvider typesProvider) {
    schemaBuilder.remove(typesProvider);
    updateSchema();
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindServletListener(GraphQLServletListener listener) {
    schemaBuilder.add(listener);
  }

  public void unbindServletListener(GraphQLServletListener listener) {
    schemaBuilder.remove(listener);
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  public void setContextBuilder(GraphQLServletContextBuilder contextBuilder) {
    schemaBuilder.setContextBuilder(contextBuilder);
  }

  public void unsetContextBuilder(GraphQLServletContextBuilder contextBuilder) {
    schemaBuilder.setContextBuilder(new DefaultGraphQLServletContextBuilder());
  }

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  public void setRootObjectBuilder(GraphQLServletRootObjectBuilder rootObjectBuilder) {
    schemaBuilder.setRootObjectBuilder(rootObjectBuilder);
  }

  public void unsetRootObjectBuilder(GraphQLRootObjectBuilder rootObjectBuilder) {
    schemaBuilder.setRootObjectBuilder(new DefaultGraphQLRootObjectBuilder());
  }

  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void setExecutionStrategyProvider(ExecutionStrategyProvider provider) {
    schemaBuilder.setExecutionStrategyProvider(provider);
  }

  public void unsetExecutionStrategyProvider(ExecutionStrategyProvider provider) {
    schemaBuilder.setExecutionStrategyProvider(new DefaultExecutionStrategyProvider());
  }

  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void setInstrumentationProvider(InstrumentationProvider provider) {
    schemaBuilder.setInstrumentationProvider(provider);
  }

  public void unsetInstrumentationProvider(InstrumentationProvider provider) {
    schemaBuilder.setInstrumentationProvider(new NoOpInstrumentationProvider());
  }

  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void setErrorHandler(GraphQLErrorHandler errorHandler) {
    schemaBuilder.setErrorHandler(errorHandler);
  }

  public void unsetErrorHandler(GraphQLErrorHandler errorHandler) {
    schemaBuilder.setErrorHandler(new DefaultGraphQLErrorHandler());
  }

  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void setPreparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
    schemaBuilder.setPreparsedDocumentProvider(preparsedDocumentProvider);
  }

  public void unsetPreparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
    schemaBuilder.setPreparsedDocumentProvider(NoOpPreparsedDocumentProvider.INSTANCE);
  }

  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void bindCodeRegistryProvider(GraphQLCodeRegistryProvider graphQLCodeRegistryProvider) {
    schemaBuilder.setCodeRegistryProvider(graphQLCodeRegistryProvider);
    updateSchema();
  }

  public void unbindCodeRegistryProvider(GraphQLCodeRegistryProvider graphQLCodeRegistryProvider) {
    schemaBuilder.setCodeRegistryProvider(() -> GraphQLCodeRegistry.newCodeRegistry().build());
    updateSchema();
  }

  @interface Config {

    int schema_update_delay() default 0;
  }
}
