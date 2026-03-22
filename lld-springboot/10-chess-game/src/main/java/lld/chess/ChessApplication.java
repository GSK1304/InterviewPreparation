package lld.chess;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@SpringBootApplication @EnableDiscoveryClient
public class ChessApplication {
    public static void main(String[] args) { SpringApplication.run(ChessApplication.class, args); }
}
