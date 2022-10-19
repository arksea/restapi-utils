package net.arksea.restapi.utils.influx;

import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;

/**
 * Http请求统计分组与忽略判断接口，
 * 分组使用两个维度：name与group
 * Created by xiaohaixing on 2018/4/4.
 */
public interface IHttpRequestLogFilterConfig {
    String getName(HttpServletRequest request, Object handledWrapper);
    String getGroup(HttpServletRequest request, Object handledWrapper);
    boolean ignore(HttpServletRequest request);
    //满足条件的请求，使用DEBUG级别记录日志
    boolean logDebugLevel(HttpStatus status, Throwable ex);

    /**
     * 当isAlwaysWrapRequest为true时会掉用此方法，可以在此做必要的预处理
     */
    default Object afterRequestWrapper(HttpServletRequest request, RequestWrapper wrapper) {
        return wrapper;
    }
    /**
     * @param request 请求对象
     * @return
     */
    boolean needTrace(HttpServletRequest request, Object handledWrapper);
    //当此配置为true时， filter总会对请求进行包装，
    //并将获取到的requestBody作为needTrace的参数传入
    //因为所有请求都多进行了一次内存拷贝，对性能会有一定影响，请注意！
    default boolean isAlwaysWrapRequest() {
        return false;
    }
    String makeExceptionResult(HttpServletRequest request, HttpStatus status, Throwable ex);
}
