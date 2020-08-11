package net.arksea.restapi.utils.influx.demo;

import net.arksea.restapi.utils.influx.AbstractRequestLogger;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Created by xiaohaixing on 2017/5/20.
 */
public class DemoRequestLogger extends AbstractRequestLogger {

    private static final Logger logger = LogManager.getLogger(DemoRequestLogger.class);
    private final String tableName;

    public DemoRequestLogger() {
        this.tableName = "request";
    }

    @Override
    public void writeLogs() {
        StringBuilder sb = new StringBuilder();
        countMap.entrySet().forEach(e -> {
            Counter counter = e.getValue();
            long req = counter.requestCount.getAndSet(0);
            long respond2xx = counter.respond2xx.getAndSet(0);
            long respond3xx = counter.respond3xx.getAndSet(0);
            long respond4xx = counter.respond4xx.getAndSet(0);
            long respond5xx = counter.respond5xx.getAndSet(0);
            long respondCount = respond2xx + respond3xx + respond4xx + respond5xx;
            long responedTime = respondCount == 0 ? 0 : counter.respondTime.getAndSet(0) / respondCount;
            if (req>0 || respondCount>0) {
                String name = e.getKey().getLeft();
                String group = e.getKey().getRight();
                sb.append(tableName).append(",group=").append(format(group))
                    .append(",name=").append(name)
                    .append(" request=").append(req)
                    .append(",respond2xx=").append(respond2xx)
                    .append(",respond3xx=").append(respond3xx)
                    .append(",respond4xx=").append(respond4xx)
                    .append(",respond5xx=").append(respond5xx)
                    .append(",responedTime=").append(responedTime)
                    .append("\n");
            }
        });
        String body = sb.toString();
        if (StringUtils.isEmpty(body)) {
            return;
        }
        logger.info(body);
    }

    private String format(String str) {
        return "".equals(str) ? "blank" : str;
    }
}
