package graphql.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperProvider {
	ObjectMapper provide();
}
