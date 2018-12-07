package aws.lambda.custom.runtime.integration;

public class InvocationError {

    private String errorMessage;
    private String errorType;
    private String stackTrace;

    public InvocationError(Exception e) {
        this.errorMessage = e.getMessage();
        this.errorType = e.getCause().toString();
        this.stackTrace = e.getStackTrace().toString();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
