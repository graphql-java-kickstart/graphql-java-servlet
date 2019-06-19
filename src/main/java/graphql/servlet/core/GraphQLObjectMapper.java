package graphql.servlet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.servlet.config.ConfiguringObjectMapperProvider;
import graphql.servlet.config.ObjectMapperConfigurer;
import graphql.servlet.config.ObjectMapperProvider;
import graphql.servlet.core.internal.GraphQLRequest;
import graphql.servlet.core.internal.VariablesDeserializer;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Andrew Potter
 */
public class GraphQLObjectMapper {
    private static final TypeReference<Map<String, List<String>>>
        MULTIPART_MAP_TYPE_REFERENCE = new TypeReference<Map<String, List<String>>>() {
        };
    private final ObjectMapperProvider objectMapperProvider;
    private final Supplier<GraphQLErrorHandler> graphQLErrorHandlerSupplier;

    private volatile ObjectMapper mapper;

    protected GraphQLObjectMapper(ObjectMapperProvider objectMapperProvider, Supplier<GraphQLErrorHandler> graphQLErrorHandlerSupplier) {
        this.objectMapperProvider = objectMapperProvider;
        this.graphQLErrorHandlerSupplier = graphQLErrorHandlerSupplier;
    }

    // Double-check idiom for lazy initialization of instance fields.
    public ObjectMapper getJacksonMapper() {
        ObjectMapper result = mapper;
        if (result == null) { // First check (no locking)
            synchronized(this) {
                result = mapper;
                if (result == null) { // Second check (with locking)
                    mapper = result = objectMapperProvider.provide();
                }
            }
        }

        return result;
    }

    /**
     * @return an {@link ObjectReader} for deserializing {@link GraphQLRequest}
     */
    public ObjectReader getGraphQLRequestMapper() {
        return getJacksonMapper().reader().forType(GraphQLRequest.class);
    }

    public GraphQLRequest readGraphQLRequest(InputStream inputStream) throws IOException {
        return getGraphQLRequestMapper().readValue(inputStream);
    }

    public GraphQLRequest readGraphQLRequest(String text) throws IOException {
        return getGraphQLRequestMapper().readValue(text);
    }

    public List<GraphQLRequest> readBatchedGraphQLRequest(InputStream inputStream) throws IOException {
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

    public String serializeResultAsJson(ExecutionResult executionResult) {
        try {
            return getJacksonMapper().writeValueAsString(createResultFromExecutionResult(executionResult));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean areErrorsPresent(ExecutionResult executionResult) {
        return graphQLErrorHandlerSupplier.get().errorsPresent(executionResult.getErrors());
    }

    public ExecutionResult sanitizeErrors(ExecutionResult executionResult) {
        Object data = executionResult.getData();
        Map<Object, Object> extensions = executionResult.getExtensions();
        List<GraphQLError> errors = executionResult.getErrors();

        GraphQLErrorHandler errorHandler = graphQLErrorHandlerSupplier.get();
        if(errorHandler.errorsPresent(errors)) {
            errors = errorHandler.processErrors(errors);
        } else {
            errors = null;
        }

        return new ExecutionResultImpl(data, errors, extensions);
    }

    public Map<String, Object> createResultFromExecutionResult(ExecutionResult executionResult) {
        return convertSanitizedExecutionResult(sanitizeErrors(executionResult));
    }

    public Map<String, Object> convertSanitizedExecutionResult(ExecutionResult executionResult) {
        return convertSanitizedExecutionResult(executionResult, true);
    }

    public Map<String, Object> convertSanitizedExecutionResult(ExecutionResult executionResult, boolean includeData) {
        final Map<String, Object> result = new LinkedHashMap<>();

        if (areErrorsPresent(executionResult)) {
            result.put("errors", executionResult.getErrors());
        }

        if(executionResult.getExtensions() != null && !executionResult.getExtensions().isEmpty()){
            result.put("extensions", executionResult.getExtensions());
        }

        if(includeData) {
            result.put("data", executionResult.getData());
        }

        return result;
    }

    public Map<String, Object> deserializeVariables(String variables) {
        try {
            return VariablesDeserializer.deserializeVariablesObject(getJacksonMapper().readValue(variables, Object.class), getJacksonMapper());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String,List<String>> deserializeMultipartMap(Part part) {
        try {
            return getJacksonMapper().readValue(part.getInputStream(), MULTIPART_MAP_TYPE_REFERENCE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private ObjectMapperProvider objectMapperProvider = new ConfiguringObjectMapperProvider();
        private Supplier<GraphQLErrorHandler> graphQLErrorHandler = DefaultGraphQLErrorHandler::new;

        public Builder withObjectMapperConfigurer(ObjectMapperConfigurer objectMapperConfigurer) {
            return withObjectMapperConfigurer(() -> objectMapperConfigurer);
        }

        public Builder withObjectMapperConfigurer(Supplier<ObjectMapperConfigurer> objectMapperConfigurer) {
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
