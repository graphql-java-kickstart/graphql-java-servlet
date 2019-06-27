package graphql.servlet.config;

import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.config.GraphQLProvider;

import java.util.Collection;

/**
 * This interface is used by OSGi bundles to plugin new field into the root query type
 */
public interface GraphQLQueryProvider extends GraphQLProvider {

    /**
     * @return a collection of field definitions that will be added to the root query type.
     */
    Collection<GraphQLFieldDefinition> getQueries();

}
