[![Build Status](https://travis-ci.org/graphql-java/graphql-java-servlet.svg?branch=master)](https://travis-ci.org/graphql-java/graphql-java-servlet)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-servlet/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-servlet)
[![Chat on Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/graphql-java/graphql-java)

# GraphQL Servlet

This module implements a GraphQL Java Servlet. It also supports Relay.js and OSGi out of the box.

# Downloading

You can download releases from maven central:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'com.graphql-java:graphql-java-servlet:2.1.0'
}
```

```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-servlet</artifactId>
    <version>2.1.0</version>
</dependency>
```

# Usage

The servlet supports the following request formats:
* GET request with query parameters:
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

## Standalone servlet

The simplest form of the servlet takes a graphql-java `GraphQLSchema` and an `ExecutionStrategy`:
```java
GraphQLServlet servlet = new SimpleGraphQLServlet(schema, executionStrategy);

// or

GraphQLServlet servlet = new SimpleGraphQLServlet(schema, executionStrategy, operationListeners, servletListeners);
```

You can also add [operation listeners](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLOperationListener.java) and [servlet listeners](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLServletListener.java) to an existing servlet.
These listeners provide hooks into query execution (before, on success, and on failure) and servlet execution (before, on error, and finally):
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
* [GraphQLQueryProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLQueryProvider.java): Provides query fields to the GraphQL schema.
* [GraphQLMutationProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLMutationProvider.java): Provides mutation fields to the GraphQL schema.
* [GraphQLTypesProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLTypesProvider.java): Provides type information to the GraphQL schema.
* [ExecutionStrategyProvider](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/ExecutionStrategyProvider.java): Provides an execution strategy for running each query.
* [GraphQLContextBuilder](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLContextBuilder.java): Builds a context for running each query.

### Deploying using an Apache Karaf feature

You can use the graphql-java-servlet as part of an Apache Karaf feature, which makes it easy to setup all the proper 
dependencies. Here's an example pom.xml file to setup the Karaf feature:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <packaging>feature</packaging>

    <artifactId>graphql-java-servlet-features</artifactId>

    <dependencies>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>2.8.4</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.8.4</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.8.4</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jdk8</artifactId>
            <version>2.8.4</version>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>20.0</version>
        </dependency>
        <dependency>
            <groupId>commons-fileupload</groupId>
            <artifactId>commons-fileupload</artifactId>
            <version>1.3.1</version>
        </dependency>
        <dependency>
            <groupId>org.antlr</groupId>
            <artifactId>antlr4-runtime</artifactId>
            <version>4.5.1</version>
        </dependency>

        <dependency>
            <groupId>com.graphql-java</groupId>
            <artifactId>graphql-java-servlet</artifactId>
            <version>${graphql.java.servlet.version}</version>
        </dependency>
        <dependency>
            <groupId>com.graphql-java</groupId>
            <artifactId>graphql-java</artifactId>
            <version>${graphql.java.version}</version>
        </dependency>
        <dependency>
            <groupId>com.graphql-java</groupId>
            <artifactId>graphql-java-annotations</artifactId>
            <version>0.13.1</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.karaf.tooling</groupId>
                <artifactId>karaf-maven-plugin</artifactId>
                <version>4.0.8</version>
                <extensions>true</extensions>
                <configuration>
                    <startLevel>80</startLevel>
                    <addTransitiveFeatures>true</addTransitiveFeatures>
                    <includeTransitiveDependency>true</includeTransitiveDependency>
                </configuration>
            </plugin>
        </plugins>

    </build>

</project>
```

And here is a sample src/main/feature/feature.xml file to add some dependencies on other features:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<features xmlns="http://karaf.apache.org/xmlns/features/v1.4.0" name="cxs-graphql-api-features">
    <feature name="graphql-java-servlet" description="GraphQL Java Servlet Feature" version="1.0.0.SNAPSHOT">
        <feature prerequisite="true" dependency="false">scr</feature>
        <feature prerequisite="true" dependency="false">war</feature>
    </feature>
</features>
```

### Example GraphQL provider implementation

Here's an example of a GraphQL provider that implements three interfaces at the same time.

```java
package org.oasis_open.contextserver.graphql;

import graphql.schema.*;
import org.osgi.service.component.annotations.Component;

import graphql.servlet.GraphQLQueryProvider;
import graphql.servlet.GraphQLTypesProvider;

import java.util.*;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

@Component(
        name="ExampleGraphQLProvider"
)
public class ExampleGraphQLProvider implements GraphQLQueryProvider, GraphQLMutationProvider, GraphQLTypesProvider {

    public Collection<GraphQLFieldDefinition> getQueries() {
        List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<GraphQLFieldDefinition>();
        fieldDefinitions.add(newFieldDefinition()
                .type(GraphQLString)
                .name("hello")
                .staticValue("world")
                .build());
        return fieldDefinitions;
    }

    public Collection<GraphQLFieldDefinition> getMutations() {
        return null;
    }

    public Collection<GraphQLType> getTypes() {

        List<GraphQLType> types = new ArrayList<GraphQLType>();
        return types;
    }

}
```