package graphql.servlet.examples.osgi;

import graphql.kickstart.servlet.OsgiGraphQLHttpServlet;
import org.osgi.service.component.annotations.Component;

@Component(
    property = { "alias=/graphql", "servlet-name=Example"}
)
public class ExampleServlet extends OsgiGraphQLHttpServlet {

}
