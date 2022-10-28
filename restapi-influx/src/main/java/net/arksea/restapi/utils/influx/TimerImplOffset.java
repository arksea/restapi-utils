package net.arksea.restapi.utils.influx;

import java.math.BigDecimal;

/**
 * 引用自stackoverflow, kangcz的回复：
 * https://stackoverflow.com/questions/1712205/current-time-in-microseconds-in-java
 * Created by xiaohaixing on 2018/12/20.
 */
public class TimerImplOffset implements Timer {

    private final long offset;

    private static long calculateOffset() {
        final long nano = System.nanoTime();
        final long nanoFromMilli = System.currentTimeMillis() * 1_000_000;
        return nanoFromMilli - nano;
    }

    public TimerImplOffset() {
        final int count = 500;
        BigDecimal offsetSum = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            offsetSum = offsetSum.add(BigDecimal.valueOf(calculateOffset()));
        }
        offset = offsetSum.divide(BigDecimal.valueOf(count)).longValue();
    }

    @Override
    public long nowNano() {
        return offset + System.nanoTime();
    }

    @Override
    public long nowMicro() {
        return (offset + System.nanoTime()) / 1000;
    }

    @Override
    public long nowMilli() {
        return System.currentTimeMillis();
    }
}
