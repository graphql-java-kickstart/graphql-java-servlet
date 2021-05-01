package graphql.kickstart.execution;

public class StaticGraphQLRootObjectBuilder implements GraphQLRootObjectBuilder {

  private final Object rootObject;

  public StaticGraphQLRootObjectBuilder(Object rootObject) {
    this.rootObject = rootObject;
  }

  @Override
  public Object build() {
    return rootObject;
  }

  protected Object getRootObject() {
    return rootObject;
  }
}
