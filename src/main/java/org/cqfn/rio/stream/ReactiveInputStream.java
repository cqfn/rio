/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
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
