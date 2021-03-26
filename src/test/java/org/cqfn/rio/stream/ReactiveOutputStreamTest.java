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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import org.cqfn.rio.WriteGreed;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ReactiveOutputStream}.
 * @since 0.4
 */
class ReactiveOutputStreamTest {

    @Test
    void transferDataToStream() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] data = "123".getBytes();
        new ReactiveOutputStream(out)
            .write(Flowable.fromArray(ByteBuffer.wrap(data)), WriteGreed.SYSTEM)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            out.toByteArray(),
            new IsEqual<>(data)
        );
    }

    @Test
    void transferDataToPipedStream() throws IOException {
        try (
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in)
        ) {
            final byte[] data = "abc".getBytes();
            final byte[] res = new byte[data.length];
            new ReactiveOutputStream(out)
                .write(Flowable.fromArray(ByteBuffer.wrap(data)), WriteGreed.SYSTEM)
                .toCompletableFuture().join();
            in.read(res);
            MatcherAssert.assertThat(
                res,
                new IsEqual<>(data)
            );
        }
    }

}
