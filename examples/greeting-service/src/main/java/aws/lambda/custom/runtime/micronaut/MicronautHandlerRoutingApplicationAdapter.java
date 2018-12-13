package aws.lambda.custom.runtime.micronaut;


import aws.lambda.custom.runtime.AwsApiGatewayRoutingApplicationAdapter;
import aws.lambda.custom.runtime.model.AwsLambdaContext;
import com.agorapulse.micronaut.agp.ApiGatewayProxyHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MicronautHandlerRoutingApplicationAdapter implements AwsApiGatewayRoutingApplicationAdapter {


    @Inject
    private ApiGatewayProxyHandler apiGatewayProxyHandler;

    @Override
    public APIGatewayProxyResponseEvent invoke(APIGatewayProxyRequestEvent event, AwsLambdaContext context) {
        return apiGatewayProxyHandler.handleRequest(event, context);
    }
}
