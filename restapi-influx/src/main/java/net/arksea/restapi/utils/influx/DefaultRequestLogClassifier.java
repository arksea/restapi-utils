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
public class DefaultRequestLogClassifier implements IRequestLogClassifier {
    private final Set<String> targetMethods;
    public DefaultRequestLogClassifier(Class targetInterface) {
        targetMethods = new HashSet<>();
        Method[] methods = targetInterface.getMethods();
        for (Method m : methods) {
            targetMethods.add(m.getName());
        }
    }
    @Override
    public String getGroupByMethodName(String methodName) {
        if (methodName.startsWith("find") || methodName.startsWith("get") || methodName.startsWith("select")) {
            return "select,method="+methodName;
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "delete,method=" + methodName;
        } else if (methodName.startsWith("add") || methodName.startsWith("insert") || methodName.startsWith("put")) {
            return "insert,method="+methodName;
        } else if (methodName.startsWith("update") || methodName.startsWith("save") || methodName.startsWith("set")) {
            return "update,method="+methodName;
        } else {
            return "other,method="+methodName;
        }
    }
    @Override
    public boolean needLog(String methodName) {
        return targetMethods.contains(methodName);
    }
}
