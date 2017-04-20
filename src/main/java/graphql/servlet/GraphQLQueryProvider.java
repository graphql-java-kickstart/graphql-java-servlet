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

import java.util.Collection;

/**
 * This interface is used by OSGi bundles to plugin new field into the root query type
 */
public interface GraphQLQueryProvider {

    /**
     * @return a collection of field definitions that will be added to the root query type.
     */
    Collection<GraphQLFieldDefinition> getQueries();

}
