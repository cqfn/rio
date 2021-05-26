/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.bench;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Collections;
import java.util.List;
import io.reactivex.BackpressureStrategy;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.reactivestreams.Publisher;

import java.lang.management.*;

/**
 * Benchmarks CLI app.
 */
public final class Benchmark {

    /**
     * CLI options.
     */
    private static final Options OPTS = new Options()
        .addOption(
            Option.builder("s")
                .longOpt("source")
                .hasArg()
                .desc("Source file path")
                .type(String.class)
                .build()
        ).addOption(
            Option.builder("p")
                .longOpt("provider")
                .desc("IO provider class name")
                .hasArg()
                .required()
                .build()
        ).addOption(
            Option.builder("d")
                .longOpt("destination")
                .hasArg()
                .desc("Destination file path")
                .type(String.class)
                .build()
        ).addOption(
            Option.builder("w")
                .longOpt("warm-up")
                .hasArg()
                .desc("Warm up count")
                .type(Integer.class)
                .build()
        ).addOption(
            Option.builder("c")
                .longOpt("count")
                .hasArg()
                .desc("Count to repeat")
                .type(Integer.class)
                .build()
        ).addOption(
            Option.builder()
                .longOpt("size")
                .hasArg()
                .desc("Size to generate")
                .type(Integer.class)
                .build()
        ).addOption(
            Option.builder("l")
                .hasArg()
                .desc("Parallel operations")
                .type(Integer.class)
                .build()
        ).addOption(
            Option.builder()
                .longOpt("dir")
                .hasArg()
                .desc("Test dir")
                .type(String.class)
                .build()
        ).addOption(
            Option.builder()
                .longOpt("memory")
                .desc("Analyze memory usage, not performance")
                .type(Boolean.class)
                .build()
        );

    public static void main(final String... args) throws Exception {
        final CommandLine cli;
        try {
            cli = new DefaultParser().parse(OPTS, args);
        } catch (final ParseException err) {
            new HelpFormatter().printHelp("Benchmarks", OPTS);
            System.exit(1);
            return;
        }

        String name = ManagementFactory.getRuntimeMXBean().getName();
        System.err.printf("started: %s\n", name);

        final BenchmarkTarget target =
            (BenchmarkTarget) Class.forName(cli.getOptionValue('p')).getConstructor().newInstance();
        final String dir = cli.getOptionValue("dir");
        final Path src = optPath(cli, 's', dir);
        final Path dst = optPath(cli, 'd', dir);
        final int warmup = Integer.parseInt(cli.getOptionValue('w'));
        final int count = Integer.parseInt(cli.getOptionValue('c'));
        final int par = Optional.ofNullable(cli.getOptionValue('l')).map(Integer::parseInt).orElse(1);
        final int size = Optional.ofNullable(cli.getOptionValue("size")).map(Integer::parseInt).orElse(0);
        final boolean memory = Optional.ofNullable(cli.getOptionValue("memory")).map(Boolean::parseBoolean).orElse(false);
        // warmup
        for (int wm = 0; wm < warmup; wm++) {
            consume(target, dst, producer(target, src, size, par)).join();
        }
        // benchmark
        final AtomicBoolean run = new AtomicBoolean(false);
        ScheduledExecutorService profiler = null;
        if (memory) {
            final MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
            profiler = Executors.newSingleThreadScheduledExecutor();
            profiler.scheduleAtFixedRate(
                () -> {
                    if (run.get()) {
                        MemoryUsage usage = mbean.getHeapMemoryUsage();
                        System.out.println(usage.getUsed());
                    }
                },
                1, 1, TimeUnit.MILLISECONDS
            );
            Thread.sleep(5);
        }
        for (int pos = 0; pos < count; pos++) {
            System.gc();
            run.set(true);
            final long start = System.nanoTime();
            consume(target, dst, producer(target, src, size, par)).join();
            final long end = System.nanoTime();
            run.set(false);
            if (!memory) {
                System.out.println(end - start);
            }
        }
        if (profiler != null) {
            profiler.shutdown();
        }
        System.exit(0);
    }

    private static Path optPath(final CommandLine cli, final char opt,
        final String dir) {
        final String val = cli.getOptionValue(opt);
        if (val == null) {
            return null;
        }
        return Paths.get(dir, val);
    }

    private static List<Publisher<ByteBuffer>> producer(final BenchmarkTarget target,
        final Path path, int size, int par) {
        return IntStream.range(0, par).mapToObj(x -> producer(target, size, path))
            .collect(Collectors.toList());
    }

    private static Publisher<ByteBuffer> producer(final BenchmarkTarget target, int size,
        final Path path) {
        if (path == null) {
            return Flowable.create(
                new BenchmarkTarget.BufferSourceGenerator(size, 8*1024),
                BackpressureStrategy.ERROR
            );
        }
        return target.read(path);
    }

    private static CompletableFuture<?> consume(final BenchmarkTarget target,
        final Path path, final List<Publisher<ByteBuffer>> sources) {
        final AtomicInteger cnt = new AtomicInteger(0);
        return CompletableFuture.allOf(sources.stream().map(Benchmark.consumer(target, path, cnt))
            .collect(Collectors.toList()).toArray(new CompletableFuture<?>[0]));
    }

    private static Function<Publisher<ByteBuffer>, CompletableFuture<?>> consumer(
            final BenchmarkTarget target, final Path path, final AtomicInteger cnt) {
        if (path == null) {
            return stream -> Flowable.fromPublisher(stream)
                .ignoreElements()
                .to(CompletableInterop.await())
                .toCompletableFuture();
        }
        return stream -> target.write(Path.of(path.toString() + cnt.incrementAndGet()), stream);
    }

    private static final class SizeStats {
        private final AtomicLong bts = new AtomicLong();
        private final AtomicLong timer = new AtomicLong();

        void put(final long size) {
            this.bts.addAndGet(size);
        }

        void start() {
            this.timer.set(System.nanoTime());
        }

        void stop() {
            final long stop = System.nanoTime();
            this.timer.updateAndGet(strt -> stop - strt);
        }

        @Override
        public String toString() {
            final long total = this.bts.get();
            final long time = this.timer.get(); // nanoseconds
            final long bps = (long) (1_000_000_000.0 * total / time);
            return String.format("%s (%s/s)", bytesFormat(total), bytesFormat(bps));
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
    }
}
