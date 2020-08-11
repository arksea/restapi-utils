package net.arksea.restapi.utils.influx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public class RequestLogProxy<T extends U, U> implements InvocationHandler {
    private static final Logger logger = LogManager.getLogger(RequestLogProxy.class);
    private final T target;
    private Class<U> targetInterface;
    private final IRequestLogger requestLogger;
    private final Set<String> targetMethods;
    private final String interfaceName;

    public RequestLogProxy(IRequestLogger requestLogger, T target, Class<U> targetInterface) {
        this.target = target;
        this.targetInterface = targetInterface;
        this.interfaceName = targetInterface.getSimpleName();
        this.requestLogger = requestLogger;
        targetMethods = new HashSet<>();
        Method[] methods = targetInterface.getMethods();
        for (Method m : methods) {
            logger.trace("Target Interface {} method = {}", interfaceName, m.getName());
            targetMethods.add(m.getName());
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (needLog(methodName)) {
            long start = System.currentTimeMillis();
            String group = getGroupByMethodName(methodName);
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

    protected String getGroupByMethodName(String methodName) {
        return methodName;
    }

    private boolean needLog(String methodName) {
        return targetMethods.contains(methodName);
    }
}
