package net.arksea.restapi.utils.influx;

public class TimerImpl implements Timer {

    @Override
    public long nowNano() {
        return System.currentTimeMillis() * 1000_000L;
    }

    @Override
    public long nowMicro() {
        return System.currentTimeMillis() * 1000L;
    }

    @Override
    public long nowMilli() {
        return System.currentTimeMillis();
    }
}
