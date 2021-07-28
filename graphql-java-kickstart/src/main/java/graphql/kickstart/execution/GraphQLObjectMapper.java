package graphql.kickstart.execution;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.kickstart.execution.config.ConfiguringObjectMapperProvider;
import graphql.kickstart.execution.config.GraphQLServletObjectMapperConfigurer;
import graphql.kickstart.execution.config.ObjectMapperProvider;
import graphql.kickstart.execution.error.DefaultGraphQLErrorHandler;
import graphql.kickstart.execution.error.GraphQLErrorHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.SneakyThrows;

/** @author Andrew Potter */
public class GraphQLObjectMapper {

  private static final TypeReference<Map<String, List<String>>> MULTIPART_MAP_TYPE_REFERENCE =
      new TypeReference<Map<String, List<String>>>() {};
  private final ObjectMapperProvider objectMapperProvider;
  private final Supplier<GraphQLErrorHandler> graphQLErrorHandlerSupplier;

  private ObjectMapper mapper;

  protected GraphQLObjectMapper(
      ObjectMapperProvider objectMapperProvider,
      Supplier<GraphQLErrorHandler> graphQLErrorHandlerSupplier) {
    this.objectMapperProvider = objectMapperProvider;
    this.graphQLErrorHandlerSupplier = graphQLErrorHandlerSupplier;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  // Double-check idiom for lazy initialization of instance fields.
  public ObjectMapper getJacksonMapper() {
    ObjectMapper result = mapper;
    if (result == null) { // First check (no locking)
      synchronized (this) {
        result = mapper;
        if (result == null) { // Second check (with locking)
          mapper = result = objectMapperProvider.provide();
        }
      }
    }

    return result;
  }

  /** @return an {@link ObjectReader} for deserializing {@link GraphQLRequest} */
  public ObjectReader getGraphQLRequestMapper() {
    return getJacksonMapper().reader().forType(GraphQLRequest.class);
  }

  public GraphQLRequest readGraphQLRequest(InputStream inputStream) throws IOException {
    return getGraphQLRequestMapper().readValue(inputStream);
  }

  public GraphQLRequest readGraphQLRequest(String text) throws IOException {
    return getGraphQLRequestMapper().readValue(text);
  }

  public List<GraphQLRequest> readBatchedGraphQLRequest(InputStream inputStream)
      throws IOException {
    MappingIterator<GraphQLRequest> iterator = getGraphQLRequestMapper().readValues(inputStream);
    List<GraphQLRequest> requests = new ArrayList<>();

    while (iterator.hasNext()) {
      requests.add(iterator.next());
    }

    return requests;
  }

  public List<GraphQLRequest> readBatchedGraphQLRequest(String query) throws IOException {
    MappingIterator<GraphQLRequest> iterator = getGraphQLRequestMapper().readValues(query);
    List<GraphQLRequest> requests = new ArrayList<>();

    while (iterator.hasNext()) {
      requests.add(iterator.next());
    }

    return requests;
  }

  @SneakyThrows
  public String serializeResultAsJson(ExecutionResult executionResult) {
    return getJacksonMapper().writeValueAsString(createResultFromExecutionResult(executionResult));
  }

  public void serializeResultAsJson(Writer writer, ExecutionResult executionResult)
      throws IOException {
    getJacksonMapper().writeValue(writer, createResultFromExecutionResult(executionResult));
  }

  /**
   * Serializes result as bytes in UTF-8 encoding instead of string.
   *
   * @param executionResult query execution result to serialize.
   * @return result serialized into Json representation in UTF-8 encoding, converted into {@code
   *     byte[]}.
   */
  @SneakyThrows
  public byte[] serializeResultAsBytes(ExecutionResult executionResult) {
    return getJacksonMapper().writeValueAsBytes(createResultFromExecutionResult(executionResult));
  }

  public boolean areErrorsPresent(ExecutionResult executionResult) {
    return graphQLErrorHandlerSupplier.get().errorsPresent(executionResult.getErrors());
  }

  public ExecutionResult sanitizeErrors(ExecutionResult executionResult) {
    Object data = executionResult.getData();
    Map<Object, Object> extensions = executionResult.getExtensions();
    List<GraphQLError> errors = executionResult.getErrors();

    GraphQLErrorHandler errorHandler = graphQLErrorHandlerSupplier.get();
    if (errorHandler.errorsPresent(errors)) {
      errors = errorHandler.processErrors(errors);
    } else {
      errors = null;
    }
    return new ExecutionResultImpl(data, errors, extensions);
  }

  public Map<String, Object> createResultFromExecutionResult(ExecutionResult executionResult) {
    ExecutionResult sanitizedExecutionResult = sanitizeErrors(executionResult);
    return convertSanitizedExecutionResult(sanitizedExecutionResult);
  }

  public Map<String, Object> convertSanitizedExecutionResult(ExecutionResult executionResult) {
    return convertSanitizedExecutionResult(executionResult, true);
  }

  public Map<String, Object> convertSanitizedExecutionResult(
      ExecutionResult executionResult, boolean includeData) {
    final Map<String, Object> result = new LinkedHashMap<>();

    if (areErrorsPresent(executionResult)) {
      result.put(
          "errors",
          executionResult.getErrors().stream()
              .map(GraphQLError::toSpecification)
              .collect(toList()));
    }

    if (executionResult.getExtensions() != null && !executionResult.getExtensions().isEmpty()) {
      result.put("extensions", executionResult.getExtensions());
    }

    if (includeData) {
      result.put("data", executionResult.getData());
    }

    return result;
  }

  @SneakyThrows
  public Map<String, Object> deserializeVariables(String variables) {
    return VariablesDeserializer.deserializeVariablesObject(
        getJacksonMapper().readValue(variables, Object.class), getJacksonMapper());
  }

  @SneakyThrows
  public Map<String, Object> deserializeExtensions(String extensions) {
    return ExtensionsDeserializer.deserializeExtensionsObject(
        getJacksonMapper().readValue(extensions, Object.class), getJacksonMapper());
  }

  @SneakyThrows
  public Map<String, List<String>> deserializeMultipartMap(InputStream inputStream) {
    return getJacksonMapper().readValue(inputStream, MULTIPART_MAP_TYPE_REFERENCE);
  }

  public static class Builder {

    private ObjectMapperProvider objectMapperProvider = new ConfiguringObjectMapperProvider();
    private Supplier<GraphQLErrorHandler> graphQLErrorHandler = DefaultGraphQLErrorHandler::new;

    public Builder withObjectMapperConfigurer(
        GraphQLServletObjectMapperConfigurer objectMapperConfigurer) {
      return withObjectMapperConfigurer(() -> objectMapperConfigurer);
    }

    public Builder withObjectMapperConfigurer(
        Supplier<GraphQLServletObjectMapperConfigurer> objectMapperConfigurer) {
      this.objectMapperProvider = new ConfiguringObjectMapperProvider(objectMapperConfigurer.get());
      return this;
    }

    public Builder withObjectMapperProvider(ObjectMapperProvider objectMapperProvider) {
      this.objectMapperProvider = objectMapperProvider;
      return this;
    }

    public Builder withGraphQLErrorHandler(GraphQLErrorHandler graphQLErrorHandler) {
      return withGraphQLErrorHandler(() -> graphQLErrorHandler);
    }

    public Builder withGraphQLErrorHandler(Supplier<GraphQLErrorHandler> graphQLErrorHandler) {
      this.graphQLErrorHandler = graphQLErrorHandler;
      return this;
    }

    public GraphQLObjectMapper build() {
      return new GraphQLObjectMapper(objectMapperProvider, graphQLErrorHandler);
    }
  }
}
