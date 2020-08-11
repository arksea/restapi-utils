package net.arksea.restapi.utils.influx;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * 默认分组与过滤器
 * 1、所有接口方法都记录
 * 2、组名直接使用方法名
 * Create by xiaohaixing on 2020/8/11
 */
public class DefaultRequestLogFilter implements IRequestLogFilter {
    private final Set<String> targetMethods;
    public DefaultRequestLogFilter(Class targetInterface) {
        targetMethods = new HashSet<>();
        Method[] methods = targetInterface.getMethods();
        for (Method m : methods) {
            targetMethods.add(m.getName());
        }
    }
    public String getGroupByMethodName(String methodName) {
        return methodName;
    }

    public boolean needLog(String methodName) {
        return targetMethods.contains(methodName);
    }
}
