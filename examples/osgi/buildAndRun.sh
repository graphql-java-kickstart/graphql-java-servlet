#!/usr/bin/env bash
mvn clean install
pushd apache-karaf-package/target || exit 1
tar zxvf graphql-java-servlet-osgi-examples-apache-karaf-package-10.1.0.tar.gz
cd graphql-java-servlet-osgi-examples-apache-karaf-package-10.1.0/bin || exit 1
./karaf debug
popd || exit 1
