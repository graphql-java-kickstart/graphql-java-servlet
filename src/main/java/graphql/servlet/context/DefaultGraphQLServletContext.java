package graphql.servlet.context;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DefaultGraphQLServletContext extends DefaultGraphQLContext implements GraphQLServletContext {

    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final Map<String, List<Part>> parts;

    private DefaultGraphQLServletContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, HttpServletRequest httpServletRequest,
                                         HttpServletResponse httpServletResponse,
                                         Map<String, List<Part>> parts) {
        super(dataLoaderRegistry, subject);
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.parts = parts;
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Override
    public List<Part> getFileParts() {
        return parts.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(part -> part.getContentType() != null)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Part>> getParts() {
        return parts;
    }

    public static Builder createWebContext(DataLoaderRegistry registry, Subject subject) {
        return new Builder(registry, subject);
    }

    public static Builder createWebContext() {
        return new Builder(null, null);
    }

    public static class Builder {
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;
        private Map<String, List<Part>> parts = new HashMap<>();
        private DataLoaderRegistry dataLoaderRegistry;
        private Subject subject;

        private Builder(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
            this.dataLoaderRegistry = dataLoaderRegistry;
            this.subject = subject;
        }

        public DefaultGraphQLServletContext build() {
            return new DefaultGraphQLServletContext(dataLoaderRegistry, subject, httpServletRequest, httpServletResponse, parts);
        }

        public Builder with(HttpServletRequest httpServletRequest) {
            this.httpServletRequest = httpServletRequest;
            return this;
        }

        public Builder with(HttpServletResponse httpServletResponse) {
            this.httpServletResponse = httpServletResponse;
            return this;
        }

        public Builder with(Map<String, List<Part>> parts) {
            if (parts != null) {
                this.parts.putAll(parts);
            }
            return this;
        }
    }
}
