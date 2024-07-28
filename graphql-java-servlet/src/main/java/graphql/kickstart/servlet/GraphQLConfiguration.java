package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.servlet.cache.CachingHttpRequestInvoker;
import graphql.kickstart.servlet.cache.GraphQLResponseCacheManager;
import graphql.kickstart.servlet.config.DefaultGraphQLSchemaServletProvider;
import graphql.kickstart.servlet.config.GraphQLSchemaServletProvider;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.kickstart.servlet.core.GraphQLServletRootObjectBuilder;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.kickstart.servlet.input.NoOpBatchInputPreProcessor;
import graphql.schema.GraphQLSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import lombok.Getter;

public class GraphQLConfiguration {

  private final GraphQLInvocationInputFactory invocationInputFactory;
  private final Supplier<BatchInputPreProcessor> batchInputPreProcessor;
  private final GraphQLInvoker graphQLInvoker;
  private final GraphQLObjectMapper objectMapper;
  private final List<GraphQLServletListener> listeners;
  private final long subscriptionTimeout;
  @Getter private final long asyncTimeout;
  private final ContextSetting contextSetting;
  private final GraphQLResponseCacheManager responseCacheManager;
  @Getter private final Executor asyncExecutor;
  private HttpRequestHandler requestHandler;

  private GraphQLConfiguration(
      GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      GraphQLQueryInvoker queryInvoker,
      GraphQLObjectMapper objectMapper,
      List<GraphQLServletListener> listeners,
      long subscriptionTimeout,
      long asyncTimeout,
      ContextSetting contextSetting,
      Supplier<BatchInputPreProcessor> batchInputPreProcessor,
      GraphQLResponseCacheManager responseCacheManager,
      Executor asyncExecutor) {
    this.invocationInputFactory = invocationInputFactory;
    this.asyncExecutor = asyncExecutor;
    this.graphQLInvoker = graphQLInvoker != null ? graphQLInvoker : queryInvoker.toGraphQLInvoker();
    this.objectMapper = objectMapper;
    this.listeners = listeners;
    this.subscriptionTimeout = subscriptionTimeout;
    this.asyncTimeout = asyncTimeout;
    this.contextSetting = contextSetting;
    this.batchInputPreProcessor = batchInputPreProcessor;
    this.responseCacheManager = responseCacheManager;
  }

  public static GraphQLConfiguration.Builder with(GraphQLSchema schema) {
    return with(new DefaultGraphQLSchemaServletProvider(schema));
  }

  public static GraphQLConfiguration.Builder with(GraphQLSchemaServletProvider schemaProvider) {
    return new Builder().with(GraphQLInvocationInputFactory.newBuilder(schemaProvider));
  }

  public static GraphQLConfiguration.Builder with(
      GraphQLInvocationInputFactory invocationInputFactory) {
    return new Builder().with(invocationInputFactory);
  }

  public GraphQLInvocationInputFactory getInvocationInputFactory() {
    return invocationInputFactory;
  }

  public GraphQLInvoker getGraphQLInvoker() {
    return graphQLInvoker;
  }

  public GraphQLObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public List<GraphQLServletListener> getListeners() {
    return new ArrayList<>(listeners);
  }

  public void add(GraphQLServletListener listener) {
    listeners.add(listener);
  }

  public boolean remove(GraphQLServletListener listener) {
    return listeners.remove(listener);
  }

  public long getSubscriptionTimeout() {
    return subscriptionTimeout;
  }

  public ContextSetting getContextSetting() {
    return contextSetting;
  }

  public BatchInputPreProcessor getBatchInputPreProcessor() {
    return batchInputPreProcessor.get();
  }

  public GraphQLResponseCacheManager getResponseCacheManager() {
    return responseCacheManager;
  }

  public HttpRequestHandler getHttpRequestHandler() {
    if (requestHandler == null) {
      requestHandler = createHttpRequestHandler();
    }
    return requestHandler;
  }

  private HttpRequestHandler createHttpRequestHandler() {
    if (responseCacheManager == null) {
      return new HttpRequestHandlerImpl(this);
    } else {
      return new HttpRequestHandlerImpl(this, new CachingHttpRequestInvoker(this));
    }
  }

  public static class Builder {

