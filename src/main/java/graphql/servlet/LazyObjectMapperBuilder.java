package graphql.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * @author Andrew Potter
 */
public class LazyObjectMapperBuilder {
    private final ObjectMapperConfigurer configurer;
    private volatile ObjectMapper mapper;

    public LazyObjectMapperBuilder(ObjectMapperConfigurer configurer) {
        this.configurer = configurer;
    }

    // Double-check idiom for lazy initialization of instance fields.
    public ObjectMapper getMapper() {
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
        configurer.configure(mapper);

        return mapper;
    }
}
