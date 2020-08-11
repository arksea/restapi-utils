package net.arksea.restapi.utils.influx;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public class RequestLogProxyCreator implements IRequestLogProxyCreator {
    private IRequestLogger requestLogger;
    private IRequestLogFilter requestLogFilter;
    public RequestLogProxyCreator(IRequestLogger requestLogger) {
        this.requestLogger = requestLogger;
        this.requestLogFilter = null;
    }
    public RequestLogProxyCreator(IRequestLogger requestLogger, IRequestLogFilter requestLogFilter) {
        this.requestLogger = requestLogger;
        this.requestLogFilter = requestLogFilter;
    }
    @Override
    public <T extends  U,U> T newInstance(T target, Class<U> targetInterface) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> proxyClass = Proxy.getProxyClass(classLoader, targetInterface);
            Constructor con = proxyClass.getConstructor(InvocationHandler.class);
            IRequestLogFilter filter = requestLogFilter == null ? new DefaultRequestLogFilter(targetInterface) : requestLogFilter;
            return (T)con.newInstance(new RequestLogProxy(requestLogger, target, targetInterface, filter));
        } catch (Exception e) {
            throw new RuntimeException("创建代理类失败", e);
        }
    }
}
