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

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Potter
 */
public class DefaultGraphQLErrorHandler implements GraphQLErrorHandler {

    public static final Logger log = LoggerFactory.getLogger(DefaultGraphQLErrorHandler.class);

    @Override
    public List<GraphQLError> processErrors(List<GraphQLError> errors) {
        final List<GraphQLError> clientErrors = filterGraphQLErrors(errors);
        if (clientErrors.size() < errors.size()) {

            // Some errors were filtered out to hide implementation - put a generic error in place.
            clientErrors.add(new GenericGraphQLError("Internal Server Error(s) while executing query"));

            errors.stream()
                .filter(error -> !isClientError(error))
                .forEach(error -> log.error("Error executing query ({}): {}", error.getClass().getSimpleName(), error.getMessage()));
        }

        return clientErrors;
    }

    protected List<GraphQLError> filterGraphQLErrors(List<GraphQLError> errors) {
        return errors.stream()
            .filter(this::isClientError)
            .collect(Collectors.toList());
    }

    protected boolean isClientError(GraphQLError error) {
        return !(error instanceof ExceptionWhileDataFetching);
    }
}
