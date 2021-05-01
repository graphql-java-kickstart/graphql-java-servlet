package graphql.kickstart.execution.config;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

public interface GraphQLSchemaProvider {

  static GraphQLSchema copyReadOnly(GraphQLSchema schema) {
    return GraphQLSchema.newSchema(schema).mutation((GraphQLObjectType) null).build();
  }

  /** @return a schema for handling mbean calls. */
  GraphQLSchema getSchema();

  GraphQLSchema getReadOnlySchema();
}
