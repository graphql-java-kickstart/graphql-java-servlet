package graphql.kickstart.execution;

import graphql.GraphQL;
import graphql.kickstart.execution.config.GraphQLBuilder;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;

public class BatchedDataLoaderGraphQLBuilder {

  GraphQL newGraphQL(GraphQLBatchedInvocationInput invocationInput, GraphQLBuilder graphQLBuilder) {
    return invocationInput.getInvocationInputs().stream()
        .findFirst()
        .map(GraphQLSingleInvocationInput::getSchema)
        .map(schema -> graphQLBuilder.build(schema, graphQLBuilder.getInstrumentationSupplier()))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Batched invocation input must contain at least one query"));
  }
}
