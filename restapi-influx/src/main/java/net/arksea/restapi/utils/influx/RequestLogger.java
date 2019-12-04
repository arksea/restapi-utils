package net.arksea.restapi.utils.influx;

import akka.dispatch.OnComplete;
import net.arksea.httpclient.asker.FuturedHttpClient;
import net.arksea.httpclient.asker.HttpResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by xiaohaixing on 2017/5/20.
 */
public class RequestLogger implements IRequestLogger {

    private static final Logger logger = LogManager.getLogger(RequestLogger.class);
    private final FuturedHttpClient futuredHttpClient;
    private static final int timeout = 10000;
    private final AtomicLong logCount = new AtomicLong(0L);
    private final AtomicLong logSucceedCount = new AtomicLong(0L);
    private volatile String error;

    private final Map<Pair<String,String>,Counter> countMap = new ConcurrentHashMap<>();

    private final String dbUrl;
    private final String tableName;

    class Counter {
        AtomicLong requestCount = new AtomicLong(0);
        AtomicLong respondTime = new AtomicLong(0);  //请求响应时间
        AtomicLong respond2xx = new AtomicLong(0);
        AtomicLong respond3xx = new AtomicLong(0);
        AtomicLong respond4xx = new AtomicLong(0);
        AtomicLong respond5xx = new AtomicLong(0);
    }

    public RequestLogger(String dbUrl, FuturedHttpClient futuredHttpClient) {
        this.futuredHttpClient = futuredHttpClient;
        this.dbUrl = dbUrl;
        this.tableName = "request";
    }

    public RequestLogger(String tableName, String dbUrl, FuturedHttpClient futuredHttpClient) {
        this.futuredHttpClient = futuredHttpClient;
        this.dbUrl = dbUrl;
        this.tableName = tableName;
    }

    public double getLogSuccessRate() {
        long c = logCount.get();
        long s = logSucceedCount.get();
        if (c > 0) {
            return s*1.0 / c;
        } else {
            return 0.0;
        }
    }
    public String getLastError() {
        return error;
    }
    public long getLogCount() {
        return logCount.get();
    }

    @Override
    public void respond(String uri, String group, int status, long respondTime) {
        logger.trace("Trace RequestLogger.respond: uri={},group={},status={},time={}",uri,group,status,respondTime);
        Counter counter = getCounter(uri, group);
        counter.respondTime.addAndGet(respondTime);
        if (status >= 300 && status < 400) {
            counter.respond3xx.incrementAndGet();
        } else if (status >= 400 && status < 500) {
            counter.respond4xx.incrementAndGet();
        } else if (status >= 500 && status < 600) {
            counter.respond5xx.incrementAndGet();
        } else {
            counter.respond2xx.incrementAndGet();
        }
    }

    @Override
    public void request(String uri, String group) {
        logger.trace("Trace RequestLogger.request: uri={},group={}",uri,group);
        Counter counter = getCounter(uri, group);
        counter.requestCount.incrementAndGet();
    }

    private Counter getCounter(String name, String group) {
        Pair<String,String> key = Pair.of(name,group);
        Counter counter = countMap.get(key);
        if (counter == null) {
            synchronized (this) {
                counter = countMap.get(key);
                if (counter==null) {
                    counter = new Counter();
                    countMap.put(key,counter);
                }
            }
        }
        return counter;
    }

    @Override
    public void writeLogs() {
        HttpPost post = new HttpPost(dbUrl);
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
                    .append(",name=").append(name.toString())
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
        logCount.incrementAndGet();
        futuredHttpClient.ask(post, "request", timeout).onComplete(
            new OnComplete<HttpResult>() {
                public void onComplete(Throwable ex, HttpResult ret) throws Throwable {
                    if (ex == null) {
                        if (ret.error == null) {
                            int code = ret.response.getStatusLine().getStatusCode();
                            if (code == 204 || code == 200) {
                                logSucceedCount.incrementAndGet();
                            } else {
                                error = ret.value;
                            }
                        } else {
                            error = ret.error.getMessage();
                        }
                    }
                }
            },futuredHttpClient.system.dispatcher());
    }

    private String format(String str) {
        return "".equals(str) ? "blank" : str;
    }
}
