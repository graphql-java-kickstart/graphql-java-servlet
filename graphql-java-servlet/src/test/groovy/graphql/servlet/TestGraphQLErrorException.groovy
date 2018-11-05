package graphql.servlet

import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation

/**
 * @author Andrew Potter
 */
class TestGraphQLErrorException extends RuntimeException implements GraphQLError {

    public TestGraphQLErrorException(String message) {
        super(message);
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> customAttributes = new LinkedHashMap<>();
        customAttributes.put("foo", "bar");
        return customAttributes;
    }

    @Override
    List<SourceLocation> getLocations() {
        return null
    }

    @Override
    ErrorType getErrorType() {
        return ErrorType.ValidationError;
    }
}
