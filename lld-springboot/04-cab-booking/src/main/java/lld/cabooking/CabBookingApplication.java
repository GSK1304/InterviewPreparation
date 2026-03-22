package lld.cabooking;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@SpringBootApplication @EnableDiscoveryClient
public class CabBookingApplication {
    public static void main(String[] args) { SpringApplication.run(CabBookingApplication.class, args); }
}
