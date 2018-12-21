package aws.lambda.custom.runtime.model;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

  /*

    function toLambdaErr({ name, message, stack }) {
      return {
        errorType: name,
        errorMessage: message,
        stackTrace: (stack || '').split('\n').slice(1),
      }
    }

  */

public class LambdaError {

    private String errorMessage;
    private String errorType;
    private String stackTrace;

    public LambdaError(String errorMessage, String errorType, String stackTrace) {

        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.stackTrace = stackTrace;
    }

    public LambdaError(Exception e) {

        this.errorType = Optional.ofNullable(e.getMessage()).orElse("NO ERROR PROVIDED");
        this.errorMessage = Optional.ofNullable(e.getCause()).map(c -> c.toString()).orElse(this.errorType);
        this.stackTrace = Optional.ofNullable(
                e.getStackTrace()).map(st -> Stream.of(e.getStackTrace()).map((a) -> a.toString()).collect(Collectors.joining("\n", "[", "]")))
                .orElse(null);

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
