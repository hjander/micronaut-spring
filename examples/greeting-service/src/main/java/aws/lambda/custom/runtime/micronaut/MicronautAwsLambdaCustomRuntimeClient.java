package aws.lambda.custom.runtime.micronaut;


import aws.lambda.custom.runtime.AwsLambdaCustomRuntimeClient;
import aws.lambda.custom.runtime.model.InitialisationError;
import aws.lambda.custom.runtime.model.InvocationError;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.jackson.annotation.JacksonFeatures;

import javax.validation.constraints.NotBlank;

import static com.fasterxml.jackson.databind.DeserializationFeature.UNWRAP_ROOT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRAP_ROOT_VALUE;

@Client(value = "${aws.lambda.runtime.api}", path = AwsLambdaCustomRuntimeClient.RUNTIME_PATH)
public interface MicronautAwsLambdaCustomRuntimeClient extends AwsLambdaCustomRuntimeClient {


    // TODO : try Single as Response
    @Get(value = "/invocation/next",consumes = MediaType.APPLICATION_JSON)
    HttpResponse<String> getNextFunctionInvocation();


    @Post(value = "/invocation/{awsRequestId}/response", produces = MediaType.APPLICATION_JSON)
    HttpResponse postFunctionInvocationResponse(String awsRequestId, @Body APIGatewayProxyResponseEvent apgResponse);

    /*

    function toLambdaErr({ name, message, stack }) {
      return {
        errorType: name,
        errorMessage: message,
        stackTrace: (stack || '').split('\n').slice(1),
      }
    }

         */
    @Post(value = "/invocation/{awsRequestId}/error", consumes = MediaType.APPLICATION_JSON)
    HttpResponse<Void> postFunctionInvocationError(@Header("Lambda-Runtime-Function-Error-Type") String errorType,
                                                   @NotBlank String awsRequestId, @Body InvocationError error);


    @Post(value = "/init/error", produces = MediaType.APPLICATION_JSON)
    void postRuntimeInitError(InitialisationError initError);

}
