package lld.splitwise;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@SpringBootApplication @EnableDiscoveryClient
public class SplitwiseApplication {
    public static void main(String[] args) { SpringApplication.run(SplitwiseApplication.class, args); }
}
