[![Build Status](https://travis-ci.org/graphql-java/graphql-java-servlet.svg?branch=master)](https://travis-ci.org/graphql-java/graphql-java-servlet)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-servlet/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-servlet)

# GraphQL Servlet

This module implements a GraphQL Java Servlet. It also supports Relay.js and OSGi out of the box.

# Downloading

You can download it from maven central:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'com.graphql-java:graphql-java-servlet:0.10.0'
}
```

```xml
<dependency>
  <groupId>com.graphql-java</groupId>
  <artifactId>graphql-java-servlet</artifactId>
  <version>0.10.0</version>
</dependency>
```

# Usage

The are a few important components this package provides:

* GraphQLQueryProvider/GraphQLMutationProvider interfaces. These will allow you
  to define which "domain model" views and which mutations you are going to expose.
* GraphQLServlet as an entry point servlet. Use `bindQueryProvider`/`bindMutationProvider` or automatically wire
them in OSGi.

Both GET and POST are supported. In POST, plain request body with a JSON is supported, as well as a multipart with a part
called 'graphql'.
