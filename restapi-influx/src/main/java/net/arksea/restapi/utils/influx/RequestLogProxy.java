package net.arksea.restapi.utils.influx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public class RequestLogProxy<T extends U, U> implements InvocationHandler {
    private final T target;
    private Class<U> targetInterface;
    private final IRequestLogger requestLogger;
    private final String interfaceName;
    private IRequestLogFilter requestLogFilter;

    public RequestLogProxy(IRequestLogger requestLogger, T target, Class<U> targetInterface, IRequestLogFilter requestLogFilter) {
        this.target = target;
        this.targetInterface = targetInterface;
        this.requestLogFilter = requestLogFilter;
        this.interfaceName = targetInterface.getSimpleName();
        this.requestLogger = requestLogger;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (requestLogFilter.needLog(methodName)) {
            long start = System.currentTimeMillis();
            String group = requestLogFilter.getGroupByMethodName(methodName);
            try {
                requestLogger.request(interfaceName, group);
                Object ret = method.invoke(target, args);
                long useTime = System.currentTimeMillis() - start;
                requestLogger.respond(interfaceName, group, 200, useTime);
                return ret;
            } catch (Throwable ex) {
                long useTime = System.currentTimeMillis() - start;
                requestLogger.respond(interfaceName, group, 500, useTime);
                throw ex;
            }
        } else {
            return method.invoke(target, args);
        }
    }
}
