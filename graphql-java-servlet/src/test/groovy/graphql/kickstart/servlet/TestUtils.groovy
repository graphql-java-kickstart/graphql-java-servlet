package graphql.kickstart.servlet

import com.google.common.io.ByteStreams
import graphql.Scalars
import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.kickstart.execution.context.ContextSetting
import graphql.kickstart.servlet.apollo.ApolloScalars
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder
import graphql.kickstart.servlet.core.GraphQLServletListener
import graphql.kickstart.servlet.input.BatchInputPreProcessor
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring
import graphql.schema.idl.errors.SchemaProblem
import lombok.NonNull

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

class TestUtils {

  static def createDefaultServlet(
      DataFetcher queryDataFetcher = { env -> env.arguments.arg },
      DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
      DataFetcher subscriptionDataFetcher = { env ->
        AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
        publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
          publisherRef.get().offer(env.arguments.arg)
          publisherRef.get().noMoreData()
        }))
        return publisherRef.get()
      },
      GraphQLServletListener... listeners) {
    createServlet(queryDataFetcher, mutationDataFetcher, subscriptionDataFetcher, null, listeners)
  }

  static def createBatchCustomizedServlet(
      DataFetcher queryDataFetcher = { env -> env.arguments.arg },
      DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
      DataFetcher subscriptionDataFetcher = { env ->
        AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
        publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
          publisherRef.get().offer(env.arguments.arg)
          publisherRef.get().noMoreData()
        }))
        return publisherRef.get()
      }) {
    createServlet(queryDataFetcher, mutationDataFetcher, subscriptionDataFetcher, createBatchExecutionHandler())
  }

  static def createDataLoadingServlet(
      DataFetcher queryDataFetcher = { env -> env.arguments.arg },
      DataFetcher fieldDataFetcher = { env -> env.arguments.arg },
      DataFetcher otherDataFetcher,
      ContextSetting contextSetting, GraphQLServletContextBuilder contextBuilder) {
    GraphQLSchema schema = createGraphQlSchemaWithTwoLevels(queryDataFetcher, fieldDataFetcher, otherDataFetcher)
    GraphQLHttpServlet servlet = GraphQLHttpServlet.with(GraphQLConfiguration
        .with(schema)
        .with(contextSetting)
        .with(contextBuilder)
        .with(executor())
        .build())
    servlet.init()
    return servlet
  }

  private static def createServlet(
      DataFetcher queryDataFetcher = { env -> env.arguments.arg },
      DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
      DataFetcher subscriptionDataFetcher = { env ->
        AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
        publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
          publisherRef.get().offer(env.arguments.arg)
          publisherRef.get().noMoreData()
        }))
        return publisherRef.get()
      },
      BatchInputPreProcessor batchHandler,
      GraphQLServletListener... listeners) {
    GraphQLHttpServlet servlet = GraphQLHttpServlet.with(
        graphQLConfiguration(
            createGraphQlSchema(queryDataFetcher, mutationDataFetcher, subscriptionDataFetcher),
            batchHandler,
            listeners
        )
    )
    servlet.init()
    return servlet
  }

  static def graphQLConfiguration(GraphQLSchema schema, BatchInputPreProcessor batchInputPreProcessor, GraphQLServletListener... listeners) {
    def configBuilder = GraphQLConfiguration.with(schema)
    if (batchInputPreProcessor != null) {
      configBuilder.with(batchInputPreProcessor)
    }
    if (listeners != null) {
      configBuilder.with(Arrays.asList(listeners))
    }
    configBuilder.with(executor());
    configBuilder.build()
  }

  private static Executor executor() {
    new Executor() {
      @Override
      void execute(@NonNull Runnable command) {
        command.run()
      }
    }
  }

  static def createBatchExecutionHandler() {
    new TestBatchInputPreProcessor()
  }

  static def createGraphQlSchema(
      DataFetcher queryDataFetcher = { env -> env.arguments.arg },
      DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
      DataFetcher subscriptionDataFetcher = { env ->
        AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
        publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
          publisherRef.get().offer(env.arguments.arg)
          publisherRef.get().noMoreData()
        }))
        return publisherRef.get()
      }) {
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
          field.name("object")
          field.type(
              GraphQLObjectType.newObject()
                  .name("NestedObject")
                  .field { nested ->
                    nested.name("a")
                    nested.type(Scalars.GraphQLString)
                    nested.argument { argument ->
                      argument.name("arg")
                      argument.type(Scalars.GraphQLString)
                    }
                    nested.dataFetcher(queryDataFetcher)
                  }
                  .field { nested ->
                    nested.name("b")
                    nested.type(Scalars.GraphQLString)
                    nested.argument { argument ->
                      argument.name("arg")
                      argument.type(Scalars.GraphQLString)
                    }
                    nested.dataFetcher(queryDataFetcher)
                  }
          )
          field.dataFetcher(new StaticDataFetcher([:]))
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
        .field { field ->
          field.name("echoFile")
          field.type(Scalars.GraphQLString)
          field.argument { argument ->
            argument.name("file")
            argument.type(ApolloScalars.Upload)
          }
          field.dataFetcher({ env -> new String(ByteStreams.toByteArray(env.arguments.file.getInputStream())) })
        }
        .field { field ->
          field.name("echoFiles")
          field.type(GraphQLList.list(Scalars.GraphQLString))
          field.argument { argument ->
            argument.name("files")
            argument.type(GraphQLList.list(GraphQLNonNull.nonNull(ApolloScalars.Upload)))
          }
          field.dataFetcher({ env ->
            env.arguments.files.collect {
              new String(ByteStreams.toByteArray(it.getInputStream()))
            }
          })
        }
        .build()

    GraphQLObjectType subscription = GraphQLObjectType.newObject()
        .name("Subscription")
        .field { field ->
          field.name("echo")
          field.type(Scalars.GraphQLString)
          field.argument { argument ->
            argument.name("arg")
            argument.type(Scalars.GraphQLString)
          }
          field.dataFetcher(subscriptionDataFetcher)
        }
        .build()


    return GraphQLSchema.newSchema()
        .query(query)
        .mutation(mutation)
        .subscription(subscription)
        .additionalType(ApolloScalars.Upload)
        .build()
  }

  static def createGraphQlSchemaWithTwoLevels(DataFetcher queryDataFetcher, DataFetcher fieldDataFetcher, DataFetcher otherQueryFetcher) {
    String sdl = """schema {
                        query: Query
                    }

                    type Query{
                            query(arg : String): QueryEcho
                            queryTwo(arg: String): OtherQueryEcho
                    }
                        
                    type QueryEcho {
                        echo(arg: String): FieldEcho
                    }
                    
                    type OtherQueryEcho {
                        echo(arg: String): String
                    }
                    
                    type FieldEcho {
                        echo(arg:String): String
                    }
                    """

    def wiring = RuntimeWiring.newRuntimeWiring()
        .type(TypeRuntimeWiring.newTypeWiring("Query").dataFetcher("query", { env -> env.arguments.arg })
            .dataFetcher("queryTwo", { env -> env.arguments.arg }))
        .type(TypeRuntimeWiring.newTypeWiring("QueryEcho").dataFetcher("echo", queryDataFetcher))
        .type(TypeRuntimeWiring.newTypeWiring("FieldEcho").dataFetcher("echo", fieldDataFetcher))
        .type(TypeRuntimeWiring.newTypeWiring("OtherQueryEcho").dataFetcher("echo", otherQueryFetcher))
        .build()


    try {
      def registry = new SchemaParser().parse(new StringReader(sdl))
      def options = SchemaGenerator.Options.defaultOptions()
      return new SchemaGenerator().makeExecutableSchema(options, registry, wiring)
    } catch (SchemaProblem e) {
      assert false: "The schema could not be compiled : ${e}"
      return null
    }
  }
}
