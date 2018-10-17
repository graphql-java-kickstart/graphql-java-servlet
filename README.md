[![Build Status](https://travis-ci.org/graphql-java-kickstart/graphql-java-servlet.svg?branch=master)](https://travis-ci.org/graphql-java-kickstart/graphql-java-servlet)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet/badge.svg?service=github)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet)
[![Chat on Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/graphql-java-kickstart/Lobby)

# GraphQL Servlet

This module implements a GraphQL Java Servlet. It also supports Relay.js, Apollo and OSGi out of the box.

# Downloading

You can download releases from jcenter and maven central:

```groovy
repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compile 'com.graphql-java-kickstart:graphql-java-servlet:6.2.0'
}
```

```xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-java-servlet</artifactId>
    <version>6.2.0</version>
</dependency>
```

```xml
<repositories>
    <repository>
      <id>jcenter</id>
      <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>
```
For Gradle:
```groovy
repositories {
    jcenter()
}
```

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

## Standalone servlet

The simplest form of the servlet takes a graphql-java `GraphQLSchema` and an `ExecutionStrategy`:
```java
GraphQLServlet servlet = new SimpleGraphQLServlet(schema, executionStrategy);

// or

GraphQLServlet servlet = new SimpleGraphQLServlet(schema, executionStrategy, operationListeners, servletListeners);
```

## Servlet Listeners

You can also add [servlet listeners](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLServletListener.java) to an existing servlet.
These listeners provide hooks into query execution (before, success, failure, and finally) and servlet execution (before, success, error, and finally):
```java
servlet.addListener(new GraphQLServletListener() {
    @Override
    GraphQLServletListener.RequestCallback onRequest(HttpServletRequest request, HttpServletResponse response) {

        return new GraphQLServletListener.RequestCallback() {
            @Override
            void onSuccess(HttpServletRequest request, HttpServletResponse response) {

            }

            @Override
            void onError(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {

            }

            @Override
            void onFinally(HttpServletRequest request, HttpServletResponse response) {

            }
        }
    }

    @Override
    GraphQLServletListener.OperationCallback onOperation(GraphQLContext context, String operationName, String query, Map<String, Object> variables) {

        return new GraphQLServletListener.OperationCallback() {
            @Override
            void onSuccess(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data) {

            }

            @Override
            void onError(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data, List<GraphQLError> errors) {

            }

            @Override
            void onFinally(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data) {

            }
        }
    }
})
```

## Relay.js support

Relay.js support is provided by the [EnhancedExecutionStrategy](https://github.com/graphql-java/graphql-java-annotations/blob/master/src/main/java/graphql/annotations/EnhancedExecutionStrategy.java) of [graphql-java-annotations](https://github.com/graphql-java/graphql-java-annotations).
You **MUST** pass this execution strategy to the servlet for Relay.js support.

This is the default execution strategy for the `OsgiGraphQLServlet`, and must be added as a dependency when using that servlet.

## Apollo support

Query batching is supported, no configuration required.

## Spring Framework support

To use the servlet with Spring Framework, either use the [Spring Boot starter](https://github.com/graphql-java/graphql-spring-boot) or simply define a `ServletRegistrationBean` in a web app:
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

## Examples

You can now find some example on how to use graphql-java-servlet.

### OSGi Examples

#### Requirements

The OSGi examples use Maven as a build tool because it requires plugins that are not (yet) available for Gradle.
Therefore you will need Maven 3.2+.

#### Building & running the OSGi examples

You can build the OSGi examples sub-projects by simply executing the following command from the examples/osgi directory:

    mvn clean install
     
This will generate a complete Apache Karaf distribution in the following files:
     
     examples/osgi/apache-karaf-package/target/graphql-java-servlet-osgi-examples-apache-karaf-package-VERSION.tar.gz(.zip)
     
You can simply uncompress this file and launch the OSGi server using the command from the uncompressed directory:

    bin/karaf
    
You should then be able to access the GraphQL endpoint at the following URL once the server is started:

    http://localhost:8181/graphql/schema.json
    
If you see the JSON result of an introspection query, then all is ok. If not, check the data/log/karaf.log file for 
any errors.
    
We also provide a script file to do all of the building and running at once (only for Linux / MacOS ):

    ./buildAndRun.sh

#### Deploying inside Apache Karaf server

You can use the graphql-java-servlet as part of an Apache Karaf feature, as you can see in the example project here:
* [pom.xml](examples/osgi/apache-karaf-feature/pom.xml)

And here is a sample src/main/feature/feature.xml file to add some dependencies on other features:
* [feature.xml](examples/osgi/apache-karaf-feature/src/main/feature/feature.xml)

#### Example GraphQL provider implementation

Here's an example of a GraphQL provider that implements three interfaces at the same time.

* [ExampleGraphQLProvider](examples/osgi/providers/src/main/java/graphql/servlet/examples/osgi/ExampleGraphQLProvider.java)

## Request-scoped DataLoaders

It is possible to use dataloaders in a request scope by customizing [GraphQLContextBuilder](https://github.com/graphql-java/graphql-java-servlet/blob/master/src/main/java/graphql/servlet/GraphQLContextBuilder.java).
And instantiating a new [DataLoaderRegistry](https://github.com/graphql-java/java-dataloader/blob/master/src/main/java/org/dataloader/DataLoaderRegistry.java) for each GraphQLContext.
For eg:
```java
public class CustomGraphQLContextBuilder implements GraphQLContextBuilder {

    private final DataLoader userDataLoader;

    public CustomGraphQLContextBuilder(DataLoader userDataLoader) {
        this.userDataLoader = userDataLoader;
    }

    @Override
    public GraphQLContext build(HttpServletRequest req) {
        GraphQLContext context = new GraphQLContext(req);
        context.setDataLoaderRegistry(buildDataLoaderRegistry());

        return context;
    }

    @Override
    public GraphQLContext build() {
        GraphQLContext context = new GraphQLContext();
        context.setDataLoaderRegistry(buildDataLoaderRegistry());

        return context;
    }

    @Override
    public GraphQLContext build(HandshakeRequest request) {
        GraphQLContext context = new GraphQLContext(request);
        context.setDataLoaderRegistry(buildDataLoaderRegistry());

        return context;
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        dataLoaderRegistry.register("userDataLoader", userDataLoader);
        return dataLoaderRegistry;
    }
}

```
 It is then possible to access the [DataLoader](https://github.com/graphql-java/java-dataloader/blob/master/src/main/java/org/dataloader/DataLoader.java) in the resolvers by accessing the [DataLoaderRegistry] from context. For eg:
 ```java
 public CompletableFuture<String> getEmailAddress(User user, DataFetchingEnvironment dfe) { // User is the graphQL type
         final DataLoader<String, UserDetail> userDataloader =
                dfe.getContext().getDataLoaderRegistry().get().getDataLoader("userDataLoader"); // UserDetail is the data that is loaded

         return userDataloader.load(User.getName())
                 .thenApply(userDetail -> userDetail != null ? userDetail.getEmailAddress() : null);
     }

 ```
