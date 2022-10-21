package net.arksea.restapi.utils.influx;

import java.util.Map;

/**
 * b3-propagation
 * https://github.com/openzipkin/b3-propagation
 */
public class HttpTraceInfo {
    private String requestName = "unknown";
    private String requestGroup  = "unknown";
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String sampled;
    private String flags;
    private Map<String, String> tags;

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

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public boolean needSampled() {
        return "1".equals(sampled);
    }
}
