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
        ApplicationContext ctx = Micronaut.run(MicronautAwsLambdaCustomRuntime.class);
        ctx.getBean(MicronautAwsLambdaCustomRuntime.class).start();
    }


    @Bean
    @Singleton
    ApiGatewayProxyHandler apiGatewayProxyHandler() {
        return new ApiGatewayProxyHandler();
    }
}
