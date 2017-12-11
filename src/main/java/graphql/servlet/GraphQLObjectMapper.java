package graphql.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import graphql.ExecutionResult;
import graphql.servlet.internal.GraphQLRequest;
import graphql.servlet.internal.VariablesDeserializer;

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
    private final Supplier<ObjectMapperConfigurer> objectMapperConfigurerSupplier;
    private final Supplier<GraphQLErrorHandler> graphQLErrorHandlerSupplier;

    private volatile ObjectMapper mapper;

    protected GraphQLObjectMapper(Supplier<ObjectMapperConfigurer> objectMapperConfigurerSupplier, Supplier<GraphQLErrorHandler> graphQLErrorHandlerSupplier) {
        this.objectMapperConfigurerSupplier = objectMapperConfigurerSupplier;
        this.graphQLErrorHandlerSupplier = graphQLErrorHandlerSupplier;
    }

    // Double-check idiom for lazy initialization of instance fields.
    public ObjectMapper getJacksonMapper() {
        ObjectMapper result = mapper;
        if (result == null) { // First check (no locking)
            synchronized(this) {
                result = mapper;
                if (result == null) // Second check (with locking)
                    mapper = result = createObjectMapper();
            }
        }

        return result;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).registerModule(new Jdk8Module());
        objectMapperConfigurerSupplier.get().configure(mapper);

        return mapper;
    }

    /**
     * Creates an {@link ObjectReader} for deserializing {@link GraphQLRequest}
     */
    public ObjectReader getGraphQLRequestMapper() {
        // Add object mapper to injection so VariablesDeserializer can access it...
        InjectableValues.Std injectableValues = new InjectableValues.Std();
        injectableValues.addValue(ObjectMapper.class, getJacksonMapper());

        return getJacksonMapper().reader(injectableValues).forType(GraphQLRequest.class);
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

    public Map<String, Object> createResultFromExecutionResult(ExecutionResult executionResult) {

        GraphQLErrorHandler errorHandler = graphQLErrorHandlerSupplier.get();

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", executionResult.getData());

        if (errorHandler.errorsPresent(executionResult.getErrors())) {
            result.put("errors", errorHandler.processErrors(executionResult.getErrors()));
        }

        if(executionResult.getExtensions() != null){
            result.put("extensions", executionResult.getExtensions());
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Supplier<ObjectMapperConfigurer> objectMapperConfigurer = DefaultObjectMapperConfigurer::new;
        private Supplier<GraphQLErrorHandler> graphQLErrorHandler = DefaultGraphQLErrorHandler::new;

        public Builder withObjectMapperConfigurer(ObjectMapperConfigurer objectMapperConfigurer) {
            return withObjectMapperConfigurer(() -> objectMapperConfigurer);
        }

        public Builder withObjectMapperConfigurer(Supplier<ObjectMapperConfigurer> objectMapperConfigurer) {
            this.objectMapperConfigurer = objectMapperConfigurer;
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
            return new GraphQLObjectMapper(objectMapperConfigurer, graphQLErrorHandler);
        }
    }
}
