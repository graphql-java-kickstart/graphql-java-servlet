module graphql.kickstart.execution {
	requires lombok;
	requires graphql.java;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jdk8;

	exports graphql.kickstart.execution;
	exports graphql.kickstart.execution.config;
	exports graphql.kickstart.execution.context;
	exports graphql.kickstart.execution.error;
	exports graphql.kickstart.execution.input;
	exports graphql.kickstart.execution.instrumentation;
	exports graphql.kickstart.execution.subscriptions;
	exports graphql.kickstart.execution.subscriptions.apollo;

	opens graphql.kickstart.execution;
	opens graphql.kickstart.execution.config;
	opens graphql.kickstart.execution.context;
	opens graphql.kickstart.execution.error;
	opens graphql.kickstart.execution.input;
	opens graphql.kickstart.execution.instrumentation;
	opens graphql.kickstart.execution.subscriptions;
	opens graphql.kickstart.execution.subscriptions.apollo;
}
