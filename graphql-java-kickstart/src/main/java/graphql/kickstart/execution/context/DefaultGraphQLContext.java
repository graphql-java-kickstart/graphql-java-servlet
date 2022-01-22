package graphql.kickstart.execution.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.dataloader.DataLoaderRegistry;

/**
 * An object for the DefaultGraphQLContextBuilder to return. Can be extended to include more
 * context.
 */
public class DefaultGraphQLContext implements GraphQLKickstartContext {

  private final DataLoaderRegistry dataLoaderRegistry;

  public DefaultGraphQLContext(DataLoaderRegistry dataLoaderRegistry) {
    this.dataLoaderRegistry =
        Objects.requireNonNull(dataLoaderRegistry, "dataLoaderRegistry is required");
  }

  public DefaultGraphQLContext() {
    this(new DataLoaderRegistry());
  }

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistry;
  }

  @Override
  public Map<Object, Object> getMapOfContext() {
    Map<Object, Object> map = new HashMap<>();
    map.put(DataLoaderRegistry.class, dataLoaderRegistry);
    return map;
  }
}
