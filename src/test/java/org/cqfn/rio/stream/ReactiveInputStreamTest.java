/*
 * MIT License
 *
 * Copyright (c) 2020 cqfn.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights * to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package org.cqfn.rio.stream;

import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.cqfn.rio.Buffers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Test for {@link ReactiveInputStream}.
 * @since 0.4
 */
class ReactiveInputStreamTest {

    @Test
    void transfersData() throws ExecutionException, InterruptedException {
        final byte[] bytes = "abc123".getBytes();
        MatcherAssert.assertThat(
            LengthSubscriber.of(
                new ReactiveInputStream(
                    new ByteArrayInputStream(bytes)
                ).read(Buffers.Standard.K1)
            ).get(),
            new IsEqual<>(bytes.length)
        );
    }

    @Test
    void countsDataWithPipedStreams() throws Exception {
        final CompletableFuture<Integer> length = new CompletableFuture<>();
        final byte[] data = "xyz098".getBytes();
        try (PipedOutputStream newout = new PipedOutputStream()) {
            new ReactiveInputStream(new PipedInputStream(newout))
                .read(Buffers.Standard.K1)
                .subscribe(new LengthSubscriber(length));
            newout.write(data, 0, data.length);
        }
        MatcherAssert.assertThat(
            length.join(),
            new IsEqual<>(data.length)
        );
    }

    @Test
    @Timeout(10)
    void transferDataWithPipedStreams() throws Exception {
        final byte[] data = "any bytes".getBytes();
        Publisher<ByteBuffer> res;
        final int times = 200;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PipedOutputStream newout = new PipedOutputStream()) {
            res = new ReactiveInputStream(new PipedInputStream(newout)).read(Buffers.Standard.K1);
            for (int cnt = 0; cnt < times; cnt = cnt + 1) {
                newout.write(data, 0, data.length);
                out.write(data, 0, data.length);
            }
        }
        MatcherAssert.assertThat(
            this.single(res).toFuture().get(),
            new IsEqual<>(out.toByteArray())
        );
    }

    /**
     * Subscrier to get publisher length.
     */
    private static final class LengthSubscriber implements Subscriber<ByteBuffer> {

        /**
         * Result future.
         */
        private final CompletableFuture<Integer> future;

        /**
         * Tmp accumulator.
         */
        private volatile int acc;

        /**
         * New Subscriber.
         * @param future Result
         */
        private LengthSubscriber(final CompletableFuture<Integer> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(final Subscription sub) {
            sub.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(final ByteBuffer buf) {
            this.acc += buf.remaining();
        }

        @Override
        public void onError(final Throwable err) {
            this.future.completeExceptionally(err);
        }

        @Override
        public void onComplete() {
            this.future.complete(this.acc);
        }

        /**
         * Length of publisher of byte buffers.
         * @param src Publisher
         * @return Length future
         */
        static CompletableFuture<Integer> of(final Publisher<ByteBuffer> src) {
            final CompletableFuture<Integer> length = new CompletableFuture<>();
            src.subscribe(new LengthSubscriber(length));
            return length;
        }
    }

    private Single<byte[]> single(final Publisher<ByteBuffer> source) {
        return Flowable.fromPublisher(source).reduce(
            ByteBuffer.allocate(0),
            (left, right) -> {
                right.mark();
                final ByteBuffer result;
                if (left.capacity() - left.limit() >= right.limit()) {
                    left.position(left.limit());
                    left.limit(left.limit() + right.limit());
                    result = left.put(right);
                } else {
                    result = ByteBuffer.allocate(
                        2 * Math.max(left.capacity(), right.capacity())
                    ).put(left).put(right);
                }
                right.reset();
                result.flip();
                return result;
            }
        ).map(
            buf -> {
                final byte[] bytes = new byte[buf.remaining()];
                buf.get(bytes);
                return bytes;
            }
        );
    }
}
