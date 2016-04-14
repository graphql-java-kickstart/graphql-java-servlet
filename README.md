[![Download](https://api.bintray.com/packages/yrashk/maven/graphql-java-servlet/images/download.svg)](https://bintray.com/yrashk/maven/graphql-java-servlet/_latestVersion)

# GraphQL Servlet

This module implements a Relay.js-compatible GraphQL server. It also supports OSGi out of the box.

# Downloading

You can download it from bintray (Gradle syntax):

```groovy
repositories {
  jcenter()
}

dependencies {
  compile 'graphql-java-servlet:graphql-java-servlet:0.2.0'
}
```

# Usage

The are a few important components this package provides:

* GraphQLQueryProvider/GrapgQLMutationProvider interfaces. These will allow you
  to define which "domain model" views and which mutations you are going to expose.
* GraphQLServlet as an entry point servlet. Use `bindQueryProvider`/`bindMutationProvider` or automatically wire
them in OSGi.
