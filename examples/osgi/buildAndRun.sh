#!/usr/bin/env bash
mvn clean install
pushd apache-karaf-package/target
tar zxvf graphql-java-servlet-osgi-examples-apache-karaf-package-7.3.4-SNAPSHOT.tar.gz
cd graphql-java-servlet-osgi-examples-apache-karaf-package-7.3.4-SNAPSHOT/bin
./karaf debug
popd
