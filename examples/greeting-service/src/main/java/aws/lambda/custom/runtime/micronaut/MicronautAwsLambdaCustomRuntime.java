package aws.lambda.custom.runtime.micronaut;

import aws.lambda.custom.runtime.AwsApiGatewayRoutingApplicationAdapter;
import aws.lambda.custom.runtime.AwsLambdaCustomRuntime;
import aws.lambda.custom.runtime.model.*;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

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

@Singleton
public class MicronautAwsLambdaCustomRuntime implements AwsLambdaCustomRuntime {

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(MicronautAwsLambdaCustomRuntime.class);


    @Inject Environment env;

    @Inject private MicronautAwsLambdaCustomRuntimeClient lambdaCustomRuntimeClient;
    @Inject private AwsApiGatewayRoutingApplicationAdapter applicationAdapter;

    private LambdaRuntimeEnvironmentVariables lambdaEnvVariables;


    @Override
    public void initializeAwsLambdaRuntimeVariables() {

        this.lambdaEnvVariables = new LambdaRuntimeEnvironmentVariables(
                null, //env.get(".handler", String.class).orElseThrow(IllegalArgumentException::new), TODO ?
                env.get("aws.lambda.function.name", String.class).orElseThrow(IllegalArgumentException::new),
                env.get("aws.lambda.log.group.name", String.class).orElseThrow(IllegalArgumentException::new),
                env.get("aws.lambda.log.stream.name", String.class).orElseThrow(IllegalArgumentException::new),
                env.get("aws.lambda.function.version", String.class).orElseThrow(IllegalArgumentException::new),
                env.get("aws.lambda.function.memory.size", String.class).orElseThrow(IllegalArgumentException::new));
    }


    /**
     * start the custom runtime.
     */
    @Override
    public void start() {

       LOG.info("Starting AWS Custom runtime loop");

        initializeAwsLambdaRuntimeVariables();


        String handler;
        try {
            handler = getHandler();
        } catch (Exception e) {
            handleRuntimeInitializeError(new InitialisationError(e));
            System.exit(1);
            return;
        }
        try {
            //processEvents(handler);
            processEvents();

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }


    void processEvents() {

        while (true) {
            invocationCycle();
        }
    }

    void invocationCycle() {

        // TODO: read AWS headers only in start
        initializeAwsLambdaRuntimeVariables();


        LambdaInvocation nextInvocation = retrieveNextInvocation();

        try {
            System.out.printf("Invoking function with %n event:%n %s%n context:%n%s", nextInvocation.getEvent(), nextInvocation.getContext());

            APIGatewayProxyResponseEvent lambdaResult = handleApplicationInvocation(nextInvocation);

            System.out.printf("Posting result to Lambda API Gateway");

            handleInvocationResult(lambdaResult, nextInvocation.getContext());
        } catch (Exception e) {

            System.err.printf("Error invoking function: %n%s:", e);

            handleInvocationError(new InvocationError(e), nextInvocation.getContext());
        }
    }

    /**
     * Execute the actual call. Assure Request Header is set.
     * @param nextInvocation
     * @return
     */
    private APIGatewayProxyResponseEvent handleApplicationInvocation(LambdaInvocation nextInvocation) {

        APIGatewayProxyResponseEvent lambdaResult = applicationAdapter.invoke(nextInvocation.getEvent(), nextInvocation.getContext());

        return lambdaResult;
    }


    LambdaInvocation retrieveNextInvocation() {

        HttpResponse<String> lambdaRuntimeResponse = lambdaCustomRuntimeClient.getNextFunctionInvocation();

        if (lambdaRuntimeResponse.getStatus().getCode() != 200) {
            throw new Error("Unexpected invocation next response:" + lambdaRuntimeResponse);
        }

        if (Objects.nonNull(lambdaRuntimeResponse.getHeaders().get("lambda-runtime-trace-id"))) {
            // TODO: process.env._X_AMZN_TRACE_ID = lambdaRuntimeResponse.getHeaders().get("lambda-runtime-trace-id");
        } else {
            // TODO: delete process.env._X_AMZN_TRACE_ID
        }

        long deadlineMs = Long.parseLong(lambdaRuntimeResponse.getHeaders().get("lambda-runtime-deadline-ms"));
        int remainingTimeInMillis = (int)Duration.between( Instant.now(), Instant.ofEpochMilli(deadlineMs)).toMillis();


        // BASE64 encoded, later maybe deserialize
        ClientContext clientContext = Optional.ofNullable(lambdaRuntimeResponse.getHeaders().get("lambda-runtime-client-context"))
                .map(cc -> {
                    try {
                        return objectMapper.readValue(cc, ClientContext.class);
                    } catch (IOException e) {
                        return null;
                    }

               // }).orElseThrow(RuntimeException::new); TODO : clientcontext optional or mandatory ???!!!
                }).orElse(null);

        CognitoIdentity identityJson = Optional.ofNullable(lambdaRuntimeResponse.getHeaders().get("lambda-runtime-cognito-identity"))
                .map(cc -> {
                    try {
                        return objectMapper.readValue(cc, CognitoIdentity.class);
                    } catch (IOException e) {
                        return null;
                    }
                }).orElse(null);

        APIGatewayProxyRequestEvent event = lambdaRuntimeResponse.getBody().map(lambdaEvent -> {
            try {
                return objectMapper.readValue(lambdaEvent, APIGatewayProxyRequestEvent.class);
            } catch (IOException e) {
                return null;
                //e.printStackTrace();
            }
        }).orElse(null);

        AwsLambdaContext context = new AwsLambdaContext(
                lambdaRuntimeResponse.getHeaders().get("lambda-runtime-aws-request-id"),
                lambdaRuntimeResponse.getHeaders().get("lambda-runtime-invoked-function-arn"),
                lambdaEnvVariables.getAwsLambdaLogGroupName(),
                lambdaEnvVariables.getAwsLambdaLogStreamName(),
                lambdaEnvVariables.getAwsLambdaFunctionName(),
                lambdaEnvVariables.getAwsLambdaFunctionVersion(),
                identityJson,
                clientContext,
                lambdaEnvVariables.getAwsLambdaFunctionMemorySize(),
                remainingTimeInMillis);


        return new LambdaInvocation(event, context);
    }


    void handleInvocationResult(APIGatewayProxyResponseEvent result, AwsLambdaContext context) {

        // body: JSON.stringify(result) !
        HttpResponse res = lambdaCustomRuntimeClient.postFunctionInvocationResponse(context.getAwsRequestId(), result);

        if (res.getStatus().getCode() != HTTP_ACCEPTED) {
            throw new Error("Unexpected /invocation/response response:" + res);
        }
    }


    /**
     * Handle errors while initializing the runtime
     * @param err
     */
    void handleRuntimeInitializeError(InitialisationError err) {
        lambdaCustomRuntimeClient.postRuntimeInitError(err);
    }


    /**
     * Communicate invocation errors
     * @param error
     * @param context
     */
    void handleInvocationError(InvocationError error, AwsLambdaContext context) {
        HttpResponse<Void> res = lambdaCustomRuntimeClient.postFunctionInvocationError(error.getErrorType(), context.getAwsRequestId(), error);
        if (res.getStatus().getCode() != HTTP_ACCEPTED) {
            throw new Error("Unexpected response:" + res);
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

        return lambdaEnvVariables.get_handler();

    }


}
