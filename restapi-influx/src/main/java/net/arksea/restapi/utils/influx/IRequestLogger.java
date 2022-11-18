package net.arksea.restapi.utils.influx;

import zipkin2.Span;

/**
 *
 * Created by xiaohaixing on 2018/4/2.
 */
public interface IRequestLogger {
    default void monitor(String name, String group, int status, long duration) {}
    default void trace(Span span) {}
    default void writeLogs() {}
}
