package net.arksea.restapi.utils.influx;

import zipkin2.Span;

/**
 *
 * Created by xiaohaixing on 2018/4/2.
 */
public interface IRequestLogger {
    void monitor(String name, String group, int status, long duration);
    void trace(Span span);
    void writeLogs();
}
