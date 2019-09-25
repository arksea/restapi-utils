package net.arksea.restapi.utils.influx;

/**
 *
 * Created by xiaohaixing on 2018/4/2.
 */
public interface IRequestLogger {
    void respond(String uri, String group, int status, long respondTime);

    void request(String uri, String group);

    void writeLogs();
}
