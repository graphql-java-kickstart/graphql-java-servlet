package graphql.kickstart.execution;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Map;

public class ObjectMapDeserializeHelper {

  public static Map<String, Object> deserializeObjectMapObject(
      Object object, ObjectCodec codec, String fieldName) {
    if (object instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> genericObjectMap = (Map<String, Object>) object;
      return genericObjectMap;
    } else if (object instanceof String) {
      try {
        return codec.readValue(
            codec.getFactory().createParser((String) object),
            new TypeReference<Map<String, Object>>() {});
      } catch (IOException e) {
        throw new ObjectMapDeserializationException(
            String.format("Cannot deserialize field '%s'", fieldName), e);
      }
    } else {
      throw new ObjectMapDeserializationException(
          String.format("Field '%s' should be either an object or a string", fieldName));
    }
  }
}
