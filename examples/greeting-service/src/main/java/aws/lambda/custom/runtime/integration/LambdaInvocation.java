package aws.lambda.custom.runtime.integration;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public class LambdaInvocation extends NextInvocationResponse {

    private final APIGatewayProxyRequestEvent event;
    private final AwsLambdaContext context;

    public LambdaInvocation(APIGatewayProxyRequestEvent event, AwsLambdaContext context) {
        this.event = event;
        this.context = context;
    }


    public APIGatewayProxyRequestEvent getEvent() {
        return event;
    }

    public AwsLambdaContext getContext() {
        return context;
    }
}
