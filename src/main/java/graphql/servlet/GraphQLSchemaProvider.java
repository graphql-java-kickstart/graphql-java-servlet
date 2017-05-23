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

import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;

public interface GraphQLSchemaProvider {

    static GraphQLSchema copyReadOnly(GraphQLSchema schema) {
        return GraphQLSchema.newSchema().query(schema.getQueryType()).build(schema.getAdditionalTypes());
    }

    /**
     * @param request the http request
     * @return a schema based on the request (auth, etc).  Optional is empty when called from an mbean.
     */
    GraphQLSchema getSchema(HttpServletRequest request);


    /**
     * @return a schema for handling mbean calls.
     */
    GraphQLSchema getSchema();

    /**
     * @param request the http request
     * @return a read-only schema based on the request (auth, etc).  Should return the same schema as {@link #getSchema(HttpServletRequest)} for a given request.
     */
    GraphQLSchema getReadOnlySchema(HttpServletRequest request);
}
