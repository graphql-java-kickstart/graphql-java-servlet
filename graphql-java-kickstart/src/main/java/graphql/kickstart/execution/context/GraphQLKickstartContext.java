package graphql.kickstart.execution.context;

import java.util.Map;
import lombok.NonNull;
import org.dataloader.DataLoaderRegistry;

/** Represents the context required by the servlet to execute a GraphQL request. */
public interface GraphQLKickstartContext {

  static GraphQLKickstartContext of(Map<Object, Object> map) {
    return new DefaultGraphQLContext(map);
  }

  static GraphQLKickstartContext of(DataLoaderRegistry dataLoaderRegistry) {
    return new DefaultGraphQLContext(dataLoaderRegistry);
  }

  static GraphQLKickstartContext of(
      DataLoaderRegistry dataLoaderRegistry, Map<Object, Object> map) {
    return new DefaultGraphQLContext(dataLoaderRegistry, map);
  }

  /** @return the Dataloader registry to use for the execution. Must not return <code>null</code> */
  @NonNull
  DataLoaderRegistry getDataLoaderRegistry();

  Map<Object, Object> getMapOfContext();
}