    private GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder;
    private GraphQLInvocationInputFactory invocationInputFactory;
    private GraphQLInvoker graphQLInvoker;
    private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
    private GraphQLObjectMapper objectMapper = GraphQLObjectMapper.newBuilder().build();
    private List<GraphQLServletListener> listeners = new ArrayList<>();
    private long subscriptionTimeout = 0;
    private long asyncTimeout = 30000;
    private ContextSetting contextSetting = ContextSetting.PER_QUERY;
    private Supplier<BatchInputPreProcessor> batchInputPreProcessorSupplier =
        NoOpBatchInputPreProcessor::new;
    private GraphQLResponseCacheManager responseCacheManager;
    private Executor asyncExecutor;
    private AsyncTaskDecorator asyncTaskDecorator;

    public Builder with(GraphQLInvoker graphQLInvoker) {
      this.graphQLInvoker = graphQLInvoker;
      return this;
    }

    public Builder with(GraphQLQueryInvoker queryInvoker) {
      if (queryInvoker != null) {
        this.queryInvoker = queryInvoker;
      }
      return this;
    }

    public Builder with(GraphQLObjectMapper objectMapper) {
      if (objectMapper != null) {
        this.objectMapper = objectMapper;
      }
      return this;
    }

    public Builder with(List<GraphQLServletListener> listeners) {
      if (listeners != null) {
        this.listeners = listeners;
      }
      return this;
    }

    public Builder with(GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder) {
      if (this.invocationInputFactoryBuilder != null) {
        throw new IllegalArgumentException("Cannot set invocationInputFactoryBuilder if invocationInputFactory is used");
      }
      this.invocationInputFactoryBuilder = invocationInputFactoryBuilder;
      return this;
    }

    public Builder with(GraphQLInvocationInputFactory invocationInputFactory) {
      if (this.invocationInputFactoryBuilder != null) {
        throw new IllegalArgumentException("Cannot set invocationInputFactory if invocationInputFactoryBuilder is used");
      }
      this.invocationInputFactory = invocationInputFactory;
      return this;
    }

    public Builder with(GraphQLServletContextBuilder contextBuilder) {
      if (this.invocationInputFactoryBuilder == null) {
        throw new IllegalArgumentException("Cannot use a contextBuilder without setting invocationInputFactoryBuilder first");
      }
      this.invocationInputFactoryBuilder.withGraphQLContextBuilder(contextBuilder);
      return this;
    }

    public Builder with(GraphQLServletRootObjectBuilder rootObjectBuilder) {
      if (this.invocationInputFactoryBuilder == null) {
        throw new IllegalArgumentException("Cannot use a rootObjectBuilder without setting invocationInputFactoryBuilder first");
      }
      this.invocationInputFactoryBuilder.withGraphQLRootObjectBuilder(rootObjectBuilder);
      return this;
    }

    public Builder subscriptionTimeout(long subscriptionTimeout) {
      this.subscriptionTimeout = subscriptionTimeout;
      return this;
    }

    public Builder asyncTimeout(long asyncTimeout) {
      this.asyncTimeout = asyncTimeout;
      return this;
    }

    public Builder with(Executor asyncExecutor) {
      this.asyncExecutor = asyncExecutor;
      return this;
    }

    public Builder with(ContextSetting contextSetting) {
      if (contextSetting != null) {
        this.contextSetting = contextSetting;
      }
      return this;
    }

    public Builder with(BatchInputPreProcessor batchInputPreProcessor) {
      if (batchInputPreProcessor != null) {
        this.batchInputPreProcessorSupplier = () -> batchInputPreProcessor;
      }
      return this;
    }

    public Builder with(Supplier<BatchInputPreProcessor> batchInputPreProcessor) {
      if (batchInputPreProcessor != null) {
        this.batchInputPreProcessorSupplier = batchInputPreProcessor;
      }
      return this;
    }

    public Builder with(GraphQLResponseCacheManager responseCache) {
      this.responseCacheManager = responseCache;
      return this;
    }

    public Builder with(AsyncTaskDecorator asyncTaskDecorator) {
      this.asyncTaskDecorator = asyncTaskDecorator;
      return this;
    }

    private Executor getAsyncTaskExecutor() {
      if (asyncExecutor != null) {
        return new AsyncTaskExecutor(asyncExecutor, asyncTaskDecorator);
      }

      return null;
    }

    public GraphQLConfiguration build() {
      return new GraphQLConfiguration(
          this.invocationInputFactory != null
              ? this.invocationInputFactory
              : invocationInputFactoryBuilder.build(),
          graphQLInvoker,
          queryInvoker,
          objectMapper,
          listeners,
          subscriptionTimeout,
          asyncTimeout,
          contextSetting,
          batchInputPreProcessorSupplier,
          responseCacheManager,
          getAsyncTaskExecutor());
    }
  }
}
