package aws.lambda.custom.runtime.integration;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import javax.validation.constraints.NotBlank;

import static aws.lambda.custom.runtime.integration.LambdaCustomRuntimeClient.RUNTIME_PATH;

@Client(value = "${AWS_LAMBDA_RUNTIME_API}", path = RUNTIME_PATH)
public interface LambdaCustomRuntimeClient {

    String RUNTIME_PATH = "/2018-06-01/runtime";

    // TODO : try Single as Response
    @Get(value = "/invocation/next",consumes = MediaType.APPLICATION_JSON)
    HttpResponse<String> getNextFunctionInvocation();


    @Post(value = "/invocation/{awsRequestId}/response", produces = MediaType.APPLICATION_JSON)
    HttpResponse<Void> postFunctionInvocationResponse(@NotBlank String awsRequestId, APIGatewayProxyResponseEvent apgResponse);



    /*

function toLambdaErr({ name, message, stack }) {
  return {
    errorType: name,
    errorMessage: message,
    stackTrace: (stack || '').split('\n').slice(1),
  }
}

     */
    @Post(value = "/invocation/{awsRequestId}/error", produces = MediaType.APPLICATION_JSON)
    HttpResponse<Void> postFunctionInvocationError(@Header("Lambda-Runtime-Function-Error-Type") String errorType,
                                                   @NotBlank String awsRequestId, @Body InvocationError error);


    @Post(value = "/init/error", produces = MediaType.APPLICATION_JSON)
    void postRuntimeInitError(InitialisationError initError);

}
