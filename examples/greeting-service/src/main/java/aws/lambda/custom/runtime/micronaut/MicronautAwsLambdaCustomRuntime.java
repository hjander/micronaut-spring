package aws.lambda.custom.runtime.micronaut;

import aws.lambda.custom.runtime.AwsApiGatewayRoutingApplicationAdapter;
import aws.lambda.custom.runtime.AwsLambdaCustomRuntime;
import aws.lambda.custom.runtime.model.AwsLambdaContext;
import aws.lambda.custom.runtime.model.LambdaError;
import aws.lambda.custom.runtime.model.LambdaInvocation;
import aws.lambda.custom.runtime.model.LambdaRuntimeEnvironmentVariables;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.StartupEvent;
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
import static java.net.HttpURLConnection.HTTP_OK;

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

    private static final String ENABLE_RUNTIME = "enable.runtime"; // for disabling runtime loop while graal class analyzing

    @Inject Environment env;

    @Inject private MicronautAwsLambdaCustomRuntimeClient lambdaCustomRuntimeClient;
    @Inject private AwsApiGatewayRoutingApplicationAdapter applicationAdapter;

    private LambdaRuntimeEnvironmentVariables lambdaEnvVariables;


    //@EventListener
    public void onStartup(StartupEvent startupEvent) {
        System.out.println("Starting up Custom runtime");
        this.start();
    }


    @Override
    public void initializeAwsLambdaRuntimeVariables() {

        this.lambdaEnvVariables = new LambdaRuntimeEnvironmentVariables( // TODO: move into class
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

        System.out.println("Going to initialize runtime loop");

        String handler;
        try {
            initializeAwsLambdaRuntimeVariables();
            handler = getHandler();

        } catch (Exception e) {

            handleRuntimeInitializeError(new LambdaError(e));
            System.exit(1);
            return;
        }
        try {
            //processEvents(handler);
            if(env.get(ENABLE_RUNTIME, Boolean.class).orElse(true)) {
                processEvents();
            }else{
                System.out.println("Runtime disabled");
            }

        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }


    void processEvents() {

        System.out.println("Starting invocation cycle");

        while (true) {
            invocationCycle();
        }
    }

    void invocationCycle() {

        // TODO: read AWS headers only in start
        initializeAwsLambdaRuntimeVariables();

        System.out.println("retrieve next invocation");
        LambdaInvocation nextInvocation = retrieveNextInvocation();

        try {
            System.out.printf("Invoking function with %n event:%n %s%n context:%n%s", nextInvocation.getEvent(), nextInvocation.getContext());

            APIGatewayProxyResponseEvent lambdaResult = handleApplicationInvocation(nextInvocation);

            System.out.printf("Posting result to Lambda API Gateway");

            if(hasError(lambdaResult)){
                handleInvocationError(new LambdaError(lambdaResult.getStatusCode().toString(), null, null), nextInvocation.getContext());
            }else{
                handleInvocationResult(lambdaResult, nextInvocation.getContext());
            }

        } catch (Exception e) {

            System.err.printf("Error invoking function: %n%s:", e);

            handleInvocationError(new LambdaError(e), nextInvocation.getContext());
        }
    }

    private boolean hasError(APIGatewayProxyResponseEvent lambdaResult) {
        return lambdaResult.getStatusCode().intValue()>=400;
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

        if (lambdaRuntimeResponse.getStatus().getCode() != HTTP_OK) {
            throw new Error("Unexpected invocation next response:" + lambdaRuntimeResponse);
        }

        if (Objects.nonNull(lambdaRuntimeResponse.getHeaders().get("lambda-runtime-trace-id"))) {
            // TODO: process.env._X_AMZN_TRACE_ID = lambdaRuntimeResponse.getHeaders().get("lambda-runtime-trace-id");
        } else {
            // TODO: delete process.env._X_AMZN_TRACE_ID
        }

        long deadlineMs = Long.parseLong(lambdaRuntimeResponse.getHeaders().get("lambda-runtime-deadline-ms"));
        int remainingTimeInMillis = (int)Duration.between( Instant.now(), Instant.ofEpochMilli(deadlineMs)).toMillis();


        // TODO: BASE64 encoded, later maybe deserialize
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
     * @param initError
     */
    void handleRuntimeInitializeError(LambdaError initError) {
        lambdaCustomRuntimeClient.postRuntimeInitError(initError);
    }


    /**
     * Communicate invocation errors
     * @param invocationError
     * @param context
     */
    void handleInvocationError(LambdaError invocationError, AwsLambdaContext context) {
        HttpResponse<Void> res = lambdaCustomRuntimeClient.postFunctionInvocationError(invocationError.getErrorType(), context.getAwsRequestId(), invocationError);
        if (res.getStatus().getCode() != HTTP_ACCEPTED) {
            throw new Error("Unexpected response:" + res);
        }

    }


    private String getHandler() {

        return lambdaEnvVariables.get_handler();

    }


}
