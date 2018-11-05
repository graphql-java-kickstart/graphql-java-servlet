package graphql.servlet;

public interface GraphQLMBean {
    String[] getQueries();
    String[] getMutations();
    String executeQuery(String query);
}
