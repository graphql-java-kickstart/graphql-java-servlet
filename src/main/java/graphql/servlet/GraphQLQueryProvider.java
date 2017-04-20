/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package graphql.servlet;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.Collection;

/**
 * This interface is used by OSGi bundles to plugin new field into the root query type
 */
public interface GraphQLQueryProvider {

    /**
     * @return a collection of field definitions that will be added to the root query type.
     */
    Collection<GraphQLFieldDefinition> getQueryFieldDefinitions();

    /**
     * @deprecated use query field definitions instead
     * @return a GraphQL object type to add to the root query type
     */
    default GraphQLObjectType getQuery() { return null; }

    /**
     * @deprecated use query field definitions instead
     * @return an object that will be used as a staticValue for the root query type field
     */
    default Object context() { return null; }

    /**
     * @deprecated use query field definitions instead
     * @return the name to use for the field for this query provider in the root query type
     */
    default String getName() {
        return getQuery().getName();
    }
}
