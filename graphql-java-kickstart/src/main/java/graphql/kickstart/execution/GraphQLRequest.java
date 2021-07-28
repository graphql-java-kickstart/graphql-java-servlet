package graphql.kickstart.execution;

import static graphql.kickstart.execution.OperationNameExtractor.extractOperationName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import graphql.introspection.IntrospectionQuery;
import java.util.HashMap;
import java.util.Map;

/** @author Andrew Potter */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLRequest {

  private String query;

  @JsonDeserialize(using = VariablesDeserializer.class)
  private Map<String, Object> variables = new HashMap<>();

  @JsonDeserialize(using = ExtensionsDeserializer.class)
  private Map<String, Object> extensions = new HashMap<>();

  private String operationName;

  public GraphQLRequest() {}

  public GraphQLRequest(
      String query,
      Map<String, Object> variables,
      Map<String, Object> extensions,
      String operationName) {
    this.query = query;
    this.operationName = operationName;
    if (extensions != null) {
      this.extensions = extensions;
    }
    if (variables != null) {
      this.variables = variables;
    }
  }

  public static GraphQLRequest createIntrospectionRequest() {
    return new GraphQLRequest(
        IntrospectionQuery.INTROSPECTION_QUERY,
        new HashMap<>(),
        new HashMap<>(),
        "IntrospectionQuery");
  }

  public static GraphQLRequest createQueryOnlyRequest(String query) {
    return new GraphQLRequest(query, new HashMap<>(), new HashMap<>(), null);
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, Object> variables) {
    if (variables != null) {
      this.variables = variables;
    }
  }

  public Map<String, Object> getExtensions() {
    return extensions;
  }

  public void setExtensions(Map<String, Object> extensions) {
    if (extensions != null) {
      this.extensions = extensions;
    }
  }

  public String getOperationName() {
    return extractOperationName(query, operationName, null);
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }
}
