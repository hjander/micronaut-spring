./gradlew clean build assemble
java -cp build/libs/greeting-service-all.jar io.micronaut.graal.reflect.GraalClassLoadingAnalyzer
native-image --no-server \
             --verbose \
             --class-path build/libs/greeting-service-all.jar \
             --allow-incomplete-classpath \
             -H:ReflectionConfigurationFiles=build/reflect.json,custom-reflect.json \
             -H:EnableURLProtocols=http \
             -H:IncludeResources="logback.xml|bootstrap.yml|META-INF/services/*.*" \
             -H:Name=greeting-service \
             -H:Class=aws.lambda.custom.runtime.micronaut.Application \
             -H:+ReportUnsupportedElementsAtRuntime \
             -H:-AllowVMInspection \
             -H:-UseServiceLoaderFeature \
             --rerun-class-initialization-at-runtime='sun.security.jca.JCAUtil$CachedSecureRandomHolder,javax.net.ssl.SSLContext' \
             --delay-class-initialization-to-runtime=io.netty.handler.codec.http.HttpObjectEncoder,io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder,io.netty.handler.ssl.util.ThreadLocalInsecureRandom,com.sun.jndi.dns.DnsClient,io.netty.handler.ssl.util.BouncyCastleSelfSignedCertGenerator,io.netty.handler.ssl.ReferenceCountedOpenSslEngine,io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator