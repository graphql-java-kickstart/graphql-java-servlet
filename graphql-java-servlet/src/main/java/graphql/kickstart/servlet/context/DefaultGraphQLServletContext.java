package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.SneakyThrows;
import org.dataloader.DataLoaderRegistry;

/** @deprecated Use {@link graphql.kickstart.execution.context.GraphQLKickstartContext} instead */
public class DefaultGraphQLServletContext extends DefaultGraphQLContext
    implements GraphQLServletContext {

  protected DefaultGraphQLServletContext(
      DataLoaderRegistry dataLoaderRegistry,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) {
    super(dataLoaderRegistry);
    put(HttpServletRequest.class, httpServletRequest);
    put(HttpServletResponse.class, httpServletResponse);
  }

  public static Builder createServletContext(DataLoaderRegistry registry) {
    return new Builder(registry);
  }

  public static Builder createServletContext() {
    return new Builder(new DataLoaderRegistry());
  }

  /**
   * @deprecated Use {@code
   *     dataFetchingEnvironment.getGraphQlContext().get(HttpServletRequest.class)} instead. Since
   *     13.0.0
   */
  @Override
  @Deprecated
  public HttpServletRequest getHttpServletRequest() {
    return (HttpServletRequest) getMapOfContext().get(HttpServletRequest.class);
  }

  /**
   * @deprecated Use {@code
   *     dataFetchingEnvironment.getGraphQlContext().get(HttpServletResponse.class)} instead. Since
   *     13.0.0
   */
  @Override
  @Deprecated
  public HttpServletResponse getHttpServletResponse() {
    return (HttpServletResponse) getMapOfContext().get(HttpServletResponse.class);
  }

  /**
   * @deprecated Use {@code
   *     dataFetchingEnvironment.getGraphQlContext().get(HttpServletRequest.class)} instead to get
   *     the request and retrieve the file parts yourself. Since 13.0.0
   */
  @Override
  @Deprecated
  @SneakyThrows
  public List<Part> getFileParts() {
    return getHttpServletRequest().getParts().stream()
        .filter(part -> part.getContentType() != null)
        .collect(Collectors.toList());
  }

  /**
   * @deprecated Use {@code
   *     dataFetchingEnvironment.getGraphQlContext().get(HttpServletRequest.class)} instead to get
   *     the request and retrieve the parts yourself. Since 13.0.0
   */
  @Override
  @Deprecated
  @SneakyThrows
  public Map<String, List<Part>> getParts() {
    return getHttpServletRequest().getParts().stream()
        .collect(Collectors.groupingBy(Part::getName));
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
