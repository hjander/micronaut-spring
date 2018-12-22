package aws.lambda.custom.runtime.micronaut;

import com.agorapulse.micronaut.agp.ApiGatewayProxyHandler;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

import javax.inject.Singleton;


@Factory
public class Bootstrap {

    public static void main(String[] args) {

        System.out.println("Starting environment");

        ApplicationContext ctx = Micronaut.run(MicronautAwsLambdaCustomRuntime.class);
        MicronautAwsLambdaCustomRuntime customRuntime = ctx.getBean(MicronautAwsLambdaCustomRuntime.class);

        System.out.println("Got Custom Runtime. Starting up...");

        customRuntime.start();

    }


    @Bean
    @Singleton
    ApiGatewayProxyHandler apiGatewayProxyHandler(ApplicationContext context) {

        // TODO: create with : new ApiGatewayProxyHandler(context);
        return new ApiGatewayProxyHandler();
    }
}