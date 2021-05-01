package graphql.kickstart.execution;

import static graphql.kickstart.execution.StringUtils.isNotEmpty;

import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class OperationNameExtractor {

  static String extractOperationName(
      String gqlQuery, String requestedOperationName, String defaultIfNotFound) {
    if (isNotEmpty(requestedOperationName)) {
      return requestedOperationName;
    }
    if (isNotEmpty(gqlQuery)) {
      return parseForOperationName(gqlQuery, defaultIfNotFound);
    }
    return defaultIfNotFound;
  }

  private static String parseForOperationName(String gqlQuery, String defaultIfNotFound) {
    try {
      Document document = new Parser().parseDocument(gqlQuery);
      List<OperationDefinition> operations =
          document.getDefinitionsOfType(OperationDefinition.class);
      if (operations.size() == 1) {
        String name = operations.get(0).getName();
        if (isNotEmpty(name)) {
          return name;
        }
      }
    } catch (InvalidSyntaxException ignored) {
      // ignored
    }
    return defaultIfNotFound;
  }
}
