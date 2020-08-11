package net.arksea.restapi.utils.influx;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by xiaohaixing on 2017/5/20.
 */
public abstract class AbstractRequestLogger implements IRequestLogger {

    private static final Logger logger = LogManager.getLogger(AbstractRequestLogger.class);
    protected final Map<Pair<String,String>,Counter> countMap = new ConcurrentHashMap<>();

    protected class Counter {
        public AtomicLong requestCount = new AtomicLong(0);
        public AtomicLong respondTime = new AtomicLong(0);  //请求响应时间
        public AtomicLong respond2xx = new AtomicLong(0);
        public AtomicLong respond3xx = new AtomicLong(0);
        public AtomicLong respond4xx = new AtomicLong(0);
        public AtomicLong respond5xx = new AtomicLong(0);
    }

    @Override
    public void respond(String uri, String group, int status, long respondTime) {
        logger.trace("Trace AbstractRequestLogger.respond: uri={},group={},status={},time={}",uri,group,status,respondTime,new Exception("trace"));
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
        logger.trace("Trace AbstractRequestLogger.request: uri={},group={}",uri,group,new Exception("trace"));
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
}
