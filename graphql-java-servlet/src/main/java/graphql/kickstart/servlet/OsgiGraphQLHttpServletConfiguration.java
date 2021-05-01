package graphql.kickstart.servlet;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "GraphQL HTTP Servlet",
    description = "GraphQL HTTP Servlet Configuration")
@interface OsgiGraphQLHttpServletConfiguration {

  @AttributeDefinition(name = "alias", description = "Servlet alias")
  String alias() default "/graphql";

  @AttributeDefinition(name = "jmx.objectname", description = "JMX object name")
  String jmx_objectname() default "graphql.servlet:type=graphql";
}
