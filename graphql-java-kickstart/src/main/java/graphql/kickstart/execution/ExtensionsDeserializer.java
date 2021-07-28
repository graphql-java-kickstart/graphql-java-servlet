package graphql.kickstart.execution;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Map;

public class ExtensionsDeserializer extends JsonDeserializer<Map<String, Object>> {

  public static Map<String, Object> deserializeExtensionsObject(
      Object extensions, ObjectCodec codec) {
    return ObjectMapDeserializeHelper.deserializeObjectMapObject(extensions, codec, "extensions");
  }

  @Override
  public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    return ExtensionsDeserializer.deserializeExtensionsObject(
        p.readValueAs(Object.class), p.getCodec());
  }
}
