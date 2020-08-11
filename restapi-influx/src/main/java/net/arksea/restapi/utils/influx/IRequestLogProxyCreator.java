package net.arksea.restapi.utils.influx;

/**
 * Create by xiaohaixing on 2020/8/11
 */
public interface IRequestLogProxyCreator {
    <T extends U, U> T newInstance(T target, Class<U> targetInterface);
}
