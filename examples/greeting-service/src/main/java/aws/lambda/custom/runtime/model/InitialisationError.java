package aws.lambda.custom.runtime.model;

public class InitialisationError {

    Exception e;

    public Exception getE() {
        return e;
    }

    public InitialisationError(Exception e) {
        this.e = e;
    }
}
