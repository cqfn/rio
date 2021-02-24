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

package org.cqfn.rio.channel;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ExecutorService;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.IoExecutor;
import org.reactivestreams.Publisher;

/**
 * Readable channel as publisher representation.
 * @since 0.2
 */
public final class ReadableChannel {

    /**
     * Source channel.
     */
    private final ChannelSource<? extends ReadableByteChannel> chan;

    /**
     * IO exec.
     */
    private final ExecutorService exec;

    /**
     * Extends channel with publisher providers methods.
     * @param chan Source channel
     */
    public ReadableChannel(final ChannelSource<? extends ReadableByteChannel> chan) {
        this(chan, IoExecutor.shared());
    }

    /**
     * Extends channel with publisher providers methods.
     * @param chan Source channel
     * @param exec IO executor service
     */
    public ReadableChannel(final ChannelSource<? extends ReadableByteChannel> chan,
        final ExecutorService exec) {
        this.chan = chan;
        this.exec = exec;
    }

    /**
     * Read channel reactively as a publisher.
     * @param buf Buffer allocation strategy
     * @return Publisher of byte buffers
     */
    public Publisher<ByteBuffer> read(final Buffers buf) {
        return new ReadableChannelPublisher(this.chan, buf, this.exec);
    }
}

