/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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

