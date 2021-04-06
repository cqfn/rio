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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.channel.ReadableChannel;
import org.reactivestreams.Publisher;

/**
 * Reactive read methods for {@link InputStream}.
 * @since 0.2
 */
public final class ReactiveInputStream {

    /**
     * Input stream.
     */
    private final InputStream src;

    /**
     * Extend {@link InputStream} with reactive read methods.
     * @param src Input stream
     */
    public ReactiveInputStream(final InputStream src) {
        this.src = src;
    }

    /**
     * Read input stream as a publisher of byte buffers.
     * @param buf Buffer allocation strategy
     * @return Publisher of bute buffers
     */
    public Publisher<ByteBuffer> read(final Buffers buf) {
        return new ReadableChannel(() -> Channels.newChannel(this.src)).read(buf);
    }
}
