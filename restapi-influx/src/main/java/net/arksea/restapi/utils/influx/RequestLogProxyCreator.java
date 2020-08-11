package net.arksea.restapi.utils.influx;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public class RequestLogProxyCreator implements IRequestLogProxyCreator {
    @Override
    public <T extends  U,U> T newInstance(IRequestLogger requestLogger, T target, Class<U> targetInterface) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> proxyClass = Proxy.getProxyClass(classLoader, targetInterface);
            Constructor con = proxyClass.getConstructor(InvocationHandler.class);
            return (T)con.newInstance(new RequestLogProxy(requestLogger, target, targetInterface));
        } catch (Exception e) {
            throw new RuntimeException("创建代理类失败", e);
        }
    }
}
