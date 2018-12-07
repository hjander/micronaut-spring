package aws.lambda.custom.runtime.integration;

public class NextInvocationResponse {

    /**
     The request ID, which identifies the request that triggered the function invocation.
     For example, 8476a536-e9f4-11e8-9739-2dfe598c3fcd.
     */
    private String lambdaRuntimeAwsRequestId;


    /**
     * The date that the function times out in Unix time milliseconds.
     * For example, 1542409706888.
     */
    private String lambdaRuntimeDeadlineMs;


    /**
     * The ARN of the Lambda function, version, or alias that's specified in the invocation.
     * For example, arn:aws:lambda:us-east-2:123456789012:function:custom-runtime.
     */
    private String lambdaRuntimeInvokedFunctionArn;


    /**
     * The AWS X-Ray tracing header.
     * For example, Root=1-5bef4de7-ad49b0e87f6ef6c87fc2e700;Parent=9a9197af755a6419;Sampled=1.
     */
    private String lambdaRuntimeTraceId;


    /**
     * For invocations from the AWS Mobile SDK, data about the client application and device.
     */
    private String lambdaRuntimeClientContext;


    /**
     * For invocations from the AWS Mobile SDK, data about the Amazon Cognito identity provider.
     */
    private String lambdaRuntimeCognitoIdentity;

}
