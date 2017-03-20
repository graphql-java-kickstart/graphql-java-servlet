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
package graphql.servlet

import javax.servlet.http.Part

/**
 * @author Andrew Potter
 */
class TestMultipartPart implements Part {

    String name
    String content

    @Override
    InputStream getInputStream() throws IOException {
        new ByteArrayInputStream(content.getBytes())
    }

    @Override
    String getContentType() {
        return null
    }

    @Override
    long getSize() {
        content.getBytes().length
    }

    @Override
    void write(String fileName) throws IOException {

    }

    @Override
    void delete() throws IOException {

    }

    @Override
    String getHeader(String name) {
        return null
    }

    @Override
    Collection<String> getHeaders(String name) {
        return null
    }

    @Override
    Collection<String> getHeaderNames() {
        return null
    }
}
