package graphql.servlet;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.Part;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphQLHttpContext extends DefaultGraphQLContext {

    private Map<String, List<Part>> parts;

    public GraphQLHttpContext() {
        this(null, null);
    }

    public GraphQLHttpContext(Subject subject, DataLoaderRegistry dataLoaderRegistry) {
        super(subject, dataLoaderRegistry);
    }

    /**
     * @return list of all parts representing files
     */
    public List<Part> getFileParts() {
        return getParts().values()
                .stream()
                .flatMap(Collection::stream)
                .filter(part -> part.getContentType() != null)
                .collect(Collectors.toList());
    }

    /**
     * @return map representing all form fields
     */
    public Map<String, List<Part>> getParts() {
        return parts != null ? parts : new HashMap<>();
    }

    public void setParts(Map<String, List<Part>> parts) {
        this.parts = parts;
    }

}
