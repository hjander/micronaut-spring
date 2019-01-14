package aws.lambda.custom.runtime.micronaut;


import aws.lambda.custom.runtime.AwsLambdaCustomRuntimeClient;
import aws.lambda.custom.runtime.model.LambdaError;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import javax.validation.constraints.NotBlank;

@Client(value = "${aws.lambda.runtime.api}", path = AwsLambdaCustomRuntimeClient.RUNTIME_PATH)
public interface MicronautAwsLambdaCustomRuntimeClient {//extends AwsLambdaCustomRuntimeClient {


    // TODO : try Single as Response
    @Get(value = "/invocation/next",consumes = MediaType.APPLICATION_JSON)
    HttpResponse<String> getNextFunctionInvocation();


    @Post(value = "/invocation/{awsRequestId}/response", produces = MediaType.APPLICATION_JSON)
    HttpResponse postFunctionInvocationResponse(String awsRequestId, @Body APIGatewayProxyResponseEvent apgResponse);


    @Post(value = "/invocation/{awsRequestId}/error", consumes = MediaType.APPLICATION_JSON)
    HttpResponse<Void> postFunctionInvocationError(@Header("Lambda-Runtime-Function-Error-Type") String errorType,
                                                   @NotBlank String awsRequestId, @Body LambdaError error);


    @Post(value = "/init/error", produces = MediaType.APPLICATION_JSON)
    void postRuntimeInitError(LambdaError initError);

}
