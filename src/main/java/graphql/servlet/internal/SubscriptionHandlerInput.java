package graphql.servlet.internal;

import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

public class SubscriptionHandlerInput {

    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLObjectMapper graphQLObjectMapper;

    public SubscriptionHandlerInput(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper) {
        this.invocationInputFactory = invocationInputFactory;
        this.queryInvoker = queryInvoker;
        this.graphQLObjectMapper = graphQLObjectMapper;
    }

    public GraphQLInvocationInputFactory getInvocationInputFactory() {
        return invocationInputFactory;
    }

    public GraphQLQueryInvoker getQueryInvoker() {
        return queryInvoker;
    }

    public GraphQLObjectMapper getGraphQLObjectMapper() {
        return graphQLObjectMapper;
    }
}
