package org.cqfn.rio.bench;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 *
 * @since
 */
public final class Stats {

    private final long[] values;
    private final AtomicLong bytes = new AtomicLong();
    private final int par;

    public Stats(final int size, final int par) {
        this.values = new long[size];
        this.par = par;
    }

    public void put(final int pos, final long nano) {
        this.values[pos] = nano;
    }

    public void putBytes(final int bts) {
        this.bytes.addAndGet(bts);
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

    private int count() {
        return this.values.length;
    }

    private long min() {
        return LongStream.of(this.values).min().orElse(0L);
    }

    private long max() {
        return LongStream.of(this.values).max().orElse(0L);
    }

    private String size() {
        return bytesFormat(this.bytes.get());
    }

    private String speed() {
       return bytesFormat((long)(1.0 * TimeUnit.SECONDS.toNanos(1) * this.bytes.get() / this.sum())) + "/s";
    }

    public void print(final TimeUnit unit, final Output out) {
        final long conv = unit.toNanos(1);
        out.print(
            this.par,
            this.count(),
            this.size(),
            this.sum() / conv,
            this.min() / conv,
            this.max() / conv,
            this.avg() / conv,
            this.stddev() / conv,
            this.stderr() / conv,
            speed()
        );
    }

    private static String bytesFormat(final long bytes) {
        double bts = bytes;
        String suffix = "B";
        if (bts >= 1024) {
            bts /= 1024;
            suffix = "KB";
        }
        if (bts >= 1024) {
            bts /= 1024;
            suffix = "MB";
        }
        if (bts >= 1024) {
            bts /= 1024;
            suffix = "GB";
        }
        return String.format("%.2f %s", bts, suffix);
    }

    public interface Output {
        void print(int par, int count, String size, double sum, double min, double max,
                double avg, double stddev, double stderr, String speed);
    }

    public static final class StdOut implements Output {

        private static final DecimalFormat DFMT = new DecimalFormat("0.0000");

        private final PrintStream out;
        private final String prefix;

        public StdOut(final String prefix) {
            this(System.out, prefix);
        }

        public StdOut(final PrintStream out, final String prefix) {
            this.out = out;
            this.prefix = prefix;
        }

        @Override
        public void print(int par, int cnt, String size, double sum, double min, double max,
                double avg, double stddev, double stderr, String speed) {
            this.out.format(
                "%s: PAR=%d CNT=%d SIZE=%s SUM=%s MIN=%s MAX=%s AVG=%s STDDEV=%s STDERR=%s SPEED=%s\n",
                this.prefix,
                par, cnt, size, DFMT.format(sum), DFMT.format(min), DFMT.format(max),
                DFMT.format(avg), DFMT.format(stddev), DFMT.format(stderr), speed
            );
        }
    }

    public static final class MarkdownOut implements Output {

        private static final DecimalFormat DFMT = new DecimalFormat("0.0000");

        private final PrintStream out;
        private final String prefix;

        public MarkdownOut(final PrintStream out, final String prefix) {
            this.out = out;
            this.prefix = prefix;
        }

        @Override
        public void print(int par, int cnt, String size, double sum, double min, double max,
                double avg, double stddev, double stderr, String speed) {
            this.out.format(
                "%s | %d | %s | %d | %s | %s | %s | %s |\n",
                fixedLengthString(this.prefix.replaceAll("Target", ""), 7),
                cnt, size, par,
                DFMT.format(avg), DFMT.format(stddev), DFMT.format(stderr),
                speed
            );
        }
    }

    private static String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }
}
