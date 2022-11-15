package graphql.kickstart.servlet.apollo;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import jakarta.servlet.http.Part;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApolloScalars {

  public static final GraphQLScalarType Upload =
      GraphQLScalarType.newScalar()
          .name("Upload")
          .description("A file part in a multipart request")
          .coercing(
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
                    throw new CoercingParseValueException(
                        "Expected type "
                            + Part.class.getName()
                            + " but was "
                            + input.getClass().getName());
                  }
                }

                @Override
                public Part parseLiteral(Object input) {
                  throw new CoercingParseLiteralException(
                      "Must use variables to specify Upload values");
                }
              })
          .build();
}
