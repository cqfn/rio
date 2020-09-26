package org.cqfn.rio.bench;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 *
 * @since
 */
public final class Stats {

    private final long[] values;

    public Stats(final int size) {
        this.values = new long[size];
    }

    public void put(final int pos, final long nano) {
        this.values[pos] = nano;
    }

    private long sum() {
        long sum = 0;
        for (final long milli : this.values) {
            sum += milli;
        }
        return sum;
    }

    private double avg() {
        return 1.0 * this.sum() / this.values.length;
    }

    private double stddev() {
        double sum = 0;
        final double avg = this.avg();
        for (final long milli : this.values) {
            sum += Math.pow(milli - avg, 2);
        }
        return Math.sqrt(sum / (this.values.length - 1));
    }

    private double stderr() {
        final double stddev = this.stddev();
        return stddev / Math.sqrt(this.values.length);
    }

    private int size() {
        return this.values.length;
    }

    private long min() {
        return LongStream.of(this.values).min().orElse(0L);
    }

    private long max() {
        return LongStream.of(this.values).max().orElse(0L);
    }

    public String format(final TimeUnit unit) {
        final long conv = unit.toNanos(1);
        return String.format(
            "CNT=%d SUM=%d MIN=%d MAX=%d AVG=%f STDDEV=%f STDERR=%f",
            this.size(),
            this.sum() / conv,
            this.min() / conv,
            this.max() / conv,
            this.avg() / conv,
            this.stddev() / conv,
            this.stderr() / conv
        );
    }
}
