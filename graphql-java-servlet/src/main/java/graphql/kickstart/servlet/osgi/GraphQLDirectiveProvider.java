package graphql.kickstart.servlet.osgi;

import graphql.schema.GraphQLDirective;
import java.util.Collection;


public interface GraphQLDirectiveProvider extends GraphQLProvider {

    /** @return A collection of directive definitions that will be added to the schema. */
    Collection<GraphQLDirective> getDirectives();

}
