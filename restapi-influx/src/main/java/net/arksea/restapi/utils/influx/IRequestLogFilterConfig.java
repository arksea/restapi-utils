package net.arksea.restapi.utils.influx;

import org.springframework.http.HttpStatus;

import javax.servlet.ServletRequest;

/**
 * Http请求统计分组与忽略判断接口，
 * 分组使用两个维度：name与group
 * Created by xiaohaixing on 2018/4/4.
 */
public interface IRequestLogFilterConfig {
    String getName(ServletRequest request);
    String getGroup(ServletRequest request);
    boolean ignore(ServletRequest request);
    //满足条件的请求，使用DEBUG级别记录日志
    boolean logDebugLevel(HttpStatus status, Throwable ex);
    boolean needTrace(ServletRequest request);
    String makeExceptionResult(ServletRequest request, HttpStatus status, Throwable ex);
}
