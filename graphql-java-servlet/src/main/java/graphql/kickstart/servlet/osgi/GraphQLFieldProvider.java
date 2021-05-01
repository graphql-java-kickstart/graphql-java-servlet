package graphql.kickstart.servlet.osgi;

import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

public interface GraphQLFieldProvider extends GraphQLProvider {

  Collection<GraphQLFieldDefinition> getFields();
}
