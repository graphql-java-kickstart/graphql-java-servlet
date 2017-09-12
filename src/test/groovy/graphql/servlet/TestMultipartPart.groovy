package graphql.servlet

/**
 * @author Andrew Potter
 */
class TestMultipartContentBuilder {

    private StringBuilder content = new StringBuilder()
    private String boundary = "--test"

    TestMultipartContentBuilder nline() {
        content.append('\r\n')

        this
    }

    TestMultipartContentBuilder addPart(String name, String part) {
        content.append(boundary)
        nline()
        content.append("Content-Disposition: form-data; name=\"$name\"")
        nline()
        nline()
        content.append(part)
        nline()

        this
    }

    byte[] build() {
        nline()
        content.append(boundary)
        content.append('--')
        nline()
        content.toString().getBytes()
    }
}
