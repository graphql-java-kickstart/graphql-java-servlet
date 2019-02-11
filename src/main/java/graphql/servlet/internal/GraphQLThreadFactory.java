package graphql.servlet.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import graphql.servlet.AbstractGraphQLHttpServlet;

/**
 * {@link ThreadFactory} implementation for {@link AbstractGraphQLHttpServlet} async operations
 * 
 * @author John Nutting
 */
public class GraphQLThreadFactory implements ThreadFactory {

	final static String NAME_PREFIX = "GraphQLServlet-";
	final AtomicInteger threadNumber = new AtomicInteger(1);

	@Override
	public Thread newThread(final Runnable r) {
		Thread t = new Thread(r, NAME_PREFIX + threadNumber.getAndIncrement());
		t.setDaemon(false);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}

}