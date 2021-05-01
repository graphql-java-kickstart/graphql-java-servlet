package graphql.kickstart.execution.config;

import graphql.schema.GraphQLSchema;

/** @author Andrew Potter */
public class DefaultGraphQLSchemaProvider implements GraphQLSchemaProvider {

  private final GraphQLSchema schema;
  private final GraphQLSchema readOnlySchema;

  public DefaultGraphQLSchemaProvider(GraphQLSchema schema) {
    this(schema, GraphQLSchemaProvider.copyReadOnly(schema));
  }

  public DefaultGraphQLSchemaProvider(GraphQLSchema schema, GraphQLSchema readOnlySchema) {
    this.schema = schema;
    this.readOnlySchema = readOnlySchema;
  }

  @Override
  public GraphQLSchema getSchema() {
    return schema;
  }

  @Override
  public GraphQLSchema getReadOnlySchema() {
    return readOnlySchema;
  }
}
