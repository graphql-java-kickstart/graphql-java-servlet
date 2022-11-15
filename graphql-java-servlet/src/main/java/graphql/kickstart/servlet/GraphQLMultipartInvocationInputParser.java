package graphql.kickstart.servlet;

import static java.util.stream.Collectors.joining;

import graphql.GraphQLException;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.core.internal.VariableMapper;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GraphQLMultipartInvocationInputParser extends AbstractGraphQLInvocationInputParser {

  private static final String[] MULTIPART_KEYS = new String[] {"operations", "graphql", "query"};

  GraphQLMultipartInvocationInputParser(
      GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper,
      ContextSetting contextSetting) {
    super(invocationInputFactory, graphQLObjectMapper, contextSetting);
  }

  @Override
  public GraphQLInvocationInput getGraphQLInvocationInput(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      final Map<String, List<Part>> parts =
          request.getParts().stream().collect(Collectors.groupingBy(Part::getName));

      for (String key : MULTIPART_KEYS) {
        // Check to see if there is a part under the key we seek
        if (!parts.containsKey(key)) {
          continue;
        }

        final Optional<Part> queryItem = findPart(parts);
        if (!queryItem.isPresent()) {
          log.info("Bad POST multipart request: no part named {}", Arrays.toString(MULTIPART_KEYS));
          throw new GraphQLException(
              "Bad POST multipart request: no part named " + Arrays.toString(MULTIPART_KEYS));
        }

        return getGraphQLInvocationInput(request, response, parts, key, queryItem.get());
      }

      log.info("Bad POST multipart request: no part named {}", Arrays.toString(MULTIPART_KEYS));
      throw new GraphQLException(
          "Bad POST multipart request: no part named " + Arrays.toString(MULTIPART_KEYS));
    } catch (ServletException e) {
      throw new IOException("Cannot get parts from request", e);
    }
  }

  private GraphQLInvocationInput getGraphQLInvocationInput(
      HttpServletRequest request,
      HttpServletResponse response,
      Map<String, List<Part>> parts,
      String key,
      Part queryItem)
      throws IOException {
    InputStream inputStream = queryItem.getInputStream();

    final Optional<Map<String, List<String>>> variablesMap =
        getPart(parts, "map")
            .map(
                part -> {
                  try (InputStream is = part.getInputStream()) {
                    return graphQLObjectMapper.deserializeMultipartMap(is);
                  } catch (IOException e) {
                    throw new PartIOException("Unable to read input stream from part", e);
                  }
                });

    String query = read(inputStream, request.getCharacterEncoding());
    if ("query".equals(key) && isSingleQuery(query)) {
      GraphQLRequest graphqlRequest =
          buildRequestFromQuery(query, graphQLObjectMapper, parts, request.getCharacterEncoding());
      variablesMap.ifPresent(m -> mapMultipartVariables(graphqlRequest, m, parts));
      return invocationInputFactory.create(graphqlRequest, request, response);
    } else if (isSingleQuery(query)) {
      GraphQLRequest graphqlRequest = graphQLObjectMapper.readGraphQLRequest(query);
      variablesMap.ifPresent(m -> mapMultipartVariables(graphqlRequest, m, parts));
      return invocationInputFactory.create(graphqlRequest, request, response);
    } else {
      List<GraphQLRequest> graphqlRequests = graphQLObjectMapper.readBatchedGraphQLRequest(query);
      variablesMap.ifPresent(
          map -> graphqlRequests.forEach(r -> mapMultipartVariables(r, map, parts)));
      return invocationInputFactory.create(contextSetting, graphqlRequests, request, response);
    }
  }

  private Optional<Part> findPart(Map<String, List<Part>> parts) {
    return Arrays.stream(MULTIPART_KEYS)
        .filter(parts::containsKey)
        .map(key -> getPart(parts, key))
        .findFirst()
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Optional<Part> getPart(Map<String, List<Part>> parts, String name) {
    return Optional.ofNullable(parts.get(name))
        .filter(list -> !list.isEmpty())
        .map(list -> list.get(0));
  }

  private void mapMultipartVariables(
      GraphQLRequest request,
      Map<String, List<String>> variablesMap,
      Map<String, List<Part>> fileItems) {
    Map<String, Object> variables = request.getVariables();

    variablesMap.forEach(
        (partName, objectPaths) -> {
          Part part =
              getPart(fileItems, partName)
                  .orElseThrow(
                      () ->
                          new RuntimeException(
                              "unable to find part name "
                                  + partName
                                  + " as referenced in the variables map"));

          objectPaths.forEach(
              objectPath -> VariableMapper.mapVariable(objectPath, variables, part));
        });
  }

  private GraphQLRequest buildRequestFromQuery(
      String query,
      GraphQLObjectMapper graphQLObjectMapper,
      Map<String, List<Part>> parts,
      String charset)
      throws IOException {
    Map<String, Object> variables = null;
    final Optional<Part> variablesItem = getPart(parts, "variables");
    if (variablesItem.isPresent()) {
      variables =
          graphQLObjectMapper.deserializeVariables(
              read(variablesItem.get().getInputStream(), charset));
    }

    Map<String, Object> extensions = null;
    final Optional<Part> extensionsItem = getPart(parts, "extensions");
    if (extensionsItem.isPresent()) {
      extensions =
          graphQLObjectMapper.deserializeExtensions(
              read(extensionsItem.get().getInputStream(), charset));
    }

    String operationName = null;
    final Optional<Part> operationNameItem = getPart(parts, "operationName");
    if (operationNameItem.isPresent()) {
      operationName = read(operationNameItem.get().getInputStream(), charset).trim();
    }

    return new GraphQLRequest(query, variables, extensions, operationName);
  }

  private String read(InputStream inputStream, String charset) throws IOException {
    try (InputStreamReader streamReader = new InputStreamReader(inputStream, charset);
        BufferedReader reader = new BufferedReader(streamReader)) {
      return reader.lines().collect(joining());
    }
  }
}
