package net.arksea.restapi.utils.influx;

import akka.dispatch.OnComplete;
import net.arksea.httpclient.asker.FuturedHttpClient;
import net.arksea.httpclient.asker.HttpResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Created by xiaohaixing on 2017/5/20.
 */
public class HttpRequestLogger extends AbstractRequestLogger {

    private static final Logger logger = LogManager.getLogger(HttpRequestLogger.class);
    private transient final FuturedHttpClient futuredHttpClient;
    private static final int timeout = 10000;

    private transient final String dbUrl;
    private transient final String tableName;
    private long lastLogErrorTime = 0;
    private long lastLogSucceedTime = 0;

    @Deprecated //要求提供表名参数
    public HttpRequestLogger(String dbUrl, FuturedHttpClient futuredHttpClient) {
        this.futuredHttpClient = futuredHttpClient;
        this.dbUrl = dbUrl;
        this.tableName = "request";
    }

    public HttpRequestLogger(String tableName, String dbUrl, FuturedHttpClient futuredHttpClient) {
        this.futuredHttpClient = futuredHttpClient;
        this.dbUrl = dbUrl;
        this.tableName = tableName;
    }

    @Override
    public void writeLogs() {
        HttpPost post = new HttpPost(dbUrl);
        StringBuilder sb = new StringBuilder();
        countMap.forEach((key, counter) -> {
            long req = counter.requestCount.getAndSet(0);
            long respond2xx = counter.respond2xx.getAndSet(0);
            long respond3xx = counter.respond3xx.getAndSet(0);
            long respond4xx = counter.respond4xx.getAndSet(0);
            long respond5xx = counter.respond5xx.getAndSet(0);
            long respondCount = respond2xx + respond3xx + respond4xx + respond5xx;
            long responedTime = respondCount == 0 ? 0 : counter.respondTime.getAndSet(0) / respondCount;
            if (req > 0 || respondCount > 0) {
                String name = key.getLeft();
                String group = key.getRight();
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
        logger.debug(body);
        post.setEntity(new StringEntity(body, "UTF-8"));
        futuredHttpClient.ask(post, "request", timeout).onComplete(
            new OnComplete<HttpResult>() {
                @Override
                public void onComplete(Throwable ex, HttpResult ret) {
                    if (ex == null) {
                        if (ret.error == null) {
                            int code = ret.response.getStatusLine().getStatusCode();
                            if (code == 204 || code == 200) {
                                if (needLogSucceed()) {
                                    logger.info("Write InfluxDB {} succeed", tableName);
                                }
                            } else if(needLogError()) {
                                logger.warn("Write InfluxDB {} failed, result={}", tableName, ret.value);
                            }
                        } else if (needLogError()) {
                            logger.warn("Write InfluxDB {} failed", tableName, ret.error);
                        }
                    } else if (needLogError()) {
                        logger.warn("Write InfluxDB {} failed", tableName, ex);
                    }
                }
            },futuredHttpClient.system.dispatcher());
    }

    private String format(String str) {
        return "".equals(str) ? "blank" : str;
    }

    private boolean needLogError() {
        long now = System.currentTimeMillis();
        if (now - lastLogErrorTime > 600_000) {
            lastLogErrorTime = now;
            lastLogSucceedTime = 0;
            return true;
        } else {
            return false;
        }
    }

    private boolean needLogSucceed() {
        long now = System.currentTimeMillis();
        if (lastLogSucceedTime == 0) {
            lastLogSucceedTime = now;
            lastLogErrorTime = 0;
            return true;
        } else {
            return false;
        }
    }
}
