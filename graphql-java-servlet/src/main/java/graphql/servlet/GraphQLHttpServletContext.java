package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

class GraphQLHttpServletContext extends GraphQLHttpContext {

    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;

    GraphQLHttpServletContext(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }

    public Optional<HttpServletRequest> getHttpServletRequest() {
        return Optional.ofNullable(httpServletRequest);
    }

    public Optional<HttpServletResponse> getHttpServletResponse() {
        return Optional.ofNullable(httpServletResponse);
    }

}
