#!/usr/bin/env bash
mvn clean install
pushd apache-karaf-package/target
tar zxvf graphql-java-servlet-osgi-examples-apache-karaf-package-3.0.1.tar.gz
cd graphql-java-servlet-osgi-examples-apache-karaf-package-3.0.1/bin
./karaf debug
popd