/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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
