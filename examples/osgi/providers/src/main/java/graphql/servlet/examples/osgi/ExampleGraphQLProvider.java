package graphql.servlet.examples.osgi;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import graphql.kickstart.servlet.osgi.GraphQLMutationProvider;
import graphql.kickstart.servlet.osgi.GraphQLQueryProvider;
import graphql.kickstart.servlet.osgi.GraphQLTypesProvider;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.*;

@Component(
    name = "ExampleGraphQLProvider",
    immediate = true
)
public class ExampleGraphQLProvider implements GraphQLQueryProvider, GraphQLMutationProvider,
    GraphQLTypesProvider {

  public Collection<GraphQLFieldDefinition> getQueries() {
    List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<GraphQLFieldDefinition>();
    fieldDefinitions.add(newFieldDefinition()
        .type(GraphQLString)
        .name("hello")
        .description(
            "Basic example of a GraphQL Java Servlet provider using the Apache Karaf OSGi Runtime")
        .staticValue("world")
        .build());
    return fieldDefinitions;
  }

  public Collection<GraphQLFieldDefinition> getMutations() {
    return new ArrayList<GraphQLFieldDefinition>();
  }

  public Collection<GraphQLType> getTypes() {
    return new ArrayList<GraphQLType>();
  }
}
