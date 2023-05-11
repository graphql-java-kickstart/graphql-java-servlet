#!/bin/bash

# Set jdk11 as source and target
sed -i 's/SOURCE_COMPATIBILITY=.*/SOURCE_COMPATIBILITY=11/' gradle.properties
sed -i 's/TARGET_COMPATIBILITY=.*/TARGET_COMPATIBILITY=11/' gradle.properties

# Replace jakarta imports and dependencies with javax
grep -rl 'import jakarta' ./graphql-java-servlet | xargs sed -i 's/import jakarta/import javax/g'
sed -i 's/.*jakarta.websocket:jakarta.websocket-client-api.*//' graphql-java-servlet/build.gradle
sed -i \
  's/jakarta.servlet:jakarta.servlet-api.*/javax.servlet:javax.servlet-api:$LIB_JAVAX_SERVLET"/' \
  graphql-java-servlet/build.gradle
sed -i \
  's/jakarta.websocket.*/javax.websocket:javax.websocket-api:$LIB_JAVAX_WEBSOCKET"/' \
  graphql-java-servlet/build.gradle

# Final check if there are something else to replace
grep -rl 'jakarta' ./graphql-java-servlet | xargs sed -i 's/jakarta/javax/g'

# Set the version 5 for spring framework
sed -i \
  's/org.springframework:spring-test.*/org.springframework:spring-test:$LIB_SPRINGFRAMEWORK_5"/' \
  graphql-java-servlet/build.gradle
sed -i \
  's/org.springframework:spring-web.*/org.springframework:spring-web:$LIB_SPRINGFRAMEWORK_5"/' \
  graphql-java-servlet/build.gradle

echo "Replaced jakarta occurrences with javax"