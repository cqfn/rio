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
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import org.cqfn.rio.IoExecutor;
import org.cqfn.rio.WriteGreed;
import org.reactivestreams.Publisher;

/**
 * Writable channel reactive representation.
 * @since 0.2
 */
public final class WritableChannel {

    /**
     * Channel source.
     */
    private final ChannelSource<? extends WritableByteChannel> src;

    /**
     * ExecutorService.
     */
    private final ExecutorService exec;

    /**
     * Extend writable channel with methods to accept reactive publishers.
     * @param src Writable channel source
     */
    public WritableChannel(final ChannelSource<? extends WritableByteChannel> src) {
        this(src, IoExecutor.shared());
    }

    /**
     * Extend writable channel with methods to accept reactive publishers.
     * @param src Writable channel source
     * @param exec IO executor service
     */
    public WritableChannel(final ChannelSource<? extends WritableByteChannel> src,
        final ExecutorService exec) {
        this.src = src;
        this.exec = exec;
    }

    /**
     * Write data publisher into the channel.
     * @param data Publisher
     * @return Future
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data) {
        return this.write(data, WriteGreed.SYSTEM.adaptive());
    }

    /**
     * Write data from publisher into the channel.
     * @param data Source
     * @param greed Of data consumer
     * @return Completable future for write operation and cancellation support
     */
    public CompletionStage<Void> write(final Publisher<ByteBuffer> data, final WriteGreed greed) {
        final WritableChannelSubscriber sub =
            new WritableChannelSubscriber(this.src, greed, this.exec);
        sub.acceptAsync(data);
        return sub;
    }
}
