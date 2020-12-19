package graphql.kickstart.servlet.core;

public interface GraphQLMBean {

  String[] getQueries();

  String[] getMutations();

  String executeQuery(String query);
}
