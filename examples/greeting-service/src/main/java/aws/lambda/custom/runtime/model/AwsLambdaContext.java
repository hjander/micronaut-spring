package aws.lambda.custom.runtime.model;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;


/**
 * The data with which an actual function call is executed.
 * Data are created or supplied by the AWS Lambda Environment
 */
public class AwsLambdaContext implements Context {


    public AwsLambdaContext(String awsRequestId, String invokedFunctionArn, String logGroupName, String logStreamName,
                            String functionName, String functionVersion, CognitoIdentity identity, ClientContext clientContext,
                            String memoryLimitInMB, int remainingTimeInMillis) {

        this.awsRequestId = awsRequestId;
        this.invokedFunctionArn = invokedFunctionArn;
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        this.functionName = functionName;
        this.functionVersion = functionVersion;
        this.identity = identity;
        this.clientContext = clientContext;
        this.memoryLimitInMB = Integer.parseInt(memoryLimitInMB);
        this.remainingTimeInMillis = remainingTimeInMillis;
    }

    private final String awsRequestId;

    private final String invokedFunctionArn;

    private final String logGroupName;
    
    private final String logStreamName;

    private final String functionName;

    private final String functionVersion;

    private final CognitoIdentity identity;

    private final ClientContext clientContext;

    private final int memoryLimitInMB;

    private final int remainingTimeInMillis;



    public String getAwsRequestId() {
        return awsRequestId;
    }

    public String getInvokedFunctionArn() {
        return invokedFunctionArn;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getFunctionVersion() {
        return functionVersion;
    }

    public CognitoIdentity getIdentity() {
        return identity;
    }

    public ClientContext getClientContext() {
        return clientContext;
    }

    public int getMemoryLimitInMB() { return memoryLimitInMB; }

    public int getRemainingTimeInMillis() {
        return remainingTimeInMillis;
    }

    @Override
    public LambdaLogger getLogger() {
        return null;
    }
}
