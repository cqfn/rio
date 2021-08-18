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
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.cqfn.rio.Buffers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Test for {@link ReactiveInputStream}.
 * @since 0.4
 */
class ReactiveInputStreamTest {

    @Test
    void transfersData() throws ExecutionException, InterruptedException {
        final byte[] bytes = "abc123".getBytes();
        MatcherAssert.assertThat(
            this.single(
                new ReactiveInputStream(
                    new ByteArrayInputStream(bytes)
                ).read(Buffers.Standard.K1)
            ).toFuture().get(),
            new IsEqual<>(bytes)
        );
    }

    @Test
    @Disabled
    void transferDataWithPipedStreams() throws IOException, ExecutionException,
        InterruptedException {
        try (
            PipedOutputStream newout = new PipedOutputStream();
            PipedInputStream newin = new PipedInputStream(newout)
        ) {
            final byte[] data = "xyz098".getBytes();
            final Future<byte[]> future = this.single(
                new ReactiveInputStream(newin).read(Buffers.Standard.K1)
            ).toFuture();
            newout.write(data, 0, data.length);
            MatcherAssert.assertThat(
                future.get(),
                new IsEqual<>(data)
            );
        }
    }

    Single<byte[]> single(final Publisher<ByteBuffer> source) {
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
