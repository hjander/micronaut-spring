package aws.lambda.custom.runtime.micronaut;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;



public class Application {

    public static void main(String[] args) {

        System.out.println("-----STARTING ENVIRONMENT------");

        ApplicationContext ctx = Micronaut.run(Application.class);
//        MicronautAwsLambdaCustomRuntime customRuntime = ctx.getBean(MicronautAwsLambdaCustomRuntime.class);
//
//        System.out.println("GOT CUSTOM RUNTIME. STARTING UP");
//
//        customRuntime.start();

        System.out.println("FINISHED EXECUTING CUSTOM RUNTIME. SHUTTING DOWN");
    }


}