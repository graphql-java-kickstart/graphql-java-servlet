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
