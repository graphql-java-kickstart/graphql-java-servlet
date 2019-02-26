package graphql.servlet

import com.google.common.io.ByteStreams
import graphql.Scalars
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.schema.*

import java.util.concurrent.atomic.AtomicReference

class TestUtils {

    static def createDefaultServlet(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                                    DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
                                    DataFetcher subscriptionDataFetcher = { env ->
                                        AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>();
                                        publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
                                            publisherRef.get().offer(env.arguments.arg)
                                            publisherRef.get().noMoreData()
                                        }))
                                        return publisherRef.get()
                                    }, boolean asyncServletModeEnabled = false) {
        GraphQLBatchExecutionHandlerFactory defaultHandler = new DefaultGraphQLBatchExecutionHandlerFactory();
        createServlet(queryDataFetcher, mutationDataFetcher, subscriptionDataFetcher, asyncServletModeEnabled, defaultHandler)
    }

    static def createBatchCustomizedServlet(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                                            DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
                                            DataFetcher subscriptionDataFetcher = { env ->
                                                AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>();
                                                publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
                                                    publisherRef.get().offer(env.arguments.arg)
                                                    publisherRef.get().noMoreData()
                                                }))
                                                return publisherRef.get()
                                            }, boolean asyncServletModeEnabled = false) {
        createServlet(queryDataFetcher, mutationDataFetcher, subscriptionDataFetcher, asyncServletModeEnabled, createBatchExecutionHandlerFactory())
    }

    private static def createServlet(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                                     DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
                                     DataFetcher subscriptionDataFetcher = { env ->
                                 AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>();
                                 publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
                                     publisherRef.get().offer(env.arguments.arg)
                                     publisherRef.get().noMoreData()
                                 }))
                                 return publisherRef.get()
                             }, boolean asyncServletModeEnabled = false,
                                     GraphQLBatchExecutionHandlerFactory batchHandlerFactory) {
        GraphQLHttpServlet servlet = GraphQLHttpServlet.with(GraphQLConfiguration
                .with(createGraphQlSchema(queryDataFetcher, mutationDataFetcher, subscriptionDataFetcher))
                .with(createInstrumentedQueryInvoker())
                .with(asyncServletModeEnabled)
                .with(batchHandlerFactory)
                .build())
        servlet.init(null)
        return servlet
    }

    static def createInstrumentedQueryInvoker() {
        Instrumentation instrumentation = new TestInstrumentation()
        GraphQLQueryInvoker.newBuilder().with([instrumentation]).build()
    }

    static def createBatchExecutionHandlerFactory() {
        new TestBatchExecutionHandlerFactoryBatch()
    }

    static def createGraphQlSchema(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                                   DataFetcher mutationDataFetcher = { env -> env.arguments.arg },
                                   DataFetcher subscriptionDataFetcher = { env ->
                                       AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>();
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

}
