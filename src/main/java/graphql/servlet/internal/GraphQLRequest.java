package graphql.servlet.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Potter
 */
public class GraphQLRequest {
    private String query;
    @JsonDeserialize(using = VariablesDeserializer.class)
    private Map<String, Object> variables = new HashMap<>();
    private String operationName;

    public GraphQLRequest() {
    }

    public GraphQLRequest(String query, Map<String, Object> variables, String operationName) {
        this.query = query;
        this.variables = variables;
        this.operationName = operationName;
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
        this.variables = variables;
    }

    public String getOperationName() {
        if(operationName != null && !operationName.isEmpty()) {
            return operationName;
        }

        return null;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}


