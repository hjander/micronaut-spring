package aws.lambda.custom.runtime.integration;

public class InitialisationError {

    Exception e;

    public Exception getE() {
        return e;
    }

    public InitialisationError(Exception e) {
        this.e = e;
    }
}
