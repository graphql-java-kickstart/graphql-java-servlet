package graphql.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultGraphQLServletContext extends DefaultGraphQLContext implements GraphQLServletContext {

    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    private DefaultGraphQLServletContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, HttpServletRequest httpServletRequest,
                                         HttpServletResponse httpServletResponse) {
        super(dataLoaderRegistry, subject);
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
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
        try {
            return httpServletRequest.getParts().stream()
                    .filter(part -> part.getContentType() != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<Part>> getParts() {
        try {
            return httpServletRequest.getParts()
                .stream()
                .collect(Collectors.groupingBy(Part::getName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Builder createServletContext(DataLoaderRegistry registry, Subject subject) {
        return new Builder(registry, subject);
    }

    public static Builder createServletContext() {
        return new Builder(new DataLoaderRegistry(), null);
    }

    public static class Builder {
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;
        private DataLoaderRegistry dataLoaderRegistry;
        private Subject subject;

        private Builder(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
            this.dataLoaderRegistry = dataLoaderRegistry;
            this.subject = subject;
        }

        public DefaultGraphQLServletContext build() {
            return new DefaultGraphQLServletContext(dataLoaderRegistry, subject, httpServletRequest, httpServletResponse);
        }

        public Builder with(HttpServletRequest httpServletRequest) {
            this.httpServletRequest = httpServletRequest;
            return this;
        }

        public Builder with(DataLoaderRegistry dataLoaderRegistry) {
            this.dataLoaderRegistry = dataLoaderRegistry;
            return this;
        }

        public Builder with(Subject subject) {
            this.subject = subject;
            return this;
        }

        public Builder with(HttpServletResponse httpServletResponse) {
            this.httpServletResponse = httpServletResponse;
            return this;
        }
    }
}
