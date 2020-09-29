package net.arksea.restapi.utils.influx;

/**
 * 接口请求记录分组与过滤器
 * Create by xiaohaixing on 2020/8/11
 */
public interface IRequestLogClassifier {
    //组名：根据方法名决定如何进行统计分组
    default String getGroupByMethodName(String methodName) {
        return methodName;
    }

    //过滤器：根据名字决定哪些方法需要记录
    default boolean needLog(String methodName) {
        return true;
    }
}
