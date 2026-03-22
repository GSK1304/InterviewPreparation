package lld.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Exclude EurekaClientAutoConfiguration to prevent the server from trying to
// register itself as a Eureka client. Without this exclusion, Spring Cloud
// initialises a DiscoveryClient even though register-with-eureka=false is set,
// causing "Connection refused" errors in the log on startup.
@SpringBootApplication(exclude = {
    org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration.class
})
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
