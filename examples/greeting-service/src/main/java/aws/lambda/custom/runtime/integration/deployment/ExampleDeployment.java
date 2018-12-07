package aws.lambda.custom.runtime.integration.deployment;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;

public class ExampleDeployment {

    public static void main(String[] args) {

        App app = new App();

        new GreetingServiceStack(app, "hello-cdk");

        app.run();
    }


    class GreetingServiceStack extends Stack {

        public GreetingServiceStack(final App parent, final String name) {

            super(parent, name);



            SinkQueue sinkQueue = new SinkQueue(this, "MySinkQueue", SinkQueueProps.builder().withRequiredTopicCount(5).build());

        }

    }
}
