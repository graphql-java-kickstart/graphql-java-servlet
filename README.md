[![Build Status](https://travis-ci.org/graphql-java/graphql-java-servlet.svg?branch=master)](https://travis-ci.org/graphql-java/graphql-java-servlet)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-servlet/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-servlet)

# GraphQL Servlet

This module implements a GraphQL Java Servlet. It also supports Relay.js and OSGi out of the box.

# Downloading

You can download releases from maven central:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'com.graphql-java:graphql-java-servlet:2.0.0'
}
```

```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-servlet</artifactId>
    <version>2.0.0</version>
</dependency>
```

# Usage

The servlet supports both GET and POST.
In POST, plain request body containing JSON is supported, as well as a multipart upload with a part called 'graphql'.

## Standalone servlet

The simplest form of the servlet takes a graphql-java `GraphQLSchema` and an `ExecutionStrategy`:
```java
GraphQLServlet servlet = new SimpleGraphQLServlet(schema, executionStrategy);

// or

GraphQLServlet servlet = new SimpleGraphQLServlet(schema, executionStrategy, operationListeners);
```

You can also add [operation listeners](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLOperationListener.java) and [servlet listeners](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLServletListener.java) to an existing servlet.
These listeners provide hooks into query execution - before, on success, and on failure:
```java
servlet.addOperationListener(new GraphQLOperationListener() {
    @Override
    void beforeGraphQLOperation(GraphQLContext context, String operationName, String query, Map<String, Object> variables) {

    }

    @Override
    void onSuccessfulGraphQLOperation(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data) {

    }

    @Override
    void onFailedGraphQLOperation(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data, List<GraphQLError> errors) {

    }
})

servlet.addServletListener(new GraphQLServletListener() {
    @Override
    void onStart(HttpServletRequest request, HttpServletResponse response) {

    }

    @Override
    void onError(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {

    }

    @Override
    void onFinally(HttpServletRequest request, HttpServletResponse response) {

    }
})
```

## Relay.js support

Relay.js support is provided by the [EnhancedExecutionStrategy](https://github.com/graphql-java/graphql-java-annotations/blob/master/src/main/java/graphql/annotations/EnhancedExecutionStrategy.java) of [graphql-java-annotations](https://github.com/graphql-java/graphql-java-annotations).
You **MUST** pass this execution strategy to the servlet for Relay.js support.

This is the default execution strategy for the `OsgiGraphQLServlet`, and must be added as a dependency when using that servlet.

## Spring Framework support

To use the servlet with Spring Framework, simply define a `ServletRegistrationBean` bean in a web app:
```java
@Bean
ServletRegistrationBean graphQLServletRegistrationBean(GraphQLSchema schema, ExecutionStrategy executionStrategy, List<GraphQLOperationListener> operationListeners) {
    return new ServletRegistrationBean(new SimpleGraphQLServlet(schema, executionStrategy, operationListeners), "/graphql");
}
```

## OSGI support

The [OsgiGraphQLServlet](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/OsgiGraphQLServlet.java) uses a "provider" model to supply the servlet with the required objects:
* [GraphQLQueryProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLQueryProvider.java): Provides query objects to the GraphQL schema.
* [GraphQLMutationProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLMutationProvider.java): Provides mutation objects to the GraphQL schema.
* [GraphQLTypesProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLTypesProvider.java): Provides type information to the GraphQL schema.
* [ExecutionStrategyProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/ExecutionStrategyProvider.java): Provides an execution strategy for running each query.
* [GraphQLContextBuilder](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLContextBuilder.java): Builds a context for running each query.
