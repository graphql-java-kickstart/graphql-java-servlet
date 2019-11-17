package graphql.servlet.core.internal;

import graphql.servlet.input.GraphQLInvocationInputFactory;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.subscription.SubscriptionConnectionListener;

import java.util.Optional;

public class SubscriptionHandlerInput {

    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLObjectMapper graphQLObjectMapper;
    private final SubscriptionConnectionListener subscriptionConnectionListener;

    public SubscriptionHandlerInput(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper, SubscriptionConnectionListener subscriptionConnectionListener) {
        this.invocationInputFactory = invocationInputFactory;
        this.queryInvoker = queryInvoker;
        this.graphQLObjectMapper = graphQLObjectMapper;
        this.subscriptionConnectionListener = subscriptionConnectionListener;
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

    public Optional<SubscriptionConnectionListener> getSubscriptionConnectionListener() {
        return Optional.ofNullable(subscriptionConnectionListener);
    }
}
