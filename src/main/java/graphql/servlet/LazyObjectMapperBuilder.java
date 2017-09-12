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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * @author Andrew Potter
 */
public class LazyObjectMapperBuilder {
    private final ObjectMapperConfigurer configurer;
    private volatile ObjectMapper mapper;

    public LazyObjectMapperBuilder(ObjectMapperConfigurer configurer) {
        this.configurer = configurer;
    }

    // Double-check idiom for lazy initialization of instance fields.
    public ObjectMapper getMapper() {
        ObjectMapper result = mapper;
        if (result == null) { // First check (no locking)
            synchronized(this) {
                result = mapper;
                if (result == null) // Second check (with locking)
                    mapper = result = createObjectMapper();
            }
        }

        return result;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).registerModule(new Jdk8Module());
        configurer.configure(mapper);

        return mapper;
    }
}
