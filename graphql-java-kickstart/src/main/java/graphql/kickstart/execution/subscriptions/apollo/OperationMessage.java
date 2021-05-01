package graphql.kickstart.execution.subscriptions.apollo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationMessage {

  private Type type;
  private String id;
  private Object payload;

  public static OperationMessage newKeepAliveMessage() {
    return new OperationMessage(Type.GQL_CONNECTION_KEEP_ALIVE, null, null);
  }

  public Type getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public Object getPayload() {
    return payload;
  }

  public enum Type {

    // Server Messages
    GQL_CONNECTION_ACK("connection_ack"),
    GQL_CONNECTION_ERROR("connection_error"),
    GQL_CONNECTION_KEEP_ALIVE("ka"),
    GQL_DATA("data"),
    GQL_ERROR("error"),
    GQL_COMPLETE("complete"),

    // Client Messages
    GQL_CONNECTION_INIT("connection_init"),
    GQL_CONNECTION_TERMINATE("connection_terminate"),
    GQL_START("start"),
    GQL_STOP("stop");

    private static final Map<String, Type> reverseLookup = new HashMap<>();

    static {
      for (Type type : Type.values()) {
        reverseLookup.put(type.getValue(), type);
      }
    }

    private final String value;

    Type(String value) {
      this.value = value;
    }

    @JsonCreator
    public static Type findType(String value) {
      return reverseLookup.get(value);
    }

    @JsonValue
    public String getValue() {
      return value;
    }
  }
}
