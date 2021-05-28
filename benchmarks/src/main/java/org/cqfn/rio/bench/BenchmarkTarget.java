/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */
package org.cqfn.rio.bench;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import io.reactivex.FlowableOnSubscribe;
import java.util.Random;
import io.reactivex.FlowableEmitter;
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


/**
 * Benchmark target provider.
 */
public interface BenchmarkTarget {

    /**
     * Read file as a stream.
     * @param path File path
     * @return Stream of byte buffers
     */
    Publisher<ByteBuffer> read(Path path);

    /**
     * Write a stream to file.
     * @param path File path
     * @param data String of byte buffers
     * @return Future
     */
    CompletableFuture<?> write(Path path, Publisher<ByteBuffer> data);

        /**
         * Random buffer generator.
         */
        public static final class BufferSourceGenerator implements FlowableOnSubscribe<ByteBuffer> {

            /**
             * Random number generator.
             */
            private final Random RNG = new Random();

            /**
             * Amount of buffers.
             */
            private final AtomicLong cnt;

            /**
             * Buffer length.
             */
            private final int len;

            /**
             * New test source.
             *
             * @param source Source buffer
             * @param size   Amount of buffers
             */
            BufferSourceGenerator(final long size, int length) {
                this.cnt = new AtomicLong(size);
                this.len = length;
            }

            @Override
            public void subscribe(FlowableEmitter<ByteBuffer> emitter) throws Exception {
                final byte[] buffer = new byte[this.len];
                while (!emitter.isCancelled()) {
                    for (long req = emitter.requested(); req > 0; req--) {
                        final long val = this.cnt.decrementAndGet();
                        if (val >= 0L) {
                            RNG.nextBytes(buffer);
                            emitter.onNext(ByteBuffer.wrap(buffer));
                        } else {
                            emitter.onComplete();
                            return;
                        }
                    }
                }
            }
        }
}
