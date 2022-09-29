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
    /**
     * @param request 请求对象
     * @param requestBody 当isAlwaysWrapRequest为true时会传入此参数，为false时此参数为null
     * @return
     */
    boolean needTrace(ServletRequest request, String requestBody);
    //当此配置为true时， filter总会对请求进行包装，
    //并将获取到的requestBody作为needTrace的参数传入
    //因为所有请求都多进行了一次内存拷贝，对性能会有一定影响，请注意！
    default boolean isAlwaysWrapRequest() {
        return false;
    }
    String makeExceptionResult(ServletRequest request, HttpStatus status, Throwable ex);
}
