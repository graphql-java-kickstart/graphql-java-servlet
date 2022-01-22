package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import lombok.SneakyThrows;
import org.dataloader.DataLoaderRegistry;

public class DefaultGraphQLServletContext extends DefaultGraphQLContext
    implements GraphQLServletContext {

  private final HttpServletRequest httpServletRequest;
  private final HttpServletResponse httpServletResponse;

  protected DefaultGraphQLServletContext(
      DataLoaderRegistry dataLoaderRegistry,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) {
    super(dataLoaderRegistry);
    this.httpServletRequest = httpServletRequest;
    this.httpServletResponse = httpServletResponse;
  }

  public static Builder createServletContext(DataLoaderRegistry registry) {
    return new Builder(registry);
  }

  public static Builder createServletContext() {
    return new Builder(new DataLoaderRegistry());
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
  @SneakyThrows
  public List<Part> getFileParts() {
    return httpServletRequest.getParts().stream()
        .filter(part -> part.getContentType() != null)
        .collect(Collectors.toList());
  }

  @Override
  @SneakyThrows
  public Map<String, List<Part>> getParts() {
    return httpServletRequest.getParts().stream().collect(Collectors.groupingBy(Part::getName));
  }

  @Override
  public Map<Object, Object> getMapOfContext() {
    Map<Object, Object> map = new HashMap<>();
    map.put(DataLoaderRegistry.class, getDataLoaderRegistry());
    map.put(HttpServletRequest.class, httpServletRequest);
    map.put(HttpServletResponse.class, httpServletResponse);
    return map;
  }

  public static class Builder {

    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private DataLoaderRegistry dataLoaderRegistry;

    private Builder(DataLoaderRegistry dataLoaderRegistry) {
      this.dataLoaderRegistry = dataLoaderRegistry;
    }

    public DefaultGraphQLServletContext build() {
      return new DefaultGraphQLServletContext(
          dataLoaderRegistry, httpServletRequest, httpServletResponse);
    }

    public Builder with(HttpServletRequest httpServletRequest) {
      this.httpServletRequest = httpServletRequest;
      return this;
    }

    public Builder with(DataLoaderRegistry dataLoaderRegistry) {
      this.dataLoaderRegistry = dataLoaderRegistry;
      return this;
    }

    public Builder with(HttpServletResponse httpServletResponse) {
      this.httpServletResponse = httpServletResponse;
      return this;
    }
  }
}
