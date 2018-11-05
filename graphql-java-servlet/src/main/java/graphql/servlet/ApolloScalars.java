package graphql.servlet;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import javax.servlet.http.Part;

public class ApolloScalars {
    public static final GraphQLScalarType Upload =
        new GraphQLScalarType("Upload",
                              "A file part in a multipart request",
                              new Coercing<Part, Void>() {
                                  @Override
                                  public Void serialize(Object dataFetcherResult) {
                                      throw new CoercingSerializeException("Upload is an input-only type");
                                  }

                                  @Override
                                  public Part parseValue(Object input) {
                                      if (input instanceof Part) {
                                          return (Part) input;
                                      } else if (null == input) {
                                          return null;
                                      } else {
                                          throw new CoercingParseValueException("Expected type " +
                                                                                Part.class.getName() +
                                                                                " but was " +
                                                                                input.getClass().getName());
                                      }
                                  }

                                  @Override
                                  public Part parseLiteral(Object input) {
                                      throw new CoercingParseLiteralException(
                                          "Must use variables to specify Upload values");
                                  }
                              });
}
