package net.arksea.restapi.utils.influx;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public class RequestLogProxyCreator implements IRequestLogProxyCreator {
    private final IRequestLogger requestLogger;
    private final IRequestLogClassifier logClassifier;
    public RequestLogProxyCreator(IRequestLogger requestLogger) {
        this.requestLogger = requestLogger;
        this.logClassifier = null;
    }
    public RequestLogProxyCreator(IRequestLogger requestLogger, IRequestLogClassifier logClassifier) {
        this.requestLogger = requestLogger;
        this.logClassifier = logClassifier;
    }
    @Override
    public <T extends  U,U> T newInstance(T target, Class<U> targetInterface) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> proxyClass = Proxy.getProxyClass(classLoader, targetInterface);
            Constructor con = proxyClass.getConstructor(InvocationHandler.class);
            IRequestLogClassifier classifier = logClassifier == null ? new DefaultRequestLogClassifier(targetInterface) : logClassifier;
            return (T)con.newInstance(new RequestLogProxy(requestLogger, target, targetInterface.getSimpleName(), classifier));
        } catch (Exception e) {
            throw new RuntimeException("创建代理类失败", e);
        }
    }
    @Override
    public <T extends  U,U> T newInstance(T target, Class<U> targetInterface, String logName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> proxyClass = Proxy.getProxyClass(classLoader, targetInterface);
            Constructor con = proxyClass.getConstructor(InvocationHandler.class);
            IRequestLogClassifier filter = logClassifier == null ? new DefaultRequestLogClassifier(targetInterface) : logClassifier;
            return (T)con.newInstance(new RequestLogProxy(requestLogger, target, logName, filter));
        } catch (Exception e) {
            throw new RuntimeException("创建代理类失败", e);
        }
    }
}
