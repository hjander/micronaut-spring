package aws.lambda.custom.runtime.integration;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

/**
 * Provides a wrapper around a Java Web (Micronaut, Spring, ...) application.
 * Implementations are responsible for routing requests and wrapping Request/Responses from/to APIGatewayProxy*Event
 */
public interface AwsLambdaApplicationAdapter {

     APIGatewayProxyResponseEvent invoke(APIGatewayProxyRequestEvent event, AwsLambdaContext context);
}
