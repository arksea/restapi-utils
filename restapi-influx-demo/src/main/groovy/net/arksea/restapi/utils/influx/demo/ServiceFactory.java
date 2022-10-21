package net.arksea.restapi.utils.influx.demo;

import akka.actor.ActorSystem;
import net.arksea.restapi.utils.influx.IRequestLogger;
import net.arksea.restapi.utils.influx.HttpRequestLogFilter;
import net.arksea.restapi.utils.influx.DefaultRequestLogFilterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import zipkin2.Span;

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
    HttpRequestLogFilter createRequestLogFilter() {
        IRequestLogger reqLogger = new IRequestLogger() {
            @Override
            public void monitor(String uri, String group, int status, long respondTime) {
                logger.info("RequestLogger.monitor: uri={},group={},status={},time={}",uri,group,status,respondTime);
            }
            @Override
            public void trace(Span span) {
                logger.info("RequestLogger.trace: span={}",span);
            }

            @Override
            public void writeLogs() {
                logger.info("RequestLogger.writeLogs");
            }
        };
        return new DefaultRequestLogFilterBuilder()
                .addIncludedUriPrefix("/api")
                .addIgnoreUriPrefix("/heartbeat")
                .setRequestGroupHeaderName("x-product-id")
                .setAlwaysWrapRequest(true)
                .setRequestLogger(reqLogger)
                .build();
    }
}
