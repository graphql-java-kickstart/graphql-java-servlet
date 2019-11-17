package graphql.servlet.osgi;

import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.osgi.GraphQLProvider;

import java.util.Collection;

public interface GraphQLSubscriptionProvider extends GraphQLProvider {
    Collection<GraphQLFieldDefinition> getSubscriptions();
}
