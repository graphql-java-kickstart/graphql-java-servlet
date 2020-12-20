package graphql.kickstart.execution;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Map;

/**
 * @author Andrew Potter
 */
public class VariablesDeserializer extends JsonDeserializer<Map<String, Object>> {

  public static Map<String, Object> deserializeVariablesObject(Object variables,
      ObjectCodec codec) {
    if (variables instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> genericVariables = (Map<String, Object>) variables;
      return genericVariables;
    } else if (variables instanceof String) {
      try {
        return codec
            .readValue(codec.getFactory().createParser((String) variables),
                new TypeReference<Map<String, Object>>() {
                });
      } catch (IOException e) {
        throw new VariablesDeserializationException("Cannot deserialize variables", e);
      }
    } else {
      throw new VariablesDeserializationException(
          "Variables should be either an object or a string");
    }
  }

  @Override
  public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    return deserializeVariablesObject(p.readValueAs(Object.class), p.getCodec());
  }

}

