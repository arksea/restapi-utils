package net.arksea.restapi.utils.influx;

/**
 *
 * Created by xiaohaixing on 2018/12/20.
 */
public interface Timer {
    long nowNano();
    long nowMicro();
    long nowMilli();
}
