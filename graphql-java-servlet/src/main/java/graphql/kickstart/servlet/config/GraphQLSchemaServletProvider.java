package graphql.kickstart.servlet.config;

import graphql.kickstart.execution.config.GraphQLSchemaProvider;
import graphql.schema.GraphQLSchema;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLSchemaServletProvider extends GraphQLSchemaProvider {

  /**
   * @param request the http request
   * @return a schema based on the request (auth, etc).
   */
  GraphQLSchema getSchema(HttpServletRequest request);

  /**
   * @param request the http request used to create a websocket
   * @return a schema based on the request (auth, etc).
   */
  GraphQLSchema getSchema(HandshakeRequest request);

  /**
   * @param request the http request
   * @return a read-only schema based on the request (auth, etc).  Should return the same schema
   * (query/subscription-only version) as {@link #getSchema(HttpServletRequest)} for a given
   * request.
   */
  GraphQLSchema getReadOnlySchema(HttpServletRequest request);

}
