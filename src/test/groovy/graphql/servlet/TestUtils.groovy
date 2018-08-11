package graphql.servlet

import graphql.Scalars
import graphql.schema.*

class TestUtils {

    static def createServlet(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                             DataFetcher mutationDataFetcher = { env -> env.arguments.arg }) {
        return SimpleGraphQLHttpServlet.newBuilder(createGraphQlSchema(queryDataFetcher, mutationDataFetcher)).build()
    }

    static def createGraphQlSchema(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                                   DataFetcher mutationDataFetcher = { env -> env.arguments.arg }) {
        GraphQLObjectType query = GraphQLObjectType.newObject()
                .name("Query")
                .field { GraphQLFieldDefinition.Builder field ->
            field.name("echo")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("arg")
                argument.type(Scalars.GraphQLString)
            }
            field.dataFetcher(queryDataFetcher)
        }
        .field { GraphQLFieldDefinition.Builder field ->
            field.name("returnsNullIncorrectly")
            field.type(new GraphQLNonNull(Scalars.GraphQLString))
            field.dataFetcher({ env -> null })
        }
        .build()

        GraphQLObjectType mutation = GraphQLObjectType.newObject()
                .name("Mutation")
                .field { field ->
            field.name("echo")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("arg")
                argument.type(Scalars.GraphQLString)
            }
            field.dataFetcher(mutationDataFetcher)
        }
        .build()

        return new GraphQLSchema(query, mutation, [query, mutation].toSet())
    }

}
