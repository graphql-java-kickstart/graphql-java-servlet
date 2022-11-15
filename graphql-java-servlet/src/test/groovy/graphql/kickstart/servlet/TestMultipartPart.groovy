package graphql.kickstart.servlet

import jakarta.servlet.http.Part

/**
 * @author Andrew Potter
 */
class TestMultipartContentBuilder {

  static Part createPart(String name, String part) {
    return new MockPart(name, part)
  }

  static class MockPart implements Part {
    final String name
    final String content

    MockPart(String name, String content) {
      this.name = name
      this.content = content
    }

    @Override
    InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(content.getBytes())
    }

    @Override
    String getContentType() {
      return null
    }

    @Override
    String getName() {
      return name
    }

    @Override
    String getSubmittedFileName() {
      return name
    }

    @Override
    long getSize() {
      return content.getBytes().length
    }

    @Override
    void write(String fileName) throws IOException {
      throw new IllegalArgumentException("Not supported")
    }

    @Override
    void delete() throws IOException {
      throw new IllegalArgumentException("Not supported")
    }

    @Override
    String getHeader(String name) {
      return null
    }

    @Override
    Collection<String> getHeaders(String name) {
      return Collections.emptyList()
    }

    @Override
    Collection<String> getHeaderNames() {
      return Collections.emptyList()
    }
  }

}
