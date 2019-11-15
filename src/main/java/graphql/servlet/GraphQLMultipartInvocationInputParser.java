package graphql.servlet;

import static java.util.stream.Collectors.joining;

import graphql.GraphQLException;
import graphql.servlet.context.ContextSetting;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.core.internal.GraphQLRequest;
import graphql.servlet.core.internal.VariableMapper;
import graphql.servlet.input.GraphQLInvocationInput;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GraphQLMultipartInvocationInputParser extends AbstractGraphQLInvocationInputParser {

  private static final String[] MULTIPART_KEYS = new String[]{"operations", "graphql", "query"};

  GraphQLMultipartInvocationInputParser(GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper, ContextSetting contextSetting) {
    super(invocationInputFactory, graphQLObjectMapper, contextSetting);
  }

  @Override
  public GraphQLInvocationInput getGraphQLInvocationInput(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      final Map<String, List<Part>> parts = request.getParts()
          .stream()
          .collect(Collectors.groupingBy(Part::getName));

      for (String key : MULTIPART_KEYS) {
        // Check to see if there is a part under the key we seek
        if (!parts.containsKey(key)) {
          continue;
        }

        final Optional<Part> queryItem = getPart(parts, key);
        if (!queryItem.isPresent()) {
          // If there is a part, but we don't see an item, then break and return BAD_REQUEST
          break;
        }

        InputStream inputStream = queryItem.get().getInputStream();

        final Optional<Map<String, List<String>>> variablesMap =
            getPart(parts, "map").map(graphQLObjectMapper::deserializeMultipartMap);

        if ("query".equals(key)) {
          GraphQLRequest graphqlRequest = buildRequestFromQuery(inputStream, graphQLObjectMapper, parts);
          variablesMap.ifPresent(m -> mapMultipartVariables(graphqlRequest, m, parts));
          return invocationInputFactory.create(graphqlRequest, request, response);
        } else {
          String body = read(inputStream);
          if (isSingleQuery(body)) {
            GraphQLRequest graphqlRequest = graphQLObjectMapper.readGraphQLRequest(body);
            variablesMap.ifPresent(m -> mapMultipartVariables(graphqlRequest, m, parts));
            return invocationInputFactory.create(graphqlRequest, request, response);
          } else {
            List<GraphQLRequest> graphqlRequests = graphQLObjectMapper.readBatchedGraphQLRequest(body);
            variablesMap.ifPresent(map -> graphqlRequests.forEach(r -> mapMultipartVariables(r, map, parts)));
            return invocationInputFactory.create(contextSetting, graphqlRequests, request, response);
          }
        }
      }

      log.info("Bad POST multipart request: no part named {}", Arrays.toString(MULTIPART_KEYS));
      throw new GraphQLException("Bad POST multipart request: no part named " + Arrays.toString(MULTIPART_KEYS));
    } catch (ServletException e) {
      throw new IOException("Cannot get parts from request", e);
    }
  }

  private Optional<Part> getPart(Map<String, List<Part>> parts, String name) {
    return Optional.ofNullable(parts.get(name)).filter(list -> !list.isEmpty()).map(list -> list.get(0));
  }

  private void mapMultipartVariables(GraphQLRequest request,
      Map<String, List<String>> variablesMap,
      Map<String, List<Part>> fileItems) {
    Map<String, Object> variables = request.getVariables();

    variablesMap.forEach((partName, objectPaths) -> {
      Part part = getPart(fileItems, partName)
          .orElseThrow(() -> new RuntimeException("unable to find part name " +
              partName +
              " as referenced in the variables map"));

      objectPaths.forEach(objectPath -> VariableMapper.mapVariable(objectPath, variables, part));
    });
  }

  private GraphQLRequest buildRequestFromQuery(InputStream inputStream,
      GraphQLObjectMapper graphQLObjectMapper,
      Map<String, List<Part>> parts) throws IOException {
    String query = read(inputStream);

    Map<String, Object> variables = null;
    final Optional<Part> variablesItem = getPart(parts, "variables");
    if (variablesItem.isPresent()) {
      variables = graphQLObjectMapper
          .deserializeVariables(read(variablesItem.get().getInputStream()));
    }

    String operationName = null;
    final Optional<Part> operationNameItem = getPart(parts, "operationName");
    if (operationNameItem.isPresent()) {
      operationName = read(operationNameItem.get().getInputStream()).trim();
    }

    return new GraphQLRequest(query, variables, operationName);
  }

  private String read(InputStream inputStream) throws IOException {
    try (InputStreamReader streamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(streamReader)) {
      return reader.lines().collect(joining());
    }
  }

}
