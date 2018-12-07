package aws.lambda.custom.runtime.integration;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the AWS Lambda Runtime Interface.
 * Responsible for retrieving the next invocation from the AWS Lambda Environment
 * resolving which function to call and actually call it with the supplied user/AWS-system parameters.
 * Then delivers the result back to AWS Lambda runtime or errors.
 *
 * TODO:
 * - difference between handler and awsFunctionName
 * validation
 *
 *
 */

public class AwsLambdaCustomRuntimeMicronautApplicationAdpater {

    private ObjectMapper objectMapper = new ObjectMapper();

    // TODO: per request
    private static final String AWS_LAMBDA_FUNCTION_NAME = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
    private static final String AWS_LAMBDA_FUNCTION_VERSION = System.getenv("AWS_LAMBDA_FUNCTION_VERSION");
    private static final String AWS_LAMBDA_LOG_GROUP_NAME = System.getenv("AWS_LAMBDA_LOG_GROUP_NAME");
    private static final String AWS_LAMBDA_LOG_STREAM_NAME = System.getenv("AWS_LAMBDA_LOG_STREAM_NAME");
    private static final String _HANDLER = System.getenv("_HANDLER");
    private static final int AWS_LAMBDA_FUNCTION_MEMORY_SIZE = Integer.parseInt(System.getenv("AWS_LAMBDA_FUNCTION_MEMORY_SIZE"));



    @Inject
    private LambdaCustomRuntimeClient lambdaCustomRuntimeClient;

    @Inject
    private AwsLambdaApplicationAdapter lambdaApplicationAdapter;


    public void start() {


        String handler;
        try {
            handler = getHandler();
        } catch (Exception e) {
            initError(new InitialisationError(e));
            System.exit(1);
            return;
        }
        try {
            processEvents(handler);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    void processEvents(String handler) {

        while (true) {
            LambdaInvocation nextInvocation = nextInvocation();
            APIGatewayProxyResponseEvent result;


            try {
                System.out.printf("Invoking function with event:%n %s%n context:%n%s", nextInvocation.getEvent(), nextInvocation.getContext());
                result = lambdaApplicationAdapter.invoke(nextInvocation.getEvent(), nextInvocation.getContext());

                System.out.printf("Posting result to Lambda API Gateway");
                invokeResponse(result, nextInvocation.getContext());
            } catch (Exception e) {

                System.err.printf("Error invoking function: %n%s:", e);
                invokeError(new InvocationError(e), nextInvocation.getContext());
            }

        }
    }


    void initError(InitialisationError err) {
        lambdaCustomRuntimeClient.postRuntimeInitError(err);
    }



    LambdaInvocation nextInvocation() {

        HttpResponse<String> res = lambdaCustomRuntimeClient.getNextFunctionInvocation();

        if (res.getStatus().getCode() != 200) {
            throw new Error("Unexpected invocation next response:" + res);
        }

        if (Objects.nonNull(res.getHeaders().get("lambda-runtime-trace-id"))) {
            //process.env._X_AMZN_TRACE_ID = res.getHeaders().get("lambda-runtime-trace-id");
        } else {
            //delete process.env._X_AMZN_TRACE_ID
        }

        long deadlineMs = Integer.parseInt(res.getHeaders().get("lambda-runtime-deadline-ms"));
        int remainingTimeInMillis = (int)Duration.between( Instant.now(), Instant.ofEpochMilli(deadlineMs)).toMillis();


        // BASE64 encoded, later maybe deserialize
        ClientContext clientContext = Optional.ofNullable(res.getHeaders().get("lambda-runtime-client-context"))
                .map(cc -> {
                    try {
                        return objectMapper.readValue(cc, ClientContext.class);
                    } catch (IOException e) {
                        return null;
                        //e.printStackTrace();
                    }
                }).orElseThrow(RuntimeException::new);

        CognitoIdentity identityJson = Optional.ofNullable(res.getHeaders().get("lambda-runtime-cognito-identity"))
                .map(cc -> {
                    try {
                        return objectMapper.readValue(cc, CognitoIdentity.class);
                    } catch (IOException e) {
                        return null;
                        //e.printStackTrace();
                    }
                }).orElse(null);

        APIGatewayProxyRequestEvent event = res.getBody().map(lambdaEvent -> {
            try {
                return objectMapper.readValue(lambdaEvent, APIGatewayProxyRequestEvent.class);
            } catch (IOException e) {
                return null;
                //e.printStackTrace();
            }
        }).orElse(null);

        AwsLambdaContext context = new AwsLambdaContext(
                res.getHeaders().get("lambda-runtime-aws-request-id"),
                res.getHeaders().get("lambda-runtime-invoked-function-arn"),
                AWS_LAMBDA_LOG_GROUP_NAME,
                AWS_LAMBDA_LOG_STREAM_NAME,
                AWS_LAMBDA_FUNCTION_NAME,
                AWS_LAMBDA_FUNCTION_VERSION,
                identityJson,
                clientContext,
                AWS_LAMBDA_FUNCTION_MEMORY_SIZE,
                remainingTimeInMillis);


        return new LambdaInvocation(event, context);
    }


    void invokeResponse(APIGatewayProxyResponseEvent result, AwsLambdaContext context) {

        // body: JSON.stringify(result) !
        HttpResponse<Void> res = lambdaCustomRuntimeClient.postFunctionInvocationResponse(context.getAwsRequestId(), result);

        if (res.getStatus().getCode() != 202) {
            throw new Error("Unexpected /invocation/response response:" + res);
        }
    }


    void invokeError(InvocationError error, AwsLambdaContext context) {
        HttpResponse<Void> res = lambdaCustomRuntimeClient.postFunctionInvocationError(error.getErrorType(), context.getAwsRequestId(), error);
        if (res.getStatus().getCode() != 202) {
            throw new Error("Unexpected ${path} response: ${JSON.stringify(res)}");
        }

    }


    /*
    task deploy(type: AWSLambdaMigrateFunctionTask, dependsOn: shadowJar) {
    functionName = "hello-world"
    handler = "example.HelloWorldFunction::hello"
    role = "arn:aws:iam::${aws.accountId}:role/lambda_basic_execution"
    runtime = Runtime.Java8
    zipFile = shadowJar.archivePath
    memorySize = 256
    timeout = 60
}

     */

    private String getHandler() {

        return _HANDLER;
        /*
        String[] handlerParts = _HANDLER.split(":");

        if (handlerParts.length != 2) {
            throw new Error("Incorrect handler format" +_HANDLER);
        }

        String modulePath = handlerParts[0];
        String handlerName = handlerParts[1];

        let app
        try {
            app = require(LAMBDA_TASK_ROOT + '/' + modulePath)
        } catch (e) {
            if (e.code === 'MODULE_NOT_FOUND') {
                throw new Error(`Unable to import module '${modulePath}'`)
            }
            throw e
        }

        const userHandler = app[handlerName]

        if (userHandler == null) {
            throw new Error(`Handler '${handlerName}' missing on module '${modulePath}'`)
        } else if (typeof userHandler !== 'function') {
            throw new Error(`Handler '${handlerName}' from '${modulePath}' is not a function`)
        }
        return userHandler
        */
    }


}
