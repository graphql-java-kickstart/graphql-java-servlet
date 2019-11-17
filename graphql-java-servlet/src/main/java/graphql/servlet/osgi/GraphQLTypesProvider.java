package graphql.servlet.osgi;

import graphql.schema.GraphQLType;
import java.util.Collection;

public interface GraphQLTypesProvider extends GraphQLProvider {

  Collection<GraphQLType> getTypes();
}
