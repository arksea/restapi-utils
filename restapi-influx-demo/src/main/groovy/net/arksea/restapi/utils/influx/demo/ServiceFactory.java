package net.arksea.restapi.utils.influx.demo;

import akka.actor.ActorSystem;
import net.arksea.restapi.utils.influx.IRequestLogger;
import net.arksea.restapi.utils.influx.RequestLogFilter;
import net.arksea.restapi.utils.influx.DefaultRequestLogFilterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 *
 * Created by xiaohaixing on 2018/4/2.
 */
@Component
public class ServiceFactory {
    private static Logger logger = LogManager.getLogger(ServiceFactory.class);
    @Bean
    ActorSystem createSystem() {
        return ActorSystem.apply();
    }

    @Bean(name="logFilter")
    RequestLogFilter createRequestLogFilter() {
        IRequestLogger reqLogger = new IRequestLogger() {
            @Override
            public void respond(String uri, String group, int status, long respondTime) {
                logger.info("RequestLogger.respond: uri={},group={},status={},time={}",uri,group,status,respondTime);
            }
            @Override
            public void request(String uri, String group) {
                logger.info("RequestLogger.request: uri={},group={}",uri,group);
            }
            @Override
            public void writeLogs() {
                logger.info("RequestLogger.writeLogs");
            }
        };
        return new DefaultRequestLogFilterBuilder()
                .addIgnoreUriPrefix("/heartbeat")
                .setRequestGroupHeaderName("x-product-id")
                .setRequestLogger(reqLogger)
                .build();
    }
}
