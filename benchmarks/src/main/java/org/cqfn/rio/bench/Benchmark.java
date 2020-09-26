package org.cqfn.rio.bench;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.reactivestreams.Publisher;

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
        final BenchmarkTarget target =
            (BenchmarkTarget) Class.forName(cli.getOptionValue('p')).getConstructor().newInstance();
        final Path src = optPath(cli, 's');
        final Path dst = optPath(cli, 'd');
        final int warmup = Integer.parseInt(cli.getOptionValue('w'));
        final int count = Integer.parseInt(cli.getOptionValue('c'));
        final int size = Optional.ofNullable(cli.getOptionValue("size")).map(Integer::parseInt).orElse(0);
        for (int wm = 0; wm < warmup; wm++) {
            consumer(target, dst, producer(target, src, size)).join();
        }
        final Stats stats = new Stats(count);
        for (int pos = 0; pos < count; pos++) {
            final long start = System.nanoTime();
            consumer(target, dst, producer(target, src, size)).join();
            final long end = System.nanoTime();
            stats.put(pos, end - start);
        }
        System.out.printf(
            "%s: %s\n",
            target.getClass().getSimpleName(),
            stats.format(TimeUnit.MILLISECONDS)
        );
        System.exit(0);
    }

    private static Path optPath(final CommandLine cli, final char opt) {
        final String val = cli.getOptionValue(opt);
        if (val == null) {
            return null;
        }
        return Paths.get(val);
    }

    private static final byte[] RANDOM_KB;

    static {
        RANDOM_KB = new byte[1024];
        new SecureRandom().nextBytes(RANDOM_KB);
    }

    private static Publisher<ByteBuffer> producer(final BenchmarkTarget target,
        final Path path, final int size) {
        if (path == null) {
            final AtomicInteger cnt = new AtomicInteger(size);
            return Flowable.generate(
                emitter -> {
                    final int next = cnt.decrementAndGet();
                    if (next >= 0) {
                        emitter.onNext(ByteBuffer.wrap(RANDOM_KB));
                    } else {
                        emitter.onComplete();
                    }
                }
            );
        }
        return target.read(path);
    }

    private static CompletableFuture<?> consumer(final BenchmarkTarget target, final Path path,
        final Publisher<ByteBuffer> stream) {
        if (path == null) {
            return Flowable.fromPublisher(stream)
                .ignoreElements()
                .to(CompletableInterop.await())
                .toCompletableFuture();
        }
        return target.write(path, stream);
    }
}
