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
  private final Map<Object, Object> map;

  public DefaultGraphQLContext(DataLoaderRegistry dataLoaderRegistry, Map<Object, Object> map) {
    this.dataLoaderRegistry =
        Objects.requireNonNull(dataLoaderRegistry, "dataLoaderRegistry is required");
    this.map = Objects.requireNonNull(map, "map is required");
  }

  public DefaultGraphQLContext(Map<Object, Object> map) {
    this(new DataLoaderRegistry(), map);
  }

  public DefaultGraphQLContext(DataLoaderRegistry dataLoaderRegistry) {
    this(dataLoaderRegistry, new HashMap<>());
  }

  public DefaultGraphQLContext() {
    this(new DataLoaderRegistry());
  }

  public void put(Object key, Object value) {
    map.put(key, value);
  }

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistry;
  }

  @Override
  public Map<Object, Object> getMapOfContext() {
    return map;
  }
}
