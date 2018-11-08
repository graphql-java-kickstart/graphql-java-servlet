[![Build Status](https://travis-ci.org/graphql-java-kickstart/graphql-java-servlet.svg?branch=master)](https://travis-ci.org/graphql-java-kickstart/graphql-java-servlet)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet/badge.svg?service=github)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet)
[![Chat on Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/graphql-java-kickstart/Lobby)

# GraphQL Servlet

Implementation of GraphQL Java Servlet including support for Relay.js, Apollo and OSGi out of the box.
This project wraps the Java implementation of GraphQL provided by [GraphQL Java](https://www.graphql-java.com).
The documentation on this site focuses around the usage of the servlet. Although some parts may dive deeper
into the aspects of GraphQL Java as well, make sure to look at the 
[GraphQL Java documentation](https://www.graphql-java.com/documentation/latest/) for more in depth details
regarding GraphQL Java itself. 

We try to stay up to date with GraphQL Java as much as possible. The current version supports **GraphQL Java 10.0**.
 
This project requires at least Java 8.

## Quick start

See [Getting started](https://www.graphql-java-kickstart.com/docs/graphql-java-servlet/getting-started/) for more detailed instructions.

To add `graphql-java-servlet` to your project and get started quickly, do the following.

### Build with Gradle

Make sure `mavenCentral` is amongst your repositories:
```gradle
repositories {
    mavenCentral()
}
```

Add the `graphql-java-servlet` dependency:
```gradle
dependencies {
    compile 'com.graphql-java-kickstart:graphql-java-servlet:6.2.0'
}
```

### Build with Maven

Add the `graphql-java-servlet` dependency:
```xml
<dependency>
  <groupId>com.graphql-java-kickstart</groupId>
  <artifactId>graphql-java-servlet</artifactId>
  <version>6.2.0</version>
</dependency>
```

### Create a Servlet class

Creating the Servlet class requires various parameters to be provided at the moment. We're working on simplifying
this, to make it easier to get started. For now, take a look at [Create a Servlet class]({{< ref "getting-started/#create-a-servlet-class" >}})
to see what's needed to create a Servlet with a schema.

## Using the latest development build

Snapshot versions of the current `master` branch are availble on JFrog. Check the next snapshot version on 
[Github](https://github.com/graphql-java-kickstart/graphql-java-servlet/blob/master/gradle.properties)

### Build with Gradle

Add the Snapshot repository:
```gradle
repositories {
    mavenCentral()
    maven { url "http://oss.jfrog.org/artifactory/oss-snapshot-local" }
}
```

### Build with Maven

Add the Snapshot repository:
```xml
<repositories>
  <repository>
    <id>oss-snapshot-local</id>
    <name>jfrog</name>
    <url>http://oss.jfrog.org/artifactory/oss-snapshot-local</url>
    <snapshots>
      <enabled>true</enabled>
      <updatePolicy>always</updatePolicy>
    </snapshots>
  </repository>
</repositories>
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
