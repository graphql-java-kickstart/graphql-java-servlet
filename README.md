[![Build Status](https://travis-ci.org/graphql-java-kickstart/graphql-java-servlet.svg?branch=master)](https://travis-ci.org/graphql-java-kickstart/graphql-java-servlet)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet/badge.svg?service=github)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet)
[![Chat on Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/graphql-java-kickstart/Lobby)

# GraphQL Servlet

Implementation of GraphQL Java Servlet including support for Relay.js, Apollo and OSGi out of the box.
This project wraps the Java implementation of GraphQL provided by [GraphQL Java](https://www.graphql-java.com).
See [GraphQL Java documentation](https://www.graphql-java.com/documentation/latest/) for more in depth details
regarding GraphQL Java itself. 

We try to stay up to date with GraphQL Java as much as possible. The current version supports **GraphQL Java 14.0**.
 
This project requires at least Java 8.

## Installation and getting started

See [Getting started](https://www.graphql-java-kickstart.com/servlet/getting-started/) for more detailed instructions.

# Usage

The servlet supports the following request formats:
* GET request to `../schema.json`: Get the result of an introspection query.
* GET request with query parameters (query only, no mutation):
    * query
    * operationName (optional)
    * variables (optional)
* POST body JSON object with fields:
    * query
    * operationName (optional)
    * variables (optional)
* POST multipart part named "graphql" containing JSON object with fields:
    * query
    * operationName (optional)
    * variables (optional)
* POST multipart parts named "query", "operationName" (optional), and "variables" (optional)
* POST with Content Type "application/graphql" will treat the HTTP POST body contents as the GraphQL query string

## Relay.js support

Relay.js support is provided by the [EnhancedExecutionStrategy](https://github.com/graphql-java/graphql-java-annotations/blob/master/src/main/java/graphql/annotations/EnhancedExecutionStrategy.java) of [graphql-java-annotations](https://github.com/graphql-java/graphql-java-annotations).
You **MUST** pass this execution strategy to the servlet for Relay.js support.

This is the default execution strategy for the `OsgiGraphQLHttpServlet`, and must be added as a dependency when using that servlet.

## Spring Framework support

To use the servlet with Spring Framework, either use the [Spring Boot starter](https://github.com/graphql-java/graphql-spring-boot) or simply define a `ServletRegistrationBean` in a web app:
```java
@Bean
ServletRegistrationBean graphQLServletRegistrationBean(GraphQLSchema schema, ExecutionStrategy executionStrategy, List<GraphQLOperationListener> operationListeners) {
    return new ServletRegistrationBean(new SimpleGraphQLServlet(schema, executionStrategy, operationListeners), "/graphql");
}
```
