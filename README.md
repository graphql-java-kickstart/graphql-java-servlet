# GraphQL Java Servlet
[![Maven Central](https://img.shields.io/maven-central/v/com.graphql-java-kickstart/graphql-java-servlet.svg)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java-kickstart/graphql-java-servlet)
[![Build Status](https://github.com/graphql-java-kickstart/graphql-java-servlet/workflows/Publish%20snapshot/badge.svg)](https://github.com/graphql-java-kickstart/graphql-java-servlet/actions?query=workflow%3A%22Publish+snapshot%22)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=graphql-java-kickstart_graphql-java-servlet&metric=alert_status)](https://sonarcloud.io/dashboard?id=graphql-java-kickstart_graphql-java-servlet)
[![GitHub contributors](https://img.shields.io/github/contributors/graphql-java-kickstart/graphql-java-servlet)](https://github.com/graphql-java-kickstart/graphql-java-servlet/graphs/contributors)
[![Discuss on GitHub](https://img.shields.io/badge/GitHub-discuss-orange)](https://github.com/graphql-java-kickstart/graphql-java-servlet/discussions)


## We are looking for contributors!
Are you interested in improving our documentation, working on the codebase, reviewing PRs?

[Reach out to us on Discussions](https://github.com/graphql-java-kickstart/graphql-java-servlet/discussions) and join the team!

We hope you'll get involved! Read our [Contributors' Guide](CONTRIBUTING.md) for more details.

## Overview
Implementation of GraphQL Java Servlet including support for Relay.js, Apollo and OSGi out of the box.
This project wraps the Java implementation of GraphQL provided by [GraphQL Java](https://www.graphql-java.com).
See [GraphQL Java documentation](https://www.graphql-java.com/documentation/latest/) for more in depth details
regarding GraphQL Java itself. 

We try to stay up to date with GraphQL Java as much as possible maintaining the retro-compatibility
with javax and Springframework 5.

On each release we publish three flavours of this project:
 - [latest jakarta](#jakarta-and-springframework-6)
 - [jakarta5](#jakarta5)
 - [javax](#javax-and-springframework-5)
 
All of them also supports legacy projects that can compile with older JDK versions: the minimum JDK
version supported is the `11`.

## Jakarta and Springframework 6.*
This is the main flavour using the latest version of `Jakarta` (currently the `6.*`) and the latest
version of `Springframework` (currently the `6.*`). All the codebase can be found in the branch: 
`master`

```xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-java-servlet</artifactId>
    <version>${graphql-java-servlet.version}</version>
</dependency>
```

## Jakarta5
This flavour use the `jakarta` version `5.*` and it is meant to be used for all the projects that 
are already migrated to jakarta, but they are waiting that `jakarta6` will become more broadly used.
All the codebase can be found in the branch: `jakarta5`

```xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-java-servlet-jakarta5</artifactId>
    <version>${graphql-java-servlet-jakarta5.version}</version>
</dependency>
```

## Javax and Springframework 5.*
This is the legacy flavour using the `javax` dependency and the version `5.*` of `Springframework` 
(since it is still broadly used by a lot of projects). All the codebase can be found in the branch: 
`master` 

```xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-java-servlet-javax</artifactId>
    <version>${graphql-java-servlet.version}</version>
</dependency>
```


## Installation and getting started

See [Getting started](https://www.graphql-java-kickstart.com/servlet/getting-started/) for more
detailed instructions.
