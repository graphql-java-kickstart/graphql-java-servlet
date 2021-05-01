package graphql.kickstart.servlet.osgi;

import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

public interface GraphQLSubscriptionProvider extends GraphQLFieldProvider {

  Collection<GraphQLFieldDefinition> getSubscriptions();

  default Collection<GraphQLFieldDefinition> getFields() {
    return getSubscriptions();
  }
}
