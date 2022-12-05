package graphql.kickstart.servlet.config;

import graphql.kickstart.execution.config.DefaultGraphQLSchemaProvider;
import graphql.schema.GraphQLSchema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.HandshakeRequest;

/** @author Andrew Potter */
public class DefaultGraphQLSchemaServletProvider extends DefaultGraphQLSchemaProvider
    implements GraphQLSchemaServletProvider {

  public DefaultGraphQLSchemaServletProvider(GraphQLSchema schema) {
    super(schema);
  }

  @Override
  public GraphQLSchema getSchema(HttpServletRequest request) {
    return getSchema();
  }

  @Override
  public GraphQLSchema getSchema(HandshakeRequest request) {
    return getSchema();
  }

  @Override
  public GraphQLSchema getReadOnlySchema(HttpServletRequest request) {
    return getReadOnlySchema();
  }
}
