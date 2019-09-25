package net.arksea.restapi.utils.influx;

import net.arksea.restapi.RestExceptionHandler;
import org.springframework.http.HttpStatus;

import javax.servlet.ServletRequest;

/**
 *
 * Created by xiaohaixing on 2018/4/4.
 */
public interface IRequestGroup {
    String getName(ServletRequest request);
    String getGroup(ServletRequest request);
    boolean ignore(ServletRequest request);
    String getRequestId(ServletRequest request);
    default boolean logDebugLevel(HttpStatus status, Throwable ex) {
        return RestExceptionHandler.logDebugLevel(status, ex);
    }
}
