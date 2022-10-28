package net.arksea.restapi.utils.influx;

import javax.servlet.ServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * b3-propagation
 * https://github.com/openzipkin/b3-propagation
 */
public class HttpTraceInfo {
    static final String TRACE_INFO_ATTRIBUTE_NAME = "-restapi-trace-info";
    private String requestName = "unknown";
    private String requestGroup  = "unknown";
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String sampled;
    private String flags;
    private Map<String, String> tags = new HashMap<>();

    public static HttpTraceInfo get(final ServletRequest req) {
        return (HttpTraceInfo)req.getAttribute(HttpTraceInfo.TRACE_INFO_ATTRIBUTE_NAME);
    }

    public static String makeSpanId() {
        return UUID.randomUUID().toString().replaceAll("-","").substring(0,16);
    }

    public String getRequestName() {
        return requestName;
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public String getRequestGroup() {
        return requestGroup;
    }

    public void setRequestGroup(String requestGroup) {
        this.requestGroup = requestGroup;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public String getSampled() {
        return sampled;
    }

    public void setSampled(String sampled) {
        this.sampled = sampled;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void addTags(Map<String, String> tags) {
        this.tags.putAll(tags);
    }

    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }

    public boolean needSampled() {
        return "1".equals(sampled);
    }
}
