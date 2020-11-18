package graphql.kickstart.servlet;

public interface HttpRequestHandlerFactory {

    /**
     * Create a Request Handler instance for current servlet constructs
     * and frameworks not needing explicit servlet construct (Jersey).
     * @param configuration GraphQLConfiguration object
     * @return HttpRequestHandler interface instance
     */
    static HttpRequestHandler create(GraphQLConfiguration configuration) {
        return new HttpRequestHandlerImpl(configuration);
    }
}
