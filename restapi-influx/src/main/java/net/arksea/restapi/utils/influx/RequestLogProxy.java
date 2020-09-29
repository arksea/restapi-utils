package net.arksea.restapi.utils.influx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public class RequestLogProxy<T extends U, U> implements InvocationHandler {
    private final T target;
    private final String logName;
    private final IRequestLogger requestLogger;
    private final IRequestLogClassifier logClassifier;

    public RequestLogProxy(IRequestLogger requestLogger, T target, String logName, IRequestLogClassifier logClassifier) {
        this.target = target;
        this.logClassifier = logClassifier;
        this.logName = logName;
        this.requestLogger = requestLogger;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (logClassifier.needLog(methodName)) {
            long start = System.currentTimeMillis();
            String group = logClassifier.getGroupByMethodName(methodName);
            try {
                requestLogger.request(logName, group);
                Object ret = method.invoke(target, args);
                long useTime = System.currentTimeMillis() - start;
                requestLogger.respond(logName, group, 200, useTime);
                return ret;
            } catch (Throwable ex) {
                long useTime = System.currentTimeMillis() - start;
                requestLogger.respond(logName, group, 500, useTime);
                throw ex;
            }
        } else {
            return method.invoke(target, args);
        }
    }
}
