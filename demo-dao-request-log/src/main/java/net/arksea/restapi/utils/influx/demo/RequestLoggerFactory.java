package net.arksea.restapi.utils.influx.demo;

import net.arksea.restapi.utils.influx.IRequestLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RequestLoggerFactory {
    @Bean(name="databaseRequestLogger")
    public IRequestLogger create() {
        return new DemoRequestLogger();
    }
}
