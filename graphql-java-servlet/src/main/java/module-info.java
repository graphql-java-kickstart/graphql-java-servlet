module graphql.kickstart.servlet {
	requires graphql.java;
	requires lombok;
	requires graphql.kickstart.execution;

	exports graphql.kickstart.servlet;
	exports graphql.kickstart.servlet.apollo;
	exports graphql.kickstart.servlet.config;
	exports graphql.kickstart.servlet.context;
	exports graphql.kickstart.servlet.core;
	exports graphql.kickstart.servlet.input;
	exports graphql.kickstart.servlet.osgi;
	exports graphql.kickstart.servlet.subscriptions;

	opens graphql.kickstart.servlet;
	opens graphql.kickstart.servlet.apollo;
	opens graphql.kickstart.servlet.config;
	opens graphql.kickstart.servlet.context;
	opens graphql.kickstart.servlet.core;
	opens graphql.kickstart.servlet.input;
	opens graphql.kickstart.servlet.osgi;
	opens graphql.kickstart.servlet.subscriptions;
}
