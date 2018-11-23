package graphql.servlet.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * @author Andrew Potter
 */
public class VariablesDeserializer extends JsonDeserializer<Map<String, Object>> {
    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserializeVariablesObject(p.readValueAs(Object.class), (ObjectMapper) ctxt.findInjectableValue(ObjectMapper.class.getName(), null, null));
    }

    public static Map<String, Object> deserializeVariablesObject(Object variables, ObjectMapper mapper) {
        if (variables instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> genericVariables = (Map<String, Object>) variables;
            return genericVariables;
        } else if (variables instanceof String) {
            try {
                return mapper.readValue((String) variables, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("variables should be either an object or a string");
        }
    }

}

