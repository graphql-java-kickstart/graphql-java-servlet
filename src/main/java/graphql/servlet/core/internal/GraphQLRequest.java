package graphql.servlet.core.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import graphql.introspection.IntrospectionQuery;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Potter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLRequest {
    private String query;
    @JsonDeserialize(using = VariablesDeserializer.class)
    private Map<String, Object> variables = new HashMap<>();
    private String operationName;

    public GraphQLRequest() {
    }

    public GraphQLRequest(String query, Map<String, Object> variables, String operationName) {
        this.query = query;
        this.operationName = operationName;
        if (variables != null) {
            this.variables = variables;
        }
    }

    public static GraphQLRequest createIntrospectionRequest() {
        return new GraphQLRequest(IntrospectionQuery.INTROSPECTION_QUERY, new HashMap<>(), null);
    }

    public static GraphQLRequest createQueryOnlyRequest(String query) {
        return new GraphQLRequest(query, new HashMap<>(), null);
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

    public String getOperationName() {
        if (operationName != null && !operationName.isEmpty()) {
            return operationName;
        }

        return null;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}


