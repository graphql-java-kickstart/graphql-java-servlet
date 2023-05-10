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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    return new Builder(GraphQLInvocationInputFactory.newBuilder(schemaProvider));
  }

  public static GraphQLConfiguration.Builder with(
      GraphQLInvocationInputFactory invocationInputFactory) {
    return new Builder(invocationInputFactory);
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
    private ContextSetting contextSetting = ContextSetting.PER_QUERY_WITH_INSTRUMENTATION;
    private Supplier<BatchInputPreProcessor> batchInputPreProcessorSupplier =
        NoOpBatchInputPreProcessor::new;
    private GraphQLResponseCacheManager responseCacheManager;
    private int asyncCorePoolSize = 10;
    private int asyncMaxPoolSize = 200;
    private Executor asyncExecutor;
    private AsyncTaskDecorator asyncTaskDecorator;

    private Builder(GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder) {
      this.invocationInputFactoryBuilder = invocationInputFactoryBuilder;
    }

    private Builder(GraphQLInvocationInputFactory invocationInputFactory) {
      this.invocationInputFactory = invocationInputFactory;
    }

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

    public Builder with(GraphQLServletContextBuilder contextBuilder) {
      this.invocationInputFactoryBuilder.withGraphQLContextBuilder(contextBuilder);
      return this;
    }

    public Builder with(GraphQLServletRootObjectBuilder rootObjectBuilder) {
      this.invocationInputFactoryBuilder.withGraphQLRootObjectBuilder(rootObjectBuilder);
      return this;
    }

    public Builder with(long subscriptionTimeout) {
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

    public Builder asyncCorePoolSize(int asyncCorePoolSize) {
      this.asyncCorePoolSize = asyncCorePoolSize;
      return this;
    }

    public Builder asyncMaxPoolSize(int asyncMaxPoolSize) {
      this.asyncMaxPoolSize = asyncMaxPoolSize;
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

    private Executor getAsyncExecutor() {
      if (asyncExecutor != null) {
        return asyncExecutor;
      }
      return new ThreadPoolExecutor(
          asyncCorePoolSize,
          asyncMaxPoolSize,
          60,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(Integer.MAX_VALUE));
    }

    private Executor getAsyncTaskExecutor() {
      return new AsyncTaskExecutor(getAsyncExecutor(), asyncTaskDecorator);
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
