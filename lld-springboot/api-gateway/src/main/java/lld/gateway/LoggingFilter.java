package lld.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Configuration
public class LoggingFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Bean
    @Order(-1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            String path    = exchange.getRequest().getPath().toString();
            String method  = exchange.getRequest().getMethod().name();
            long   start   = Instant.now().toEpochMilli();

            log.info("[GATEWAY] --> {} {} routed to service",method, path);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = Instant.now().toEpochMilli() - start;
                int  status   = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
                log.info("[GATEWAY] <-- {} {} | status={} | {}ms", method, path, status, duration);
            }));
        };
    }
}
